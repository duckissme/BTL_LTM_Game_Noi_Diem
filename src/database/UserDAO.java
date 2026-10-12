package database;

import entity.Player;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public static Player checkLogin(String username, String password) {
        String query = "SELECT total_score, total_matches FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.err.println("[UserDAO] Không có kết nối tới cơ sở dữ liệu!");
                return null;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int score = rs.getInt("total_score");
                        int matches = rs.getInt("total_matches");
                        return new Player(username, score, matches);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn đăng nhập: " + e.getMessage());
        }
        return null;
    }

    public static int register(String username, String password) {
        // Trả về: 0 = Thành công, 1 = Đã tồn tại, 2 = Lỗi hệ thống/DB
        String checkQuery = "SELECT id FROM users WHERE username = ?";
        String insertQuery = "INSERT INTO users (username, password, total_score, total_matches) VALUES (?, ?, 0, 0)";

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.err.println("[UserDAO] Không có kết nối tới cơ sở dữ liệu!");
                return 2;
            }

            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return 1; // Đã tồn tại
                    }
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.executeUpdate();
                return 0; // Đăng ký thành công
            }
        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn đăng ký: " + e.getMessage());
            return 2;
        }
    }

    public static String getLeaderboard() {
        StringBuilder rankStr = new StringBuilder();
        String query = "SELECT username, total_score, total_matches FROM users ORDER BY total_score DESC, total_matches DESC LIMIT 10";

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.err.println("[UserDAO] Không có kết nối tới cơ sở dữ liệu khi lấy BXH!");
                return "";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String uname = rs.getString("username");
                    int score = rs.getInt("total_score");
                    int matches = rs.getInt("total_matches");

                    rankStr.append(uname).append(",")
                           .append(score).append(",")
                           .append(matches).append("|");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn Bảng xếp hạng: " + e.getMessage());
        }

        if (rankStr.length() > 0 && rankStr.charAt(rankStr.length() - 1) == '|') {
            rankStr.setLength(rankStr.length() - 1);
        }
        return rankStr.toString();
    }

    public static void refreshPlayerInfo(Player player) {
        if (player == null || player.getUsername() == null) return;
        String query = "SELECT total_score, total_matches FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.err.println("[UserDAO] Không có kết nối tới cơ sở dữ liệu khi làm mới thông tin player!");
                return;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, player.getUsername());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        player.setScore(rs.getInt("total_score"));
                        player.setTotalMatches(rs.getInt("total_matches"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi làm mới thông tin player [" + player.getUsername() + "]: " + e.getMessage());
        }
    }

    public static Player getUserByUsername(String username) {
        if (username == null) return null;
        String query = "SELECT total_score, total_matches FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.err.println("[UserDAO] Không có kết nối tới cơ sở dữ liệu khi tìm player!");
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new Player(username, rs.getInt("total_score"), rs.getInt("total_matches"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn thông tin player [" + username + "]: " + e.getMessage());
        }
        return null;
    }
}
