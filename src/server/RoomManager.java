package server;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RoomManager: Quản lý danh sách các phòng đấu đang diễn ra trên Server.
 */
public class RoomManager {

    private static final ConcurrentHashMap<String, GameRoom> activeRooms = new ConcurrentHashMap<>();

    public static GameRoom createRoom(ClientHandler playerRed, ClientHandler playerBlue) {
        String roomId = "ROOM_" + UUID.randomUUID().toString().substring(0, 8);
        GameRoom room = new GameRoom(roomId, playerRed, playerBlue);
        activeRooms.put(roomId, room);
        return room;
    }

    public static GameRoom getRoom(String roomId) {
        if (roomId == null) return null;
        return activeRooms.get(roomId);
    }

    public static GameRoom findRoomByPlayer(String username) {
        if (username == null) return null;
        for (GameRoom room : activeRooms.values()) {
            if (room.hasPlayer(username)) {
                return room;
            }
        }
        return null;
    }

    public static void removeRoom(String roomId) {
        if (roomId != null) {
            activeRooms.remove(roomId);
        }
    }
}
