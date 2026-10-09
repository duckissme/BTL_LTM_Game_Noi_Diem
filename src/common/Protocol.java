package common;

public class Protocol {

    public static final String DELIMITER = ";";

    public static final String LIST_DELIMITER = "\\|";

    // =====================================================================
    // 1. XÁC THỰC: ĐĂNG NHẬP / ĐĂNG KÝ
    // =====================================================================

    /** C -> S:  LOGIN;tài_khoản;mật_khẩu */
    public static final String CMD_LOGIN = "LOGIN";
    /** S -> C:  LOGIN_SUCCESS;tài_khoản;điểm;số_trận_thắng */
    public static final String CMD_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    /** S -> C:  LOGIN_FAIL;lý_do (lý do có thể bỏ trống) */
    public static final String CMD_LOGIN_FAIL = "LOGIN_FAIL";

    /** C -> S:  REGISTER;tài_khoản;mật_khẩu */
    public static final String CMD_REGISTER = "REGISTER";
    /** S -> C:  REGISTER_SUCCESS */
    public static final String CMD_REGISTER_SUCCESS = "REGISTER_SUCCESS";
    /** S -> C:  REGISTER_FAIL;lý_do (lý do có thể bỏ trống) */
    public static final String CMD_REGISTER_FAIL = "REGISTER_FAIL";

    // =====================================================================
    // 2. SẢNH CHỜ (LOBBY)
    // =====================================================================

    /** S -> C:  LOBBY_DATA;tên,điểm,thắng,trạng_thái|tên,điểm,thắng,trạng_thái|... */
    public static final String CMD_LOBBY_DATA = "LOBBY_DATA";
    /** S -> C:  LEADERBOARD;tên,điểm,thắng|tên,điểm,thắng|... (đã sắp xếp theo hạng) */
    public static final String CMD_LEADERBOARD = "LEADERBOARD";

    // =====================================================================
    // 3. THÁCH ĐẤU
    // =====================================================================

    /** C -> S:  CHALLENGE_REQ;tên_đối_thủ  (mình gửi lời mời) */
    public static final String CMD_CHALLENGE_REQ = "CHALLENGE_REQ";
    /** S -> C:  CHALLENGE_INVITE;tên_người_mời  (có người mời mình) */
    public static final String CMD_CHALLENGE_INVITE = "CHALLENGE_INVITE";
    /**
     * C -> S:  CHALLENGE_RESP;tên_người_mời;ACCEPT|REJECT  (mình trả lời lời mời)
     * S -> C:  CHALLENGE_RESP;tên_đối_thủ;ACCEPT|REJECT    (đối thủ trả lời lời mời của mình)
     */
    public static final String CMD_CHALLENGE_RESP = "CHALLENGE_RESP";
    public static final String RESP_ACCEPT = "ACCEPT";
    public static final String RESP_REJECT = "REJECT";
    /**
     * C -> S:  CHALLENGE_CANCEL;tên_đối_thủ  (mình hủy lời mời đã gửi)
     * S -> C:  CHALLENGE_CANCEL               (người mời đã hủy lời mời gửi cho mình)
     */
    public static final String CMD_CHALLENGE_CANCEL = "CHALLENGE_CANCEL";

    // =====================================================================
    // 4. TRẬN ĐẤU (GAME ARENA)
    // =====================================================================

    /** S -> C:  START_GAME;tên_đối_thủ;màu_quân;màu_lượt_đi (màu: RED/BLUE) */
    public static final String CMD_START_GAME = "START_GAME";

    /** C -> S:  MOVE;H|V;hàng;cột */
    public static final String CMD_MOVE = "MOVE";

    /** S -> C:  DRAW_LINE;H|V;hàng;cột;màu (màu: RED/BLUE) */
    public static final String CMD_DRAW_LINE = "DRAW_LINE";

    /** S -> C:  CAPTURE_BOX;hàng;cột;màu;điểm_đỏ;điểm_xanh */
    public static final String CMD_CAPTURE_BOX = "CAPTURE_BOX";

    /** S -> C:  CHANGE_TURN;màu_lượt_tiếp;thời_gian_còn_lại (mặc định 30s) */
    public static final String CMD_CHANGE_TURN = "CHANGE_TURN";

    /** S -> C:  TURN_TIMEOUT;màu_người_hết_giờ */
    public static final String CMD_TURN_TIMEOUT = "TURN_TIMEOUT";

    /** C -> S:  SURRENDER (xin hàng / đầu hàng) */
    public static final String CMD_SURRENDER = "SURRENDER";

    /** C -> S:  LEAVE_ROOM (thoát ván về sảnh) */
    public static final String CMD_LEAVE_ROOM = "LEAVE_ROOM";

    /** S -> C:  GAME_OVER;người_thắng;lý_do (lý do: FULL_BOARD, SURRENDER, DISCONNECT...) */
    public static final String CMD_GAME_OVER = "GAME_OVER";

    // =====================================================================
    // 5. TÁI ĐẤU (REMATCH)
    // =====================================================================

    /** C -> S:  REMATCH_REQ (yêu cầu chơi tiếp) */
    public static final String CMD_REMATCH_REQ = "REMATCH_REQ";

    /** S -> C:  REMATCH_INVITE (đối thủ mời chơi tiếp) */
    public static final String CMD_REMATCH_INVITE = "REMATCH_INVITE";

    /** C -> S / S -> C:  REMATCH_RESP;ACCEPT|REJECT */
    public static final String CMD_REMATCH_RESP = "REMATCH_RESP";

    // =====================================================================
    // 6. CHAT TRONG PHÒNG
    // =====================================================================

    /** C -> S:  CHAT;nội_dung */
    public static final String CMD_CHAT = "CHAT";

    /** S -> C:  CHAT_MSG;người_gửi;nội_dung */
    public static final String CMD_CHAT_MSG = "CHAT_MSG";

    // =====================================================================
    // HẰNG SỐ MÀU QUÂN & NÉT VẼ
    // =====================================================================

    public static final String COLOR_RED = "RED";
    public static final String COLOR_BLUE = "BLUE";

    public static final String LINE_H = "H";
    public static final String LINE_V = "V";

    // =====================================================================
    // TRẠNG THÁI NGƯỜI CHƠI
    // Lưu ý: hiện Server (MockServerTest) gửi chuỗi "Đang rỗi" / "Đang bận",
    // còn Client nhận biết trạng thái bận bằng cách tìm chữ "bận" (LobbyFrame.isBusy).
    // Hai hằng dưới đây chưa được dùng ở đâu.
    // =====================================================================

    public static final String STATUS_FREE = "RỖI";
    public static final String STATUS_BUSY = "BẬN";
}
