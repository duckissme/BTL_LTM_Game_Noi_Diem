package database;

import common.Protocol;
import server.GameEngine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Data Access Object quản lý lưu trữ lịch sử trận đấu và cập nhật điểm sau trận.
 */
public class MatchDAO {

    /**
     * Lưu kết quả trận đấu vào bảng 'matches' và cập nhật điểm/trận cho 2 người chơi.
     * 
     * @param redUser   Tên tài khoản người chơi Đỏ
     * @param blueUser  Tên tài khoản người chơi Xanh
     * @param redScore  Điểm số người chơi Đỏ chiếm được
     * @param blueScore Điểm số người chơi Xanh chiếm được
     * @return true nếu lưu thành công, false nếu có lỗi
     */
    public static boolean saveMatchResult(String redUser, String blueUser, int redScore, int blueScore) {
        return saveMatchResult(redUser, blueUser, redScore, blueScore, null);
    }

    public static boolean saveMatchResult(String redUser, String blueUser, int redScore, int blueScore, String winner) {
        return saveMatchResult(redUser, blueUser, redScore, blueScore, winner, null);
    }

    /**
     * @param winner    Tên người thắng (tên không khớp 2 người chơi thì winner_id để NULL)
     * @param endReason Lý do kết thúc: Protocol.REASON_FULL_BOARD / REASON_SURRENDER (có thể null)
     */
    public static boolean saveMatchResult(String redUser, String blueUser, int redScore, int blueScore,
                                          String winner, String endReason) {
        String getIdQuery = "SELECT id FROM users WHERE username = ?";
        String insertMatchQuery = "INSERT INTO matches (player_red_id, player_blue_id, red_score, blue_score, winner_id, end_reason)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        String updateUserQuery = "UPDATE users SET total_score = total_score + ?, total_matches = total_matches + 1 WHERE id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;

            conn.setAutoCommit(false); // Bắt đầu transaction

            int redId = -1;
            int blueId = -1;

            // 1. Lấy user ID của Đỏ và Xanh
            try (PreparedStatement pstmt = conn.prepareStatement(getIdQuery)) {
                pstmt.setString(1, redUser);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) redId = rs.getInt("id");
                }
                pstmt.setString(1, blueUser);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) blueId = rs.getInt("id");
                }
            }

            if (redId == -1 || blueId == -1) {
                conn.rollback();
                return false;
            }

            // 2. Chèn lịch sử trận đấu
            try (PreparedStatement pstmt = conn.prepareStatement(insertMatchQuery)) {
                pstmt.setInt(1, redId);
                pstmt.setInt(2, blueId);
                pstmt.setInt(3, redScore);
                pstmt.setInt(4, blueScore);
                // winner_id = NULL: không xác định được người thắng (bình thường không xảy ra)
                if (winner != null && winner.equalsIgnoreCase(redUser)) {
                    pstmt.setInt(5, redId);
                } else if (winner != null && winner.equalsIgnoreCase(blueUser)) {
                    pstmt.setInt(5, blueId);
                } else {
                    pstmt.setNull(5, Types.INTEGER);
                }
                pstmt.setString(6, endReason);
                pstmt.executeUpdate();
            }

            // 3. Tính điểm cộng (Thắng +50, Thua +10, Hòa +20)
            int redPoints;
            int bluePoints;
            if (winner != null && !winner.isEmpty()) {
                if (winner.equalsIgnoreCase(redUser)) {
                    redPoints = 50;
                    bluePoints = 10;
                } else if (winner.equalsIgnoreCase(blueUser)) {
                    redPoints = 10;
                    bluePoints = 50;
                } else {
                    redPoints = 20;
                    bluePoints = 20;
                }
            } else {
                redPoints = redScore > blueScore ? 50 : (redScore == blueScore ? 20 : 10);
                bluePoints = blueScore > redScore ? 50 : (redScore == blueScore ? 20 : 10);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateUserQuery)) {
                pstmt.setInt(1, redPoints);
                pstmt.setInt(2, redId);
                pstmt.executeUpdate();

                pstmt.setInt(1, bluePoints);
                pstmt.setInt(2, blueId);
                pstmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu kết quả trận đấu: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tự nâng cấp bảng 'matches' của CSDL cũ: thêm cột winner_id và end_reason nếu chưa có.
     * Gọi 1 lần khi Server khởi động, để các máy đã tạo CSDL từ file SQL cũ không phải chạy lại script.
     */
    public static void ensureSchema() {
        String checkQuery = "SELECT COUNT(*) FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'matches' AND COLUMN_NAME = 'winner_id'";
        String alterQuery = "ALTER TABLE matches"
                + " ADD COLUMN winner_id INT NULL,"
                + " ADD COLUMN end_reason VARCHAR(20) NULL,"
                + " ADD FOREIGN KEY (winner_id) REFERENCES users(id)";

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkQuery)) {
                if (rs.next() && rs.getInt(1) > 0) return; // Đã có cột, không cần nâng cấp
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(alterQuery);
                System.out.println("[MatchDAO] Đã nâng cấp bảng matches: thêm cột winner_id, end_reason.");
            }
        } catch (SQLException e) {
            System.err.println("[MatchDAO] Lỗi khi nâng cấp bảng matches: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch sử trận đấu của một người chơi theo định dạng gói tin HISTORY (xem Protocol.CMD_HISTORY):
     * "tổng,thắng,thua;trận|trận|..." (mới nhất trước, tối đa Protocol.HISTORY_LIMIT trận).
     *
     * @return chuỗi dữ liệu, hoặc null nếu lỗi CSDL
     */
    public static String getHistory(String username) {
        String query = "SELECT m.player_red_id, m.red_score, m.blue_score, m.winner_id, m.end_reason, m.finished_at,"
                + " ur.username AS red_name, ub.username AS blue_name"
                + " FROM matches m"
                + " JOIN users ur ON ur.id = m.player_red_id"
                + " JOIN users ub ON ub.id = m.player_blue_id"
                + " WHERE ur.username = ? OR ub.username = ?"
                + " ORDER BY m.finished_at DESC, m.match_id DESC";

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return null;

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, username);

                int total = 0, wins = 0, losses = 0;
                StringBuilder list = new StringBuilder();
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String redName = rs.getString("red_name");
                        String blueName = rs.getString("blue_name");
                        boolean iAmRed = redName.equalsIgnoreCase(username);
                        int myScore = iAmRed ? rs.getInt("red_score") : rs.getInt("blue_score");
                        int oppScore = iAmRed ? rs.getInt("blue_score") : rs.getInt("red_score");
                        String reason = rs.getString("end_reason");

                        String result;
                        int winnerId = rs.getInt("winner_id");
                        if (!rs.wasNull()) {
                            int redId = rs.getInt("player_red_id");
                            result = (winnerId == redId) == iAmRed ? Protocol.RESULT_WIN : Protocol.RESULT_LOSE;
                        } else if (myScore + oppScore == GameEngine.TOTAL_BOXES) {
                            // Trận cũ chưa lưu người thắng nhưng đã chiếm hết bàn cờ:
                            // 25 ô là số lẻ nên tỉ số luôn chênh lệch -> suy ra chắc chắn từ tỉ số
                            result = myScore > oppScore ? Protocol.RESULT_WIN : Protocol.RESULT_LOSE;
                        } else {
                            // Trận cũ kết thúc sớm (có người bỏ cuộc) mà không lưu người thắng: không đoán
                            result = Protocol.RESULT_UNKNOWN;
                        }

                        total++;
                        if (Protocol.RESULT_WIN.equals(result)) wins++;
                        else if (Protocol.RESULT_LOSE.equals(result)) losses++;

                        if (total <= Protocol.HISTORY_LIMIT) {
                            Timestamp finishedAt = rs.getTimestamp("finished_at");
                            list.append(iAmRed ? blueName : redName).append(",")
                                .append(iAmRed ? Protocol.COLOR_RED : Protocol.COLOR_BLUE).append(",")
                                .append(myScore).append(",")
                                .append(oppScore).append(",")
                                .append(result).append(",")
                                .append(reason == null ? "" : reason).append(",")
                                .append(finishedAt == null ? 0 : finishedAt.getTime()).append("|");
                        }
                    }
                }
                if (list.length() > 0) {
                    list.setLength(list.length() - 1); // Bỏ dấu '|' cuối
                }
                return total + "," + wins + "," + losses + Protocol.DELIMITER + list;
            }
        } catch (SQLException e) {
            System.err.println("[MatchDAO] Lỗi khi lấy lịch sử trận đấu: " + e.getMessage());
            return null;
        }
    }
}
