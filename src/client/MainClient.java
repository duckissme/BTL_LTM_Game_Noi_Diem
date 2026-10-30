package client;

import client.network.SocketClient;
import client.view.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Điểm khởi chạy chính (Entry Point) của ứng dụng Client giao diện đồ họa (Swing).
 */
public class MainClient {

    /**
     * Khởi tạo kết nối mạng nền tới Server và hiển thị màn hình Đăng nhập (LoginFrame).
     */
    public static void main(String[] args) {
        if (!SocketClient.getInstance().connect(SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT)) {
            System.err.println("[Client] Chưa kết nối được Server, sẽ tự thử lại khi đăng nhập.");
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new LoginFrame().setVisible(true);
        });
    }
}
