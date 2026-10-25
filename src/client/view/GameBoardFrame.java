package client.view;

import common.Protocol;
import client.network.MessageListener;
import client.network.SocketClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Cửa sổ Trận Đấu (Game Arena) - Thành viên 4: Minh.
 *
 * Bản chất: Dumb View — Chỉ vẽ và hiển thị dữ liệu nhận từ Server,
 * không tự tính điểm, không tự quyết định thắng thua hay tính hợp lệ của nước đi.
 *
 * Cấu trúc 3 tầng:
 *  1. Tầng trên (Top Bar): Thông tin 2 người chơi (Đỏ/Xanh), ô chiếm được và Tỉ số.
 *  2. Tầng giữa (Game Board): Bàn cờ 5x5 ô (6x6 dấu chấm = 36 chấm), nhận click gửi MOVE,
 *     nhận DRAW_LINE / CAPTURE_BOX từ Server để vẽ lại.
 *     Bên phải là khung chat (ChatPanel): gửi CHAT, chỉ hiển thị tin khi nhận CHAT_MSG từ Server.
 *  3. Tầng dưới (Bottom Bar): Hiển thị lượt hiện tại, đồng hồ đếm ngược 30s và nút "Thoát".
 */
public class GameBoardFrame extends JFrame implements MessageListener {

    // Kích thước bàn cờ chuẩn theo thiết kế: 5x5 ô = 6x6 điểm
    public static final int BOX_ROWS = 5;
    public static final int BOX_COLS = 5;
    public static final int DOT_ROWS = BOX_ROWS + 1; // 6
    public static final int DOT_COLS = BOX_COLS + 1; // 6

    // Màu sắc theo bảng màu chuẩn của dự án (AppUI & LobbyFrame)
    private static final Color PRIMARY = AppUI.PRIMARY;
    private static final Color TEXT_MAIN = AppUI.TEXT_DARK;
    private static final Color TEXT_MUTED = AppUI.TEXT_MUTED;
    private static final Color BG_APP = new Color(248, 250, 252);
    private static final Color BORDER_LIGHT = new Color(226, 232, 240);

    // Màu phe Đỏ và Xanh
    public static final Color COLOR_RED_VAL = new Color(239, 68, 68);       // Đỏ tươi
    public static final Color COLOR_BLUE_VAL = new Color(37, 99, 235);      // Xanh dương tươi
    static final Color COLOR_RED_BG = new Color(254, 242, 242);     // Đỏ nhạt cho box điểm & bong bóng chat
    static final Color COLOR_RED_BORDER = new Color(252, 165, 165);
    static final Color COLOR_BLUE_BG = new Color(239, 246, 255);    // Xanh nhạt cho box điểm & bong bóng chat
    static final Color COLOR_BLUE_BORDER = new Color(191, 219, 254);

    private static final Color DOT_COLOR = new Color(30, 41, 59);           // Màu chấm tròn (Slate-800)
    private static final Color GRID_LINE_COLOR = new Color(203, 213, 225);  // Cạnh chưa vẽ
    private static final Color HOVER_COLOR = new Color(148, 163, 184, 180); // Cạnh hover

    // Thông tin người chơi & phòng
    private final String currentUser;
    private final String opponentUser;
    private final String myColor;       // Protocol.COLOR_RED hoặc Protocol.COLOR_BLUE
    private String currentTurn;         // Lượt hiện tại (RED hoặc BLUE)
    private final LobbyFrame lobbyFrame; // Để hiện lại khi thoát/kết thúc

    private final String redPlayerName;
    private final String bluePlayerName;

    // Trạng thái bàn cờ do Server cấp quyền (Dumb View chỉ lưu để vẽ)
    // Cấp phát kích thước an toàn [8][8] để tránh lỗi nếu Server gửi kích thước khác
    private final String[][] hLines = new String[8][8]; // Cạnh ngang [r][c] -> "RED" / "BLUE"
    private final String[][] vLines = new String[8][8]; // Cạnh dọc [r][c] -> "RED" / "BLUE"
    private final String[][] boxes = new String[8][8];  // Ô [r][c] -> "RED" / "BLUE"

    // Điểm số nhận từ Server
    private int scoreRed = 0;
    private int scoreBlue = 0;

    // Chuỗi trận giữa 2 người do Server gửi (MATCH_INFO / GAME_OVER): trận thứ mấy và số trận thắng mỗi bên
    private int matchNumber = 1;
    private int winsRed = 0;
    private int winsBlue = 0;

    // Thành phần giao diện Top Bar
    private JLabel lblRedScore;
    private JLabel lblBlueScore;
    private JLabel lblMatchScore;
    private JLabel lblMatchInfo;      // "Trận N • còn mm:ss"
    private Timer matchCountdownTimer;
    private int matchRemainingSeconds = Protocol.MATCH_TIME_SECONDS;

    // Thành phần giao diện Bottom Bar
    private JLabel lblTurnIndicator;
    private JLabel lblTimerText;
    private Timer turnCountdownTimer;
    private int remainingSeconds = Protocol.TURN_TIME_SECONDS;

    // Thành phần bàn cờ trung tâm
    private GameBoardPanel boardPanel;

    // Cạnh đang được rê chuột (hover): [type: "H"/"V", r, c]
    private HoverEdge hoveredEdge = null;

    // Khung chat trong trận (bên phải bàn cờ)
    private ChatPanel chatPanel;

    // Thông báo nổi phía trên bàn cờ, tự ẩn sau TOAST_MS (vd: hết giờ).
    // Không dùng hộp thoại vì hộp thoại chặn màn hình trong khi đồng hồ lượt vẫn chạy.
    private static final int TOAST_MS = 3000;
    private static final Color TOAST_WARN = new Color(234, 88, 12);   // Cam: mình bị mất lượt
    private String toastText = null;
    private Color toastColor = PRIMARY;
    private Timer toastTimer;

    // Hộp thoại kết thúc trận đấu và điều khiển tái đấu
    private JDialog gameOverDialog = null;
    private AppUI.GradientButton btnRematchDialog = null;
    private JLabel lblRematchNotice = null;
    private volatile boolean isLeavingToLobby = false;

    /**
     * Khởi tạo màn hình bàn cờ với đầy đủ thông số
     */
    public GameBoardFrame(String currentUser, String opponentUser, String myColor, String startingTurn, LobbyFrame lobbyFrame) {
        this.currentUser = currentUser;
        this.opponentUser = opponentUser;
        this.myColor = (myColor == null || myColor.equalsIgnoreCase(Protocol.COLOR_BLUE)) ? Protocol.COLOR_BLUE : Protocol.COLOR_RED;
        this.currentTurn = (startingTurn == null || startingTurn.equalsIgnoreCase(Protocol.COLOR_BLUE)) ? Protocol.COLOR_BLUE : Protocol.COLOR_RED;
        this.lobbyFrame = lobbyFrame;

        if (Protocol.COLOR_RED.equalsIgnoreCase(this.myColor)) {
            this.redPlayerName = currentUser;
            this.bluePlayerName = opponentUser;
        } else {
            this.redPlayerName = opponentUser;
            this.bluePlayerName = currentUser;
        }

        initUI();
        startTurnTimer(Protocol.TURN_TIME_SECONDS);
        startMatchTimer(Protocol.MATCH_TIME_SECONDS);

        // Đăng ký nhận gói tin trực tiếp từ SocketClient
        try {
            SocketClient.getInstance().setMessageListener(this);
        } catch (Exception ignored) {}
    }

    public GameBoardFrame(String currentUser, String opponentUser, String myColor, String startingTurn) {
        this(currentUser, opponentUser, myColor, startingTurn, null);
    }

    private void initUI() {
        setTitle("Dots and Boxes Game");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(840 + ChatPanel.PANEL_WIDTH, 720);
        setMinimumSize(new Dimension(760 + ChatPanel.PANEL_WIDTH, 660));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndExit();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);

        // TẦNG 1: TOP BAR (Thông tin 2 người chơi & Tỉ số)
        root.add(createTopBar(), BorderLayout.NORTH);

        // TẦNG 2: MIDDLE AREA (Bàn cờ trung tâm 5x5 ô + Khung chat bên phải)
        boardPanel = new GameBoardPanel();
        JPanel middle = new JPanel(new BorderLayout());
        middle.setOpaque(false);
        middle.add(boardPanel, BorderLayout.CENTER);
        chatPanel = new ChatPanel(currentUser, Protocol.COLOR_RED.equalsIgnoreCase(myColor), this::sendChat);
        chatPanel.addSystemMessage("Ván đấu bắt đầu. Chúc hai bạn chơi vui vẻ!");
        middle.add(chatPanel, BorderLayout.EAST);
        root.add(middle, BorderLayout.CENTER);

        // TẦNG 3: BOTTOM BAR (Lượt đi, Timer 30s, Nút Thoát)
        root.add(createBottomBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // =========================================================================
    // 1. TẦNG TRÊN (TOP BAR - PLAYER INFORMATION & SCORE)
    // =========================================================================

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_LIGHT);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 96));
        topBar.setBorder(new EmptyBorder(14, 28, 14, 28));

        // BÊN TRÁI: Người chơi ĐỎ
        JPanel pnlRed = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlRed.setOpaque(false);

        JLabel dotRed = new CircleAvatarIconLabel(32, COLOR_RED_VAL);
        JPanel pnlRedNames = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlRedNames.setOpaque(false);
        JLabel lblRedTitle = new JLabel("Người chơi Đỏ");
        lblRedTitle.setFont(new Font(AppUI.FONT, Font.BOLD, 14));
        lblRedTitle.setForeground(TEXT_MAIN);

        String redSub = redPlayerName + (Protocol.COLOR_RED.equalsIgnoreCase(myColor) ? " (Bạn)" : "");
        JLabel lblRedUser = new JLabel(redSub);
        lblRedUser.setFont(new Font(AppUI.FONT, Font.PLAIN, 13));
        lblRedUser.setForeground(TEXT_MUTED);

        pnlRedNames.add(lblRedTitle);
        pnlRedNames.add(lblRedUser);

        lblRedScore = new JLabel("0", SwingConstants.CENTER);
        JPanel boxRedScore = createScoreBadge("Ô chiếm được", lblRedScore, COLOR_RED_BG, COLOR_RED_BORDER, COLOR_RED_VAL);

        pnlRed.add(dotRed);
        pnlRed.add(pnlRedNames);
        pnlRed.add(boxRedScore);

        // Ở GIỮA: Trận thứ mấy + thời gian còn lại của trận, bên dưới là tỉ số (số trận thắng Đỏ - Xanh)
        JPanel pnlCenter = new JPanel(new GridBagLayout());
        pnlCenter.setOpaque(false);
        lblMatchInfo = new JLabel(" ", SwingConstants.CENTER);
        lblMatchInfo.setFont(new Font(AppUI.FONT, Font.BOLD, 13));
        lblMatchInfo.setForeground(TEXT_MUTED);
        lblMatchScore = new JLabel(" ", SwingConstants.CENTER);
        lblMatchScore.setFont(new Font(AppUI.FONT, Font.BOLD, 22));
        lblMatchScore.setForeground(TEXT_MAIN);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnlCenter.add(lblMatchInfo, gbc);
        gbc.gridy = 1;
        pnlCenter.add(lblMatchScore, gbc);
        updateMatchInfoDisplay();
        updateSeriesScoreDisplay();

        // BÊN PHẢI: Người chơi XANH
        JPanel pnlBlue = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlBlue.setOpaque(false);

        lblBlueScore = new JLabel("0", SwingConstants.CENTER);
        JPanel boxBlueScore = createScoreBadge("Ô chiếm được", lblBlueScore, COLOR_BLUE_BG, COLOR_BLUE_BORDER, COLOR_BLUE_VAL);

        JPanel pnlBlueNames = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlBlueNames.setOpaque(false);
        JLabel lblBlueTitle = new JLabel("Người chơi Xanh", SwingConstants.RIGHT);
        lblBlueTitle.setFont(new Font(AppUI.FONT, Font.BOLD, 14));
        lblBlueTitle.setForeground(TEXT_MAIN);

        String blueSub = bluePlayerName + (Protocol.COLOR_BLUE.equalsIgnoreCase(myColor) ? " (Bạn)" : "");
        JLabel lblBlueUser = new JLabel(blueSub, SwingConstants.RIGHT);
        lblBlueUser.setFont(new Font(AppUI.FONT, Font.PLAIN, 13));
        lblBlueUser.setForeground(TEXT_MUTED);

        pnlBlueNames.add(lblBlueTitle);
        pnlBlueNames.add(lblBlueUser);

        JLabel dotBlue = new CircleAvatarIconLabel(32, COLOR_BLUE_VAL);

        pnlBlue.add(boxBlueScore);
        pnlBlue.add(pnlBlueNames);
        pnlBlue.add(dotBlue);

        topBar.add(pnlRed, BorderLayout.WEST);
        topBar.add(pnlCenter, BorderLayout.CENTER);
        topBar.add(pnlBlue, BorderLayout.EAST);

        return topBar;
    }

    private JPanel createScoreBadge(String title, JLabel lblValue, Color bg, Color border, Color textFg) {
        JPanel badge = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(bg);
                g2.fill(shape);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(shape);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(96, 54));
        badge.setBorder(new EmptyBorder(5, 6, 5, 6));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font(AppUI.FONT, Font.PLAIN, 11));
        lblTitle.setForeground(TEXT_MUTED);

        lblValue.setFont(new Font(AppUI.FONT, Font.BOLD, 22));
        lblValue.setForeground(textFg);

        badge.add(lblTitle, BorderLayout.NORTH);
        badge.add(lblValue, BorderLayout.CENTER);
        return badge;
    }

    // =========================================================================
    // 2. TẦNG GIỮA (GAME BOARD PANEL)
    // =========================================================================

    private class GameBoardPanel extends JPanel {

        private static final int SPACING = 68;       // Khoảng cách giữa 2 chấm
        private static final int DOT_RADIUS = 6;     // Bán kính chấm tròn
        private static final float LINE_STROKE = 5.0f; // Bề dày nét vẽ đã nối
        private static final float GRID_STROKE = 1.4f; // Bề dày nét mờ bàn cờ

        GameBoardPanel() {
            setOpaque(false);

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    handleMouseMove(e.getX(), e.getY());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoveredEdge != null) {
                        hoveredEdge = null;
                        setCursor(Cursor.getDefaultCursor());
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    handleClick(e.getX(), e.getY());
                }
            };
            addMouseMotionListener(adapter);
            addMouseListener(adapter);
        }

        private int getOriginX() {
            int boardW = (DOT_COLS - 1) * SPACING;
            return (getWidth() - boardW) / 2;
        }

        private int getOriginY() {
            int boardH = (DOT_ROWS - 1) * SPACING;
            // Chừa một chút chỗ cho dòng chữ nhãn bàn cờ bên dưới
            return (getHeight() - boardH - 36) / 2;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int x0 = getOriginX();
            int y0 = getOriginY();

            // 1. TÔ CÁC Ô VUÔNG ĐÃ BỊ CHIẾM (CAPTURE_BOX)
            for (int r = 0; r < BOX_ROWS; r++) {
                for (int c = 0; c < BOX_COLS; c++) {
                    String owner = boxes[r][c];
                    if (owner != null) {
                        int bx = x0 + c * SPACING;
                        int by = y0 + r * SPACING;
                        Color fillC = Protocol.COLOR_RED.equalsIgnoreCase(owner)
                                ? new Color(239, 68, 68, 85)
                                : new Color(37, 99, 235, 85);
                        g2.setColor(fillC);
                        g2.fill(new RoundRectangle2D.Float(bx + 2, by + 2, SPACING - 4, SPACING - 4, 8, 8));
                    }
                }
            }

            // 2. VẼ LƯỚI KHUNG MỜ CỦA BÀN CỜ (CÁC CẠNH TIỀM NĂNG)
            g2.setStroke(new BasicStroke(GRID_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(GRID_LINE_COLOR);

            // Cạnh ngang mờ
            for (int r = 0; r < DOT_ROWS; r++) {
                for (int c = 0; c < BOX_COLS; c++) {
                    if (hLines[r][c] == null) {
                        g2.drawLine(x0 + c * SPACING, y0 + r * SPACING,
                                x0 + (c + 1) * SPACING, y0 + r * SPACING);
                    }
                }
            }
            // Cạnh dọc mờ
            for (int r = 0; r < BOX_ROWS; r++) {
                for (int c = 0; c < DOT_COLS; c++) {
                    if (vLines[r][c] == null) {
                        g2.drawLine(x0 + c * SPACING, y0 + r * SPACING,
                                x0 + c * SPACING, y0 + (r + 1) * SPACING);
                    }
                }
            }

            // 3. HIỆU ỨNG HOVER KHI RÊ CHUỘT
            if (hoveredEdge != null) {
                g2.setStroke(new BasicStroke(LINE_STROKE - 1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Color hoverC = Protocol.COLOR_RED.equalsIgnoreCase(myColor)
                        ? new Color(239, 68, 68, 140)
                        : new Color(37, 99, 235, 140);
                g2.setColor(hoverC);

                if ("H".equals(hoveredEdge.type)) {
                    int r = hoveredEdge.r;
                    int c = hoveredEdge.c;
                    g2.drawLine(x0 + c * SPACING, y0 + r * SPACING,
                            x0 + (c + 1) * SPACING, y0 + r * SPACING);
                } else if ("V".equals(hoveredEdge.type)) {
                    int r = hoveredEdge.r;
                    int c = hoveredEdge.c;
                    g2.drawLine(x0 + c * SPACING, y0 + r * SPACING,
                            x0 + c * SPACING, y0 + (r + 1) * SPACING);
                }
            }

            // 4. VẼ CÁC CẠNH ĐÃ ĐƯỢC KẾT NỐI (DRAW_LINE)
            g2.setStroke(new BasicStroke(LINE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Cạnh ngang đã vẽ
            for (int r = 0; r < DOT_ROWS; r++) {
                for (int c = 0; c < BOX_COLS; c++) {
                    String color = hLines[r][c];
                    if (color != null) {
                        g2.setColor(Protocol.COLOR_RED.equalsIgnoreCase(color) ? COLOR_RED_VAL : COLOR_BLUE_VAL);
                        g2.drawLine(x0 + c * SPACING, y0 + r * SPACING,
                                x0 + (c + 1) * SPACING, y0 + r * SPACING);
                    }
                }
            }

            // Cạnh dọc đã vẽ
            for (int r = 0; r < BOX_ROWS; r++) {
                for (int c = 0; c < DOT_COLS; c++) {
                    String color = vLines[r][c];
                    if (color != null) {
                        g2.setColor(Protocol.COLOR_RED.equalsIgnoreCase(color) ? COLOR_RED_VAL : COLOR_BLUE_VAL);
                        g2.drawLine(x0 + c * SPACING, y0 + r * SPACING,
                                x0 + c * SPACING, y0 + (r + 1) * SPACING);
                    }
                }
            }

            // 5. VẼ CÁC ĐIỂM CHẤM TRÒN (6x6 = 36 DẤU CHẤM)
            g2.setColor(DOT_COLOR);
            for (int r = 0; r < DOT_ROWS; r++) {
                for (int c = 0; c < DOT_COLS; c++) {
                    int px = x0 + c * SPACING;
                    int py = y0 + r * SPACING;
                    g2.fill(new Ellipse2D.Float(px - DOT_RADIUS, py - DOT_RADIUS, DOT_RADIUS * 2, DOT_RADIUS * 2));
                }
            }

            // 6. NHÃN BÀN CỜ PHÍA DƯỚI
            String label = "BÀN CỜ 5 × 5 Ô - 6 × 6 ĐIỂM";
            g2.setFont(new Font(AppUI.FONT, Font.BOLD, 12));
            g2.setColor(new Color(100, 116, 139));
            FontMetrics fm = g2.getFontMetrics();
            int lblX = (getWidth() - fm.stringWidth(label)) / 2;
            int lblY = y0 + (DOT_ROWS - 1) * SPACING + 38;
            g2.drawString(label, lblX, lblY);

            // 7. THÔNG BÁO NỔI (viên thuốc phía trên bàn cờ)
            if (toastText != null) {
                g2.setFont(new Font(AppUI.FONT, Font.BOLD, 14));
                FontMetrics tfm = g2.getFontMetrics();
                int th = 38;
                int tw = tfm.stringWidth(toastText) + 40;
                int tx = (getWidth() - tw) / 2;
                int ty = Math.max(8, y0 - th - 22);
                RoundRectangle2D pill = new RoundRectangle2D.Float(tx, ty, tw, th, th, th);
                g2.setColor(new Color(15, 23, 42, 40)); // Bóng mờ
                g2.fill(new RoundRectangle2D.Float(tx, ty + 3, tw, th, th, th));
                g2.setColor(toastColor);
                g2.fill(pill);
                g2.setColor(Color.WHITE);
                g2.drawString(toastText, tx + 20, ty + (th - tfm.getHeight()) / 2 + tfm.getAscent());
            }

            g2.dispose();
        }

        /**
         * Tìm cạnh gần nhất với tọa độ chuột (H hoặc V, r, c).
         * Trả về null nếu chuột cách xa mọi cạnh hoặc cạnh đã được nối rồi.
         */
        private HoverEdge findNearestEdge(int mx, int my) {
            int x0 = getOriginX();
            int y0 = getOriginY();
            double bestDist = Double.MAX_VALUE;
            HoverEdge bestEdge = null;
            double threshold = SPACING * 0.38; // Vùng bắt click xung quanh cạnh

            // Duyệt cạnh ngang
            for (int r = 0; r < DOT_ROWS; r++) {
                for (int c = 0; c < BOX_COLS; c++) {
                    if (hLines[r][c] != null) continue; // Cạnh đã vẽ thì bỏ qua
                    int x1 = x0 + c * SPACING;
                    int y1 = y0 + r * SPACING;
                    int x2 = x0 + (c + 1) * SPACING;
                    int y2 = y1;
                    double dist = ptSegDist(mx, my, x1, y1, x2, y2);
                    if (dist < bestDist && dist <= threshold) {
                        bestDist = dist;
                        bestEdge = new HoverEdge("H", r, c);
                    }
                }
            }

            // Duyệt cạnh dọc
            for (int r = 0; r < BOX_ROWS; r++) {
                for (int c = 0; c < DOT_COLS; c++) {
                    if (vLines[r][c] != null) continue;
                    int x1 = x0 + c * SPACING;
                    int y1 = y0 + r * SPACING;
                    int x2 = x1;
                    int y2 = y0 + (r + 1) * SPACING;
                    double dist = ptSegDist(mx, my, x1, y1, x2, y2);
                    if (dist < bestDist && dist <= threshold) {
                        bestDist = dist;
                        bestEdge = new HoverEdge("V", r, c);
                    }
                }
            }

            return bestEdge;
        }

        private void handleMouseMove(int mx, int my) {
            HoverEdge edge = findNearestEdge(mx, my);
            if (!HoverEdge.equals(hoveredEdge, edge)) {
                hoveredEdge = edge;
                setCursor(edge != null ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                repaint();
            }
        }

        private void handleClick(int mx, int my) {
            HoverEdge edge = findNearestEdge(mx, my);
            if (edge == null) return;

            // Kiểm tra xem có đang tới lượt của mình không (cảnh báo nhẹ nếu chưa tới lượt)
            if (!myColor.equalsIgnoreCase(currentTurn)) {
                Toolkit.getDefaultToolkit().beep();
                // Dumb View vẫn có thể gửi hoặc nhắc người chơi
            }

            // Gửi ngay gói tin MOVE lên Server qua TV3 (SocketClient)
            String packet = Protocol.CMD_MOVE + Protocol.DELIMITER
                    + edge.type + Protocol.DELIMITER
                    + edge.r + Protocol.DELIMITER
                    + edge.c;

            if (!SocketClient.getInstance().send(packet)) {
                AppUI.showConnectionError(GameBoardFrame.this);
            }
        }
    }

    /**
     * Tính khoảng cách từ điểm (px, py) tới đoạn thẳng (x1, y1) - (x2, y2)
     */
    private static double ptSegDist(double px, double py, double x1, double y1, double x2, double y2) {
        return Line2D.ptSegDist(x1, y1, x2, y2, px, py);
    }

    private static class HoverEdge {
        final String type; // "H" hoặc "V"
        final int r;
        final int c;

        HoverEdge(String type, int r, int c) {
            this.type = type;
            this.r = r;
            this.c = c;
        }

        static boolean equals(HoverEdge e1, HoverEdge e2) {
            if (e1 == e2) return true;
            if (e1 == null || e2 == null) return false;
            return e1.type.equals(e2.type) && e1.r == e2.r && e1.c == e2.c;
        }
    }

    // =========================================================================
    // 2b. KHUNG CHAT TRONG TRẬN (bên phải bàn cờ)
    // =========================================================================

    /**
     * Gửi CHAT lên Server (ChatPanel gọi khi người dùng bấm Gửi).
     * Không tự hiển thị tin của mình: Server sẽ gửi lại CHAT_MSG cho cả 2 người (Dumb View).
     *
     * @return true nếu đã gửi được
     */
    private boolean sendChat(String text) {
        if (!SocketClient.getInstance().send(Protocol.CMD_CHAT + Protocol.DELIMITER + text)) {
            AppUI.showConnectionError(this);
            return false;
        }
        return true;
    }

    // =========================================================================
    // 3. TẦNG DƯỚI (BOTTOM BAR - TURN & CONTROL)
    // =========================================================================

    private JPanel createBottomBar() {
        JPanel bottomBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_LIGHT);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bottomBar.setOpaque(false);
        bottomBar.setPreferredSize(new Dimension(0, 68));
        bottomBar.setBorder(new EmptyBorder(12, 28, 12, 28));

        // TRÁI: Nhãn lượt hiện tại
        JPanel pnlTurn = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlTurn.setOpaque(false);

        JLabel lblTitleTurn = new JLabel("Lượt hiện tại:");
        lblTitleTurn.setFont(new Font(AppUI.FONT, Font.PLAIN, 14));
        lblTitleTurn.setForeground(TEXT_MAIN);

        lblTurnIndicator = new JLabel();
        lblTurnIndicator.setFont(new Font(AppUI.FONT, Font.BOLD, 15));
        updateTurnDisplay();

        pnlTurn.add(lblTitleTurn);
        pnlTurn.add(lblTurnIndicator);

        // GIỮA: Đồng hồ đếm ngược 30s
        JPanel pnlTimer = new JPanel(new GridBagLayout());
        pnlTimer.setOpaque(false);

        JPanel timerPill = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D pill = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(new Color(239, 246, 255));
                g2.fill(pill);
                g2.setColor(new Color(191, 219, 254));
                g2.draw(pill);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        timerPill.setOpaque(false);
        timerPill.setBorder(new EmptyBorder(2, 16, 2, 16));

        JLabel iconClock = new JLabel(new ClockVectorIcon(18, PRIMARY));
        lblTimerText = new JLabel(Protocol.TURN_TIME_SECONDS + "s");
        lblTimerText.setFont(new Font(AppUI.FONT, Font.BOLD, 15));
        lblTimerText.setForeground(TEXT_MAIN);

        timerPill.add(iconClock);
        timerPill.add(lblTimerText);
        pnlTimer.add(timerPill);

        // PHẢI: Nút "THOÁT"
        JPanel pnlExit = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlExit.setOpaque(false);

        JButton btnExit = new JButton("THOÁT") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();

                Color bg = pressed ? new Color(241, 245, 249) : (hover ? new Color(248, 250, 252) : Color.WHITE);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 10, 10);
                g2.setColor(bg);
                g2.fill(shape);
                g2.setColor(new Color(203, 213, 225));
                g2.draw(shape);

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(TEXT_MAIN);
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btnExit.setFont(new Font(AppUI.FONT, Font.BOLD, 13));
        btnExit.setPreferredSize(new Dimension(110, 40));
        btnExit.setContentAreaFilled(false);
        btnExit.setBorderPainted(false);
        btnExit.setFocusPainted(false);
        btnExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExit.addActionListener(e -> confirmAndExit());
        pnlExit.add(btnExit);

        bottomBar.add(pnlTurn, BorderLayout.WEST);
        bottomBar.add(pnlTimer, BorderLayout.CENTER);
        bottomBar.add(pnlExit, BorderLayout.EAST);

        return bottomBar;
    }

    private void updateTurnDisplay() {
        boolean isRed = Protocol.COLOR_RED.equalsIgnoreCase(currentTurn);
        lblTurnIndicator.setText(isRed ? "ĐỎ" : "XANH");
        lblTurnIndicator.setForeground(isRed ? COLOR_RED_VAL : COLOR_BLUE_VAL);
        lblTurnIndicator.setIcon(new CircleDotIcon(14, isRed ? COLOR_RED_VAL : COLOR_BLUE_VAL));
        lblTurnIndicator.setIconTextGap(6);
    }

    private void startTurnTimer(int seconds) {
        remainingSeconds = seconds;
        if (lblTimerText != null) {
            lblTimerText.setText(remainingSeconds + "s");
        }
        if (turnCountdownTimer != null && turnCountdownTimer.isRunning()) {
            turnCountdownTimer.stop();
        }

        turnCountdownTimer = new Timer(1000, e -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                if (lblTimerText != null) {
                    lblTimerText.setText(remainingSeconds + "s");
                    if (remainingSeconds <= 5) {
                        lblTimerText.setForeground(COLOR_RED_VAL);
                    } else {
                        lblTimerText.setForeground(TEXT_MAIN);
                    }
                }
            }
        });
        turnCountdownTimer.start();
    }

    private void stopTurnTimer() {
        if (turnCountdownTimer != null && turnCountdownTimer.isRunning()) {
            turnCountdownTimer.stop();
            turnCountdownTimer = null;
        }
    }

    /**
     * Đồng hồ cả trận chỉ để hiển thị; Server tự kết thúc trận khi hết giờ (GAME_OVER lý do TIME_UP).
     */
    private void startMatchTimer(int seconds) {
        stopMatchTimer();
        matchRemainingSeconds = seconds;
        updateMatchInfoDisplay();
        matchCountdownTimer = new Timer(1000, e -> {
            if (matchRemainingSeconds > 0) {
                matchRemainingSeconds--;
                updateMatchInfoDisplay();
            }
        });
        matchCountdownTimer.start();
    }

    private void stopMatchTimer() {
        if (matchCountdownTimer != null) {
            matchCountdownTimer.stop();
            matchCountdownTimer = null;
        }
    }

    private void updateMatchInfoDisplay() {
        if (lblMatchInfo == null) return;
        String time = String.format("%02d:%02d", matchRemainingSeconds / 60, matchRemainingSeconds % 60);
        lblMatchInfo.setText("Trận " + matchNumber + "  •  còn " + time);
        lblMatchInfo.setForeground(matchRemainingSeconds <= 30 ? COLOR_RED_VAL : TEXT_MUTED);
    }

    private void updateSeriesScoreDisplay() {
        if (lblMatchScore == null) return;
        lblMatchScore.setText("Tỉ số: " + winsRed + " - " + winsBlue);
    }

    /**
     * Xác nhận khi người chơi nhấn nút "Thoát" hoặc đóng cửa sổ
     */
    private void confirmAndExit() {
        int opt = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn thoát ván đấu không?\nThoát trận sẽ bị xử thua!",
                "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (opt == JOptionPane.YES_OPTION) {
            // Gửi gói tin SURRENDER và LEAVE_ROOM lên Server
            SocketClient.getInstance().send(Protocol.CMD_SURRENDER);
            SocketClient.getInstance().send(Protocol.CMD_LEAVE_ROOM);
            backToLobby();
        }
    }

    private void backToLobby() {
        isLeavingToLobby = true;
        if (gameOverDialog != null && gameOverDialog.isShowing()) {
            gameOverDialog.dispose();
            gameOverDialog = null;
        }
        btnRematchDialog = null;
        lblRematchNotice = null;
        stopTurnTimer();
        stopMatchTimer();
        dispose();
        if (lobbyFrame != null) {
            lobbyFrame.setVisible(true);
            try {
                // Đăng ký lại LobbyFrame nhận gói tin
                SocketClient.getInstance().setMessageListener(lobbyFrame);
                SocketClient.getInstance().send(Protocol.CMD_LEAVE_ROOM);
                SocketClient.getInstance().send("GET_LOBBY");
            } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // 4. TIẾP NHẬN VÀ CẬP NHẬT THEO LỆNH TỪ SERVER (DUMB VIEW)
    // =========================================================================

    @Override
    public void onMessageReceived(String message) {
        if (isLeavingToLobby || message == null || message.trim().isEmpty()) return;

        String[] parts = message.split(Protocol.DELIMITER);
        String cmd = parts[0];

        switch (cmd) {
            case Protocol.CMD_DRAW_LINE:
                // S -> C: DRAW_LINE;H|V;r;c;color
                if (parts.length >= 5) {
                    String type = parts[1];
                    int r = Integer.parseInt(parts[2]);
                    int c = Integer.parseInt(parts[3]);
                    String color = parts[4];
                    handleDrawLine(type, r, c, color);
                }
                break;

            case Protocol.CMD_CAPTURE_BOX:
                // S -> C: CAPTURE_BOX;r;c;color;sc_red;sc_blue
                if (parts.length >= 6) {
                    int r = Integer.parseInt(parts[1]);
                    int c = Integer.parseInt(parts[2]);
                    String color = parts[3];
                    int scRed = Integer.parseInt(parts[4]);
                    int scBlue = Integer.parseInt(parts[5]);
                    handleCaptureBox(r, c, color, scRed, scBlue);
                }
                break;

            case Protocol.CMD_CHANGE_TURN:
                // S -> C: CHANGE_TURN;next_color;remaining_time
                if (parts.length >= 2) {
                    String nextColor = parts[1];
                    int time = (parts.length >= 3) ? Integer.parseInt(parts[2]) : Protocol.TURN_TIME_SECONDS;
                    handleChangeTurn(nextColor, time);
                }
                break;

            case Protocol.CMD_TURN_TIMEOUT:
                // S -> C: TURN_TIMEOUT;loser_color;số_lần_liên_tiếp
                // (sau đó Server gửi CHANGE_TURN, hoặc GAME_OVER nếu đã hết giờ quá số lần cho phép)
                if (parts.length >= 2) {
                    boolean isMe = parts[1].equalsIgnoreCase(myColor);
                    int count = (parts.length >= 3) ? Integer.parseInt(parts[2]) : 1;
                    String times = " (" + count + "/" + Protocol.MAX_CONSECUTIVE_TIMEOUTS + ")";
                    if (isMe) {
                        boolean lastChance = count == Protocol.MAX_CONSECUTIVE_TIMEOUTS - 1;
                        showToast("Bạn đã hết giờ" + times + (lastChance
                                ? " – thêm 1 lần nữa sẽ bị xử thua!"
                                : ", mất lượt!"), TOAST_WARN);
                    } else {
                        showToast(opponentUser + " đã hết giờ" + times + ", tới lượt bạn!", PRIMARY);
                    }
                    Toolkit.getDefaultToolkit().beep();
                }
                break;

            case Protocol.CMD_MATCH_INFO:
                // S -> C: MATCH_INFO;số_thứ_tự_trận;thắng_đỏ;thắng_xanh;giây_còn_lại
                if (parts.length >= 5) {
                    matchNumber = Integer.parseInt(parts[1]);
                    winsRed = Integer.parseInt(parts[2]);
                    winsBlue = Integer.parseInt(parts[3]);
                    updateSeriesScoreDisplay();
                    startMatchTimer(Integer.parseInt(parts[4]));
                }
                break;

            case Protocol.CMD_GAME_OVER:
                // S -> C: GAME_OVER;winner_user;reason;thắng_đỏ;thắng_xanh
                String winner = (parts.length >= 2) ? parts[1] : "";
                String reason = (parts.length >= 3) ? parts[2] : "";
                if (parts.length >= 5) {
                    winsRed = Integer.parseInt(parts[3]);
                    winsBlue = Integer.parseInt(parts[4]);
                    updateSeriesScoreDisplay();
                }
                handleGameOver(winner, reason);
                break;

            case Protocol.CMD_REMATCH_INVITE:
                // S -> C: REMATCH_INVITE
                handleRematchInvite();
                break;

            case Protocol.CMD_REMATCH_RESP:
                // S -> C: REMATCH_RESP;ACCEPT|REJECT
                if (parts.length >= 2) {
                    if (Protocol.RESP_ACCEPT.equalsIgnoreCase(parts[1])) {
                        resetGame();
                    } else {
                        if (isLeavingToLobby) return;
                        boolean waitingRematch = (btnRematchDialog != null && "Đang chờ...".equals(btnRematchDialog.getText()));
                        if (btnRematchDialog != null) {
                            btnRematchDialog.setEnabled(false);
                            btnRematchDialog.setText(waitingRematch ? "Đối thủ từ chối" : "Đối thủ đã rời");
                        }
                        if (lblRematchNotice != null) {
                            lblRematchNotice.setText(waitingRematch 
                                    ? "Đối thủ đã từ chối tái đấu. Vui lòng quay lại sảnh."
                                    : "Đối thủ đã rời phòng. Vui lòng quay lại sảnh.");
                            lblRematchNotice.setForeground(COLOR_RED_VAL);
                        }
                    }
                }
                break;

            case Protocol.CMD_CHALLENGE_INVITE:
                // Tự động từ chối nếu nhận lời mời thách đấu khi đang ở trong phòng thi đấu
                if (parts.length >= 2) {
                    String challenger = parts[1].trim();
                    SocketClient.getInstance().send(Protocol.CMD_CHALLENGE_RESP + Protocol.DELIMITER 
                            + challenger + Protocol.DELIMITER + Protocol.RESP_REJECT);
                }
                break;

            case Protocol.CMD_START_GAME:
                // Nếu Server gửi lại START_GAME để reset ván mới
                resetGame();
                break;

            case Protocol.CMD_CHAT_MSG:
                // S -> C: CHAT_MSG;người_gửi;nội_dung
                // Tách tối đa 3 phần vì nội dung có thể chứa dấu ';'
                String[] chatParts = message.split(Protocol.DELIMITER, 3);
                if (chatParts.length >= 3) {
                    chatPanel.addMessage(chatParts[1], chatParts[2]);
                }
                break;
        }
    }

    @Override
    public void onDisconnected(String reason) {
        stopTurnTimer();
        stopMatchTimer();
        JOptionPane.showMessageDialog(this,
                "Mất kết nối tới Server!\nVui lòng đăng nhập lại.",
                "Mất kết nối", JOptionPane.WARNING_MESSAGE);
        dispose();
        if (lobbyFrame != null) {
            lobbyFrame.dispose();
        }
        new LoginFrame().setVisible(true);
    }

    /**
     * Hiện thông báo nổi phía trên bàn cờ, tự ẩn sau TOAST_MS mili-giây.
     */
    private void showToast(String text, Color color) {
        toastText = text;
        toastColor = color;
        if (toastTimer != null) {
            toastTimer.stop();
        }
        toastTimer = new Timer(TOAST_MS, e -> {
            toastText = null;
            boardPanel.repaint();
        });
        toastTimer.setRepeats(false);
        toastTimer.start();
        boardPanel.repaint();
    }

    private void handleDrawLine(String type, int r, int c, String color) {
        if ("H".equalsIgnoreCase(type) && r < hLines.length && c < hLines[r].length) {
            hLines[r][c] = color;
        } else if ("V".equalsIgnoreCase(type) && r < vLines.length && c < vLines[r].length) {
            vLines[r][c] = color;
        }
        if (hoveredEdge != null && hoveredEdge.type.equalsIgnoreCase(type) && hoveredEdge.r == r && hoveredEdge.c == c) {
            hoveredEdge = null;
        }
        boardPanel.repaint();
    }

    private void handleCaptureBox(int r, int c, String color, int scRed, int scBlue) {
        if (r < boxes.length && c < boxes[r].length) {
            boxes[r][c] = color;
        }
        this.scoreRed = scRed;
        this.scoreBlue = scBlue;

        lblRedScore.setText(String.valueOf(scRed));
        lblBlueScore.setText(String.valueOf(scBlue));

        boardPanel.repaint();
    }

    private void handleChangeTurn(String nextColor, int remainingTime) {
        this.currentTurn = nextColor;
        updateTurnDisplay();
        startTurnTimer(remainingTime);
        boardPanel.repaint();
    }

    /**
     * Hộp thoại kết thúc trận: Hiển thị thông báo ai thắng ai thua
     * và 2 nút: "Chơi tiếp" và "Về sảnh".
     */
    private void handleGameOver(String winner, String reason) {
        stopTurnTimer();
        stopMatchTimer();

        boolean isWin = currentUser.equalsIgnoreCase(winner);
        boolean isDraw = Protocol.WINNER_DRAW.equalsIgnoreCase(winner) || "HÒA".equalsIgnoreCase(winner);

        String title;
        String statusText;
        if (isDraw) {
            title = "Kết quả: Hòa nhau!";
            statusText = "Ván đấu kết thúc với tỉ số hòa!";
        } else if (isWin) {
            title = "CHIẾN THẮNG! 🎉";
            statusText = "Chúc mừng bạn đã giành chiến thắng!";
        } else {
            title = "THẤT BẠI! 😢";
            statusText = "Bạn đã thua ván đấu này!";
        }

        String reasonDesc = "";
        if ("FULL_BOARD".equalsIgnoreCase(reason)) {
            reasonDesc = "Đã chiếm hết 25 ô trên bàn cờ.";
        } else if ("SURRENDER".equalsIgnoreCase(reason)) {
            reasonDesc = isWin ? "Đối thủ đã xin hàng/thoát trận." : "Bạn đã xin hàng/thoát trận.";
        } else if ("DISCONNECT".equalsIgnoreCase(reason)) {
            reasonDesc = isWin ? "Đối thủ đã mất kết nối." : "Bạn đã mất kết nối.";
        } else if (Protocol.REASON_TIMEOUT.equalsIgnoreCase(reason)) {
            reasonDesc = (isWin ? "Đối thủ" : "Bạn") + " đã hết giờ "
                    + Protocol.MAX_CONSECUTIVE_TIMEOUTS + " lượt liên tiếp.";
        } else if (Protocol.REASON_TIME_UP.equalsIgnoreCase(reason)) {
            reasonDesc = "Hết " + (Protocol.MATCH_TIME_SECONDS / 60) + " phút: Đỏ " + scoreRed
                    + " ô - Xanh " + scoreBlue + " ô.";
        }

        // Tạo Dialog hiện đại đồng bộ giao diện
        if (gameOverDialog != null && gameOverDialog.isShowing()) {
            gameOverDialog.dispose();
        }
        gameOverDialog = new JDialog(this, "Kết thúc trận " + matchNumber, true);
        gameOverDialog.setSize(440, 310);
        gameOverDialog.setLocationRelativeTo(this);
        gameOverDialog.setResizable(false);
        gameOverDialog.setLayout(new BorderLayout());
        gameOverDialog.getContentPane().setBackground(Color.WHITE);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(24, 24, 12, 24));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font(AppUI.FONT, Font.BOLD, 22));
        lblTitle.setForeground(isWin ? COLOR_RED_VAL : (isDraw ? TEXT_MAIN : TEXT_MUTED));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblStatus = new JLabel(statusText, SwingConstants.CENTER);
        lblStatus.setFont(new Font(AppUI.FONT, Font.PLAIN, 14));
        lblStatus.setForeground(TEXT_MAIN);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblFinalScore = new JLabel("Tỉ số sau " + matchNumber + " trận: " + winsRed + " - " + winsBlue
                + "  (Đỏ - Xanh)", SwingConstants.CENTER);
        lblFinalScore.setFont(new Font(AppUI.FONT, Font.BOLD, 16));
        lblFinalScore.setForeground(PRIMARY);
        lblFinalScore.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblReason = new JLabel(reasonDesc.isEmpty() ? " " : reasonDesc, SwingConstants.CENTER);
        lblReason.setFont(new Font(AppUI.FONT, Font.PLAIN, 12));
        lblReason.setForeground(TEXT_MUTED);
        lblReason.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblRematchNotice = lblReason;

        body.add(lblTitle);
        body.add(Box.createVerticalStrut(10));
        body.add(lblStatus);
        body.add(Box.createVerticalStrut(10));
        body.add(lblFinalScore);
        body.add(Box.createVerticalStrut(8));
        body.add(lblReason);

        // 2 NÚT BẤM: "Chơi tiếp" và "Về sảnh"
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        pnlBtns.setBackground(Color.WHITE);
        pnlBtns.setBorder(new EmptyBorder(8, 0, 24, 0));

        AppUI.GradientButton btnRematch = new AppUI.GradientButton("Chơi tiếp");
        btnRematch.setPreferredSize(new Dimension(140, 42));
        btnRematch.setFont(new Font(AppUI.FONT, Font.BOLD, 14));
        btnRematchDialog = btnRematch;
        btnRematch.addActionListener(e -> {
            btnRematch.setEnabled(false);
            btnRematch.setText("Đang chờ...");
            // Gửi lệnh yêu cầu tái đấu
            SocketClient.getInstance().send(Protocol.CMD_REMATCH_REQ);
        });

        JButton btnLobby = new JButton("Về sảnh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(241, 245, 249) : new Color(248, 250, 252);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(new Color(203, 213, 225));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(TEXT_MAIN);
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btnLobby.setFont(new Font(AppUI.FONT, Font.BOLD, 14));
        btnLobby.setPreferredSize(new Dimension(140, 42));
        btnLobby.setContentAreaFilled(false);
        btnLobby.setBorderPainted(false);
        btnLobby.setFocusPainted(false);
        btnLobby.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLobby.addActionListener(e -> {
            isLeavingToLobby = true;
            if (gameOverDialog != null) {
                gameOverDialog.dispose();
                gameOverDialog = null;
            }
            btnRematchDialog = null;
            lblRematchNotice = null;
            SocketClient.getInstance().send(Protocol.CMD_LEAVE_ROOM);
            backToLobby();
        });

        pnlBtns.add(btnRematch);
        pnlBtns.add(btnLobby);

        gameOverDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                isLeavingToLobby = true;
                if (gameOverDialog != null) {
                    gameOverDialog.dispose();
                    gameOverDialog = null;
                }
                btnRematchDialog = null;
                lblRematchNotice = null;
                SocketClient.getInstance().send(Protocol.CMD_LEAVE_ROOM);
                backToLobby();
            }
        });

        gameOverDialog.add(body, BorderLayout.CENTER);
        gameOverDialog.add(pnlBtns, BorderLayout.SOUTH);
        gameOverDialog.setVisible(true);
    }

    private void handleRematchInvite() {
        Component parent = (gameOverDialog != null && gameOverDialog.isShowing()) ? gameOverDialog : this;
        int opt = JOptionPane.showConfirmDialog(
                parent,
                "Đối thủ muốn chơi tiếp ván mới!\nBạn có đồng ý không?",
                "Lời mời tái đấu",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (opt == JOptionPane.YES_OPTION) {
            SocketClient.getInstance().send(Protocol.CMD_REMATCH_RESP + Protocol.DELIMITER + Protocol.RESP_ACCEPT);
            resetGame();
        } else {
            SocketClient.getInstance().send(Protocol.CMD_REMATCH_RESP + Protocol.DELIMITER + Protocol.RESP_REJECT);
            backToLobby();
        }
    }

    /**
     * Khởi tạo lại trạng thái bàn cờ và điểm số cho ván đấu mới
     */
    private void resetGame() {
        isLeavingToLobby = false;
        if (gameOverDialog != null && gameOverDialog.isShowing()) {
            gameOverDialog.dispose();
            gameOverDialog = null;
        }
        btnRematchDialog = null;
        lblRematchNotice = null;
        for (int r = 0; r < hLines.length; r++) {
            for (int c = 0; c < hLines[r].length; c++) {
                hLines[r][c] = null;
            }
        }
        for (int r = 0; r < vLines.length; r++) {
            for (int c = 0; c < vLines[r].length; c++) {
                vLines[r][c] = null;
            }
        }
        for (int r = 0; r < boxes.length; r++) {
            for (int c = 0; c < boxes[r].length; c++) {
                boxes[r][c] = null;
            }
        }
        scoreRed = 0;
        scoreBlue = 0;
        lblRedScore.setText("0");
        lblBlueScore.setText("0");
        // Số thứ tự trận và đồng hồ trận được cập nhật khi nhận MATCH_INFO của trận mới
        stopMatchTimer();
        this.currentTurn = Protocol.COLOR_RED;
        updateTurnDisplay();
        startTurnTimer(Protocol.TURN_TIME_SECONDS);
        boardPanel.repaint();
        chatPanel.addSystemMessage("— Ván mới bắt đầu —");
    }

    // =========================================================================
    // CÁC ICON VECTOR VẼ TRỰC TIẾP
    // =========================================================================

    private static class CircleAvatarIconLabel extends JLabel {
        private final int diameter;
        private final Color color;

        CircleAvatarIconLabel(int diameter, Color color) {
            this.diameter = diameter;
            this.color = color;
            setPreferredSize(new Dimension(diameter, diameter));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fill(new Ellipse2D.Float(0, 0, diameter, diameter));
            g2.dispose();
        }
    }

    private static class CircleDotIcon implements Icon {
        private final int d;
        private final Color c;

        CircleDotIcon(int d, Color c) {
            this.d = d;
            this.c = c;
        }

        @Override
        public void paintIcon(Component cmp, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c);
            g2.fill(new Ellipse2D.Float(x, y + 1, d, d));
            g2.dispose();
        }

        @Override public int getIconWidth() { return d; }
        @Override public int getIconHeight() { return d; }
    }

    private static class ClockVectorIcon implements Icon {
        private final int size;
        private final Color color;

        ClockVectorIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new Ellipse2D.Float(x + 1, y + 1, size - 2, size - 2));

            // Kim đồng hồ (10 giờ 10)
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            g2.draw(new Line2D.Float(cx, cy, cx, cy - size * 0.32f));
            g2.draw(new Line2D.Float(cx, cy, cx + size * 0.26f, cy));
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // =========================================================================
    // HÀM MAIN KIỂM THỬ TRỰC TIẾP GIAO DIỆN
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            GameBoardFrame frame = new GameBoardFrame("Minh_Player", "DoiThu_X", Protocol.COLOR_RED, Protocol.COLOR_RED);
            frame.setVisible(true);
        });
    }
}
