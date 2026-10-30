package entity;

import common.PlayerStatus;

/**
 * Entity đại diện cho người chơi, lưu trữ thông tin tài khoản, điểm số,
 * số trận đã đấu và trạng thái hoạt động trong hệ thống.
 */
public class Player {
    private String username;
    private int score;
    private int totalMatches;
    private PlayerStatus status;

    /**
     * Khởi tạo đối tượng Player mới với trạng thái mặc định là FREE (Đang rỗi).
     */
    public Player(String username, int score, int totalMatches) {
        this.username = username;
        this.score = score;
        this.totalMatches = totalMatches;
        this.status = PlayerStatus.FREE; 
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    /**
     * Chuyển đổi trạng thái enum thành chuỗi tiếng Việt hiển thị trên giao diện sảnh chờ.
     */
    public String getStatusString() {
        return status == PlayerStatus.FREE ? "Đang rỗi" : "Đang bận";
    }
}
