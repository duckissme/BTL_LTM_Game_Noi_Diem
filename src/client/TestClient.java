package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Dummy Client giao diện dòng lệnh (CLI) phục vụ việc kiểm thử nhanh các lệnh Protocol qua terminal.
 */
public class TestClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8888);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("==== DUMMY CLIENT ĐÃ KẾT NỐI SERVER ====");
            System.out.println("Nhập cú pháp (VD: LOGIN;admin;123 hoặc LOGOUT)");

            // Luồng 1: Liên tục lắng nghe Server trả về
            Thread listenerThread = new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("\n[Server phản hồi] >> " + response);
                        System.out.print("Nhập lệnh tiếp theo: "); // In lại dấu nhắc
                    }
                } catch (IOException e) {
                    System.out.println("\nĐã ngắt kết nối khỏi Server.");
                }
            });
            listenerThread.start();

            // Luồng 2: Nhập từ bàn phím và đẩy lên Server
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("EXIT")) {
                    break;
                }
                out.println(input);
            }
        } catch (IOException e) {
            System.err.println("Không thể kết nối đến Server. Hãy chắc chắn Server đang chạy.");
        }
    }
}