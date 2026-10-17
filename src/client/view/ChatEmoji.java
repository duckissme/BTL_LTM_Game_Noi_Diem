package client.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Bộ biểu tượng cảm xúc (emoji) dùng trong khung chat.
 *
 * Java Swing trên Windows KHÔNG vẽ được emoji màu (chỉ ra ô vuông hoặc nét đen trắng),
 * nên mỗi emoji được vẽ lại bằng vector (Graphics2D) trên lưới 24x24.
 * Trên đường truyền vẫn gửi ký tự emoji Unicode chuẩn, Client tự đổi sang hình khi hiển thị.
 */
final class ChatEmoji {

    /** Danh sách emoji trong bảng chọn: {ký tự Unicode, tên hiển thị khi rê chuột} */
    static final String[][] LIST = {
            {"😀", "Cười"},
            {"😂", "Cười ra nước mắt"},
            {"😍", "Yêu thích"},
            {"😮", "Ngạc nhiên"},
            {"😢", "Buồn"},
            {"😡", "Tức giận"},
            {"😎", "Ngầu"},
            {"👍", "Tuyệt vời"},
            {"❤", "Trái tim"},
            {"🔥", "Cháy quá"}
    };

    /** Ký tự biến thể U+FE0F hay đi kèm emoji (ví dụ "❤️"), bỏ qua khi hiển thị */
    static final int VARIATION_SELECTOR = 0xFE0F;

    // Bảng màu
    private static final Color FACE = new Color(251, 191, 36);
    private static final Color FACE_EDGE = new Color(217, 119, 6);
    private static final Color FEATURE = new Color(120, 53, 15);
    private static final Color TEAR = new Color(59, 130, 246);
    private static final Color HEART = new Color(239, 68, 68);
    private static final Color ANGRY_FACE = new Color(248, 113, 113);
    private static final Color ANGRY_EDGE = new Color(185, 28, 28);
    private static final Color ANGRY_FEATURE = new Color(69, 10, 10);
    private static final Color GLASSES = new Color(30, 41, 59);
    private static final Color SLEEVE = new Color(37, 99, 235);
    private static final Color FIRE_OUTER = new Color(249, 115, 22);
    private static final Color FIRE_INNER = new Color(250, 204, 21);
    private static final Color FIRE_EDGE = new Color(154, 52, 18);

    private ChatEmoji() {
    }

    /**
     * @return ký tự emoji (trong LIST) bắt đầu tại vị trí index của text, hoặc null nếu không có
     */
    static String matchAt(String text, int index) {
        for (String[] e : LIST) {
            if (text.startsWith(e[0], index)) {
                return e[0];
            }
        }
        return null;
    }

    /**
     * @return true nếu text chỉ gồm emoji trong LIST (bỏ qua khoảng trắng) và không quá maxCount emoji
     */
    static boolean isOnlyEmoji(String text, int maxCount) {
        int count = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            if (Character.isWhitespace(cp) || cp == VARIATION_SELECTOR) {
                i += Character.charCount(cp);
                continue;
            }
            String emoji = matchAt(text, i);
            if (emoji == null) return false;
            count++;
            i += emoji.length();
        }
        return count > 0 && count <= maxCount;
    }

    static Icon icon(String code, int size) {
        return new EmojiIcon(code, size);
    }

    /**
     * Icon vẽ 1 emoji. Toàn bộ tọa độ thiết kế trên lưới 24x24 rồi phóng to/thu nhỏ theo size.
     */
    private static class EmojiIcon implements Icon {
        private final String code;
        private final int size;

        EmojiIcon(String code, int size) {
            this.code = code;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 24.0, size / 24.0);

            switch (code) {
                case "😀": drawSmile(g2); break;
                case "😂": drawLaugh(g2); break;
                case "😍": drawLove(g2); break;
                case "😮": drawWow(g2); break;
                case "😢": drawSad(g2); break;
                case "😡": drawAngry(g2); break;
                case "😎": drawCool(g2); break;
                case "👍": drawThumbsUp(g2); break;
                case "❤": drawHeart(g2); break;
                case "🔥": drawFire(g2); break;
                default: break;
            }
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // =========================================================================
    // CÁC HÀM VẼ (lưới 24x24)
    // =========================================================================

    private static void face(Graphics2D g, Color fill, Color edge) {
        Ellipse2D circle = new Ellipse2D.Double(1, 1, 22, 22);
        g.setColor(fill);
        g.fill(circle);
        g.setColor(edge);
        g.setStroke(new BasicStroke(1f));
        g.draw(circle);
    }

    private static void featureStroke(Graphics2D g, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    private static void eyes(Graphics2D g, Color color) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(6.8, 7.3, 2.6, 3.6));
        g.fill(new Ellipse2D.Double(14.6, 7.3, 2.6, 3.6));
    }

    private static void drawSmile(Graphics2D g) {
        face(g, FACE, FACE_EDGE);
        eyes(g, FEATURE);
        g.setColor(FEATURE);
        g.fill(new Arc2D.Double(6, 9, 12, 10, 180, 180, Arc2D.CHORD));
    }

    private static void drawLaugh(Graphics2D g) {
        face(g, FACE, FACE_EDGE);
        // Mắt nhắm hình ^ ^
        featureStroke(g, FEATURE);
        Path2D eyeL = new Path2D.Double();
        eyeL.moveTo(6, 10);
        eyeL.lineTo(8.5, 7.5);
        eyeL.lineTo(11, 10);
        Path2D eyeR = new Path2D.Double();
        eyeR.moveTo(13, 10);
        eyeR.lineTo(15.5, 7.5);
        eyeR.lineTo(18, 10);
        g.draw(eyeL);
        g.draw(eyeR);
        g.fill(new Arc2D.Double(5.5, 10.5, 13, 9.5, 180, 180, Arc2D.CHORD));
        // Nước mắt 2 bên
        g.setColor(TEAR);
        g.fill(drop(3.2, 10.5, 3.4, 5.5));
        g.fill(drop(20.8, 10.5, 3.4, 5.5));
    }

    private static void drawLove(Graphics2D g) {
        face(g, FACE, FACE_EDGE);
        g.setColor(HEART);
        g.fill(heart(8.2, 9.5, 5.6));
        g.fill(heart(15.8, 9.5, 5.6));
        featureStroke(g, FEATURE);
        g.draw(new Arc2D.Double(7, 10, 10, 7.5, 200, 140, Arc2D.OPEN));
    }

    private static void drawWow(Graphics2D g) {
        face(g, FACE, FACE_EDGE);
        eyes(g, FEATURE);
        featureStroke(g, FEATURE);
        g.draw(new Arc2D.Double(5.8, 4.5, 4.5, 3, 30, 120, Arc2D.OPEN));
        g.draw(new Arc2D.Double(13.7, 4.5, 4.5, 3, 30, 120, Arc2D.OPEN));
        g.fill(new Ellipse2D.Double(9.5, 13.2, 5, 6.3));
    }

    private static void drawSad(Graphics2D g) {
        face(g, FACE, FACE_EDGE);
        eyes(g, FEATURE);
        featureStroke(g, FEATURE);
        g.draw(new Arc2D.Double(7.5, 15, 9, 6, 20, 140, Arc2D.OPEN));
        g.setColor(TEAR);
        g.fill(drop(16, 11.5, 3.2, 5));
    }

    private static void drawAngry(Graphics2D g) {
        face(g, ANGRY_FACE, ANGRY_EDGE);
        featureStroke(g, ANGRY_FEATURE);
        g.draw(new Line2D.Double(5.5, 6.8, 10.2, 8.8));
        g.draw(new Line2D.Double(18.5, 6.8, 13.8, 8.8));
        g.fill(new Ellipse2D.Double(7, 9.4, 2.6, 2.8));
        g.fill(new Ellipse2D.Double(14.4, 9.4, 2.6, 2.8));
        g.draw(new Arc2D.Double(7.5, 15, 9, 6, 20, 140, Arc2D.OPEN));
    }

    private static void drawCool(Graphics2D g) {
        face(g, FACE, FACE_EDGE);
        // Kính râm
        g.setColor(GLASSES);
        g.fill(new RoundRectangle2D.Double(4.5, 8, 6.8, 5, 2.5, 2.5));
        g.fill(new RoundRectangle2D.Double(12.7, 8, 6.8, 5, 2.5, 2.5));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(3.5, 8.6, 20.5, 8.6));
        featureStroke(g, FEATURE);
        g.draw(new Arc2D.Double(7.5, 11, 9, 6.5, 200, 140, Arc2D.OPEN));
    }

    private static void drawThumbsUp(Graphics2D g) {
        Shape thumb = new RoundRectangle2D.Double(9.5, 2.5, 5.2, 10.5, 5.2, 5.2);
        Shape fist = new RoundRectangle2D.Double(8.5, 10, 11.5, 11.5, 4, 4);
        Area hand = new Area(thumb);
        hand.add(new Area(fist));

        g.setColor(FACE);
        g.fill(hand);
        g.setColor(FACE_EDGE);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(hand);
        // Kẽ ngón tay
        g.draw(new Line2D.Double(14, 13.8, 20, 13.8));
        g.draw(new Line2D.Double(14, 16.6, 20, 16.6));
        g.draw(new Line2D.Double(14, 19.2, 19.5, 19.2));
        // Cổ tay áo
        g.setColor(SLEEVE);
        g.fill(new RoundRectangle2D.Double(3, 10.5, 5, 11, 1.6, 1.6));
    }

    private static void drawHeart(Graphics2D g) {
        g.setColor(HEART);
        g.fill(heart(12, 12.5, 21));
        // Điểm sáng
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(new Ellipse2D.Double(5.5, 6.5, 4, 3));
    }

    private static void drawFire(Graphics2D g) {
        Path2D outer = new Path2D.Double();
        outer.moveTo(12, 1.5);
        outer.curveTo(15, 6.5, 20, 9, 19.3, 15);
        outer.curveTo(18.8, 20, 15.5, 22.5, 12, 22.5);
        outer.curveTo(8.5, 22.5, 4.8, 20, 4.8, 15.5);
        outer.curveTo(4.8, 12, 6.8, 10, 8, 7.8);
        outer.curveTo(8.5, 10.5, 9.5, 11.5, 10.5, 12);
        outer.curveTo(10, 8, 11, 5, 12, 1.5);
        outer.closePath();
        g.setColor(FIRE_OUTER);
        g.fill(outer);
        // Viền sẫm để ngọn lửa không chìm khi nằm trên bong bóng màu đỏ
        g.setColor(FIRE_EDGE);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(outer);

        Path2D inner = new Path2D.Double();
        inner.moveTo(12, 10.5);
        inner.curveTo(14, 13.5, 16.2, 15, 15.7, 18);
        inner.curveTo(15.2, 20.5, 13.6, 21.6, 12, 21.6);
        inner.curveTo(10, 21.6, 8.4, 20, 8.4, 18);
        inner.curveTo(8.4, 16, 10, 15.5, 10.5, 14);
        inner.curveTo(11.5, 15, 12, 13, 12, 10.5);
        inner.closePath();
        g.setColor(FIRE_INNER);
        g.fill(inner);
    }

    /**
     * Hình trái tim rộng w, tâm (cx, cy): ghép 2 hình tròn với 1 tam giác tiếp tuyến.
     */
    private static Shape heart(double cx, double cy, double w) {
        double r = w / 4;
        double k = Math.sqrt(0.5) * r;     // r * cos(45°)
        double cyCircle = cy - k;           // căn giữa theo chiều dọc
        double cxL = cx - r;
        double cxR = cx + r;

        Area a = new Area(new Ellipse2D.Double(cxL - r, cyCircle - r, 2 * r, 2 * r));
        a.add(new Area(new Ellipse2D.Double(cxR - r, cyCircle - r, 2 * r, 2 * r)));
        Path2D tri = new Path2D.Double();
        tri.moveTo(cxL - k, cyCircle + k);
        tri.lineTo(cx, cyCircle + k + r + k);
        tri.lineTo(cxR + k, cyCircle + k);
        tri.lineTo(cxR, cyCircle);
        tri.lineTo(cxL, cyCircle);
        tri.closePath();
        a.add(new Area(tri));
        return a;
    }

    /**
     * Giọt nước: đỉnh nhọn ở trên (cx, top), rộng w, cao h.
     */
    private static Shape drop(double cx, double top, double w, double h) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, top);
        p.curveTo(cx + w * 0.9, top + h * 0.55, cx + w * 0.55, top + h, cx, top + h);
        p.curveTo(cx - w * 0.55, top + h, cx - w * 0.9, top + h * 0.55, cx, top);
        p.closePath();
        return p;
    }
}
