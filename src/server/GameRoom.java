package server;

import common.Protocol;
import database.MatchDAO;
import java.awt.Point;
import java.util.List;

/**
 * GameRoom: Quản lý một phòng đấu 1vs1 giữa 2 người chơi.
 */
public class GameRoom {

    private final String roomId;
    private final ClientHandler playerRed;
    private final ClientHandler playerBlue;
    private final GameEngine engine;

    private String currentTurn; // Protocol.COLOR_RED hoặc Protocol.COLOR_BLUE
    private boolean isFinished = false;

    public GameRoom(String roomId, ClientHandler playerRed, ClientHandler playerBlue) {
        this.roomId = roomId;
        this.playerRed = playerRed;
        this.playerBlue = playerBlue;
        this.engine = new GameEngine();
        this.currentTurn = Protocol.COLOR_RED;
    }

    public String getRoomId() {
        return roomId;
    }

    public ClientHandler getPlayerRed() {
        return playerRed;
    }

    public ClientHandler getPlayerBlue() {
        return playerBlue;
    }

    public boolean hasPlayer(String username) {
        if (username == null) return false;
        return (playerRed != null && username.equalsIgnoreCase(playerRed.getPlayerInfo().getUsername())) ||
               (playerBlue != null && username.equalsIgnoreCase(playerBlue.getPlayerInfo().getUsername()));
    }

    public ClientHandler getOpponent(ClientHandler player) {
        return player == playerRed ? playerBlue : playerRed;
    }

    /**
     * Bắt đầu ván đấu: gửi START_GAME tới cả 2 client.
     */
    public void start() {
        if (playerRed != null && playerBlue != null) {
            playerRed.sendMessage(Protocol.CMD_START_GAME + Protocol.DELIMITER 
                    + playerBlue.getPlayerInfo().getUsername() + Protocol.DELIMITER 
                    + Protocol.COLOR_RED + Protocol.DELIMITER 
                    + currentTurn);

            playerBlue.sendMessage(Protocol.CMD_START_GAME + Protocol.DELIMITER 
                    + playerRed.getPlayerInfo().getUsername() + Protocol.DELIMITER 
                    + Protocol.COLOR_BLUE + Protocol.DELIMITER 
                    + currentTurn);
        }
    }

    /**
     * Xử lý nước đi nhận từ người chơi.
     */
    public synchronized void handleMove(ClientHandler sender, String type, int r, int c) {
        if (isFinished) return;

        String senderColor = (sender == playerRed) ? Protocol.COLOR_RED : Protocol.COLOR_BLUE;
        if (!senderColor.equalsIgnoreCase(currentTurn)) {
            // Không phải lượt đi của sender
            return;
        }

        if (!engine.isValidMove(type, r, c)) {
            return;
        }

        // 1. Đặt cạnh
        engine.placeLine(type, r, c);
        broadcast(Protocol.CMD_DRAW_LINE + Protocol.DELIMITER + type + Protocol.DELIMITER + r + Protocol.DELIMITER + c + Protocol.DELIMITER + senderColor);

        // 2. Kiểm tra ô ăn điểm
        List<Point> captured = engine.checkCapturedBoxes(type, r, c, senderColor);
        if (!captured.isEmpty()) {
            for (Point p : captured) {
                broadcast(Protocol.CMD_CAPTURE_BOX + Protocol.DELIMITER 
                        + p.x + Protocol.DELIMITER 
                        + p.y + Protocol.DELIMITER 
                        + senderColor + Protocol.DELIMITER 
                        + engine.getScoreRed() + Protocol.DELIMITER 
                        + engine.getScoreBlue());
            }

            // Kiểm tra kết thúc ván
            if (engine.isGameOver()) {
                finishGame("FULL_BOARD");
                return;
            }

            // Ăn được ô -> GIỮ LƯỢT đi tiếp (luật Dots & Boxes chuẩn)
            broadcast(Protocol.CMD_CHANGE_TURN + Protocol.DELIMITER + currentTurn + Protocol.DELIMITER + "30");
        } else {
            // Không ăn được ô -> ĐỔI LƯỢT
            switchTurn();
        }
    }

    private void switchTurn() {
        currentTurn = Protocol.COLOR_RED.equalsIgnoreCase(currentTurn) ? Protocol.COLOR_BLUE : Protocol.COLOR_RED;
        broadcast(Protocol.CMD_CHANGE_TURN + Protocol.DELIMITER + currentTurn + Protocol.DELIMITER + "30");
    }

    /**
     * Xử lý xin hàng / đầu hàng.
     */
    public synchronized void handleSurrender(ClientHandler sender) {
        if (isFinished) return;
        ClientHandler winner = getOpponent(sender);
        String winnerUser = (winner != null && winner.getPlayerInfo() != null) ? winner.getPlayerInfo().getUsername() : "UNKNOWN";
        broadcast(Protocol.CMD_GAME_OVER + Protocol.DELIMITER + winnerUser + Protocol.DELIMITER + "SURRENDER");
        saveResult();
        isFinished = true;
    }

    private void finishGame(String reason) {
        if (isFinished) return;
        isFinished = true;

        String redUser = playerRed.getPlayerInfo().getUsername();
        String blueUser = playerBlue.getPlayerInfo().getUsername();
        String winner = engine.getWinner(redUser, blueUser);

        broadcast(Protocol.CMD_GAME_OVER + Protocol.DELIMITER + winner + Protocol.DELIMITER + reason);
        saveResult();
    }

    private void saveResult() {
        if (playerRed != null && playerBlue != null) {
            String redUser = playerRed.getPlayerInfo().getUsername();
            String blueUser = playerBlue.getPlayerInfo().getUsername();
            MatchDAO.saveMatchResult(redUser, blueUser, engine.getScoreRed(), engine.getScoreBlue());
        }
    }

    public void broadcast(String message) {
        if (playerRed != null) playerRed.sendMessage(message);
        if (playerBlue != null) playerBlue.sendMessage(message);
    }
}
