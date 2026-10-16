package client.view;

import common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

/**
 * Khung chat trong trận đấu (đặt bên phải bàn cờ).
 *
 *  - Tin của ĐỐI THỦ: bong bóng bên TRÁI, nền nhạt theo màu phe đối thủ.
 *  - Tin của MÌNH:    bong bóng bên PHẢI, nền đậm theo màu phe của mình.
 *  - Thông báo hệ thống: dòng chữ nhỏ ở giữa.
 *  - Nút mặt cười mở bảng emoji (ChatEmoji). Tin chỉ có 1-3 emoji được hiển thị cỡ lớn.
 *
 * ChatPanel KHÔNG tự gửi gói tin: khi người dùng bấm Gửi, nó gọi onSend để
 * GameBoardFrame gửi CHAT lên Server. Tin nhắn chỉ hiện ra khi Server gửi lại CHAT_MSG.
 */
final class ChatPanel extends JPanel {

    static final int PANEL_WIDTH = 300;

    private static final int BUBBLE_MAX_TEXT_WIDTH = 190; // Bề rộng chữ tối đa trong 1 bong bóng
    private static final int EMOJI_SIZE = 18;             // Emoji nằm lẫn trong chữ
    private static final int BIG_EMOJI_SIZE = 34;         // Tin chỉ có emoji
    private static final int BIG_EMOJI_MAX = 3;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final Color BG_LIST = new Color(248, 250, 252);
    private static final Color BORDER_LIGHT = new Color(226, 232, 240);
    private static final Color HOVER_BG = new Color(241, 245, 249);

    private final String currentUser;
    private final Color myColor;          // Nền bong bóng của mình
    private final Color opponentColor;    // Màu tên đối thủ
    private final Color opponentBg;       // Nền bong bóng đối thủ
    private final Color opponentBorder;
    private final Predicate<String> onSend; // Trả về true nếu đã gửi được lên Server

    private final JPanel messageList;
    private final JScrollPane scroll;
    private final JTextField txtInput;
    private String lastSender = null; // Gộp các tin liên tiếp của cùng 1 người (không lặp lại tên)

    /**
     * @param currentUser tên người chơi đang dùng máy này
     * @param iAmRed      true nếu mình là phe Đỏ
     * @param onSend      hàm gửi nội dung chat lên Server, trả về true nếu gửi thành công
     */
    ChatPanel(String currentUser, boolean iAmRed, Predicate<String> onSend) {
        super(new BorderLayout(0, 10));
        this.currentUser = currentUser;
        this.onSend = onSend;
        this.myColor = iAmRed ? GameBoardFrame.COLOR_RED_VAL : GameBoardFrame.COLOR_BLUE_VAL;
        this.opponentColor = iAmRed ? GameBoardFrame.COLOR_BLUE_VAL : GameBoardFrame.COLOR_RED_VAL;
        this.opponentBg = iAmRed ? GameBoardFrame.COLOR_BLUE_BG : GameBoardFrame.COLOR_RED_BG;
        this.opponentBorder = iAmRed ? GameBoardFrame.COLOR_BLUE_BORDER : GameBoardFrame.COLOR_RED_BORDER;

        setOpaque(false);
        setPreferredSize(new Dimension(PANEL_WIDTH, 0));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel lblTitle = new JLabel("Trò chuyện");
        lblTitle.setFont(new Font(AppUI.FONT, Font.BOLD, 15));
        lblTitle.setForeground(AppUI.TEXT_DARK);

        // Danh sách tin nhắn: xếp dọc, luôn dồn lên trên
        messageList = new JPanel();
        messageList.setLayout(new BoxLayout(messageList, BoxLayout.Y_AXIS));
        messageList.setOpaque(false);

        ListHolder holder = new ListHolder();
        holder.add(messageList, BorderLayout.NORTH);

        scroll = new JScrollPane(holder);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG_LIST);

        add(lblTitle, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // Ô nhập (nhấn Enter để gửi) + nút emoji + nút Gửi
        txtInput = AppUI.createTextField("Nhập tin nhắn...");
        txtInput.addActionListener(e -> submit());
        JButton btnEmoji = new FlatIconButton(ChatEmoji.icon("😀", 22), 34);
        btnEmoji.setToolTipText("Chèn biểu tượng cảm xúc");
        btnEmoji.addActionListener(e -> showEmojiPicker(btnEmoji));

        AppUI.RoundedInputPanel inputBox = new AppUI.RoundedInputPanel(txtInput, btnEmoji);
        inputBox.setPreferredSize(new Dimension(0, 42));
        // Font "Dialog" (font logic của Java) tự mượn font hệ thống để hiện được emoji
        // trong ô nhập; "Segoe UI" chỉ hiện ô vuông. Phải đặt SAU RoundedInputPanel vì nó ghi đè font.
        txtInput.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));

        AppUI.GradientButton btnSend = new AppUI.GradientButton("Gửi");
        btnSend.setFont(new Font(AppUI.FONT, Font.BOLD, 13));
        btnSend.setPreferredSize(new Dimension(60, 42));
        btnSend.addActionListener(e -> submit());

        JPanel pnlInput = new JPanel(new BorderLayout(8, 0));
        pnlInput.setOpaque(false);
        pnlInput.add(inputBox, BorderLayout.CENTER);
        pnlInput.add(btnSend, BorderLayout.EAST);
        add(pnlInput, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(BORDER_LIGHT);
        g2.drawLine(0, 0, 0, getHeight());
        g2.dispose();
    }

    // =========================================================================
    // GỬI TIN
    // =========================================================================

    private void submit() {
        String text = txtInput.getText().trim();
        if (text.isEmpty()) return;

        if (text.length() > Protocol.CHAT_MAX_LENGTH) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,
                    "Tin nhắn tối đa " + Protocol.CHAT_MAX_LENGTH + " ký tự!",
                    "Tin nhắn quá dài", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (onSend.test(text)) {
            txtInput.setText("");
        }
    }

    /**
     * Bảng chọn emoji hiện phía trên nút mặt cười. Chọn xong thì chèn vào vị trí con trỏ.
     */
    private void showEmojiPicker(JComponent anchor) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT),
                new EmptyBorder(6, 6, 6, 6)));
        popup.setBackground(Color.WHITE);

        JPanel grid = new JPanel(new GridLayout(2, 5, 4, 4));
        grid.setBackground(Color.WHITE);
        for (String[] emoji : ChatEmoji.LIST) {
            JButton btn = new FlatIconButton(ChatEmoji.icon(emoji[0], 26), 38);
            btn.setToolTipText(emoji[1]);
            btn.addActionListener(e -> {
                popup.setVisible(false);
                txtInput.replaceSelection(emoji[0]);
                txtInput.requestFocusInWindow();
            });
            grid.add(btn);
        }
        popup.add(grid);

        Dimension size = popup.getPreferredSize();
        popup.show(anchor, anchor.getWidth() - size.width, -size.height - 8);
    }

    // =========================================================================
    // HIỂN THỊ TIN
    // =========================================================================

    /**
     * Thêm 1 tin nhắn nhận từ Server (CHAT_MSG).
     */
    void addMessage(String sender, String text) {
        boolean mine = currentUser.equalsIgnoreCase(sender);
        boolean continued = sender.equalsIgnoreCase(lastSender);
        lastSender = sender;
        float align = mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT;

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);

        // Dòng "Tên · giờ" chỉ hiện ở tin đầu tiên của một lượt nói
        if (!continued) {
            JComponent meta = createMeta(mine ? null : sender, LocalTime.now().format(TIME_FORMAT));
            meta.setAlignmentX(align);
            block.add(meta);
            block.add(Box.createVerticalStrut(3));
        }

        JComponent bubble = ChatEmoji.isOnlyEmoji(text, BIG_EMOJI_MAX)
                ? new Bubble(text, mine, BIG_EMOJI_SIZE, false)
                : new Bubble(text, mine, EMOJI_SIZE, true);
        bubble.setAlignmentX(align);
        block.add(bubble);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(continued ? 3 : 10, 10, 0, 10));
        row.add(block, mine ? BorderLayout.EAST : BorderLayout.WEST);
        addRow(row);
    }

    /**
     * Thêm dòng thông báo hệ thống ở giữa khung chat.
     */
    void addSystemMessage(String text) {
        lastSender = null;
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font(AppUI.FONT, Font.ITALIC, 11));
        lbl.setForeground(AppUI.HINT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 10, 2, 10));
        row.add(lbl);
        addRow(row);
    }

    private JComponent createMeta(String name, String time) {
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        meta.setOpaque(false);
        meta.setBorder(new EmptyBorder(0, 4, 0, 4));
        if (name != null) {
            JLabel lblName = new JLabel(name + "  ");
            lblName.setFont(new Font(AppUI.FONT, Font.BOLD, 11));
            lblName.setForeground(opponentColor);
            meta.add(lblName);
        }
        JLabel lblTime = new JLabel(time);
        lblTime.setFont(new Font(AppUI.FONT, Font.PLAIN, 11));
        lblTime.setForeground(AppUI.HINT);
        meta.add(lblTime);
        meta.setMaximumSize(meta.getPreferredSize());
        return meta;
    }

    private void addRow(JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        messageList.add(row);
        messageList.revalidate();
        messageList.repaint();
        // Đợi layout xong rồi mới cuộn xuống tin mới nhất
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // =========================================================================
    // CÁC THÀNH PHẦN GIAO DIỆN
    // =========================================================================

    /**
     * Bong bóng tin nhắn: nền bo góc + JTextPane hiển thị chữ lẫn emoji, tự xuống dòng.
     */
    private class Bubble extends JPanel {
        private final Color bg;
        private final Color border;

        Bubble(String text, boolean mine, int emojiSize, boolean withBackground) {
            super(new BorderLayout());
            setOpaque(false);
            if (withBackground) {
                bg = mine ? myColor : opponentBg;
                border = mine ? null : opponentBorder;
                setBorder(new EmptyBorder(7, 11, 7, 11));
            } else {
                bg = null;
                border = null;
                setBorder(new EmptyBorder(0, 2, 0, 2));
            }

            JTextPane pane = new JTextPane();
            pane.setEditorKit(new WrapEditorKit());
            pane.setEditable(false);
            pane.setOpaque(false);
            pane.setBorder(null);
            pane.setMargin(new Insets(0, 0, 0, 0));

            int naturalWidth = fillContent(pane, text, mine && withBackground ? Color.WHITE : AppUI.TEXT_DARK, emojiSize);

            // Rộng vừa đủ nội dung (tối đa BUBBLE_MAX_TEXT_WIDTH), cao theo số dòng sau khi xuống dòng
            int w = Math.min(naturalWidth + 2, BUBBLE_MAX_TEXT_WIDTH);
            pane.setSize(w, Short.MAX_VALUE);
            pane.setPreferredSize(new Dimension(w, pane.getPreferredSize().height));
            add(pane, BorderLayout.CENTER);

            setMaximumSize(getPreferredSize());
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (bg != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(bg);
                g2.fill(shape);
                if (border != null) {
                    g2.setColor(border);
                    g2.draw(shape);
                }
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Ghi nội dung vào JTextPane: emoji trong bộ ChatEmoji -> hình vẽ,
     * ký tự Segoe UI không hiển thị được -> font hệ thống "Dialog", còn lại -> Segoe UI.
     *
     * @return bề rộng nội dung khi chưa xuống dòng (pixel)
     */
    private static int fillContent(JTextPane pane, String text, Color fg, int emojiSize) {
        StyledDocument doc = pane.getStyledDocument();
        Font mainFont = new Font(AppUI.FONT, Font.PLAIN, 13);
        Font fallbackFont = new Font(Font.DIALOG, Font.PLAIN, 13);
        FontMetrics fmMain = pane.getFontMetrics(mainFont);
        FontMetrics fmFallback = pane.getFontMetrics(fallbackFont);

        int width = 0;
        StringBuilder run = new StringBuilder();
        boolean runIsFallback = false;
        int i = 0;
        while (i < text.length()) {
            String emoji = ChatEmoji.matchAt(text, i);
            if (emoji != null) {
                width += flushRun(doc, run, runIsFallback ? fallbackFont : mainFont, fg,
                        runIsFallback ? fmFallback : fmMain);
                SimpleAttributeSet iconStyle = new SimpleAttributeSet();
                StyleConstants.setIcon(iconStyle, ChatEmoji.icon(emoji, emojiSize));
                insert(doc, " ", iconStyle);
                width += emojiSize;
                i += emoji.length();
                continue;
            }

            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == ChatEmoji.VARIATION_SELECTOR) continue;

            boolean isFallback = !mainFont.canDisplay(cp);
            if (isFallback != runIsFallback && run.length() > 0) {
                width += flushRun(doc, run, runIsFallback ? fallbackFont : mainFont, fg,
                        runIsFallback ? fmFallback : fmMain);
            }
            runIsFallback = isFallback;
            run.appendCodePoint(cp);
        }
        width += flushRun(doc, run, runIsFallback ? fallbackFont : mainFont, fg,
                runIsFallback ? fmFallback : fmMain);
        return width;
    }

    private static int flushRun(StyledDocument doc, StringBuilder run, Font font, Color fg, FontMetrics fm) {
        if (run.length() == 0) return 0;
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setFontFamily(style, font.getFamily());
        StyleConstants.setFontSize(style, font.getSize());
        StyleConstants.setForeground(style, fg);
        String s = run.toString();
        insert(doc, s, style);
        run.setLength(0);
        return fm.stringWidth(s);
    }

    private static void insert(StyledDocument doc, String s, SimpleAttributeSet style) {
        try {
            doc.insertString(doc.getLength(), s, style);
        } catch (BadLocationException ignored) {
        }
    }

    /**
     * EditorKit cho phép ngắt dòng ngay giữa một từ quá dài
     * (mặc định JTextPane chỉ ngắt ở khoảng trắng, từ dài sẽ tràn ra ngoài bong bóng).
     */
    private static class WrapEditorKit extends StyledEditorKit {
        private final ViewFactory factory = elem -> {
            String kind = elem.getName();
            if (AbstractDocument.ContentElementName.equals(kind)) {
                return new LabelView(elem) {
                    @Override
                    public float getMinimumSpan(int axis) {
                        return axis == View.X_AXIS ? 0 : super.getMinimumSpan(axis);
                    }
                };
            } else if (AbstractDocument.ParagraphElementName.equals(kind)) {
                return new ParagraphView(elem);
            } else if (AbstractDocument.SectionElementName.equals(kind)) {
                return new BoxView(elem, View.Y_AXIS);
            } else if (StyleConstants.ComponentElementName.equals(kind)) {
                return new ComponentView(elem);
            } else if (StyleConstants.IconElementName.equals(kind)) {
                return new IconView(elem);
            }
            return new LabelView(elem);
        };

        @Override
        public ViewFactory getViewFactory() {
            return factory;
        }
    }

    /**
     * Panel chứa danh sách tin: luôn rộng bằng khung cuộn để bong bóng của mình nằm sát phải.
     */
    private static class ListHolder extends JPanel implements Scrollable {
        ListHolder() {
            super(new BorderLayout());
            setBackground(BG_LIST);
            setBorder(new EmptyBorder(0, 0, 10, 0));
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /**
     * Nút chỉ có icon, hiện nền tròn nhạt khi rê chuột.
     */
    private static class FlatIconButton extends JButton {
        FlatIconButton(Icon icon, int size) {
            super(icon);
            setPreferredSize(new Dimension(size, size));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getModel().isRollover()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(HOVER_BG);
                g2.fill(new Ellipse2D.Float(1, 1, getWidth() - 2, getHeight() - 2));
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
