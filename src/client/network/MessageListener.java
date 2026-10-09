package client.network;

/**
 * "Người nghe" gói tin từ Server.
 *
 * Mỗi màn hình (Login, Register, Lobby...) implement interface này rồi tự đăng ký
 * với SocketClient bằng SocketClient.getInstance().setMessageListener(this).
 * Tại một thời điểm chỉ có MỘT màn hình nhận gói tin (màn hình đăng ký sau cùng).
 *
 * Cả 2 hàm dưới đây đều được SocketClient gọi trên luồng giao diện Swing (EDT),
 * nên bên trong có thể cập nhật JLabel, JTable, mở JOptionPane... trực tiếp.
 */
public interface MessageListener {

    /**
     * Có một dòng dữ liệu mới từ Server gửi về.
     *
     * @param message nguyên văn 1 dòng, ví dụ: "LOGIN_SUCCESS;huy;1200;45"
     */
    void onMessageReceived(String message);

    /**
     * Kết nối tới Server bị mất NGOÀI Ý MUỐN (Server tắt, rớt mạng...).
     * Không được gọi khi chính chương trình chủ động gọi SocketClient.close().
     *
     * Đây là hàm "default": màn hình nào không cần xử lý thì không phải viết lại.
     *
     * @param reason lý do mất kết nối (để hiển thị/ghi log)
     */
    default void onDisconnected(String reason) {
    }
}
