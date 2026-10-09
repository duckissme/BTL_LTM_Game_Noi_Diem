package database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Quản lý kết nối cơ sở dữ liệu MySQL.
 * Toàn bộ thông tin cấu hình nhạy cảm (URL, User, Password) được giấu và nạp động
 * từ file cấu hình 'db.properties' hoặc biến môi trường, không lưu cứng trong mã nguồn.
 */
public class DBConnection {

    private static String url;
    private static String user;
    private static String password;
    private static boolean isLoaded = false;

    /**
     * Nạp cấu hình từ file db.properties (classpath hoặc thư mục gốc ứng dụng).
     */
    private static synchronized void loadConfig() {
        if (isLoaded) return;

        Properties props = new Properties();

        // 1. Thử nạp từ Classpath
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {}

        // 2. Thử nạp từ thư mục hiện tại của ứng dụng
        if (props.isEmpty()) {
            File propFile = new File("db.properties");
            if (propFile.exists()) {
                try (InputStream in = new FileInputStream(propFile)) {
                    props.load(in);
                } catch (Exception ignored) {}
            }
        }

        // 3. Đọc từ properties hoặc biến môi trường
        url = props.getProperty("db.url", System.getenv("DB_URL"));
        user = props.getProperty("db.user", System.getenv("DB_USER"));
        password = props.getProperty("db.password", System.getenv("DB_PASS"));

        System.out.printf("%s %s %s", url, user, password);

        isLoaded = true;
    }

    /**
     * Lấy kết nối tới MySQL.
     * @return Connection hoặc null nếu kết nối thất bại
     */
    public static Connection getConnection() {
        loadConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] Lỗi: Không tìm thấy thư viện MySQL JDBC Driver!");
            return null;
        } catch (SQLException e) {
            System.err.println("[DBConnection] Lỗi kết nối CSDL MySQL: " + e.getMessage());
            System.err.println("[DBConnection] Hãy chắc chắn dịch vụ MySQL (XAMPP/MySQL) đã được bật và file db.properties đã cấu hình đúng.");
            return null;
        }
    }
}