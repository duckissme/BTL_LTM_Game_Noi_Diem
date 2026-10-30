package client.view;

import common.Protocol;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Hộp thoại "Lịch sử trận đấu" của một người chơi (chính mình hoặc người khác), mở từ Sảnh chờ.
 *
 * Bản chất: Dumb View — chỉ hiển thị dữ liệu trong gói HISTORY Server gửi về
 * (Server đã tính sẵn thắng/thua và tổng kết), không tự truy vấn hay tính toán kết quả.
 *
 *  1. Hàng thẻ tổng kết: Tổng trận / Thắng / Thua / Tỉ lệ thắng.
 *  2. Bảng các trận gần nhất: Thời gian, Đối thủ, Phe, Tỉ số, Kết quả, Điểm cộng, Ghi chú.
 *     Nhấp đúp vào một trận để xem tiếp lịch sử của đối thủ trong trận đó.
 */
final class MatchHistoryDialog extends JDialog {

    private static final Color TEXT_MAIN = AppUI.TEXT_DARK;
    private static final Color TEXT_MUTED = AppUI.TEXT_MUTED;
    private static final Color BORDER_CARD = new Color(226, 232, 240);
    private static final Color HEADER_BG = new Color(248, 250, 252);
    private static final Color GRID = new Color(241, 245, 249);
    private static final Color SELECT_BG = new Color(239, 246, 255);

    // Màu theo kết quả: {chữ, nền}
    private static final Color WIN_FG = new Color(21, 128, 61);
    private static final Color WIN_BG = new Color(220, 252, 231);
    private static final Color LOSE_FG = new Color(185, 28, 28);
    private static final Color LOSE_BG = new Color(254, 226, 226);
    private static final Color DRAW_FG = new Color(37, 99, 235);
    private static final Color DRAW_BG = new Color(239, 246, 255);
    private static final Color UNKNOWN_FG = new Color(71, 85, 105);
    private static final Color UNKNOWN_BG = new Color(241, 245, 249);
    private static final Color RATE_FG = new Color(180, 83, 9);
    private static final Color RATE_BG = new Color(254, 243, 199);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

    // Thứ tự cột trong bảng sau khi bổ sung cột Điểm cộng
    private static final int COL_TIME = 0;
    private static final int COL_OPPONENT = 1;
    private static final int COL_SIDE = 2;
    private static final int COL_SCORE = 3;
    private static final int COL_RESULT = 4;
    private static final int COL_POINTS = 5;
    private static final int COL_NOTE = 6;

    private final String viewedUser;          // Người chơi đang được xem lịch sử
    private final boolean isSelf;             // true nếu đang xem lịch sử của chính mình
    private final Consumer<String> onViewPlayer; // Yêu cầu xem lịch sử người khác (Lobby gửi GET_HISTORY)

    /**
     * @param viewedUser   người chơi có lịch sử đang hiển thị
     * @param isSelf       true nếu đó là chính người đang dùng máy này
     * @param summary      dữ liệu tổng kết "tổng,thắng,thua"
     * @param records      danh sách trận "trận|trận|..." (có thể rỗng), định dạng xem Protocol.CMD_HISTORY
     * @param onViewPlayer gọi khi người dùng nhấp đúp một trận để xem lịch sử của đối thủ
     */
    MatchHistoryDialog(Frame owner, String viewedUser, boolean isSelf, String summary, String records,
                       Consumer<String> onViewPlayer) {
        super(owner, "Lịch sử trận đấu", true);
        this.viewedUser = viewedUser;
        this.isSelf = isSelf;
        this.onViewPlayer = onViewPlayer;

        setSize(850, 580);
        setMinimumSize(new Dimension(750, 460));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        int[] stats = parseSummary(summary);
        DefaultTableModel model = createModel();
        int shown = fillRows(model, records);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(22, 24, 18, 24));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(createTitle(shown, stats), BorderLayout.NORTH);
        top.add(createStatsRow(stats), BorderLayout.CENTER);

        root.add(top, BorderLayout.NORTH);
        root.add(createTableCard(model), BorderLayout.CENTER);
        root.add(createFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    // =========================================================================
    // PHÂN TÍCH DỮ LIỆU GÓI HISTORY
    // =========================================================================

    /** @return {tổng, thắng, thua, hòa} */
    private static int[] parseSummary(String summary) {
        int[] stats = new int[4];
        String[] fields = summary == null ? new String[0] : summary.split(",");
        for (int i = 0; i < stats.length && i < fields.length; i++) {
            try {
                stats[i] = Integer.parseInt(fields[i].trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return stats;
    }

    private static DefaultTableModel createModel() {
        String[] columns = {"Thời gian", "Đối thủ", "Phe", "Tỉ số", "Kết quả", "Điểm cộng", "Ghi chú"};
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    /**
     * Mỗi trận: đối_thủ,màu_quân,điểm_mình,điểm_đối_thủ,kết_quả,lý_do,thời_điểm
     *
     * @return số trận đã thêm vào bảng
     */
    private int fillRows(DefaultTableModel model, String records) {
        if (records == null || records.trim().isEmpty()) return 0;

        int count = 0;
        for (String record : records.split(Protocol.LIST_DELIMITER)) {
            String[] f = record.split(",", -1);
            if (f.length < 7) continue;

            String result = f[4].trim();
            model.addRow(new Object[]{
                    formatTime(f[6].trim()),
                    f[0].trim(),
                    f[1].trim(),
                    f[2].trim() + "  -  " + f[3].trim(),
                    result,
                    formatPointsChange(result),
                    describeReason(f[5].trim(), result)
            });
            count++;
        }
        return count;
    }

    /**
     * Tính toán chuỗi điểm cộng/trừ theo kết quả trận đấu:
     * - Thắng: +10
     * - Thua: -10
     * - Hòa: +5
     */
    private static String formatPointsChange(String result) {
        if (Protocol.RESULT_WIN.equalsIgnoreCase(result) || "WIN".equalsIgnoreCase(result)) {
            return "+10";
        }
        if (Protocol.RESULT_LOSE.equalsIgnoreCase(result) || "LOSE".equalsIgnoreCase(result)) {
            return "-10";
        }
        if ("DRAW".equalsIgnoreCase(result) || "HÒA".equalsIgnoreCase(result) || "Hòa".equalsIgnoreCase(result)) {
            return "+5";
        }
        return "—";
    }

    /** Chuyển đổi timestamp mili-giây sang định dạng ngày giờ dd/MM/yyyy HH:mm */
    private static String formatTime(String epochMillis) {
        try {
            long ms = Long.parseLong(epochMillis);
            if (ms <= 0) return "—";
            return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(TIME_FORMAT);
        } catch (NumberFormatException e) {
            return "—";
        }
    }

    /** Diễn giải mã lý do kết thúc trận thành văn bản thân thiện với người dùng */
    private String describeReason(String reason, String result) {
        if (Protocol.REASON_FULL_BOARD.equalsIgnoreCase(reason)) {
            return "Chiếm hết bàn cờ";
        }
        if (Protocol.REASON_SURRENDER.equalsIgnoreCase(reason)) {
            if (Protocol.RESULT_WIN.equalsIgnoreCase(result)) return "Đối thủ bỏ cuộc";
            if (Protocol.RESULT_LOSE.equalsIgnoreCase(result)) return isSelf ? "Bạn bỏ cuộc" : viewedUser + " bỏ cuộc";
            return "Bỏ cuộc";
        }
        if (Protocol.REASON_TIMEOUT.equalsIgnoreCase(reason)) {
            if (Protocol.RESULT_WIN.equalsIgnoreCase(result)) return "Đối thủ hết giờ";
            return isSelf ? "Bạn hết giờ" : viewedUser + " hết giờ";
        }
        if (Protocol.REASON_TIME_UP.equalsIgnoreCase(reason)) {
            return "Hết " + (Protocol.MATCH_TIME_SECONDS / 60) + " phút";
        }
        if (Protocol.RESULT_UNKNOWN.equalsIgnoreCase(result)) {
            return "Chưa lưu kết quả";
        }
        return "—";
    }

    // =========================================================================
    // GIAO DIỆN
    // =========================================================================

    private JPanel createTitle(int shown, int[] stats) {
        JPanel pnl = new JPanel(new GridLayout(2, 1, 0, 2));
        pnl.setOpaque(false);

        JLabel lblTitle = new JLabel(isSelf ? "Lịch sử trận đấu của bạn" : "Lịch sử trận đấu của " + viewedUser);
        lblTitle.setFont(new Font(AppUI.FONT, Font.BOLD, 20));
        lblTitle.setForeground(TEXT_MAIN);

        int total = stats[0];
        int unknown = total - stats[1] - stats[2] - stats[3];
        String sub = total > shown ? "Hiển thị " + shown + " trận gần nhất" : "Toàn bộ các trận đã đấu";
        if (unknown > 0) {
            sub += "  •  " + unknown + " trận cũ không rõ kết quả";
        }
        sub += "  •  Nhấp đúp vào một trận để xem lịch sử của đối thủ";
        JLabel lblSub = new JLabel(sub);
        lblSub.setFont(new Font(AppUI.FONT, Font.PLAIN, 13));
        lblSub.setForeground(TEXT_MUTED);

        pnl.add(lblTitle);
        pnl.add(lblSub);
        return pnl;
    }

    private JPanel createStatsRow(int[] stats) {
        int wins = stats[1];
        int losses = stats[2];
        String rate = (wins + losses) == 0 ? "—" : Math.round(wins * 100.0 / (wins + losses)) + "%";

        JPanel row = new JPanel(new GridLayout(1, 5, 10, 0));
        row.setOpaque(false);
        row.add(new StatTile("Tổng trận", String.valueOf(stats[0]), AppUI.PRIMARY, new Color(239, 246, 255)));
        row.add(new StatTile("Thắng", String.valueOf(wins), WIN_FG, WIN_BG));
        row.add(new StatTile("Thua", String.valueOf(losses), LOSE_FG, LOSE_BG));
        row.add(new StatTile("Hòa", String.valueOf(stats[3]), DRAW_FG, DRAW_BG));
        row.add(new StatTile("Tỉ lệ thắng", rate, RATE_FG, RATE_BG));
        return row;
    }

    private JComponent createTableCard(DefaultTableModel model) {
        String empty = isSelf
                ? "Bạn chưa chơi trận nào. Hãy thách đấu một người chơi ở sảnh!"
                : viewedUser + " chưa chơi trận nào.";
        JTable table = new JTable(model) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(new Font(AppUI.FONT, Font.PLAIN, 13));
                    g2.setColor(AppUI.HINT);
                    int x = (getWidth() - g2.getFontMetrics().stringWidth(empty)) / 2;
                    g2.drawString(empty, x, 56);
                    g2.dispose();
                }
            }
        };
        table.setRowHeight(46);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(GRID);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFont(new Font(AppUI.FONT, Font.PLAIN, 13));
        table.setForeground(TEXT_MAIN);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(SELECT_BG);
        table.setSelectionForeground(TEXT_MAIN);
        table.setFillsViewportHeight(true);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                onViewPlayer.accept(String.valueOf(table.getValueAt(row, COL_OPPONENT)));
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer((t, value, isSelected, hasFocus, row, col) -> {
            boolean left = col == COL_OPPONENT;
            JLabel lbl = new JLabel(String.valueOf(value).toUpperCase(), left ? SwingConstants.LEFT : SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(HEADER_BG);
            lbl.setFont(new Font(AppUI.FONT, Font.BOLD, 11));
            lbl.setForeground(TEXT_MUTED);
            lbl.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER_CARD),
                    new EmptyBorder(0, left ? 14 : 4, 0, 4)));
            return lbl;
        });

        // Tối ưu kích thước 7 cột
        int[] widths = {140, 130, 75, 80, 95, 85, 135};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(COL_TIME).setCellRenderer(new TextRenderer(false, TEXT_MUTED, SwingConstants.CENTER));
        table.getColumnModel().getColumn(COL_OPPONENT).setCellRenderer(new TextRenderer(true, TEXT_MAIN, SwingConstants.LEFT));
        table.getColumnModel().getColumn(COL_SIDE).setCellRenderer(new SideRenderer());
        table.getColumnModel().getColumn(COL_SCORE).setCellRenderer(new ScoreRenderer());
        table.getColumnModel().getColumn(COL_RESULT).setCellRenderer(new ResultRenderer());
        table.getColumnModel().getColumn(COL_POINTS).setCellRenderer(new PointsRenderer());
        table.getColumnModel().getColumn(COL_NOTE).setCellRenderer(new TextRenderer(false, TEXT_MUTED, SwingConstants.CENTER));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_CARD));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel corner = new JPanel();
        corner.setBackground(HEADER_BG);
        scroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, corner);
        return scroll;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        AppUI.GradientButton btnClose = new AppUI.GradientButton("Đóng");
        btnClose.setFont(new Font(AppUI.FONT, Font.BOLD, 14));
        btnClose.setPreferredSize(new Dimension(120, 40));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);
        getRootPane().setDefaultButton(btnClose);
        return footer;
    }

    private static Color resultFg(String result) {
        if (Protocol.RESULT_WIN.equalsIgnoreCase(result) || "WIN".equalsIgnoreCase(result)) return WIN_FG;
        if (Protocol.RESULT_LOSE.equalsIgnoreCase(result) || "LOSE".equalsIgnoreCase(result)) return LOSE_FG;
        if ("DRAW".equalsIgnoreCase(result) || "HÒA".equalsIgnoreCase(result) || "Hòa".equalsIgnoreCase(result)) return DRAW_FG;
        return UNKNOWN_FG;
    }

    // =========================================================================
    // RENDERER CÁC Ô TRONG BẢNG
    // =========================================================================

    private static Color cellBg(boolean isSelected) {
        return isSelected ? SELECT_BG : Color.WHITE;
    }

    private static class TextRenderer extends DefaultTableCellRenderer {
        private final Font font;
        private final Color color;

        TextRenderer(boolean bold, Color color, int align) {
            this.font = new Font(AppUI.FONT, bold ? Font.BOLD : Font.PLAIN, 13);
            this.color = color;
            setHorizontalAlignment(align);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setFont(font);
            setForeground(color);
            setBackground(cellBg(isSelected));
            setBorder(new EmptyBorder(0, getHorizontalAlignment() == SwingConstants.LEFT ? 14 : 4, 0, 4));
            return this;
        }
    }

    /** Ô "Phe": chấm tròn + Đỏ/Xanh theo màu quân của mình trong trận đó */
    private static class SideRenderer extends DefaultTableCellRenderer {
        SideRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setIconTextGap(6);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            boolean red = Protocol.COLOR_RED.equalsIgnoreCase(String.valueOf(value));
            Color c = red ? GameBoardFrame.COLOR_RED_VAL : GameBoardFrame.COLOR_BLUE_VAL;
            setText(red ? "Đỏ" : "Xanh");
            setIcon(new DotIcon(10, c));
            setFont(new Font(AppUI.FONT, Font.BOLD, 13));
            setForeground(c);
            setBackground(cellBg(isSelected));
            setBorder(null);
            return this;
        }
    }

    /** Ô "Tỉ số": điểm_mình - điểm_đối_thủ, tô màu theo kết quả */
    private static class ScoreRenderer extends DefaultTableCellRenderer {
        ScoreRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setFont(new Font(AppUI.FONT, Font.BOLD, 14));
            setForeground(resultFg(String.valueOf(table.getValueAt(row, COL_RESULT))));
            setBackground(cellBg(isSelected));
            setBorder(null);
            return this;
        }
    }

    /** Ô "Điểm cộng": +10 (xanh lục), -10 (đỏ), +5 (xanh lam) */
    private static class PointsRenderer extends DefaultTableCellRenderer {
        PointsRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setFont(new Font(AppUI.FONT, Font.BOLD, 13));
            String val = String.valueOf(value);
            if (val.startsWith("+10")) {
                setForeground(WIN_FG);
            } else if (val.startsWith("-10")) {
                setForeground(LOSE_FG);
            } else if (val.startsWith("+5")) {
                setForeground(DRAW_FG);
            } else {
                setForeground(TEXT_MUTED);
            }
            setBackground(cellBg(isSelected));
            setBorder(null);
            return this;
        }
    }

    /** Ô "Kết quả": viên thuốc Thắng / Thua / Hòa / Không rõ */
    private static class ResultRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            String result = String.valueOf(value);
            String text;
            Color bg;
            if (Protocol.RESULT_WIN.equalsIgnoreCase(result) || "WIN".equalsIgnoreCase(result)) {
                text = "Thắng";
                bg = WIN_BG;
            } else if (Protocol.RESULT_LOSE.equalsIgnoreCase(result) || "LOSE".equalsIgnoreCase(result)) {
                text = "Thua";
                bg = LOSE_BG;
            } else if ("DRAW".equalsIgnoreCase(result) || "HÒA".equalsIgnoreCase(result) || "Hòa".equalsIgnoreCase(result)) {
                text = "Hòa";
                bg = DRAW_BG;
            } else {
                text = "Không rõ";
                bg = UNKNOWN_BG;
            }

            JPanel pnl = new JPanel(new GridBagLayout());
            pnl.setBackground(cellBg(isSelected));
            JLabel pill = new JLabel(text, SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            pill.setFont(new Font(AppUI.FONT, Font.BOLD, 12));
            pill.setForeground(resultFg(result));
            pill.setBorder(new EmptyBorder(4, 14, 4, 14));
            pnl.add(pill);
            return pnl;
        }
    }

    // =========================================================================
    // THÀNH PHẦN VẼ TAY
    // =========================================================================

    /** Thẻ số liệu: nhãn nhỏ ở trên, con số lớn ở dưới, nền nhạt theo màu nhấn */
    private static class StatTile extends JPanel {
        private final Color bg;

        StatTile(String label, String value, Color accent, Color bg) {
            super(new GridLayout(2, 1, 0, 2));
            this.bg = bg;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 14, 10, 14));

            JLabel lblLabel = new JLabel(label);
            lblLabel.setFont(new Font(AppUI.FONT, Font.BOLD, 11));
            lblLabel.setForeground(TEXT_MUTED);

            JLabel lblValue = new JLabel(value);
            lblValue.setFont(new Font(AppUI.FONT, Font.BOLD, 22));
            lblValue.setForeground(accent);

            add(lblLabel);
            add(lblValue);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class DotIcon implements Icon {
        private final int d;
        private final Color c;

        DotIcon(int d, Color c) {
            this.d = d;
            this.c = c;
        }

        @Override
        public void paintIcon(Component cmp, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c);
            g2.fill(new Ellipse2D.Float(x, y, d, d));
            g2.dispose();
        }

        @Override public int getIconWidth() { return d; }
        @Override public int getIconHeight() { return d; }
    }
}