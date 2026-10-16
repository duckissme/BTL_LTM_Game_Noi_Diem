package server;

import common.Protocol;
import database.MatchDAO;
import database.UserDAO;
import entity.Player;
import common.PlayerStatus;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler extends Thread {
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Player playerInfo = null;
    private volatile String pendingChallengeTarget = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public Player getPlayerInfo() {
        return playerInfo;
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[Gói tin nhận từ " + (playerInfo != null ? playerInfo.getUsername() : socket.getRemoteSocketAddress()) + "]: " + message);
                String[] parts = message.split(Protocol.DELIMITER);
                String command = parts[0];

                switch (command) {
                    case Protocol.CMD_LOGIN:
                        handleLogin(parts);
                        break;
                    case Protocol.CMD_REGISTER:
                        handleRegister(parts);
                        break;
                    case "GET_LOBBY":
                        handleGetLobby();
                        break;
                    case Protocol.CMD_CHALLENGE_REQ:
                        handleChallengeReq(parts);
                        break;
                    case Protocol.CMD_CHALLENGE_RESP:
                        handleChallengeResp(parts);
                        break;
                    case Protocol.CMD_CHALLENGE_CANCEL:
                        handleChallengeCancel(parts);
                        break;
                    case Protocol.CMD_REMATCH_REQ:
                        handleRematchReq();
                        break;
                    case Protocol.CMD_REMATCH_RESP:
                        handleRematchResp(parts);
                        break;
                    case Protocol.CMD_MOVE:
                        if (parts.length >= 4 && playerInfo != null) {
                            GameRoom room = RoomManager.findRoomByPlayer(playerInfo.getUsername());
                            if (room != null) {
                                room.handleMove(this, parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                            }
                        }
                        break;
                    case Protocol.CMD_CHAT:
                        handleChat(message);
                        break;
                    case Protocol.CMD_GET_HISTORY:
                        handleGetHistory(parts);
                        break;
                    case Protocol.CMD_SURRENDER:
                    case Protocol.CMD_LEAVE_ROOM:
                        handleLeaveRoom();
                        break;
                    case "LOGOUT":
                        handleLogout();
                        break;
                    default:
                        System.out.println("[Gói tin khác]: " + command);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client [" + (playerInfo != null ? playerInfo.getUsername() : "Unknown") + "] đã ngắt kết nối.");
        } catch (Exception e) {
            System.err.println("Lỗi xử lý Client [" + (playerInfo != null ? playerInfo.getUsername() : "Unknown") + "]: " + e.getMessage());
            e.printStackTrace();
        } finally {
            handleLogout();
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Lỗi khi đóng socket: " + e.getMessage());
            }
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            sendMessage(Protocol.CMD_LOGIN_FAIL + Protocol.DELIMITER + "Thiếu thông tin đăng nhập!");
            return;
        }

        String user = parts[1].trim();
        String pass = parts[2].trim();

        if (Server.onlineUsers.containsKey(user)) {
            sendMessage(Protocol.CMD_LOGIN_FAIL + Protocol.DELIMITER + "Tài khoản hiện đang đăng nhập ở nơi khác!");
            return;
        }

        Player p = UserDAO.checkLogin(user, pass);
        if (p != null) {
            this.playerInfo = p;
            Server.onlineUsers.put(user, this);

            sendMessage(Protocol.CMD_LOGIN_SUCCESS + Protocol.DELIMITER 
                    + user + Protocol.DELIMITER 
                    + p.getScore() + Protocol.DELIMITER 
                    + p.getTotalMatches());
            System.out.println("=> User [" + user + "] đăng nhập thành công.");

            // Gửi dữ liệu xếp hạng và phát thông báo danh sách online mới
            sendMessage(Protocol.CMD_LEADERBOARD + Protocol.DELIMITER + UserDAO.getLeaderboard());
            Server.broadcastLobbyData();
        } else {
            sendMessage(Protocol.CMD_LOGIN_FAIL + Protocol.DELIMITER + "Tài khoản hoặc mật khẩu không chính xác!");
        }
    }

    private void handleRegister(String[] parts) {
        if (parts.length < 3) {
            sendMessage(Protocol.CMD_REGISTER_FAIL + Protocol.DELIMITER + "Thiếu thông tin đăng ký!");
            return;
        }

        String user = parts[1].trim();
        String pass = parts[2].trim();

        int result = UserDAO.register(user, pass);
        if (result == 0) {
            sendMessage(Protocol.CMD_REGISTER_SUCCESS);
            System.out.println("=> Đăng ký thành công tài khoản: " + user);
        } else if (result == 1) {
            sendMessage(Protocol.CMD_REGISTER_FAIL + Protocol.DELIMITER + "Tài khoản đã tồn tại trên hệ thống!");
        } else {
            sendMessage(Protocol.CMD_REGISTER_FAIL + Protocol.DELIMITER + "Lỗi hệ thống cơ sở dữ liệu!");
        }
    }

    private void handleGetLobby() {
        if (playerInfo != null) {
            GameRoom room = RoomManager.findRoomByPlayer(playerInfo.getUsername());
            if (room == null) {
                playerInfo.setStatus(PlayerStatus.FREE);
            }
            UserDAO.refreshPlayerInfo(playerInfo);
            // Gửi dữ liệu cập nhật điểm số và số trận của chính mình
            sendMessage(Protocol.CMD_LOGIN_SUCCESS + Protocol.DELIMITER 
                    + playerInfo.getUsername() + Protocol.DELIMITER 
                    + playerInfo.getScore() + Protocol.DELIMITER 
                    + playerInfo.getTotalMatches());
        }
        String myUsername = (playerInfo != null) ? playerInfo.getUsername() : null;
        sendMessage(Server.buildLobbyData(myUsername));
        sendMessage(Protocol.CMD_LEADERBOARD + Protocol.DELIMITER + UserDAO.getLeaderboard());
    }

    private void handleChallengeReq(String[] parts) {
        if (parts.length < 2 || playerInfo == null) return;
        String target = parts[1].trim();

        ClientHandler targetHandler = Server.onlineUsers.get(target);
        if (targetHandler == null || targetHandler.playerInfo == null) {
            sendMessage(Protocol.CMD_CHALLENGE_RESP + Protocol.DELIMITER + target + Protocol.DELIMITER + Protocol.RESP_REJECT);
            return;
        }

        if (targetHandler.playerInfo.getStatus() != PlayerStatus.FREE) {
            sendMessage(Protocol.CMD_CHALLENGE_RESP + Protocol.DELIMITER + target + Protocol.DELIMITER + Protocol.RESP_REJECT);
            return;
        }

        this.pendingChallengeTarget = target;
        targetHandler.sendMessage(Protocol.CMD_CHALLENGE_INVITE + Protocol.DELIMITER + this.playerInfo.getUsername());
    }

    private void handleChallengeResp(String[] parts) {
        if (parts.length < 3 || playerInfo == null) return;
        String challenger = parts[1].trim();
        String resp = parts[2].trim();

        ClientHandler challengerHandler = Server.onlineUsers.get(challenger);
        if (challengerHandler == null || challengerHandler.playerInfo == null) return;

        // Nếu người gửi lời mời đã hủy hoặc lời mời không còn hướng tới người này
        if (challengerHandler.pendingChallengeTarget == null 
                || !challengerHandler.pendingChallengeTarget.equalsIgnoreCase(this.playerInfo.getUsername())) {
            this.sendMessage(Protocol.CMD_CHALLENGE_CANCEL);
            return;
        }

        challengerHandler.pendingChallengeTarget = null;

        if (Protocol.RESP_REJECT.equalsIgnoreCase(resp)) {
            challengerHandler.sendMessage(Protocol.CMD_CHALLENGE_RESP + Protocol.DELIMITER 
                    + this.playerInfo.getUsername() + Protocol.DELIMITER 
                    + Protocol.RESP_REJECT);
        } else if (Protocol.RESP_ACCEPT.equalsIgnoreCase(resp)) {
            this.playerInfo.setStatus(PlayerStatus.BUSY);
            challengerHandler.playerInfo.setStatus(PlayerStatus.BUSY);

            // Cập nhật trạng thái sảnh chờ cho toàn bộ người chơi khác
            Server.broadcastLobbyData();

            // Khởi tạo phòng thi đấu và thông báo bắt đầu trận
            GameRoom room = RoomManager.createRoom(challengerHandler, this);
            room.start();
        }
    }

    private void handleChallengeCancel(String[] parts) {
        this.pendingChallengeTarget = null;
        if (parts.length < 2) return;
        String target = parts[1].trim();
        ClientHandler targetHandler = Server.onlineUsers.get(target);
        if (targetHandler != null) {
            targetHandler.sendMessage(Protocol.CMD_CHALLENGE_CANCEL);
        }
    }

    private void handleRematchReq() {
        if (this.playerInfo == null) return;
        GameRoom room = RoomManager.findRoomByPlayer(playerInfo.getUsername());
        if (room == null) {
            sendMessage(Protocol.CMD_REMATCH_RESP + Protocol.DELIMITER + Protocol.RESP_REJECT);
            return;
        }

        ClientHandler opponent = room.getOpponent(this);
        if (opponent == null || opponent.playerInfo == null) {
            sendMessage(Protocol.CMD_REMATCH_RESP + Protocol.DELIMITER + Protocol.RESP_REJECT);
            return;
        }

        opponent.sendMessage(Protocol.CMD_REMATCH_INVITE);
    }

    private void handleRematchResp(String[] parts) {
        if (parts.length < 2 || playerInfo == null) return;
        String resp = parts[1].trim();

        GameRoom room = RoomManager.findRoomByPlayer(playerInfo.getUsername());
        if (room == null) return;

        ClientHandler opponent = room.getOpponent(this);
        if (opponent != null) {
            opponent.sendMessage(Protocol.CMD_REMATCH_RESP + Protocol.DELIMITER + resp);
        }

        if (Protocol.RESP_ACCEPT.equalsIgnoreCase(resp)) {
            // Tái đấu: reset phòng thi đấu
            room.reset();
        } else {
            // Từ chối: giải phóng phòng và chuyển người gửi từ chối sang rỗi
            RoomManager.removeRoom(room.getRoomId());
            this.playerInfo.setStatus(PlayerStatus.FREE);
            Server.broadcastLobbyData();
        }
    }

    private void handleChat(String message) {
        if (playerInfo == null) return;

        // Lấy nguyên phần sau "CHAT;" thay vì parts[1] vì nội dung có thể chứa dấu ';'
        int idx = message.indexOf(Protocol.DELIMITER);
        if (idx < 0) return;
        String content = message.substring(idx + 1).trim();
        if (content.isEmpty()) return;
        if (content.length() > Protocol.CHAT_MAX_LENGTH) {
            int cut = Protocol.CHAT_MAX_LENGTH;
            // Emoji chiếm 2 char (cặp surrogate): không cắt đôi một emoji
            if (Character.isHighSurrogate(content.charAt(cut - 1))) {
                cut--;
            }
            content = content.substring(0, cut);
        }

        // Chỉ chat được khi đang ở trong phòng đấu
        GameRoom room = RoomManager.findRoomByPlayer(playerInfo.getUsername());
        if (room != null) {
            room.handleChat(this, content);
        }
    }

    private void handleGetHistory(String[] parts) {
        if (playerInfo == null) return; // Phải đăng nhập mới xem được lịch sử

        // Không gửi kèm tên thì xem lịch sử của chính mình
        String target = (parts.length >= 2 && !parts[1].trim().isEmpty())
                ? parts[1].trim()
                : playerInfo.getUsername();

        if (UserDAO.getUserByUsername(target) == null) {
            sendMessage(Protocol.CMD_HISTORY_FAIL + Protocol.DELIMITER + "Không tìm thấy người chơi [" + target + "]!");
            return;
        }

        String data = MatchDAO.getHistory(target);
        if (data == null) {
            sendMessage(Protocol.CMD_HISTORY_FAIL + Protocol.DELIMITER + "Không tải được lịch sử trận đấu!");
        } else {
            sendMessage(Protocol.CMD_HISTORY + Protocol.DELIMITER + target + Protocol.DELIMITER + data);
        }
    }

    private void handleLeaveRoom() {
        if (this.playerInfo != null) {
            GameRoom room = RoomManager.findRoomByPlayer(playerInfo.getUsername());
            if (room != null) {
                ClientHandler opponent = room.getOpponent(this);
                if (opponent != null) {
                    // Không chuyển opponent sang FREE tại đây vì opponent vẫn đang ở bàn cờ/popup!
                    // Chỉ gửi thông báo để opponent biết đối thủ đã rời phòng.
                    opponent.sendMessage(Protocol.CMD_REMATCH_RESP + Protocol.DELIMITER + Protocol.RESP_REJECT);
                }
                room.handleSurrender(this);
                RoomManager.removeRoom(room.getRoomId());
            }
            this.playerInfo.setStatus(PlayerStatus.FREE);
            UserDAO.refreshPlayerInfo(this.playerInfo);
            Server.broadcastLobbyData();
        }
    }

    private void handleLogout() {
        if (this.playerInfo != null) {
            GameRoom room = RoomManager.findRoomByPlayer(playerInfo.getUsername());
            if (room != null) {
                ClientHandler opponent = room.getOpponent(this);
                if (opponent != null) {
                    opponent.sendMessage(Protocol.CMD_REMATCH_RESP + Protocol.DELIMITER + Protocol.RESP_REJECT);
                }
                room.handleSurrender(this);
                RoomManager.removeRoom(room.getRoomId());
            }
            this.pendingChallengeTarget = null;
            Server.onlineUsers.remove(this.playerInfo.getUsername());
            System.out.println("=> User [" + this.playerInfo.getUsername() + "] đã đăng xuất.");
            this.playerInfo = null;
            Server.broadcastLobbyData();
        }
    }
}