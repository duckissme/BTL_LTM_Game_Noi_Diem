package client.view;

import common.Protocol;
import client.network.MessageListener;
import client.network.SocketClient;

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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public class LobbyFrame extends JFrame implements MessageListener {

    private final String currentUser;
    private final int currentPoints;
    private final int currentWins;

    private DefaultTableModel modelOnline;
    private DefaultTableModel modelLeaderboard;
    private JTable tblOnline;
    private JTable tblLeaderboard;
    private JLabel lblOnlineBadge;
    private JLabel lblHeaderPoints;
    private JLabel lblHeaderWins;

    // Hộp thoại chờ phản hồi và nhận lời mời
    private JDialog waitingDialog;
    private Timer spinnerTimer;
    private JDialog inviteDialog = null;
    private MatchHistoryDialog historyDialog = null;

    // Bảng màu (dùng chung tông với màn hình Đăng nhập / Đăng ký)
    private static final Color PRIMARY = AppUI.PRIMARY;
    private static final Color ACCENT = AppUI.ACCENT;
    private static final Color TEXT_MAIN = AppUI.TEXT_DARK;
    private static final Color TEXT_MUTED = AppUI.TEXT_MUTED;
    private static final Color BG_APP = new Color(241, 245, 249);        // Nền xám xanh dịu mắt
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_CARD = new Color(226, 232, 240);
    private static final Color HEADER_BG = new Color(248, 250, 252);     // Nền tiêu đề bảng
    private static final Color GRID = new Color(241, 245, 249);
    private static final Color HOVER_BG = new Color(248, 250, 252);      // Dòng đang rê chuột
    private static final Color SELECT_BG = new Color(239, 246, 255);     // Dòng đang chọn
    private static final Color SELF_BG = new Color(245, 243, 255);       // Dòng của chính mình
    private static final String FONT = AppUI.FONT;

    // Màu avatar theo tên người chơi
    private static final Color[] AVATAR_COLORS = {
            new Color(37, 99, 235), new Color(124, 58, 237), new Color(16, 185, 129),
            new Color(245, 158, 11), new Color(236, 72, 153), new Color(14, 165, 233)
    };

    // Khóa lưu thông tin phụ trên JTable
    private static final String HOVER_ROW = "lobby.hoverRow";
    private static final String NAME_COL = "lobby.nameCol";

    public LobbyFrame(String username, int points, int wins) {
        this.currentUser = username;
        this.currentPoints = points;
        this.currentWins = wins;

        initUI();
        try {
            SocketClient.getInstance().setMessageListener(this);
            SocketClient.getInstance().send("GET_LOBBY");
        } catch (Exception ignored) {}
    }

    private void initUI() {
        setTitle("Dots & Boxes Online - Sảnh Chờ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 660);
        setMinimumSize(new Dimension(980, 600));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);

        // 1. THANH HEADER GRADIENT
        root.add(createHeaderBar(), BorderLayout.NORTH);

        // 2. KHU VỰC TRUNG TÂM (CHIA 2 THẺ: ONLINE & BẢNG XẾP HẠNG)
        JPanel pnlCenter = new JPanel(new GridBagLayout());
        pnlCenter.setBackground(BG_APP);
        pnlCenter.setBorder(new EmptyBorder(12, 14, 14, 14));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Cột trái: Online (Chiếm 56%)
        gbc.gridx = 0;
        gbc.weightx = 0.56;
        pnlCenter.add(createOnlineCard(), gbc);

        // Cột phải: BXH (Chiếm 44%)
        gbc.gridx = 1;
        gbc.weightx = 0.44;
        pnlCenter.add(createLeaderboardCard(), gbc);

        root.add(pnlCenter, BorderLayout.CENTER);
        setContentPane(root);
    }

    /**
     * 1. Header: nền gradient, logo bàn cờ, lời chào và các chip Điểm / Thắng / Tài khoản
     */
    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, PRIMARY, w, 0, ACCENT));
                g2.fillRect(0, 0, w, h);

                // Vòng tròn trang trí mờ
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fill(new Ellipse2D.Float(w - 320, -150, 290, 290));
                g2.fill(new Ellipse2D.Float(w * 0.42f, h - 46, 150, 150));
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(18, 26, 18, 26));

        // Trái: Logo + Tên game + Lời chào
        JPanel pnlBrand = new JPanel(new BorderLayout(14, 0));
        pnlBrand.setOpaque(false);
        pnlBrand.add(new JLabel(new DotsLogoIcon(48)), BorderLayout.WEST);

        JPanel pnlTitles = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTitles.setOpaque(false);

        JLabel lblTitle = new JLabel("DOTS & BOXES ONLINE");
        lblTitle.setFont(new Font(FONT, Font.BOLD, 21));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Xin chào " + currentUser + "! Chọn đối thủ và bắt đầu thi đấu.");
        lblSub.setFont(new Font(FONT, Font.PLAIN, 13));
        lblSub.setForeground(new Color(255, 255, 255, 215));

        pnlTitles.add(lblTitle);
        pnlTitles.add(lblSub);
        pnlBrand.add(pnlTitles, BorderLayout.CENTER);
        header.add(pnlBrand, BorderLayout.WEST);

        // Phải: các chip thông tin (icon vector, không dùng Emoji gây ô vuông)
        JPanel pnlProfile = new JPanel(new GridBagLayout());
        pnlProfile.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 10, 0, 0);

        lblHeaderPoints = new JLabel(String.valueOf(currentPoints));
        lblHeaderWins = new JLabel(String.valueOf(currentWins));
        JLabel lblHeaderUser = new JLabel(currentUser);

        // Nút mở Lịch sử trận đấu của chính mình (hộp thoại mở khi Server trả về HISTORY)
        HeaderPillButton btnHistory = new HeaderPillButton("Lịch sử đấu", new HistoryVectorIcon(18, Color.WHITE));
        btnHistory.addActionListener(e -> requestHistory(currentUser));
        pnlProfile.add(btnHistory, gbc);

        pnlProfile.add(createHeaderChip(new StarVectorIcon(20, new Color(252, 211, 77)),
                "ĐIỂM", lblHeaderPoints), gbc);
        pnlProfile.add(createHeaderChip(new TrophyVectorIcon(20, Color.WHITE),
                "SỐ TRẬN", lblHeaderWins), gbc);
        pnlProfile.add(createHeaderChip(new AvatarIcon(32, currentUser, Color.WHITE, PRIMARY),
                "TÀI KHOẢN", lblHeaderUser), gbc);

        header.add(pnlProfile, BorderLayout.EAST);
        return header;
    }

    private JPanel createHeaderChip(Icon icon, String label, JLabel lblV) {
        JPanel chip = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(new Color(255, 255, 255, 38));
                g2.fill(shape);
                g2.setColor(new Color(255, 255, 255, 70));
                g2.draw(shape);
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(6, 10, 6, 16));

        JLabel lblIcon = new JLabel(icon);
        chip.add(lblIcon, BorderLayout.WEST);

        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 0));
        texts.setOpaque(false);

        JLabel lblL = new JLabel(label);
        lblL.setFont(new Font(FONT, Font.BOLD, 10));
        lblL.setForeground(new Color(255, 255, 255, 190));

        lblV.setFont(new Font(FONT, Font.BOLD, 16));
        lblV.setForeground(Color.WHITE);

        texts.add(lblL);
        texts.add(lblV);
        chip.add(texts, BorderLayout.CENTER);
        return chip;
    }

    /**
     * Phần đầu thẻ: ô icon gradient + tiêu đề + mô tả + badge bên phải
     */
    private JPanel createCardHeader(Icon tileIcon, String title, String subtitle, JComponent badge) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(new JLabel(tileIcon), BorderLayout.WEST);

        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 0));
        texts.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font(FONT, Font.BOLD, 16));
        lblTitle.setForeground(TEXT_MAIN);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font(FONT, Font.PLAIN, 12));
        lblSub.setForeground(TEXT_MUTED);

        texts.add(lblTitle);
        texts.add(lblSub);
        header.add(texts, BorderLayout.CENTER);

        // Bọc GridBag để badge luôn căn giữa theo chiều dọc
        JPanel badgeWrap = new JPanel(new GridBagLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);
        header.add(badgeWrap, BorderLayout.EAST);
        return header;
    }

    /**
     * 2. Card bên trái: Bảng người chơi Online
     */
    private JPanel createOnlineCard() {
        RoundedCard card = new RoundedCard(16);
        card.setLayout(new BorderLayout(0, 14));

        lblOnlineBadge = createPillBadge("0 đang online", new Color(220, 252, 231), new Color(21, 128, 61));
        lblOnlineBadge.setIcon(new DotIcon(7, new Color(34, 197, 94)));
        lblOnlineBadge.setIconTextGap(6);

        card.add(createCardHeader(
                new TileIcon(40, new PeopleVectorIcon(20, Color.WHITE), PRIMARY, new Color(14, 165, 233)),
                "Danh sách Online",
                "Thách đấu người đang rỗi  •  Nhấp đúp để xem lịch sử đấu",
                lblOnlineBadge), BorderLayout.NORTH);

        // Bảng dữ liệu
        String[] columns = {"Người chơi", "Điểm", "Số trận", "Trạng thái", "Thao tác"};
        modelOnline = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 4; // Chỉ cho phép tương tác tại cột Thao tác
            }
        };

        tblOnline = createTable(modelOnline, "Chưa có người chơi nào đang online");
        setupTableStyle(tblOnline, 0);

        tblOnline.getColumnModel().getColumn(0).setPreferredWidth(175);
        tblOnline.getColumnModel().getColumn(1).setPreferredWidth(60);
        tblOnline.getColumnModel().getColumn(2).setPreferredWidth(55);
        tblOnline.getColumnModel().getColumn(3).setPreferredWidth(105);
        tblOnline.getColumnModel().getColumn(4).setPreferredWidth(130);

        tblOnline.getColumnModel().getColumn(0).setCellRenderer(new PlayerCellRenderer());
        tblOnline.getColumnModel().getColumn(1).setCellRenderer(new TextCellRenderer(true, TEXT_MAIN));

        // Renderer Trạng thái
        tblOnline.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());

        // Renderer & Editor nút Thách Đấu
        tblOnline.getColumnModel().getColumn(4).setCellRenderer(new ChallengeButtonRenderer());
        tblOnline.getColumnModel().getColumn(4).setCellEditor(new ChallengeButtonEditor(tblOnline, currentUser));
        installHistoryOnDoubleClick(tblOnline, 0, 4);

        card.add(createTableScroll(tblOnline), BorderLayout.CENTER);
        return card;
    }

    /**
     * 3. Card bên phải: Bảng Xếp Hạng
     */
    private JPanel createLeaderboardCard() {
        RoundedCard card = new RoundedCard(16);
        card.setLayout(new BorderLayout(0, 14));

        JLabel lblSys = createPillBadge("Toàn hệ thống", new Color(254, 243, 199), new Color(180, 83, 9));

        card.add(createCardHeader(
                new TileIcon(40, new TrophyVectorIcon(20, Color.WHITE), new Color(245, 158, 11), new Color(234, 88, 12)),
                "Bảng xếp hạng",
                "Cao thủ điểm cao nhất  •  Nhấp đúp để xem lịch sử đấu",
                lblSys), BorderLayout.NORTH);

        String[] columns = {"Hạng", "Tên người chơi", "Điểm", "Số trận"};
        modelLeaderboard = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblLeaderboard = createTable(modelLeaderboard, "Chưa có dữ liệu xếp hạng");
        setupTableStyle(tblLeaderboard, 1);

        tblLeaderboard.getColumnModel().getColumn(0).setPreferredWidth(60);
        tblLeaderboard.getColumnModel().getColumn(1).setPreferredWidth(150);
        tblLeaderboard.getColumnModel().getColumn(2).setPreferredWidth(70);
        tblLeaderboard.getColumnModel().getColumn(3).setPreferredWidth(65);

        // Renderer Huy hiệu mạ vàng, bạc, đồng
        tblLeaderboard.getColumnModel().getColumn(0).setCellRenderer(new RankBadgeRenderer());
        tblLeaderboard.getColumnModel().getColumn(1).setCellRenderer(new PlayerCellRenderer());
        tblLeaderboard.getColumnModel().getColumn(2).setCellRenderer(new TextCellRenderer(true, PRIMARY));
        installHistoryOnDoubleClick(tblLeaderboard, 1, -1);

        card.add(createTableScroll(tblLeaderboard), BorderLayout.CENTER);
        return card;
    }

    /**
     * Bảng có dòng chữ thông báo khi chưa có dữ liệu
     */
    private static JTable createTable(DefaultTableModel model, String emptyText) {
        return new JTable(model) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(new Font(FONT, Font.PLAIN, 13));
                    g2.setColor(AppUI.HINT);
                    int x = (getWidth() - g2.getFontMetrics().stringWidth(emptyText)) / 2;
                    g2.drawString(emptyText, x, 56);
                    g2.dispose();
                }
            }
        };
    }

    private static JScrollPane createTableScroll(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD_BG);
        scrollPane.setBackground(CARD_BG);
        JPanel corner = new JPanel();
        corner.setBackground(HEADER_BG);
        scrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, corner);
        return scrollPane;
    }

    private void setupTableStyle(JTable table, int nameColumn) {
        table.putClientProperty(NAME_COL, nameColumn);
        table.setRowHeight(50);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(GRID);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFont(new Font(FONT, Font.PLAIN, 13));
        table.setForeground(TEXT_MAIN);
        table.setBackground(CARD_BG);
        table.setSelectionBackground(SELECT_BG);
        table.setSelectionForeground(TEXT_MAIN);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setBackground(HEADER_BG);
        header.setDefaultRenderer((t, value, isSelected, hasFocus, row, col) -> {
            boolean left = col == nameColumn;
            JLabel lbl = new JLabel(String.valueOf(value).toUpperCase(), left ? SwingConstants.LEFT : SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(HEADER_BG);
            lbl.setFont(new Font(FONT, Font.BOLD, 11));
            lbl.setForeground(TEXT_MUTED);
            lbl.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER_CARD),
                    new EmptyBorder(0, left ? 14 : 4, 0, 4)));
            return lbl;
        });

        TextCellRenderer centerRenderer = new TextCellRenderer(false, TEXT_MAIN);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Tô sáng dòng đang rê chuột
        MouseAdapter hoverTracker = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHoverRow(table, table.rowAtPoint(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoverRow(table, -1);
            }
        };
        table.addMouseMotionListener(hoverTracker);
        table.addMouseListener(hoverTracker);
    }

    private static void setHoverRow(JTable table, int row) {
        Object old = table.getClientProperty(HOVER_ROW);
        if (old == null || (int) old != row) {
            table.putClientProperty(HOVER_ROW, row);
            table.repaint();
        }
    }

    /**
     * Màu nền của ô theo trạng thái: chọn > hover > dòng của mình > mặc định
     */
    private Color cellBg(JTable table, int row, boolean isSelected) {
        if (isSelected) return SELECT_BG;
        Object hover = table.getClientProperty(HOVER_ROW);
        if (hover != null && (int) hover == row) return HOVER_BG;
        if (isSelfRow(table, row)) return SELF_BG;
        return CARD_BG;
    }

    private boolean isSelfRow(JTable table, int row) {
        Object col = table.getClientProperty(NAME_COL);
        return col != null && currentUser.equalsIgnoreCase(String.valueOf(table.getValueAt(row, (int) col)));
    }

    private static boolean isBusy(String status) {
        if (status == null) return false;
        String s = status.toLowerCase();
        return s.contains("bận") || s.contains("busy");
    }

    private static Color avatarColor(String name) {
        return AVATAR_COLORS[Math.floorMod(name.hashCode(), AVATAR_COLORS.length)];
    }

    private static JLabel createPillBadge(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
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
        lbl.setFont(new Font(FONT, Font.BOLD, 11));
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
        return lbl;
    }

    // =========================================================================
    // RENDERER CÁC Ô TRONG BẢNG
    // =========================================================================

    /**
     * Ô chữ căn giữa, nền theo hover / chọn / dòng của mình
     */
    private class TextCellRenderer extends DefaultTableCellRenderer {
        private final Font font;
        private final Color color;

        TextCellRenderer(boolean bold, Color color) {
            this.font = new Font(FONT, bold ? Font.BOLD : Font.PLAIN, 13);
            this.color = color;
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setFont(font);
            setForeground(color);
            setBackground(cellBg(table, row, isSelected));
            setBorder(null);
            return this;
        }
    }

    /**
     * Ô tên người chơi: avatar tròn + tên
     */
    private class PlayerCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            String name = String.valueOf(value);
            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.X_AXIS));
            pnl.setBackground(cellBg(table, row, isSelected));
            pnl.setBorder(new EmptyBorder(0, 14, 0, 6));

            JLabel lbl = new JLabel(name, new AvatarIcon(30, name, avatarColor(name), Color.WHITE), SwingConstants.LEFT);
            lbl.setIconTextGap(10);
            lbl.setFont(new Font(FONT, Font.BOLD, 13));
            // Tên của chính mình tô màu nhấn (nền dòng đã được tô tím nhạt)
            lbl.setForeground(currentUser.equalsIgnoreCase(name) ? ACCENT : TEXT_MAIN);
            // Cho phép tên co lại (hiện "...") khi cột hẹp, avatar vẫn giữ nguyên vị trí
            lbl.setMinimumSize(new Dimension(48, lbl.getPreferredSize().height));
            pnl.add(lbl);
            pnl.add(Box.createHorizontalGlue());
            return pnl;
        }
    }

    private class StatusBadgeRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            boolean busy = isBusy(String.valueOf(value));
            JPanel pnl = new JPanel(new GridBagLayout());
            pnl.setBackground(cellBg(table, row, isSelected));

            JLabel pill = busy
                    ? createPillBadge("Đang bận", new Color(254, 226, 226), new Color(185, 28, 28))
                    : createPillBadge("Đang rỗi", new Color(220, 252, 231), new Color(21, 128, 61));
            pill.setIcon(new DotIcon(7, busy ? new Color(239, 68, 68) : new Color(34, 197, 94)));
            pill.setIconTextGap(6);
            pnl.add(pill);
            return pnl;
        }
    }

    private class RankBadgeRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            String rank = String.valueOf(value);
            JPanel pnl = new JPanel(new GridBagLayout());
            pnl.setBackground(cellBg(table, row, isSelected));

            Color medal;
            switch (rank) {
                case "1": medal = new Color(245, 158, 11); break;  // Vàng Gold
                case "2": medal = new Color(148, 163, 184); break;  // Bạc Silver
                case "3": medal = new Color(217, 119, 6); break;    // Đồng Bronze
                default: medal = null; break;
            }

            JLabel badge = new JLabel(rank, SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    if (medal != null) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setPaint(new GradientPaint(0, 0, medal.brighter(), 0, getHeight(), medal));
                        g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            badge.setPreferredSize(new Dimension(28, 28));
            badge.setFont(new Font(FONT, Font.BOLD, 12));
            badge.setForeground(medal != null ? Color.WHITE : TEXT_MUTED);
            pnl.add(badge);
            return pnl;
        }
    }

    // =========================================================================
    // NÚT "THÁCH ĐẤU": NỀN GRADIENT BO TRÒN, ICON KIẾM VECTOR
    // =========================================================================

    /**
     * Nút dạng viên thuốc trong ô bảng, tự vẽ toàn bộ (nền, icon, chữ)
     */
    private static class ActionPillButton extends JButton {
        private final Icon swordsIcon = new SwordsVectorIcon(13, Color.WHITE);
        private boolean busy;
        private Color cellBackground = CARD_BG;

        ActionPillButton() {
            super("Thách đấu");
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(true);
            setFont(new Font(FONT, Font.BOLD, 12));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        void update(boolean busy, Color cellBackground) {
            this.busy = busy;
            this.cellBackground = cellBackground;
            setText(busy ? "Đang bận" : "Thách đấu");
            setCursor(busy ? Cursor.getDefaultCursor() : new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(cellBackground);
            g2.fillRect(0, 0, w, h);

            int pw = Math.min(w - 20, 124);
            int ph = 32;
            int px = (w - pw) / 2;
            int py = (h - ph) / 2;
            RoundRectangle2D pill = new RoundRectangle2D.Float(px, py, pw, ph, ph, ph);

            Color textColor;
            Icon icon = null;
            if (busy) {
                g2.setColor(new Color(241, 245, 249));
                g2.fill(pill);
                textColor = new Color(148, 163, 184);
            } else {
                Color start = PRIMARY;
                Color end = ACCENT;
                if (getModel().isPressed()) {
                    start = PRIMARY.darker();
                    end = ACCENT.darker();
                } else if (getModel().isRollover()) {
                    start = new Color(59, 130, 246);
                    end = new Color(139, 92, 246);
                }
                g2.setPaint(new GradientPaint(px, 0, start, px + pw, 0, end));
                g2.fill(pill);
                textColor = Color.WHITE;
                icon = swordsIcon;
            }

            // Icon + chữ căn giữa viên thuốc
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int gap = 7;
            int iconW = icon != null ? icon.getIconWidth() + gap : 0;
            int contentX = (w - (iconW + fm.stringWidth(getText()))) / 2;
            if (icon != null) {
                icon.paintIcon(this, g2, contentX, (h - icon.getIconHeight()) / 2);
            }
            g2.setColor(textColor);
            g2.drawString(getText(), contentX + iconW, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private class ChallengeButtonRenderer implements TableCellRenderer {
        private final ActionPillButton button = new ActionPillButton();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            String status = String.valueOf(table.getValueAt(row, 3));
            button.update(isBusy(status), cellBg(table, row, isSelected));
            return button;
        }
    }

    private class ChallengeButtonEditor extends DefaultCellEditor {
        private final ActionPillButton button;
        private int selectedRow;

        public ChallengeButtonEditor(JTable table, String selfUser) {
            super(new JCheckBox());
            button = new ActionPillButton();

            button.addActionListener(e -> {
                fireEditingStopped();
                String targetUser = String.valueOf(table.getValueAt(selectedRow, 0));
                String status = String.valueOf(table.getValueAt(selectedRow, 3));

                if (targetUser.equalsIgnoreCase(selfUser)) {
                    JOptionPane.showMessageDialog(table, "Bạn không thể tự thách đấu chính mình!",
                            "Nhắc nhở", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (isBusy(status)) {
                    JOptionPane.showMessageDialog(table, "Đối thủ [" + targetUser + "] hiện đang bận!",
                            "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String packet = Protocol.CMD_CHALLENGE_REQ + Protocol.DELIMITER + targetUser;
                // Chỉ mở hộp thoại chờ khi lời mời thực sự đã được gửi đi
                if (!SocketClient.getInstance().send(packet)) {
                    AppUI.showConnectionError(table);
                    return;
                }
                showWaitingChallengeDialog(targetUser);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.selectedRow = row;
            boolean busy = isBusy(String.valueOf(table.getValueAt(row, 3)));
            button.setEnabled(!busy);
            button.update(busy, cellBg(table, row, isSelected));
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Thao tác";
        }
    }

    // =========================================================================
    // XỬ LÝ GÓI TIN MẠNG
    // =========================================================================

    /**
     * Xử lý gói tin từ Server.
     * SocketClient đã chuyển sẵn sang luồng giao diện (EDT) nên cập nhật bảng/hộp thoại trực tiếp được.
     */
    @Override
    public void onMessageReceived(String message) {
        if (message == null || message.trim().isEmpty()) return;

        String[] parts = message.split(Protocol.DELIMITER);
        String cmd = parts[0];

        if (Protocol.CMD_LOGIN_SUCCESS.equals(cmd)) {
            if (parts.length >= 4) {
                if (lblHeaderPoints != null) lblHeaderPoints.setText(parts[2].trim());
                if (lblHeaderWins != null) lblHeaderWins.setText(parts[3].trim());
            }
        } else if (Protocol.CMD_LOBBY_DATA.equals(cmd)) {
            if (parts.length > 1) updateOnlineTable(parts[1]);
            else updateOnlineTable("");
        } else if (Protocol.CMD_LEADERBOARD.equals(cmd)) {
            if (parts.length > 1) updateLeaderboardTable(parts[1]);
        } else if (Protocol.CMD_CHALLENGE_INVITE.equals(cmd)) {
            if (parts.length > 1) showChallengeInviteDialog(parts[1]);
        } else if (Protocol.CMD_CHALLENGE_CANCEL.equals(cmd)) {
            closeChallengeInviteDialog();
            JOptionPane.showMessageDialog(this, "Đối thủ đã hủy yêu cầu thách đấu!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } else if (Protocol.CMD_CHALLENGE_RESP.equals(cmd)) {
            closeWaitingDialog();
            if (parts.length >= 3 && Protocol.RESP_REJECT.equals(parts[2])) {
                JOptionPane.showMessageDialog(this, "Người chơi [" + parts[1] + "] đã từ chối thách đấu!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (Protocol.CMD_HISTORY.equals(cmd)) {
            // HISTORY;tên;tổng_kết;danh_sách — tách tối đa 4 phần, danh sách có thể trống
            String[] h = message.split(Protocol.DELIMITER, 4);
            String viewedUser = h.length > 1 ? h[1] : currentUser;
            closeHistoryDialog();
            historyDialog = new MatchHistoryDialog(this, viewedUser, viewedUser.equalsIgnoreCase(currentUser),
                    h.length > 2 ? h[2] : "", h.length > 3 ? h[3] : "", this::requestHistory);
            historyDialog.setVisible(true);
        } else if (Protocol.CMD_HISTORY_FAIL.equals(cmd)) {
            JOptionPane.showMessageDialog(historyDialog != null ? historyDialog : this,
                    parts.length > 1 ? parts[1] : "Không tải được lịch sử trận đấu!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        } else if (Protocol.CMD_START_GAME.equals(cmd)) {
            closeWaitingDialog();
            closeChallengeInviteDialog();
            closeHistoryDialog();
            String opponent = parts.length > 1 ? parts[1] : "Đối thủ";
            String myColor = parts.length > 2 ? parts[2] : Protocol.COLOR_RED;
            String startingTurn = parts.length > 3 ? parts[3] : Protocol.COLOR_RED;
            this.setVisible(false);
            GameBoardFrame gameBoard = new GameBoardFrame(currentUser, opponent, myColor, startingTurn, this);
            gameBoard.setVisible(true);
        }
    }

    /**
     * Mất kết nối khi đang ở sảnh: đóng sảnh và quay về màn hình Đăng nhập.
     * Khi người dùng đăng nhập lại, LoginFrame sẽ tự kết nối lại tới Server.
     */
    @Override
    public void onDisconnected(String reason) {
        closeWaitingDialog();
        closeChallengeInviteDialog();
        JOptionPane.showMessageDialog(this,
                "Mất kết nối tới Server!\nVui lòng đăng nhập lại.",
                "Mất kết nối", JOptionPane.WARNING_MESSAGE);
        dispose(); // đóng luôn các hộp thoại con (vd: lời mời thách đấu đang mở)
        new LoginFrame().setVisible(true); // LoginFrame tự đăng ký nhận gói tin trong constructor
    }

    /**
     * Nạp dữ liệu danh sách người chơi trực tuyến vào bảng Online và cập nhật số lượng badge.
     */
    public void updateOnlineTable(String rawData) {
        modelOnline.setRowCount(0);
        if (rawData.trim().isEmpty()) {
            lblOnlineBadge.setText("0 đang online");
            return;
        }

        String[] userRecords = rawData.split(Protocol.LIST_DELIMITER);
        int count = 0;
        for (String record : userRecords) {
            String[] fields = record.split(",");
            if (fields.length >= 4) {
                String uname = fields[0].trim();
                if (uname.equalsIgnoreCase(currentUser)) {
                    if (lblHeaderPoints != null) lblHeaderPoints.setText(fields[1].trim());
                    if (lblHeaderWins != null) lblHeaderWins.setText(fields[2].trim());
                }
                count++;
                modelOnline.addRow(new Object[]{
                        uname,
                        fields[1].trim(),
                        fields[2].trim(),
                        fields[3].trim(),
                        "Thao tác"
                });
            }
        }
        lblOnlineBadge.setText(count + " đang online");
    }

    /**
     * Nạp dữ liệu bảng xếp hạng cao thủ từ Server vào modelLeaderboard.
     */
    public void updateLeaderboardTable(String rawData) {
        modelLeaderboard.setRowCount(0);
        if (rawData.trim().isEmpty()) return;

        String[] rankRecords = rawData.split(Protocol.LIST_DELIMITER);
        int rank = 1;
        for (String record : rankRecords) {
            String[] fields = record.split(",");
            if (fields.length >= 3) {
                if (fields[0].trim().equalsIgnoreCase(currentUser)) {
                    if (lblHeaderPoints != null) lblHeaderPoints.setText(fields[1].trim());
                    if (lblHeaderWins != null) lblHeaderWins.setText(fields[2].trim());
                }
                modelLeaderboard.addRow(new Object[]{
                        String.valueOf(rank++),
                        fields[0].trim(),
                        fields[1].trim(),
                        fields[2].trim()
                });
            }
        }
    }

    // =========================================================================
    // HỘP THOẠI THÁCH ĐẤU
    // =========================================================================

    private static JLabel createDialogLabel(String text, int style, int size, Color color) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font(FONT, style, size));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private static JPanel createDialogBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(24, 24, 8, 24));
        return body;
    }

    private static JPanel createDialogButtons() {
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        pnlBtns.setBackground(Color.WHITE);
        pnlBtns.setBorder(new EmptyBorder(8, 0, 22, 0));
        return pnlBtns;
    }

    private void showChallengeInviteDialog(String challenger) {
        closeChallengeInviteDialog();

        inviteDialog = new JDialog(this, "Lời mời thách đấu", true);
        inviteDialog.setSize(420, 290);
        inviteDialog.setLocationRelativeTo(this);
        inviteDialog.setResizable(false);
        inviteDialog.getContentPane().setBackground(Color.WHITE);
        inviteDialog.setLayout(new BorderLayout());

        // Đóng bằng nút X coi như từ chối
        inviteDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        inviteDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeChallengeInviteDialog();
                SocketClient.getInstance().send(Protocol.CMD_CHALLENGE_RESP + Protocol.DELIMITER
                        + challenger + Protocol.DELIMITER + Protocol.RESP_REJECT);
            }
        });

        JPanel body = createDialogBody();
        JLabel lblIcon = new JLabel(new TileIcon(56, new SwordsVectorIcon(24, Color.WHITE), PRIMARY, ACCENT));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(lblIcon);
        body.add(Box.createVerticalStrut(16));
        body.add(createDialogLabel("Người chơi [" + challenger + "] gửi lời thách đấu!", Font.BOLD, 16, TEXT_MAIN));
        body.add(Box.createVerticalStrut(6));
        body.add(createDialogLabel("Bạn có sẵn sàng tham gia ván đấu ngay bây giờ?", Font.PLAIN, 13, TEXT_MUTED));

        JPanel pnlBtns = createDialogButtons();

        AppUI.GradientButton btnAccept = new AppUI.GradientButton("Chấp nhận");
        btnAccept.setFont(new Font(FONT, Font.BOLD, 14));
        btnAccept.setPreferredSize(new Dimension(150, 42));
        btnAccept.addActionListener(e -> {
            closeChallengeInviteDialog();
            if (!SocketClient.getInstance().send(Protocol.CMD_CHALLENGE_RESP + Protocol.DELIMITER
                    + challenger + Protocol.DELIMITER + Protocol.RESP_ACCEPT)) {
                AppUI.showConnectionError(this);
            }
        });

        SoftButton btnReject = SoftButton.danger("Từ chối");
        btnReject.addActionListener(e -> {
            closeChallengeInviteDialog();
            if (!SocketClient.getInstance().send(Protocol.CMD_CHALLENGE_RESP + Protocol.DELIMITER
                    + challenger + Protocol.DELIMITER + Protocol.RESP_REJECT)) {
                AppUI.showConnectionError(this);
            }
        });

        pnlBtns.add(btnAccept);
        pnlBtns.add(btnReject);

        inviteDialog.add(body, BorderLayout.CENTER);
        inviteDialog.add(pnlBtns, BorderLayout.SOUTH);
        inviteDialog.setVisible(true);
    }

    /**
     * Gửi GET_HISTORY;tên lên Server. Hộp thoại chỉ mở khi Server trả về HISTORY.
     */
    private void requestHistory(String username) {
        if (!SocketClient.getInstance().send(Protocol.CMD_GET_HISTORY + Protocol.DELIMITER + username)) {
            AppUI.showConnectionError(historyDialog != null ? historyDialog : this);
        }
    }

    /**
     * Nhấp đúp vào một người chơi trong bảng để xem lịch sử đấu của người đó.
     *
     * @param skipColumn cột không xử lý nhấp đúp (cột nút Thách đấu), -1 nếu không có
     */
    private void installHistoryOnDoubleClick(JTable table, int nameColumn, int skipColumn) {
        table.setToolTipText("Nhấp đúp để xem lịch sử đấu của người chơi");
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0 || table.columnAtPoint(e.getPoint()) == skipColumn) return;
                requestHistory(String.valueOf(table.getValueAt(row, nameColumn)));
            }
        });
    }

    private void closeHistoryDialog() {
        if (historyDialog != null) {
            historyDialog.dispose();
            historyDialog = null;
        }
    }

    public void closeChallengeInviteDialog() {
        if (inviteDialog != null) {
            inviteDialog.setVisible(false);
            inviteDialog.dispose();
            inviteDialog = null;
        }
    }

    public void showWaitingChallengeDialog(String targetUser) {
        closeWaitingDialog();

        waitingDialog = new JDialog(this, "Đang chờ đối thủ", true);
        waitingDialog.setSize(420, 290);
        waitingDialog.setLocationRelativeTo(this);
        waitingDialog.getContentPane().setBackground(Color.WHITE);
        waitingDialog.setLayout(new BorderLayout());
        waitingDialog.setResizable(false);

        waitingDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        waitingDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelChallenge(targetUser);
            }
        });

        JPanel body = createDialogBody();
        JLabel lblIcon = new JLabel(new TileIcon(56, new AvatarIcon(30, targetUser, Color.WHITE, PRIMARY), PRIMARY, ACCENT));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblStatus = createDialogLabel("Đang gửi lời mời tới [" + targetUser + "]", Font.BOLD, 15, TEXT_MAIN);
        DotsLoader loader = new DotsLoader();
        loader.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lblSub = createDialogLabel("Vui lòng đợi đối thủ phản hồi trong giây lát...", Font.PLAIN, 13, TEXT_MUTED);

        body.add(lblIcon);
        body.add(Box.createVerticalStrut(16));
        body.add(lblStatus);
        body.add(Box.createVerticalStrut(10));
        body.add(loader);
        body.add(Box.createVerticalStrut(8));
        body.add(lblSub);

        final int[] dotCount = {0};
        spinnerTimer = new Timer(400, e -> {
            dotCount[0] = (dotCount[0] + 1) % 4;
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < dotCount[0]; i++) dots.append(".");
            lblStatus.setText("Đang gửi lời mời tới [" + targetUser + "]" + dots);
            loader.setPhase(dotCount[0]);
        });
        spinnerTimer.start();

        JPanel pnlButton = createDialogButtons();
        SoftButton btnCancel = SoftButton.danger("Hủy lời mời");
        btnCancel.addActionListener(e -> cancelChallenge(targetUser));
        pnlButton.add(btnCancel);

        waitingDialog.add(body, BorderLayout.CENTER);
        waitingDialog.add(pnlButton, BorderLayout.SOUTH);
        waitingDialog.setVisible(true);
    }

    private void cancelChallenge(String targetUser) {
        // Gửi không được (đã mất kết nối) thì cũng không sao: Server sẽ tự hủy lời mời khi Client rớt mạng
        SocketClient.getInstance().send(Protocol.CMD_CHALLENGE_CANCEL + Protocol.DELIMITER + targetUser);
        closeWaitingDialog();
    }

    public void closeWaitingDialog() {
        if (spinnerTimer != null && spinnerTimer.isRunning()) {
            spinnerTimer.stop();
            spinnerTimer = null;
        }
        if (waitingDialog != null) {
            waitingDialog.setVisible(false);
            waitingDialog.dispose();
            waitingDialog = null;
        }
    }

    /**
     * Nút phụ nền nhạt có viền (dùng cho Từ chối / Hủy)
     */
    private static class SoftButton extends JButton {
        private final Color bg;
        private final Color hoverBg;
        private final Color fg;
        private final Color line;

        SoftButton(String text, Color bg, Color hoverBg, Color fg, Color line) {
            super(text);
            this.bg = bg;
            this.hoverBg = hoverBg;
            this.fg = fg;
            this.line = line;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(new Font(FONT, Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(150, 42));
        }

        static SoftButton danger(String text) {
            return new SoftButton(text, new Color(254, 242, 242), new Color(254, 226, 226),
                    new Color(185, 28, 28), new Color(252, 165, 165));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(getModel().isRollover() || getModel().isPressed() ? hoverBg : bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12));
            g2.setColor(line);
            g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12));

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(fg);
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /**
     * Nút trên header gradient: nền trắng trong suốt giống các chip thông tin, sáng hơn khi rê chuột
     */
    private static class HeaderPillButton extends JButton {
        HeaderPillButton(String text, Icon icon) {
            super(text, icon);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font(FONT, Font.BOLD, 13));
            setIconTextGap(8);
            setBorder(new EmptyBorder(12, 16, 12, 18));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            int alpha = getModel().isPressed() ? 80 : (getModel().isRollover() ? 62 : 38);
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fill(shape);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.draw(shape);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Icon Lịch sử vector: đồng hồ với mũi tên quay ngược
     */
    private static class HistoryVectorIcon implements Icon {
        private final int size;
        private final Color color;

        HistoryVectorIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(size * 0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            double pad = size * 0.12;
            double d = size - pad * 2;
            // Vòng tròn hở một đoạn ở góc trên bên trái
            g2.draw(new Arc2D.Double(x + pad, y + pad, d, d, 150, -300, Arc2D.OPEN));

            // Mũi tên ở đầu cung (vị trí 150 độ)
            double cx = x + size / 2.0;
            double cy = y + size / 2.0;
            double r = d / 2;
            double ax = cx + r * Math.cos(Math.toRadians(150));
            double ay = cy - r * Math.sin(Math.toRadians(150));
            Path2D arrow = new Path2D.Double();
            arrow.moveTo(ax - size * 0.2, ay - size * 0.02);
            arrow.lineTo(ax, ay);
            arrow.lineTo(ax + size * 0.04, ay - size * 0.2);
            g2.draw(arrow);

            // Kim đồng hồ
            g2.draw(new Line2D.Double(cx, cy, cx, cy - r * 0.55));
            g2.draw(new Line2D.Double(cx, cy, cx + r * 0.42, cy + r * 0.28));
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /**
     * Ba chấm nhấp nháy báo đang chờ
     */
    private static class DotsLoader extends JComponent {
        private int phase;

        DotsLoader() {
            Dimension size = new Dimension(60, 12);
            setPreferredSize(size);
            setMaximumSize(size);
        }

        void setPhase(int phase) {
            this.phase = phase;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = 10;
            int gap = 8;
            int x = (getWidth() - (3 * d + 2 * gap)) / 2;
            for (int i = 0; i < 3; i++) {
                boolean active = i == phase % 3;
                g2.setColor(active ? PRIMARY : new Color(191, 219, 254));
                g2.fill(new Ellipse2D.Float(x + i * (d + gap), (getHeight() - d) / 2f, d, d));
            }
            g2.dispose();
        }
    }

    // =========================================================================
    // CÁC LỚP VẼ ĐỒ HỌA VECTOR TỰ ĐỘNG (CHỐNG LỖI FONT 100%)
    // =========================================================================

    /**
     * Icon Ngôi sao vector
     */
    private static class StarVectorIcon implements Icon {
        private final int size;
        private final Color color;

        public StarVectorIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);

            double midX = x + size / 2.0;
            double midY = y + size / 2.0 + size * 0.04;
            double rOuter = size / 2.0;
            double rInner = size / 4.4;
            Path2D star = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double angle = i * Math.PI / 5.0 - Math.PI / 2.0;
                double r = (i % 2 == 0) ? rOuter : rInner;
                double px = midX + r * Math.cos(angle);
                double py = midY + r * Math.sin(angle);
                if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
            }
            star.closePath();
            g2.fill(star);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /**
     * Icon Cúp chiến thắng vector
     */
    private static class TrophyVectorIcon implements Icon {
        private final int size;
        private final Color color;

        public TrophyVectorIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            float s = size;

            // Thân cúp: miệng phẳng, đáy bo tròn
            g2.fill(new Rectangle2D.Float(x + s * 0.24f, y + s * 0.04f, s * 0.52f, s * 0.22f));
            g2.fill(new Arc2D.Float(x + s * 0.24f, y - s * 0.12f, s * 0.52f, s * 0.72f, 180, 180, Arc2D.CHORD));
            // 2 quai cúp
            g2.setStroke(new BasicStroke(Math.max(1.4f, s / 11f)));
            g2.draw(new Arc2D.Float(x + s * 0.06f, y + s * 0.1f, s * 0.3f, s * 0.28f, 90, 180, Arc2D.OPEN));
            g2.draw(new Arc2D.Float(x + s * 0.64f, y + s * 0.1f, s * 0.3f, s * 0.28f, 270, 180, Arc2D.OPEN));
            // Chân + đế cúp
            g2.fill(new Rectangle2D.Float(x + s * 0.44f, y + s * 0.58f, s * 0.12f, s * 0.2f));
            g2.fill(new RoundRectangle2D.Float(x + s * 0.24f, y + s * 0.78f, s * 0.52f, s * 0.16f, 3, 3));
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /**
     * Icon 2 thanh kiếm đan chéo vector
     */
    private static class SwordsVectorIcon implements Icon {
        private final int size;
        private final Color color;

        public SwordsVectorIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.8f, s / 8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Lưỡi kiếm chéo
            g2.draw(new Line2D.Float(x + s * 0.08f, y + s * 0.08f, x + s * 0.92f, y + s * 0.92f));
            g2.draw(new Line2D.Float(x + s * 0.92f, y + s * 0.08f, x + s * 0.08f, y + s * 0.92f));

            // Chắn tay
            g2.draw(new Line2D.Float(x + s * 0.62f, y + s * 0.92f, x + s * 0.92f, y + s * 0.62f));
            g2.draw(new Line2D.Float(x + s * 0.08f, y + s * 0.62f, x + s * 0.38f, y + s * 0.92f));
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /**
     * Icon người dùng vector (đầu + vai)
     */
    private static class PeopleVectorIcon implements Icon {
        private final int size;
        private final Color color;

        public PeopleVectorIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            float s = size;
            g2.fill(new Ellipse2D.Float(x + s * 0.3f, y + s * 0.02f, s * 0.4f, s * 0.4f));
            g2.fill(new Arc2D.Float(x + s * 0.08f, y + s * 0.5f, s * 0.84f, s * 0.9f, 0, 180, Arc2D.CHORD));
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /**
     * Chấm tròn trạng thái Online / Busy
     */
    private static class DotIcon implements Icon {
        private final int diameter;
        private final Color color;

        public DotIcon(int diameter, Color color) {
            this.diameter = diameter;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fill(new Ellipse2D.Float(x, y, diameter, diameter));
            g2.dispose();
        }

        @Override public int getIconWidth() { return diameter; }
        @Override public int getIconHeight() { return diameter; }
    }

    /**
     * Avatar tròn hiển thị chữ cái đầu của tên
     */
    private static class AvatarIcon implements Icon {
        private final int size;
        private final String initial;
        private final Color bg;
        private final Color fg;

        public AvatarIcon(int size, String name, Color bg, Color fg) {
            this.size = size;
            String trimmed = name == null ? "" : name.trim();
            this.initial = trimmed.isEmpty() ? "U" : trimmed.substring(0, 1).toUpperCase();
            this.bg = bg;
            this.fg = fg;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new Ellipse2D.Float(x, y, size, size));
            g2.setFont(new Font(FONT, Font.BOLD, Math.round(size * 0.45f)));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(fg);
            g2.drawString(initial, x + (size - fm.stringWidth(initial)) / 2,
                    y + (size - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /**
     * Ô vuông bo góc nền gradient chứa một icon nhỏ ở giữa
     */
    private static class TileIcon implements Icon {
        private final int size;
        private final Icon inner;
        private final Color start;
        private final Color end;

        public TileIcon(int size, Icon inner, Color start, Color end) {
            this.size = size;
            this.inner = inner;
            this.start = start;
            this.end = end;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(x, y, start, x + size, y + size, end));
            g2.fill(new RoundRectangle2D.Float(x, y, size, size, size * 0.35f, size * 0.35f));
            inner.paintIcon(c, g2, x + (size - inner.getIconWidth()) / 2, y + (size - inner.getIconHeight()) / 2);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /**
     * Logo bàn cờ Dots & Boxes thu nhỏ trên header
     */
    private static class DotsLogoIcon implements Icon {
        private final int size;

        public DotsLogoIcon(int size) {
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            RoundRectangle2D tile = new RoundRectangle2D.Float(x, y, size - 1, size - 1, 16, 16);
            g2.setColor(new Color(255, 255, 255, 45));
            g2.fill(tile);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.draw(tile);

            float pad = size * 0.25f;
            float step = (size - 2 * pad) / 2f;

            // Ô đã chiếm
            g2.setColor(new Color(251, 191, 36, 210));
            g2.fill(new Rectangle2D.Float(x + pad + 2, y + pad + 2, step - 4, step - 4));

            // Cạnh đã nối: {cột1, hàng1, cột2, hàng2}
            int[][] edges = {{0, 0, 1, 0}, {0, 0, 0, 1}, {1, 0, 1, 1}, {0, 1, 1, 1}, {1, 1, 2, 1}, {2, 1, 2, 2}};
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int[] e : edges) {
                g2.draw(new Line2D.Float(x + pad + e[0] * step, y + pad + e[1] * step,
                        x + pad + e[2] * step, y + pad + e[3] * step));
            }

            // Các điểm 3x3
            float d = 5.5f;
            for (int r = 0; r < 3; r++) {
                for (int col = 0; col < 3; col++) {
                    g2.fill(new Ellipse2D.Float(x + pad + col * step - d / 2, y + pad + r * step - d / 2, d, d));
                }
            }
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /**
     * Thẻ trắng bo góc có bóng đổ mềm
     */
    private static class RoundedCard extends JPanel {
        private static final int SHADOW = 6;
        private static final int RADIUS = 18;

        public RoundedCard(int padding) {
            setOpaque(false);
            setBorder(new EmptyBorder(padding + SHADOW - 1, padding + SHADOW, padding + SHADOW + 1, padding + SHADOW));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Bóng đổ: các lớp mờ dần ra ngoài, lệch xuống dưới
            for (int i = 0; i < SHADOW; i++) {
                g2.setColor(new Color(15, 23, 42, 3 + i));
                g2.fill(new RoundRectangle2D.Float(i, i + 2, w - 2 * i, h - 2 * i - 2,
                        RADIUS + (SHADOW - i) * 2, RADIUS + (SHADOW - i) * 2));
            }

            RoundRectangle2D card = new RoundRectangle2D.Float(SHADOW, SHADOW - 1, w - 2 * SHADOW, h - 2 * SHADOW, RADIUS, RADIUS);
            g2.setColor(CARD_BG);
            g2.fill(card);
            g2.setColor(BORDER_CARD);
            g2.draw(card);
            g2.dispose();
        }
    }

    // =========================================================================
    // HÀM MAIN TEST TRỰC TIẾP GIAO DIỆN
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            LobbyFrame lobby = new LobbyFrame("ghghg1", 1200, 45);
            lobby.setVisible(true);

            // Dữ liệu mô phỏng
            lobby.updateOnlineTable("Username A,1200,45,Đang rỗi|" +
                                   "Username B,950,32,Đang bận|" +
                                   "Username C,1100,40,Đang rỗi|" +
                                   "Username D,800,25,Đang rỗi");

            lobby.updateLeaderboardTable("TopPlayer1,2500,110|" +
                                        "UserX,2300,98|" +
                                        "Competitory,2100,85|" +
                                        "Username A,1200,45");
        });
    }
}
