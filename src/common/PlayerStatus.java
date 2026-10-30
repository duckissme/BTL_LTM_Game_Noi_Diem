package common;

/**
 * Trạng thái hiện tại của người chơi trong hệ thống:
 * - FREE: Đang rỗi ở sảnh chờ, sẵn sàng nhận lời thách đấu.
 * - BUSY: Đang trong phòng thi đấu hoặc đang tham gia trận đấu.
 */
public enum PlayerStatus {
    FREE, 
    BUSY  
}
