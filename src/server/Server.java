package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp chủ Server lắng nghe kết nối TCP trên cổng 8888.
 * Quản lý danh sách các client đang trực tuyến và phát tán dữ liệu sảnh chờ.
 */
public class Server {
    private static final int PORT = 8888;
    
    // Sử dụng ConcurrentHashMap để đảm bảo Thread-safe khi nhiều luồng cùng truy cập
    // Key: username, Value: ClientHandler quản lý kết nối đó
    public static ConcurrentHashMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Tầng Network: Server đang khởi chạy trên cổng " + PORT + "...");

        // Thêm cột lưu người thắng / lý do kết thúc nếu CSDL được tạo từ file SQL cũ
        database.MatchDAO.ensureSchema();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(">> Có kết nối vật lý mới từ: " + clientSocket.getInetAddress());
                
                // Cấp phát 1 luồng (Thread) riêng cho mỗi Client
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo Server: " + e.getMessage());
        }
    }

    /**
     * Tạo chuỗi dữ liệu danh sách người chơi online (loại trừ excludeUsername).
     * @param excludeUsername tài khoản không muốn đưa vào danh sách (thường là chính người nhận)
     * @return gói tin dạng "LOBBY_DATA;user,score,matches,status|..."
     */
    public static String buildLobbyData(String excludeUsername) {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : onlineUsers.values()) {
            entity.Player p = client.getPlayerInfo();
            if (p != null) {
                // Không hiển thị bản thân người chơi trên danh sách Online
                if (excludeUsername != null && excludeUsername.equalsIgnoreCase(p.getUsername())) {
                    continue;
                }
                sb.append(p.getUsername()).append(",")
                  .append(p.getScore()).append(",")
                  .append(p.getTotalMatches()).append(",")
                  .append(p.getStatusString()).append("|");
            }
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '|') {
            sb.setLength(sb.length() - 1);
        }
        return common.Protocol.CMD_LOBBY_DATA + common.Protocol.DELIMITER + sb.toString();
    }

    public static String buildLobbyData() {
        return buildLobbyData(null);
    }

    /**
     * Phát tán danh sách người chơi online tới toàn bộ client đang kết nối.
     * Mỗi client sẽ nhận được danh sách đã tự động loại bỏ tên của chính mình.
     */
    public static void broadcastLobbyData() {
        for (ClientHandler client : onlineUsers.values()) {
            entity.Player p = client.getPlayerInfo();
            String myUsername = (p != null) ? p.getUsername() : null;
            client.sendMessage(buildLobbyData(myUsername));
        }
    }
}