package server;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RoomManager: Quản lý danh sách các phòng đấu đang diễn ra trên Server.
 */
public class RoomManager {

    private static final ConcurrentHashMap<String, GameRoom> activeRooms = new ConcurrentHashMap<>();

    /**
     * Tạo một phòng đấu mới gán mã UUID duy nhất và lưu vào danh sách phòng đang hoạt động.
     * @param playerRed handler của người chơi cầm quân Đỏ
     * @param playerBlue handler của người chơi cầm quân Xanh
     * @return đối tượng GameRoom vừa khởi tạo
     */
    public static GameRoom createRoom(ClientHandler playerRed, ClientHandler playerBlue) {
        String roomId = "ROOM_" + UUID.randomUUID().toString().substring(0, 8);
        GameRoom room = new GameRoom(roomId, playerRed, playerBlue);
        activeRooms.put(roomId, room);
        return room;
    }

    /** Lấy phòng đấu theo mã ID */
    public static GameRoom getRoom(String roomId) {
        if (roomId == null) return null;
        return activeRooms.get(roomId);
    }

    /**
     * Tìm phòng đấu mà người chơi có username tương ứng đang tham gia.
     */
    public static GameRoom findRoomByPlayer(String username) {
        if (username == null) return null;
        for (GameRoom room : activeRooms.values()) {
            if (room.hasPlayer(username)) {
                return room;
            }
        }
        return null;
    }

    /**
     * Xóa phòng thi đấu khỏi danh sách hoạt động khi kết thúc hoặc giải tán.
     */
    public static void removeRoom(String roomId) {
        if (roomId != null) {
            activeRooms.remove(roomId);
        }
    }
}
