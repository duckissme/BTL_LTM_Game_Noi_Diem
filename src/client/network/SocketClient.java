package client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.SwingUtilities;

/**
 * Lớp duy nhất trong Client nói chuyện trực tiếp với Server qua TCP.
 *
 * Cách hoạt động:
 *  - Dùng mẫu Singleton: cả chương trình chỉ có 1 kết nối, lấy ra bằng SocketClient.getInstance().
 *  - GỬI: các màn hình gọi send("LỆNH;tham_số;...") — mỗi gói tin là 1 dòng văn bản.
 *  - NHẬN: một luồng (thread) chạy ngầm liên tục đọc từng dòng Server gửi về,
 *          rồi chuyển cho màn hình đang đăng ký nhận (MessageListener).
 *  - Mọi dữ liệu đều mã hóa UTF-8 để không lỗi tiếng Việt.
 *
 * Ví dụ sử dụng:
 * <pre>
 *     SocketClient client = SocketClient.getInstance();
 *     client.connect("127.0.0.1", 8888);
 *     client.setMessageListener(this);
 *     if (!client.send("LOGIN;huy;123456")) {
 *         // Chưa kết nối được tới Server -> báo lỗi cho người dùng
 *     }
 * </pre>
 */
public class SocketClient {

    /** Địa chỉ Server mặc định (dùng khi chạy thử trên cùng 1 máy) */
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8888;

    /** Chờ tối đa 5 giây khi kết nối, quá thời gian thì báo lỗi thay vì treo chương trình */
    private static final int CONNECT_TIMEOUT_MS = 5000;

    private static SocketClient instance;

    // Lưu lại địa chỉ đã dùng để có thể tự kết nối lại (xem ensureConnected)
    private String lastHost = DEFAULT_HOST;
    private int lastPort = DEFAULT_PORT;

    // "volatile": các biến này được đọc/ghi từ nhiều luồng khác nhau (luồng giao diện
    // và luồng đọc ngầm). volatile đảm bảo luồng nào cũng thấy giá trị mới nhất.
    private volatile Socket socket;
    private volatile PrintWriter out;
    private volatile boolean isRunning = false;
    private volatile MessageListener messageListener;

    // Constructor private: bên ngoài không tự "new SocketClient()" được, phải dùng getInstance()
    private SocketClient() {
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    /**
     * Chọn màn hình sẽ nhận gói tin từ Server.
     * Màn hình nào đang hiển thị thì gọi hàm này với chính nó (this).
     */
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    /**
     * Mở kết nối TCP tới Server và bắt đầu luồng đọc dữ liệu ngầm.
     *
     * @return true nếu kết nối thành công (hoặc đã kết nối từ trước), false nếu thất bại
     */
    public synchronized boolean connect(String host, int port) {
        // Đang kết nối rồi thì không mở thêm kết nối thứ 2
        if (isConnected()) {
            return true;
        }
        lastHost = host;
        lastPort = port;

        Socket newSocket = new Socket();
        try {
            // Kết nối có giới hạn thời gian chờ
            newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);

            // Bọc luồng byte thành luồng chữ, chỉ rõ UTF-8 cho CẢ 2 CHIỀU đọc và ghi
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8),
                    true); // autoFlush = true: gọi println() là gửi đi ngay

            socket = newSocket;
            out = writer;
            isRunning = true;

            // Luồng đọc ngầm. Daemon = tự dừng khi chương trình tắt, không giữ ứng dụng sống.
            Thread listenerThread = new Thread(() -> listenLoop(newSocket, reader), "Client-Socket-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("[Client] Đã kết nối TCP thành công: " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Lỗi kết nối Socket: " + e.getMessage());
            closeQuietly(newSocket);
            return false;
        }
    }

    /**
     * Nếu chưa kết nối (hoặc đã mất kết nối) thì thử kết nối lại tới địa chỉ dùng lần trước.
     * Tiện dùng trước khi gửi gói tin quan trọng như Đăng nhập / Đăng ký.
     *
     * @return true nếu sau khi gọi hàm, Client đang kết nối với Server
     */
    public synchronized boolean ensureConnected() {
        return isConnected() || connect(lastHost, lastPort);
    }

    /**
     * @return true nếu đang có kết nối còn sống tới Server
     */
    public boolean isConnected() {
        Socket s = socket;
        return isRunning && s != null && !s.isClosed();
    }

    /**
     * Vòng lặp chạy trên luồng ngầm: đọc từng dòng Server gửi về cho tới khi mất kết nối.
     *
     * @param mySocket socket mà luồng này phụ trách (để không đóng nhầm kết nối mới
     *                 nếu sau này đã kết nối lại)
     */
    private void listenLoop(Socket mySocket, BufferedReader reader) {
        String reason = "Server đã đóng kết nối";
        try {
            String line;
            // readLine() đứng chờ tới khi có 1 dòng mới; trả về null khi Server đóng kết nối
            while (isRunning && (line = reader.readLine()) != null) {
                final String msg = line;
                // Swing chỉ cho phép cập nhật giao diện trên luồng EDT,
                // nên chuyển gói tin sang EDT rồi mới đưa cho màn hình xử lý
                SwingUtilities.invokeLater(() -> {
                    MessageListener listener = messageListener;
                    if (listener != null) {
                        listener.onMessageReceived(msg);
                    }
                });
            }
        } catch (IOException e) {
            // Rớt mạng, Server sập... hoặc do chính mình gọi close() (socket bị đóng khi đang đọc)
            reason = e.getMessage();
        }

        // Phân biệt 2 trường hợp thoát vòng lặp:
        //  - Mình chủ động gọi close()  -> isRunning đã = false -> không báo gì
        //  - Mất kết nối ngoài ý muốn   -> isRunning vẫn = true  -> dọn dẹp và báo cho màn hình
        boolean unexpected;
        synchronized (this) {
            unexpected = isRunning && socket == mySocket;
            if (unexpected) {
                close();
            }
        }

        if (unexpected) {
            System.err.println("[Client] Mất kết nối tới Server: " + reason);
            final String finalReason = reason;
            SwingUtilities.invokeLater(() -> {
                MessageListener listener = messageListener;
                if (listener != null) {
                    listener.onDisconnected(finalReason);
                }
            });
        }
    }

    /**
     * Gửi 1 gói tin (1 dòng văn bản) lên Server.
     *
     * Lưu ý: PrintWriter KHÔNG ném lỗi khi gửi thất bại, nên phải hỏi checkError()
     * thì mới biết gửi được hay không.
     *
     * @return true nếu đã gửi đi, false nếu chưa kết nối hoặc gửi bị lỗi
     */
    public boolean send(String message) {
        PrintWriter writer = out;
        if (!isConnected() || writer == null) {
            System.err.println("[Client] Chưa kết nối Server, không gửi được: " + message);
            return false;
        }
        writer.println(message);
        return !writer.checkError();
    }

    /**
     * Chủ động đóng kết nối (ví dụ khi tắt chương trình).
     * Đóng socket thì luồng đọc/ghi gắn với nó cũng tự đóng theo,
     * đồng thời luồng đọc ngầm sẽ thoát khỏi vòng lặp.
     */
    public synchronized void close() {
        isRunning = false; // đánh dấu trước để listenLoop biết đây là đóng chủ động
        closeQuietly(socket);
        socket = null;
        out = null;
    }

    private static void closeQuietly(Socket s) {
        if (s == null) {
            return;
        }
        try {
            s.close();
        } catch (IOException e) {
            System.err.println("[Client] Lỗi khi đóng Socket: " + e.getMessage());
        }
    }
}
