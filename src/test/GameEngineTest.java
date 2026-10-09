package test;

import common.Protocol;
import server.GameEngine;
import java.awt.Point;
import java.util.List;

/**
 * Kiểm thử độc lập thuật toán GameEngine (Dots & Boxes 5x5).
 */
public class GameEngineTest {

    public static void main(String[] args) {
        System.out.println("=== KIỂM THỬ THUẬT TOÁN GAME ENGINE ===");
        GameEngine engine = new GameEngine();

        // 1. Kiểm tra nước đi hợp lệ
        boolean valid = engine.isValidMove(Protocol.LINE_H, 0, 0);
        System.out.println("1. Nước đi H(0,0) hợp lệ: " + (valid ? "PASS" : "FAIL"));

        // 2. Đi nước H(0,0)
        engine.placeLine(Protocol.LINE_H, 0, 0);
        boolean duplicate = engine.isValidMove(Protocol.LINE_H, 0, 0);
        System.out.println("2. Không cho đánh đè lên cạnh H(0,0): " + (!duplicate ? "PASS" : "FAIL"));

        // 3. Đánh 3 cạnh còn lại của ô (0,0): V(0,0), V(0,1), H(1,0)
        engine.placeLine(Protocol.LINE_V, 0, 0);
        engine.placeLine(Protocol.LINE_V, 0, 1);
        List<Point> captured = engine.checkCapturedBoxes(Protocol.LINE_H, 1, 0, Protocol.COLOR_RED);
        engine.placeLine(Protocol.LINE_H, 1, 0);
        captured = engine.checkCapturedBoxes(Protocol.LINE_H, 1, 0, Protocol.COLOR_RED);

        System.out.println("3. Phát hiện ô (0,0) bị khép kín: " + (!captured.isEmpty() ? "PASS" : "FAIL"));
        System.out.println("   Điểm người chơi Đỏ: " + engine.getScoreRed() + " (Kỳ vọng: 1)");

        System.out.println("=== HOÀN TẤT KIỂM THỬ ===");
    }
}
