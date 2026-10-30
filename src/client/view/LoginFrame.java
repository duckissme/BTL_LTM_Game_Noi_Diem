package client.view;

import common.Protocol;
import client.network.MessageListener;
import client.network.SocketClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Màn hình đăng nhập tài khoản người chơi.
 * Nhận gói tin LOGIN_SUCCESS để chuyển tiếp vào sảnh chờ (LobbyFrame).
 */
public class LoginFrame extends JFrame implements MessageListener {

    private static final String LOGIN_TEXT = "Đăng nhập";

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private AppUI.GradientButton btnLogin;

    public LoginFrame() {
        initComponents();
        // Đăng ký nhận gói tin mạng từ Server
        SocketClient.getInstance().setMessageListener(this);
    }

    private void initComponents() {
        setTitle("Online Dots & Boxes - Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(AppUI.FRAME_SIZE);
        setLocationRelativeTo(null);
        setResizable(false);

        setContentPane(AppUI.createRoot(createFormPanel()));

        // Nhấn Enter ở bất kỳ ô nào cũng sẽ đăng nhập
        getRootPane().setDefaultButton(btnLogin);
        SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
    }

    private JPanel createFormPanel() {
        JPanel form = AppUI.createFormPanel();
        GridBagConstraints gbc = AppUI.createFormConstraints();

        AppUI.addRow(form, gbc, AppUI.createTitle("Đăng nhập"), 2);
        AppUI.addRow(form, gbc, AppUI.createSubtitle("Chào mừng trở lại! Đăng nhập để vào sảnh chờ."), 26);

        // TÀI KHOẢN
        AppUI.addRow(form, gbc, AppUI.createFieldLabel("Tài khoản"), 6);
        txtUsername = AppUI.createTextField("Nhập tên đăng nhập");
        AppUI.addRow(form, gbc, new AppUI.RoundedInputPanel(txtUsername, null), 16);

        // MẬT KHẨU (kèm nút Hiện/Ẩn)
        AppUI.addRow(form, gbc, AppUI.createFieldLabel("Mật khẩu"), 6);
        txtPassword = AppUI.createPasswordField("Nhập mật khẩu");
        AppUI.addRow(form, gbc, new AppUI.RoundedInputPanel(txtPassword, AppUI.createPasswordToggle(txtPassword)), 26);

        // NÚT ĐĂNG NHẬP
        btnLogin = new AppUI.GradientButton(LOGIN_TEXT);
        btnLogin.addActionListener(e -> handleLogin());
        AppUI.addRow(form, gbc, btnLogin, 18);

        // LIÊN KẾT ĐĂNG KÝ: chuyển sang màn hình đăng ký
        AppUI.addRow(form, gbc, AppUI.createLinkRow("Chưa có tài khoản? ", "Đăng ký ngay", () -> {
            setVisible(false);
            new RegisterFrame(this).setVisible(true);
        }), 0);

        return form;
    }

    /**
     * Thu thập tài khoản, mật khẩu và gửi gói tin LOGIN lên Server.
     */
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng điền đầy đủ Tài khoản và Mật khẩu!",
                    "Nhắc nhở", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String packet = Protocol.CMD_LOGIN + Protocol.DELIMITER
                      + username + Protocol.DELIMITER
                      + password;

        // Nếu lúc mở app Server chưa bật (hoặc vừa mất kết nối) thì thử kết nối lại trước khi gửi
        SocketClient client = SocketClient.getInstance();
        if (!client.ensureConnected() || !client.send(packet)) {
            AppUI.showConnectionError(this);
            return;
        }

        // Khóa nút trong lúc chờ Server trả lời, tránh bấm gửi nhiều lần
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");
    }

    /**
     * Mở khóa nút Đăng nhập (sau khi đăng nhập thất bại hoặc mất kết nối)
     */
    private void resetLoginButton() {
        btnLogin.setText(LOGIN_TEXT);
        btnLogin.setEnabled(true);
    }

    /** Mở dialog đăng ký nhanh (legacy fallback) */
    private void openRegisterDialog() {
        JDialog registerDialog = new JDialog(this, "Đăng ký tài khoản mới", true);
        registerDialog.setSize(360, 230);
        registerDialog.setLocationRelativeTo(this);
        registerDialog.setLayout(new BorderLayout());
        registerDialog.setResizable(false);

        JPanel pForm = new JPanel(new GridLayout(3, 2, 8, 12));
        pForm.setBorder(new EmptyBorder(16, 20, 10, 20));

        JTextField txtRegUser = new JTextField();
        JPasswordField txtRegPass = new JPasswordField();
        JPasswordField txtRegConfirm = new JPasswordField();

        pForm.add(new JLabel("Tài khoản:"));
        pForm.add(txtRegUser);
        pForm.add(new JLabel("Mật khẩu:"));
        pForm.add(txtRegPass);
        pForm.add(new JLabel("Xác nhận MK:"));
        pForm.add(txtRegConfirm);

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSubmit = new JButton("Xác nhận đăng ký");
        btnSubmit.setBackground(new Color(36, 120, 185));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);

        btnSubmit.addActionListener(e -> {
            String u = txtRegUser.getText().trim();
            String p = new String(txtRegPass.getPassword()).trim();
            String cf = new String(txtRegConfirm.getPassword()).trim();

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(registerDialog, "Tài khoản và mật khẩu không được rỗng!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!p.equals(cf)) {
                JOptionPane.showMessageDialog(registerDialog, "Mật khẩu xác nhận không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String regPacket = Protocol.CMD_REGISTER + Protocol.DELIMITER + u + Protocol.DELIMITER + p;
            if (!SocketClient.getInstance().send(regPacket)) {
                AppUI.showConnectionError(registerDialog);
                return;
            }
            registerDialog.dispose();
            JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu đăng ký. Vui lòng chờ phản hồi!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        pBtn.add(btnSubmit);
        registerDialog.add(pForm, BorderLayout.CENTER);
        registerDialog.add(pBtn, BorderLayout.SOUTH);
        registerDialog.setVisible(true);
    }

    /**
     * Tiếp nhận và phân tích phản hồi từ Server (LOGIN_SUCCESS, LOGIN_FAIL, v.v.).
     */
    @Override
    public void onMessageReceived(String message) {
        String[] parts = message.split(Protocol.DELIMITER);
        String cmd = parts[0];

        if (Protocol.CMD_LOGIN_SUCCESS.equals(cmd)) {
            String user = parts[1];
            int points = Integer.parseInt(parts[2]);
            int wins = Integer.parseInt(parts[3]);

            this.dispose();
            new LobbyFrame(user, points, wins).setVisible(true);

        } else if (Protocol.CMD_LOGIN_FAIL.equals(cmd)) {
            String reason = parts.length > 1 ? parts[1] : "Tài khoản hoặc mật khẩu không chính xác!";
            resetLoginButton();
            JOptionPane.showMessageDialog(this, reason, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);

        } else if (Protocol.CMD_REGISTER_SUCCESS.equals(cmd)) {
            JOptionPane.showMessageDialog(this, "Đăng ký thành công! Hãy đăng nhập để vào game.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else if (Protocol.CMD_REGISTER_FAIL.equals(cmd)) {
            String reason = parts.length > 1 ? parts[1] : "Tài khoản đã tồn tại trên hệ thống!";
            JOptionPane.showMessageDialog(this, reason, "Đăng ký thất bại", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Mất kết nối khi đang chờ kết quả đăng nhập: mở lại nút để người dùng thử lại
     * (lần bấm sau sẽ tự kết nối lại nhờ ensureConnected)
     */
    @Override
    public void onDisconnected(String reason) {
        resetLoginButton();
        JOptionPane.showMessageDialog(this,
                "Mất kết nối tới Server!\nBạn có thể bấm Đăng nhập để thử kết nối lại.",
                "Mất kết nối", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        // Thử kết nối ngay khi mở app. Nếu thất bại vẫn mở màn hình đăng nhập bình thường,
        // khi người dùng bấm Đăng nhập sẽ tự thử kết nối lại.
        if (!SocketClient.getInstance().connect(SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT)) {
            System.err.println("[Client] Chưa kết nối được Server, sẽ thử lại khi đăng nhập.");
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new LoginFrame().setVisible(true);
        });
    }
}
