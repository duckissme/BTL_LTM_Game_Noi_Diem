package test;

import database.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDBConnection {
    public static void main(String[] args) {
        System.out.println("=== KIỂM TRA KẾT NỐI CƠ SỞ DỮ LIỆU MYSQL ===");
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                System.out.println(">> Kết nối MySQL THÀNH CÔNG tới CSDL btl_ltm!");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id, username, total_score, total_matches FROM users")) {
                    System.out.println("\n[Danh sách tài khoản mẫu trong bảng users]:");
                    while (rs.next()) {
                        System.out.printf("- ID: %d | User: %-10s | Điểm: %-5d | Số trận: %-3d%n",
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getInt("total_score"),
                                rs.getInt("total_matches"));
                    }
                }
            } else {
                System.err.println(">> Kết nối MySQL THẤT BÀI! Vui lòng kiểm tra lại dịch vụ MySQL (XAMPP/MySQL Service) và mật khẩu trong DBConnection.java.");
            }
        } catch (Exception e) {
            System.err.println("Lỗi: " + e.getMessage());
        }
        System.out.println("============================================");
    }
}
