package server;

import common.Protocol;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Game Engine: Quản lý trạng thái bàn cờ và quy tắc trò chơi Nối Điểm (Dots & Boxes).
 * Kích thước bàn cờ chuẩn: 5x5 ô vuông (tương ứng 6x6 điểm chấm).
 */
public class GameEngine {

    public static final int BOX_ROWS = 5;
    public static final int BOX_COLS = 5;
    public static final int DOT_ROWS = BOX_ROWS + 1; // 6
    public static final int DOT_COLS = BOX_COLS + 1; // 6
    public static final int TOTAL_BOXES = BOX_ROWS * BOX_COLS; // 25

    // Cạnh ngang: 6 hàng, mỗi hàng 5 cạnh
    private final boolean[][] hLines = new boolean[DOT_ROWS][BOX_COLS];
    // Cạnh dọc: 5 hàng, mỗi hàng 6 cạnh
    private final boolean[][] vLines = new boolean[BOX_ROWS][DOT_COLS];
    // Quyền sở hữu ô: "RED" hoặc "BLUE"
    private final String[][] boxes = new String[BOX_ROWS][BOX_COLS];

    private int scoreRed = 0;
    private int scoreBlue = 0;

    public GameEngine() {
        reset();
    }

    /**
     * Khởi tạo lại trạng thái bàn cờ ban đầu.
     */
    public void reset() {
        for (int r = 0; r < DOT_ROWS; r++) {
            for (int c = 0; c < BOX_COLS; c++) {
                hLines[r][c] = false;
            }
        }
        for (int r = 0; r < BOX_ROWS; r++) {
            for (int c = 0; c < DOT_COLS; c++) {
                vLines[r][c] = false;
            }
        }
        for (int r = 0; r < BOX_ROWS; r++) {
            for (int c = 0; c < BOX_COLS; c++) {
                boxes[r][c] = null;
            }
        }
        scoreRed = 0;
        scoreBlue = 0;
    }

    /**
     * Kiểm tra nước đi có hợp lệ không (tọa độ trong giới hạn và cạnh chưa được vẽ).
     */
    public boolean isValidMove(String type, int r, int c) {
        if (Protocol.LINE_H.equalsIgnoreCase(type)) {
            if (r < 0 || r >= DOT_ROWS || c < 0 || c >= BOX_COLS) return false;
            return !hLines[r][c];
        } else if (Protocol.LINE_V.equalsIgnoreCase(type)) {
            if (r < 0 || r >= BOX_ROWS || c < 0 || c >= DOT_COLS) return false;
            return !vLines[r][c];
        }
        return false;
    }

    /**
     * Đánh dấu cạnh đã được vẽ.
     */
    public boolean placeLine(String type, int r, int c) {
        if (!isValidMove(type, r, c)) return false;

        if (Protocol.LINE_H.equalsIgnoreCase(type)) {
            hLines[r][c] = true;
        } else {
            vLines[r][c] = true;
        }
        return true;
    }

    /**
     * Kiểm tra các ô xung quanh cạnh vừa đánh xem có ô nào được khép kín không.
     * Trả về danh sách tọa độ (r, c) của các ô vừa chiếm được.
     */
    public List<Point> checkCapturedBoxes(String type, int r, int c, String playerColor) {
        List<Point> captured = new ArrayList<>();

        if (Protocol.LINE_H.equalsIgnoreCase(type)) {
            // Kiểm tra ô phía trên (r - 1, c)
            if (r > 0 && isBoxCompleted(r - 1, c) && boxes[r - 1][c] == null) {
                boxes[r - 1][c] = playerColor;
                captured.add(new Point(r - 1, c));
                addScore(playerColor);
            }
            // Kiểm tra ô phía dưới (r, c)
            if (r < BOX_ROWS && isBoxCompleted(r, c) && boxes[r][c] == null) {
                boxes[r][c] = playerColor;
                captured.add(new Point(r, c));
                addScore(playerColor);
            }
        } else if (Protocol.LINE_V.equalsIgnoreCase(type)) {
            // Kiểm tra ô bên trái (r, c - 1)
            if (c > 0 && isBoxCompleted(r, c - 1) && boxes[r][c - 1] == null) {
                boxes[r][c - 1] = playerColor;
                captured.add(new Point(r, c - 1));
                addScore(playerColor);
            }
            // Kiểm tra ô bên phải (r, c)
            if (c < BOX_COLS && isBoxCompleted(r, c) && boxes[r][c] == null) {
                boxes[r][c] = playerColor;
                captured.add(new Point(r, c));
                addScore(playerColor);
            }
        }
        return captured;
    }

    /**
     * Kiểm tra 4 cạnh của ô (r, c) đã được nối kín hết chưa.
     */
    public boolean isBoxCompleted(int r, int c) {
        if (r < 0 || r >= BOX_ROWS || c < 0 || c >= BOX_COLS) return false;
        return hLines[r][c] && hLines[r + 1][c] && vLines[r][c] && vLines[r][c + 1];
    }

    /** Cộng điểm cho người chơi chiếm được ô */
    private void addScore(String playerColor) {
        if (Protocol.COLOR_RED.equalsIgnoreCase(playerColor)) {
            scoreRed++;
        } else {
            scoreBlue++;
        }
    }

    /** Kiểm tra toàn bộ 25 ô trên bàn cờ đã được chiếm hết chưa */
    public boolean isGameOver() {
        return (scoreRed + scoreBlue) >= TOTAL_BOXES;
    }

    public int getScoreRed() {
        return scoreRed;
    }

    public int getScoreBlue() {
        return scoreBlue;
    }

    /** Xác định người chiến thắng dựa trên số ô chiếm được, hoặc DRAW nếu bằng điểm */
    public String getWinner(String redUsername, String blueUsername) {
        if (scoreRed > scoreBlue) return redUsername;
        if (scoreBlue > scoreRed) return blueUsername;
        return Protocol.WINNER_DRAW;
    }
}
