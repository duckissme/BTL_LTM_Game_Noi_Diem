package server;

import common.Protocol;
import database.MatchDAO;
import database.UserDAO;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * GameRoom: Quản lý một phòng đấu 1vs1 giữa 2 người chơi.
 * Server tự đếm giờ mỗi lượt: hết Protocol.TURN_TIME_SECONDS giây mà chưa đi thì mất lượt.
 * Mỗi trận tối đa Protocol.MATCH_TIME_SECONDS giây: hết giờ thì ai chiếm nhiều ô hơn thắng, bằng ô thì hòa.
 * Phòng giữ nguyên khi 2 bên chơi tiếp, nên đếm được số thứ tự trận và tỉ số thắng giữa 2 người.
 */
public class GameRoom {

    /** Bộ hẹn giờ dùng chung cho mọi phòng (luồng daemon, tự tắt cùng Server) */
    private static final ScheduledExecutorService TURN_TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Turn-Timer");
        t.setDaemon(true);
        return t;
    });

    private final String roomId;
    private final ClientHandler playerRed;
    private final ClientHandler playerBlue;
    private final GameEngine engine;

    private String currentTurn; // Protocol.COLOR_RED hoặc Protocol.COLOR_BLUE
    private boolean isFinished = false;

    private ScheduledFuture<?> turnTimeoutTask;
    // Tăng mỗi lần bắt đầu đếm giờ lượt mới. Hẹn giờ cũ chạy trễ (người chơi vừa đi đúng lúc hết giờ)
    // sẽ thấy số này đã đổi và tự bỏ qua.
    private int turnSeq = 0;

    // Số lần hết giờ LIÊN TIẾP của mỗi phe (đi được 1 nước thì về 0)
    private int timeoutsRed = 0;
    private int timeoutsBlue = 0;

    // Hẹn giờ kết thúc trận (MATCH_TIME_SECONDS), chống hẹn giờ cũ chạy trễ giống turnSeq
    private ScheduledFuture<?> matchTimeoutTask;
    private int matchSeq = 0;

    // Trận thứ mấy giữa 2 người (trận đầu = 1, mỗi lần chơi tiếp +1) và số trận thắng mỗi bên (hòa không cộng)
    private int matchNumber = 1;
    private int winsRed = 0;
    private int winsBlue = 0;

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
     * Bắt đầu ván đấu: gửi START_GAME tới cả 2 client và bắt đầu đếm giờ lượt đầu tiên.
     */
    public synchronized void start() {
        if (playerRed != null && playerBlue != null) {
            playerRed.sendMessage(Protocol.CMD_START_GAME + Protocol.DELIMITER
                    + playerBlue.getPlayerInfo().getUsername() + Protocol.DELIMITER
                    + Protocol.COLOR_RED + Protocol.DELIMITER
                    + currentTurn);

            playerBlue.sendMessage(Protocol.CMD_START_GAME + Protocol.DELIMITER
                    + playerRed.getPlayerInfo().getUsername() + Protocol.DELIMITER
                    + Protocol.COLOR_BLUE + Protocol.DELIMITER
                    + currentTurn);
            startMatch();
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

        // Đi được nước hợp lệ -> xóa chuỗi hết giờ liên tiếp của người này
        if (sender == playerRed) timeoutsRed = 0; else timeoutsBlue = 0;

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
                finishGame(Protocol.REASON_FULL_BOARD);
                return;
            }

            // Ăn được ô -> GIỮ LƯỢT đi tiếp (luật Dots & Boxes chuẩn), đếm lại giờ từ đầu
            announceTurn();
        } else {
            // Không ăn được ô -> ĐỔI LƯỢT
            switchTurn();
        }
    }

    private void switchTurn() {
        currentTurn = Protocol.COLOR_RED.equalsIgnoreCase(currentTurn) ? Protocol.COLOR_BLUE : Protocol.COLOR_RED;
        announceTurn();
    }

    /**
     * Báo lượt hiện tại cho 2 người (CHANGE_TURN) và bắt đầu đếm giờ lượt đó.
     */
    private void announceTurn() {
        broadcast(Protocol.CMD_CHANGE_TURN + Protocol.DELIMITER + currentTurn + Protocol.DELIMITER + Protocol.TURN_TIME_SECONDS);
        restartTurnTimer();
    }

    // =========================================================================
    // ĐẾM GIỜ CẢ TRẬN
    // =========================================================================

    /**
     * Báo số thứ tự trận + tỉ số cho 2 người, bắt đầu đếm giờ trận và lượt đầu tiên.
     */
    private void startMatch() {
        broadcast(Protocol.CMD_MATCH_INFO + Protocol.DELIMITER
                + matchNumber + Protocol.DELIMITER
                + winsRed + Protocol.DELIMITER
                + winsBlue + Protocol.DELIMITER
                + Protocol.MATCH_TIME_SECONDS);
        cancelMatchTimer();
        final int seq = ++matchSeq;
        matchTimeoutTask = TURN_TIMER.schedule(() -> handleMatchTimeout(seq), Protocol.MATCH_TIME_SECONDS, TimeUnit.SECONDS);
        announceTurn();
    }

    private void cancelMatchTimer() {
        if (matchTimeoutTask != null) {
            matchTimeoutTask.cancel(false);
            matchTimeoutTask = null;
        }
    }

    /**
     * Chạy trên luồng Turn-Timer khi hết giờ trận: so số ô, bằng nhau thì hòa.
     */
    private synchronized void handleMatchTimeout(int seq) {
        if (isFinished || seq != matchSeq) return;
        finishGame(Protocol.REASON_TIME_UP);
    }

    // =========================================================================
    // ĐẾM GIỜ MỖI LƯỢT
    // =========================================================================

    private void restartTurnTimer() {
        cancelTurnTimer();
        final int seq = ++turnSeq;
        turnTimeoutTask = TURN_TIMER.schedule(() -> handleTurnTimeout(seq), Protocol.TURN_TIME_SECONDS, TimeUnit.SECONDS);
    }

    private void cancelTurnTimer() {
        if (turnTimeoutTask != null) {
            turnTimeoutTask.cancel(false);
            turnTimeoutTask = null;
        }
    }

    /**
     * Chạy trên luồng Turn-Timer khi hết giờ: người đang giữ lượt bị mất lượt,
     * hết giờ Protocol.MAX_CONSECUTIVE_TIMEOUTS lần liên tiếp thì bị xử thua.
     */
    private synchronized void handleTurnTimeout(int seq) {
        // Trận đã kết thúc, hoặc người chơi đã kịp đi (đã sang lượt đếm giờ mới) -> bỏ qua
        if (isFinished || seq != turnSeq) return;

        boolean redTimedOut = Protocol.COLOR_RED.equalsIgnoreCase(currentTurn);
        int count = redTimedOut ? ++timeoutsRed : ++timeoutsBlue;
        broadcast(Protocol.CMD_TURN_TIMEOUT + Protocol.DELIMITER + currentTurn + Protocol.DELIMITER + count);

        if (count >= Protocol.MAX_CONSECUTIVE_TIMEOUTS) {
            forfeit(redTimedOut ? playerRed : playerBlue, Protocol.REASON_TIMEOUT);
        } else {
            switchTurn();
        }
    }

    /**
     * Xử lý xin hàng / đầu hàng.
     */
    public synchronized void handleSurrender(ClientHandler sender) {
        forfeit(sender, Protocol.REASON_SURRENDER);
    }

    /**
     * Xử thua người chơi loser (bỏ cuộc hoặc hết giờ quá số lần): đối thủ thắng bất kể tỉ số.
     */
    private void forfeit(ClientHandler loser, String reason) {
        if (isFinished) return;
        isFinished = true;
        cancelTurnTimer();
        cancelMatchTimer();
        ClientHandler winner = getOpponent(loser);
        String winnerUser = (winner != null && winner.getPlayerInfo() != null) ? winner.getPlayerInfo().getUsername() : "UNKNOWN";
        if (winner != null) {
            if (winner == playerRed) winsRed++; else winsBlue++;
        }
        broadcastGameOver(winnerUser, reason);
        saveResult(winnerUser, reason);
    }

    /**
     * Chuyển tiếp tin nhắn chat tới cả 2 người chơi (người gửi cũng nhận lại để hiển thị).
     */
    public void handleChat(ClientHandler sender, String content) {
        if (sender == null || sender.getPlayerInfo() == null) return;
        broadcast(Protocol.CMD_CHAT_MSG + Protocol.DELIMITER
                + sender.getPlayerInfo().getUsername() + Protocol.DELIMITER
                + content);
    }

    private void finishGame(String reason) {
        if (isFinished) return;
        isFinished = true;
        cancelTurnTimer();
        cancelMatchTimer();

        String redUser = playerRed.getPlayerInfo().getUsername();
        String blueUser = playerBlue.getPlayerInfo().getUsername();
        String winner = engine.getWinner(redUser, blueUser);
        if (winner.equals(redUser)) winsRed++;
        else if (winner.equals(blueUser)) winsBlue++;

        broadcastGameOver(winner, reason);
        saveResult(winner, reason);
    }

    private void broadcastGameOver(String winner, String reason) {
        broadcast(Protocol.CMD_GAME_OVER + Protocol.DELIMITER
                + winner + Protocol.DELIMITER
                + reason + Protocol.DELIMITER
                + winsRed + Protocol.DELIMITER
                + winsBlue);
    }

    private void saveResult(String winner, String reason) {
        if (playerRed != null && playerBlue != null) {
            String redUser = playerRed.getPlayerInfo().getUsername();
            String blueUser = playerBlue.getPlayerInfo().getUsername();
            boolean ok = MatchDAO.saveMatchResult(redUser, blueUser, engine.getScoreRed(), engine.getScoreBlue(), winner, reason);
            if (ok) {
                UserDAO.refreshPlayerInfo(playerRed.getPlayerInfo());
                UserDAO.refreshPlayerInfo(playerBlue.getPlayerInfo());
            }
        }
    }

    /**
     * Khởi tạo lại phòng thi đấu khi 2 bên đồng ý tái đấu (Rematch).
     */
    public synchronized void reset() {
        engine.reset();
        isFinished = false;
        currentTurn = Protocol.COLOR_RED;
        timeoutsRed = 0;
        timeoutsBlue = 0;
        matchNumber++;
        startMatch();
    }

    public void broadcast(String message) {
        if (playerRed != null) playerRed.sendMessage(message);
        if (playerBlue != null) playerBlue.sendMessage(message);
    }
}
