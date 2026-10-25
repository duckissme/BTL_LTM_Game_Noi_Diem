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

    /**
     * S -> C:  MATCH_INFO;số_thứ_tự_trận;số_trận_thắng_đỏ;số_trận_thắng_xanh;số_giây_còn_lại_của_trận
     * Gửi ngay sau START_GAME (trận 1) và mỗi lần 2 bên đồng ý chơi tiếp (trận 2, 3, ...).
     */
    public static final String CMD_MATCH_INFO = "MATCH_INFO";

    /** Thời gian tối đa của 1 trận (giây). Hết giờ thì ai chiếm nhiều ô hơn thắng, bằng ô thì hòa. */
    public static final int MATCH_TIME_SECONDS = 4 * 60;

    /** C -> S:  MOVE;H|V;hàng;cột */
    public static final String CMD_MOVE = "MOVE";

    /** S -> C:  DRAW_LINE;H|V;hàng;cột;màu (màu: RED/BLUE) */
    public static final String CMD_DRAW_LINE = "DRAW_LINE";

    /** S -> C:  CAPTURE_BOX;hàng;cột;màu;điểm_đỏ;điểm_xanh */
    public static final String CMD_CAPTURE_BOX = "CAPTURE_BOX";

    /** S -> C:  CHANGE_TURN;màu_lượt_tiếp;thời_gian_còn_lại (mặc định TURN_TIME_SECONDS) */
    public static final String CMD_CHANGE_TURN = "CHANGE_TURN";

    /**
     * S -> C:  TURN_TIMEOUT;màu_người_hết_giờ;số_lần_hết_giờ_liên_tiếp
     * Server tự đếm giờ mỗi lượt; hết giờ thì gửi gói này, ngay sau đó là CHANGE_TURN cho người kia.
     * Lần thứ MAX_CONSECUTIVE_TIMEOUTS liên tiếp thì gửi GAME_OVER (lý do TIMEOUT) thay cho CHANGE_TURN.
     */
    public static final String CMD_TURN_TIMEOUT = "TURN_TIMEOUT";

    /** Thời gian tối đa cho mỗi lượt đi (giây) */
    public static final int TURN_TIME_SECONDS = 30;

    /** Hết giờ bấy nhiêu lần liên tiếp thì bị xử thua (đi được 1 nước là đếm lại từ 0) */
    public static final int MAX_CONSECUTIVE_TIMEOUTS = 3;

    /** C -> S:  SURRENDER (xin hàng / đầu hàng) */
    public static final String CMD_SURRENDER = "SURRENDER";

    /** C -> S:  LEAVE_ROOM (thoát ván về sảnh) */
    public static final String CMD_LEAVE_ROOM = "LEAVE_ROOM";

    /**
     * S -> C:  GAME_OVER;người_thắng;lý_do;số_trận_thắng_đỏ;số_trận_thắng_xanh
     * người_thắng = DRAW nếu hòa. lý_do: FULL_BOARD, SURRENDER, TIMEOUT, TIME_UP.
     * Số trận thắng là tỉ số của chuỗi trận giữa 2 người (đã tính cả trận vừa xong, trận hòa không cộng).
     */
    public static final String CMD_GAME_OVER = "GAME_OVER";

    /** Giá trị người_thắng trong GAME_OVER khi 2 bên hòa */
    public static final String WINNER_DRAW = "DRAW";

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

    /** C -> S:  CHAT;nội_dung (nội dung có thể chứa dấu ';') */
    public static final String CMD_CHAT = "CHAT";

    /** S -> C:  CHAT_MSG;người_gửi;nội_dung (gửi cho cả 2 người trong phòng, kể cả người gửi) */
    public static final String CMD_CHAT_MSG = "CHAT_MSG";

    /** Độ dài tối đa của 1 tin nhắn chat (Server tự cắt nếu dài hơn) */
    public static final int CHAT_MAX_LENGTH = 200;

    // =====================================================================
    // 7. LỊCH SỬ TRẬN ĐẤU
    // =====================================================================

    /**
     * C -> S:  GET_HISTORY;tên_người_chơi  (xem lịch sử của bất kỳ người chơi nào)
     *          GET_HISTORY                 (bỏ trống tên = xem của chính mình)
     */
    public static final String CMD_GET_HISTORY = "GET_HISTORY";

    /**
     * S -> C:  HISTORY;tên_người_chơi;tổng,thắng,thua,hòa;trận|trận|...
     * Mọi số liệu tính theo góc nhìn của tên_người_chơi. Danh sách xếp mới nhất trước,
     * tối đa HISTORY_LIMIT trận (phần tổng kết tính trên TẤT CẢ các trận).
     * Mỗi trận: đối_thủ,màu_quân,điểm_mình,điểm_đối_thủ,kết_quả,lý_do,thời_điểm
     *   - kết_quả:   WIN / LOSE / DRAW / UNKNOWN (trận cũ chưa lưu người thắng và kết thúc sớm)
     *   - lý_do:     FULL_BOARD / SURRENDER / TIMEOUT / TIME_UP (để trống với trận cũ chưa lưu lý do)
     *   - thời_điểm: số mili-giây tính từ 01/01/1970 (epoch millis)
     * Hòa chỉ xảy ra khi hết MATCH_TIME_SECONDS mà 2 bên chiếm số ô bằng nhau.
     */
    public static final String CMD_HISTORY = "HISTORY";
    /** S -> C:  HISTORY_FAIL;lý_do (người chơi không tồn tại hoặc lỗi cơ sở dữ liệu) */
    public static final String CMD_HISTORY_FAIL = "HISTORY_FAIL";

    public static final String RESULT_WIN = "WIN";
    public static final String RESULT_LOSE = "LOSE";
    public static final String RESULT_DRAW = "DRAW";
    public static final String RESULT_UNKNOWN = "UNKNOWN";

    public static final String REASON_FULL_BOARD = "FULL_BOARD";
    public static final String REASON_SURRENDER = "SURRENDER";
    /** Hết giờ MAX_CONSECUTIVE_TIMEOUTS lượt liên tiếp */
    public static final String REASON_TIMEOUT = "TIMEOUT";
    /** Hết MATCH_TIME_SECONDS của trận */
    public static final String REASON_TIME_UP = "TIME_UP";

    /** Số trận gần nhất tối đa gửi về trong HISTORY */
    public static final int HISTORY_LIMIT = 50;

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
