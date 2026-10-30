/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test;

import common.Protocol;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Giả lập Server độc lập không cần CSDL MySQL để kiểm thử nhanh giao diện và luồng sự kiện của Client.
 */
public class MockServerTest {
    public static void main(String[] args) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8888)) {
                System.out.println("=== [MOCK SERVER] ĐANG CHẠY TẠI CỔNG 8888 ===");

                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("[MOCK SERVER] Client đã kết nối!");

                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    // Ghi rõ UTF-8 (giống chiều đọc) để tiếng Việt không bị lỗi trên mọi phiên bản Java
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);

                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("[Client -> Server]: " + line);
                        String[] parts = line.split(Protocol.DELIMITER);

                        // 1. Phản hồi Đăng nhập thành công
                        if (Protocol.CMD_LOGIN.equals(parts[0])) {
                            String username = parts[1];
                            // Trả về: LOGIN_SUCCESS;<username>;<points>;<wins>
                            out.println(Protocol.CMD_LOGIN_SUCCESS + Protocol.DELIMITER + username + Protocol.DELIMITER + "1200" + Protocol.DELIMITER + "45");

                            // Sau 0.3s, gửi dữ liệu 2 bảng Sảnh sang cho LobbyFrame nạp vào JTable
                            new Thread(() -> {
                                try {
                                    Thread.sleep(300);
                                    // Bắn bảng Online
                                    out.println(Protocol.CMD_LOBBY_DATA + Protocol.DELIMITER 
                                            + "Username A,1200,45,Đang rỗi|"
                                            + "Username B,950,32,Đang bận|"
                                            + "Username C,1100,40,Đang rỗi|"
                                            + "Username D,800,25,Đang rỗi");

                                    // Bắn bảng Xếp hạng
                                    out.println(Protocol.CMD_LEADERBOARD + Protocol.DELIMITER 
                                            + "TopPlayer1,2500,110|"
                                            + "UserX,2300,98|"
                                            + "Competitory,2100,85|"
                                            + "Username A,1200,45");
                                } catch (InterruptedException ignored) {}
                            }).start();
                        }

                        // 2. Phản hồi khi Client bấm Thách Đấu
                        if (Protocol.CMD_CHALLENGE_REQ.equals(parts[0])) {
                            String target = parts[1];
                            System.out.println("[MOCK SERVER] Nhận yêu cầu thách đấu tới: " + target);
                            // Sau 0.8s, giả lập đối thủ đồng ý và mở ván đấu
                            new Thread(() -> {
                                try {
                                    Thread.sleep(800);
                                    // S -> C: START_GAME;<opponent>;<my_color>;<turn_color>
                                    out.println(Protocol.CMD_START_GAME + Protocol.DELIMITER 
                                            + target + Protocol.DELIMITER 
                                            + Protocol.COLOR_RED + Protocol.DELIMITER 
                                            + Protocol.COLOR_RED);
                                } catch (InterruptedException ignored) {}
                            }).start();
                        }

                        // 3. Phản hồi khi Client bấm Chấp nhận/Từ chối trên popup
                        if (Protocol.CMD_CHALLENGE_RESP.equals(parts[0])) {
                            System.out.println("[MOCK SERVER] Client phản hồi thách đấu: " + line);
                        }
                        
                        if (Protocol.CMD_CHALLENGE_CANCEL.equals(parts[0])) {
                            String targetUser = parts[1];
                            System.out.println("[MOCK SERVER] Client đã hủy lời mời thách đấu gửi tới: " + targetUser);
                        }

                        // 4. Xử lý nước đi trong trận (TV4 Arena)
                        if (Protocol.CMD_MOVE.equals(parts[0])) {
                            String type = parts[1];
                            String r = parts[2];
                            String c = parts[3];
                            System.out.println("[MOCK SERVER] Nước đi của người chơi: MOVE " + type + " (" + r + "," + c + ")");

                            // Duyệt nước đi hợp lệ -> Ra lệnh vẽ cạnh của phe Đỏ
                            out.println(Protocol.CMD_DRAW_LINE + Protocol.DELIMITER + type + Protocol.DELIMITER + r + Protocol.DELIMITER + c + Protocol.DELIMITER + Protocol.COLOR_RED);

                            // Giả lập đổi lượt và đối thủ Xanh đánh trả sau 0.8s
                            new Thread(() -> {
                                try {
                                    Thread.sleep(600);
                                    out.println(Protocol.CMD_CHANGE_TURN + Protocol.DELIMITER + Protocol.COLOR_BLUE + Protocol.DELIMITER + "30");

                                    Thread.sleep(1000);
                                    // Giả lập bot Xanh vẽ 1 cạnh
                                    out.println(Protocol.CMD_DRAW_LINE + Protocol.DELIMITER + "H" + Protocol.DELIMITER + "1" + Protocol.DELIMITER + "1" + Protocol.DELIMITER + Protocol.COLOR_BLUE);
                                    Thread.sleep(400);
                                    // Đổi lại lượt cho người chơi Đỏ
                                    out.println(Protocol.CMD_CHANGE_TURN + Protocol.DELIMITER + Protocol.COLOR_RED + Protocol.DELIMITER + "30");
                                } catch (InterruptedException ignored) {}
                            }).start();
                        }

                        // 5. Xử lý xin hàng / thoát trận
                        if (Protocol.CMD_SURRENDER.equals(parts[0])) {
                            System.out.println("[MOCK SERVER] Người chơi xin hàng!");
                            out.println(Protocol.CMD_GAME_OVER + Protocol.DELIMITER + "Username C" + Protocol.DELIMITER + "SURRENDER");
                        }

                        // 6. Xử lý tái đấu
                        if (Protocol.CMD_REMATCH_REQ.equals(parts[0])) {
                            System.out.println("[MOCK SERVER] Người chơi yêu cầu tái đấu");
                            new Thread(() -> {
                                try {
                                    Thread.sleep(500);
                                    out.println(Protocol.CMD_REMATCH_RESP + Protocol.DELIMITER + Protocol.RESP_ACCEPT);
                                } catch (InterruptedException ignored) {}
                            }).start();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
