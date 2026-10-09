package client.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

/**
 * Các thành phần giao diện dùng chung cho toàn bộ ứng dụng
 */
final class AppUI {

    // Bảng màu chung
    static final Color PRIMARY = new Color(37, 99, 235);
    static final Color ACCENT = new Color(124, 58, 237);
    static final Color TEXT_DARK = new Color(15, 23, 42);
    static final Color TEXT_MUTED = new Color(100, 116, 139);
    static final Color HINT = new Color(148, 163, 184);
    static final Color INPUT_BG = new Color(248, 250, 252);
    static final Color INPUT_BORDER = new Color(203, 213, 225);
    static final String FONT = "Segoe UI";
    static final Dimension FRAME_SIZE = new Dimension(780, 520);

    private AppUI() {
    }

    /**
     * Khung gốc: panel thương hiệu bên trái + form bên phải
     */
    static JPanel createRoot(JPanel form) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(new BrandPanel(), BorderLayout.WEST);
        root.add(form, BorderLayout.CENTER);
        return root;
    }

    /**
     * Thông báo dùng chung khi không gửi được gói tin vì chưa kết nối tới Server
     */
    static void showConnectionError(Component parent) {
        JOptionPane.showMessageDialog(parent,
                "Không kết nối được tới Server!\nHãy kiểm tra Server đã bật chưa rồi thử lại.",
                "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
    }

    static JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(28, 44, 28, 44));
        return form;
    }

    static GridBagConstraints createFormConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = -1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    static void addRow(JPanel panel, GridBagConstraints gbc, JComponent comp, int bottomGap) {
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, bottomGap, 0);
        panel.add(comp, gbc);
    }

    static JLabel createTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.BOLD, 26));
        label.setForeground(TEXT_DARK);
        return label;
    }

    static JLabel createSubtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.PLAIN, 13));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.BOLD, 13));
        label.setForeground(new Color(51, 65, 85));
        return label;
    }

    static JTextField createTextField(String hint) {
        return new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintHint(this, g, hint);
            }
        };
    }

    static JPasswordField createPasswordField(String hint) {
        return new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintHint(this, g, hint);
            }
        };
    }

    /**
     * Nút chữ Hiện/Ẩn đặt bên phải ô mật khẩu
     */
    static JButton createPasswordToggle(JPasswordField field) {
        char defaultEcho = field.getEchoChar();
        JButton toggle = new JButton("Hiện");
        toggle.setFont(new Font(FONT, Font.BOLD, 12));
        toggle.setForeground(PRIMARY);
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setFocusable(false);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> {
            boolean hidden = field.getEchoChar() != 0;
            field.setEchoChar(hidden ? (char) 0 : defaultEcho);
            toggle.setText(hidden ? "Ẩn" : "Hiện");
        });
        return toggle;
    }

    /**
     * Dòng "Câu dẫn? Liên kết" căn giữa, liên kết gạch chân khi hover
     */
    static JPanel createLinkRow(String prefix, String linkText, Runnable onClick) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);

        JLabel lblPrefix = new JLabel(prefix);
        lblPrefix.setFont(new Font(FONT, Font.PLAIN, 13));
        lblPrefix.setForeground(TEXT_MUTED);

        JLabel lblLink = new JLabel(linkText);
        Font linkFont = new Font(FONT, Font.BOLD, 13);
        Font linkHoverFont = linkFont.deriveFont(Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
        lblLink.setFont(linkFont);
        lblLink.setForeground(PRIMARY);
        lblLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                onClick.run();
            }

            @Override
            public void mouseEntered(MouseEvent evt) {
                lblLink.setFont(linkHoverFont);
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                lblLink.setFont(linkFont);
            }
        });

        row.add(lblPrefix);
        row.add(lblLink);
        return row;
    }

    /**
     * Vẽ chữ gợi ý khi ô nhập trống (không ghi đè nội dung thật của ô)
     */
    private static void paintHint(JTextComponent c, Graphics g, String hint) {
        if (c.getDocument().getLength() > 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(c.getFont());
        g2.setColor(HINT);
        FontMetrics fm = g2.getFontMetrics();
        Insets in = c.getInsets();
        int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(hint, in.left, y);
        g2.dispose();
    }

    /**
     * Khung ô nhập bo góc, viền đổi màu khi được focus
     */
    static class RoundedInputPanel extends JPanel {
        private final JTextField field;

        RoundedInputPanel(JTextField field, JComponent trailing) {
            super(new BorderLayout());
            this.field = field;
            setOpaque(false);
            setBorder(new EmptyBorder(2, 14, 2, trailing == null ? 14 : 4));
            setPreferredSize(new Dimension(300, 44));

            field.setFont(new Font(FONT, Font.PLAIN, 14));
            field.setForeground(TEXT_DARK);
            field.setCaretColor(PRIMARY);
            field.setSelectionColor(new Color(191, 219, 254));
            field.setBorder(null);
            field.setBackground(INPUT_BG);
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBackground(Color.WHITE);
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    field.setBackground(INPUT_BG);
                    repaint();
                }
            });
            add(field, BorderLayout.CENTER);
            if (trailing != null) {
                add(trailing, BorderLayout.EAST);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean focused = field.isFocusOwner();
            int w = getWidth();
            int h = getHeight();

            // Vầng sáng nhẹ quanh ô khi focus
            if (focused) {
                g2.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 40));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            }
            g2.setColor(focused ? Color.WHITE : INPUT_BG);
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 10, 10));
            g2.setColor(focused ? PRIMARY : INPUT_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
            g2.draw(new RoundRectangle2D.Float(2, 2, w - 5, h - 5, 10, 10));
            g2.dispose();
        }
    }

    /**
     * Nút bo góc nền gradient, đổi sắc khi hover/nhấn và xám khi bị vô hiệu
     */
    static class GradientButton extends JButton {

        GradientButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font(FONT, Font.BOLD, 15));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(300, 46));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            // Khử răng cưa giúp viền và chữ sắc nét
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            Color start = PRIMARY;
            Color end = ACCENT;
            if (!isEnabled()) {
                start = new Color(148, 163, 184);
                end = new Color(148, 163, 184);
            } else if (getModel().isPressed()) {
                start = PRIMARY.darker();
                end = ACCENT.darker();
            } else if (getModel().isRollover()) {
                start = new Color(59, 130, 246);
                end = new Color(139, 92, 246);
            }

            g2.setPaint(new GradientPaint(0, 0, start, w, h, end));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textX = (w - fm.stringWidth(getText())) / 2;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }
    }

    /**
     * Panel thương hiệu bên trái: nền gradient + bàn cờ Dots & Boxes minh họa
     */
    static class BrandPanel extends JPanel {
        private static final int COLS = 4;
        private static final int SPACING = 42;
        // Các cạnh đã nối, mỗi cạnh: {cột1, hàng1, cột2, hàng2}
        private static final int[][] EDGES = {
                {0, 0, 1, 0}, {0, 1, 1, 1}, {0, 0, 0, 1}, {1, 0, 1, 1},
                {1, 1, 2, 1}, {1, 2, 2, 2}, {1, 1, 1, 2}, {2, 1, 2, 2},
                {1, 0, 2, 0}, {2, 2, 3, 2}, {3, 0, 3, 1}
        };

        BrandPanel() {
            setPreferredSize(new Dimension(320, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Nền gradient chéo
            g2.setPaint(new GradientPaint(0, 0, PRIMARY, w, h, ACCENT));
            g2.fillRect(0, 0, w, h);

            // Vòng tròn trang trí mờ
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fill(new Ellipse2D.Float(w - 140, -80, 240, 240));
            g2.fill(new Ellipse2D.Float(-90, h - 150, 220, 220));

            // Bàn cờ minh họa
            int gridSize = (COLS - 1) * SPACING;
            int x0 = (w - gridSize) / 2;
            int y0 = 72;

            // Ô đã chiếm
            g2.setColor(new Color(255, 255, 255, 70));
            g2.fillRect(x0 + 3, y0 + 3, SPACING - 6, SPACING - 6);
            g2.setColor(new Color(251, 191, 36, 170));
            g2.fillRect(x0 + SPACING + 3, y0 + SPACING + 3, SPACING - 6, SPACING - 6);

            // Cạnh đã nối
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, 230));
            for (int[] e : EDGES) {
                g2.drawLine(x0 + e[0] * SPACING, y0 + e[1] * SPACING,
                        x0 + e[2] * SPACING, y0 + e[3] * SPACING);
            }

            // Các điểm
            g2.setColor(Color.WHITE);
            for (int r = 0; r < COLS; r++) {
                for (int c = 0; c < COLS; c++) {
                    g2.fill(new Ellipse2D.Float(x0 + c * SPACING - 5, y0 + r * SPACING - 5, 10, 10));
                }
            }

            // Tiêu đề
            int textTop = y0 + gridSize + 64;
            drawCentered(g2, "DOTS & BOXES", new Font(FONT, Font.BOLD, 28), Color.WHITE, textTop);
            drawCentered(g2, "Online Multiplayer", new Font(FONT, Font.PLAIN, 14),
                    new Color(255, 255, 255, 210), textTop + 28);
            drawCentered(g2, "Nối điểm  •  Chiếm ô  •  Giành chiến thắng", new Font(FONT, Font.PLAIN, 12),
                    new Color(255, 255, 255, 170), h - 32);

            g2.dispose();
        }

        private void drawCentered(Graphics2D g2, String text, Font font, Color color, int baseline) {
            g2.setFont(font);
            g2.setColor(color);
            int x = (getWidth() - g2.getFontMetrics().stringWidth(text)) / 2;
            g2.drawString(text, x, baseline);
        }
    }
}
