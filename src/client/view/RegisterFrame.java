package client.view;

import common.Protocol;
import client.network.MessageListener;
import client.network.SocketClient;

import javax.swing.*;
import java.awt.*;

public class RegisterFrame extends JFrame implements MessageListener {

    private static final String REGISTER_TEXT = "Đăng ký";

    private final JFrame parentLoginFrame;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private AppUI.GradientButton btnRegister;

    public RegisterFrame(JFrame parentLoginFrame) {
        this.parentLoginFrame = parentLoginFrame;
        initComponents();
        SocketClient.getInstance().setMessageListener(this);
    }

    private void initComponents() {
        setTitle("Online Dots & Boxes - Đăng ký tài khoản");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(AppUI.FRAME_SIZE);
        // Mở đúng vị trí cửa sổ đăng nhập (cùng kích thước) để chuyển màn hình liền mạch
        if (parentLoginFrame != null) {
            setLocation(parentLoginFrame.getLocation());
        } else {
            setLocationRelativeTo(null);
        }
        setResizable(false);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                SocketClient.getInstance().close();
                System.exit(0);
            }
        });

        setContentPane(AppUI.createRoot(createFormPanel()));

        // Nhấn Enter ở bất kỳ ô nào cũng sẽ gửi đăng ký
        getRootPane().setDefaultButton(btnRegister);
        SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
    }

    private JPanel createFormPanel() {
        JPanel form = AppUI.createFormPanel();
        GridBagConstraints gbc = AppUI.createFormConstraints();

        AppUI.addRow(form, gbc, AppUI.createTitle("Tạo tài khoản"), 2);
        AppUI.addRow(form, gbc, AppUI.createSubtitle("Đăng ký miễn phí để bắt đầu thi đấu."), 18);

        // TÀI KHOẢN
        AppUI.addRow(form, gbc, AppUI.createFieldLabel("Tài khoản"), 5);
        txtUsername = AppUI.createTextField("Tạo tên đăng nhập");
        AppUI.addRow(form, gbc, new AppUI.RoundedInputPanel(txtUsername, null), 10);

        // MẬT KHẨU (kèm nút Hiện/Ẩn)
        AppUI.addRow(form, gbc, AppUI.createFieldLabel("Mật khẩu"), 5);
        txtPassword = AppUI.createPasswordField("Tối thiểu 6 ký tự");
        AppUI.addRow(form, gbc, new AppUI.RoundedInputPanel(txtPassword, AppUI.createPasswordToggle(txtPassword)), 10);

        // XÁC NHẬN MẬT KHẨU
        AppUI.addRow(form, gbc, AppUI.createFieldLabel("Xác nhận mật khẩu"), 5);
        txtConfirmPassword = AppUI.createPasswordField("Nhập lại mật khẩu");
        AppUI.addRow(form, gbc, new AppUI.RoundedInputPanel(txtConfirmPassword,
                AppUI.createPasswordToggle(txtConfirmPassword)), 18);

        // NÚT ĐĂNG KÝ
        btnRegister = new AppUI.GradientButton(REGISTER_TEXT);
        btnRegister.addActionListener(e -> handleRegister());
        AppUI.addRow(form, gbc, btnRegister, 14);

        // LIÊN KẾT QUAY LẠI ĐĂNG NHẬP
        AppUI.addRow(form, gbc, AppUI.createLinkRow("Đã có tài khoản? ", "Đăng nhập", this::goBackToLogin), 0);

        return form;
    }

    private boolean validatePassword(String pass) {
        if (pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mật khẩu!", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return false;
        }
        if (pass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu phải có độ dài ít nhất 6 ký tự!", "Độ mạnh mật khẩu", JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void handleRegister() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String confirmPassword = new String(txtConfirmPassword.getPassword()).trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên tài khoản!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return;
        }

        if (!validatePassword(password)) {
            return;
        }

        if (confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng xác nhận lại mật khẩu!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            txtConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!", "Lỗi xác nhận", JOptionPane.ERROR_MESSAGE);
            txtConfirmPassword.requestFocus();
            return;
        }

        String packet = Protocol.CMD_REGISTER + Protocol.DELIMITER + username + Protocol.DELIMITER + password;

        // Thử kết nối lại nếu cần, gửi không được thì báo lỗi ngay thay vì chờ mãi
        SocketClient client = SocketClient.getInstance();
        if (!client.ensureConnected() || !client.send(packet)) {
            AppUI.showConnectionError(this);
            return;
        }

        // Khóa nút trong lúc chờ Server trả lời, tránh bấm gửi nhiều lần
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");
    }

    /**
     * Mở khóa nút Đăng ký (sau khi đăng ký thất bại hoặc mất kết nối)
     */
    private void resetRegisterButton() {
        btnRegister.setText(REGISTER_TEXT);
        btnRegister.setEnabled(true);
    }

    @Override
    public void onMessageReceived(String message) {
        String[] parts = message.split(Protocol.DELIMITER);
        String cmd = parts[0];

        if (Protocol.CMD_REGISTER_SUCCESS.equals(cmd)) {
            JOptionPane.showMessageDialog(this, "Đăng ký thành công! Hãy đăng nhập để bắt đầu.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            goBackToLogin();
        } else if (Protocol.CMD_REGISTER_FAIL.equals(cmd)) {
            String reason = parts.length > 1 ? parts[1] : "Tài khoản đã tồn tại trên hệ thống!";
            resetRegisterButton();
            JOptionPane.showMessageDialog(this, reason, "Đăng ký thất bại", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Mất kết nối khi đang chờ kết quả đăng ký: mở lại nút để người dùng thử lại
     * (lần bấm sau sẽ tự kết nối lại nhờ ensureConnected)
     */
    @Override
    public void onDisconnected(String reason) {
        resetRegisterButton();
        JOptionPane.showMessageDialog(this,
                "Mất kết nối tới Server!\nBạn có thể bấm Đăng ký để thử kết nối lại.",
                "Mất kết nối", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Quay về màn hình Login và ép vẽ lại sạch sẽ để không bị bóng chữ
     */
    private void goBackToLogin() {
        this.dispose();
        if (parentLoginFrame != null) {
            if (parentLoginFrame instanceof MessageListener) {
                SocketClient.getInstance().setMessageListener((MessageListener) parentLoginFrame);
            }
            parentLoginFrame.setVisible(true);
            // Xóa sạch bộ đệm đồ họa cũ, chống vẽ đè chữ
            parentLoginFrame.revalidate();
            parentLoginFrame.repaint();
        }
    }
}
