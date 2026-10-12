package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        String getIdQuery = "SELECT id FROM users WHERE username = ?";
        String insertMatchQuery = "INSERT INTO matches (player_red_id, player_blue_id, red_score, blue_score) VALUES (?, ?, ?, ?)";
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
}
