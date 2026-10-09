# HƯỚNG DẪN KIỂM THỬ TRỰC TIẾP HỆ THỐNG GAME NỐI ĐIỂM (DOTS & BOXES)

---

## 1. Công cụ Khởi động Nhanh (1-Click Scripts)

Trong thư mục `D:\BTL_LTM`, bạn có thể nhấp đúp chạy trực tiếp các file script sau:

| File Script | Công dụng |
| :--- | :--- |
| **[test-full-flow.bat](file:///D:/BTL_LTM/test-full-flow.bat)** | **(Khuyên dùng)** Tự động mở Server và bật sẵn 2 cửa sổ Client để test đối kháng ngay lập tức. |
| **[run-server.bat](file:///D:/BTL_LTM/run-server.bat)** | Chỉ khởi động Server lắng nghe tại cổng `8888`. |
| **[run-client.bat](file:///D:/BTL_LTM/run-client.bat)** | Mở thêm 1 cửa sổ Client mới. |
| **[build.bat](file:///D:/BTL_LTM/build.bat)** | Tự động biên dịch lại toàn bộ dự án Java sau khi bạn sửa code. |

---

## 2. Tài khoản Có sẵn trong CSDL MySQL

Dữ liệu kiểm tra kết nối CSDL thực tế cho thấy các tài khoản sau đã sẵn sàng:

| Tên đăng nhập | Mật khẩu | Điểm số hiện tại | Số trận hiện tại |
| :--- | :--- | :--- | :--- |
| **`admin`** | `123` | 1500 | 25 |
| **`duc`** | `123` | 1800 | 40 |
| **`huy`** | `123` | 1500 | 30 |
| **`kien`** | `123` | 1200 | 15 |

*(Bạn cũng có thể bấm nút **"Đăng ký ngay"** trên giao diện để tạo tài khoản mới toanh kiểm tra luồng thêm vào DB)*.

---

## 3. Kịch bản Kiểm thử Thực tế (5 Bước Chi tiết)

### 📌 Bước 1: Khởi động hệ thống
- Chạy file **[test-full-flow.bat](file:///D:/BTL_LTM/test-full-flow.bat)**.
- Màn hình sẽ mở ra 3 cửa sổ:
  1. Cửa sổ dòng lệnh màu đen: **SERVER** đang lắng nghe cổng `8888`.
  2. Cửa sổ giao diện 1: **Client Đăng nhập thứ nhất**.
  3. Cửa sổ giao diện 2: **Client Đăng nhập thứ hai**.

---

### 📌 Bước 2: Đăng nhập & Kiểm tra Sảnh Chờ (Lobby)
1. **Ở Client 1**: Nhập `admin` / `123` -> Bấm **Đăng nhập**.
   - Cửa sổ chuyển sang **Sảnh Chờ**.
   - Thanh trên cùng hiển thị: `ĐIỂM: 1500`, `SỐ TRẬN: 25`, `TÀI KHOẢN: admin`.
   - Bảng **Danh sách Online** hiển thị `admin (Bạn)` với trạng thái **Đang rỗi**.
   - Bảng **Bảng xếp hạng** hiển thị top người chơi sắp xếp theo Điểm > Số trận (Top 1 là `duc`, Top 2 là `admin`...).
2. **Ở Client 2**: Nhập `duc` / `123` -> Bấm **Đăng nhập**.
   - Ngay lập tức, bảng Online ở cả 2 màn hình tự động cập nhật:
     - Client 1 thấy dòng của `duc` kèm nút **"Thách đấu"**.
     - Client 2 thấy dòng của `admin` kèm nút **"Thách đấu"**.

---

### 📌 Bước 3: Kiểm tra luồng Thách Đấu (Challenge 1vs1)
1. Trên **Client 1** (`admin`), nhấn nút **"Thách đấu"** ở dòng người chơi `duc`.
   - Client 1 xuất hiện hộp thoại: *"Đang gửi lời mời thách đấu tới [duc]..."*.
2. Trên **Client 2** (`duc`), ngay lập tức xuất hiện hộp thoại:
   - Tiêu đề: *"Lời mời thách đấu"*.
   - Nội dung: *"Người chơi [admin] muốn thách đấu với bạn! Bạn có muốn chấp nhận không?"*.
3. Trên **Client 2**, nhấn nút **"Chấp nhận"**:
   - Cả 2 client lập tức chuyển sang màn hình bàn cờ **GameBoardFrame**!
   - Cửa sổ Client 1 hiển thị: Bạn là phe **Đỏ** (`admin`), đối thủ là **Xanh** (`duc`).
   - Cửa sổ Client 2 hiển thị: Bạn là phe **Xanh** (`duc`), đối thủ là **Đỏ** (`admin`).
   - Lượt đi đầu tiên: **ĐỎ** (Client 1 đi trước).
   - Đồng hồ 30s đếm ngược bắt đầu chạy.
   - Trạng thái của cả 2 trên sảnh chuyển sang **Đang bận**.

---

### 📌 Bước 4: Kiểm tra Đánh Cờ & Khép Ô Ăn Điểm (Dots & Boxes Engine)
1. **Đi nước đầu tiên**:
   - Client 1 (Đỏ) rê chuột qua các cạnh -> Cạnh có hiệu ứng sáng (hover).
   - Client 1 bấm vào 1 cạnh (ví dụ cạnh trên của ô đầu tiên) -> Nét vẽ màu đỏ xuất hiện đồng thời trên cả 2 màn hình.
   - Hệ thống tự động chuyển lượt sang **XANH** và đồng hồ reset lại 30s.
2. **Thử ăn điểm (Khép ô 1x1)**:
   - Cho Đỏ và Xanh lần lượt nối 3 cạnh của ô `(0, 0)`.
   - Đến lượt người chơi nối cạnh thứ 4 để khép kín ô:
     - Ô vuông lập tức tô màu của người vừa đánh.
     - Tỉ số trên bảng điểm tăng lên **1 - 0**.
     - **Quan trọng (Luật chuẩn):** Người vừa ăn ô được **tiếp tục đánh tiếp (giữ lượt)** mà không bị đổi lượt!

---

### 📌 Bước 5: Kiểm tra Đầu hàng / Thoát trận & Cập nhật CSDL
1. Trên một trong hai client (ví dụ Client 2), nhấn nút **"THOÁT"** ở góc dưới bên phải:
   - Hộp thoại cảnh báo: *"Thoát trận sẽ bị xử thua! Bạn có chắc không?"*.
   - Chọn **Yes**.
2. **Kết quả trận đấu**:
   - Client 2 nhận thông báo: **THẤT BẠI! 😢** (Lý do: Bạn đã xin hàng/thoát trận).
   - Client 1 nhận thông báo: **CHIẾN THẮNG! 🎉** (Lý do: Đối thủ đã xin hàng/thoát trận).
3. **Kiểm tra lưu dữ liệu MySQL**:
   - Server tự động gọi `MatchDAO.saveMatchResult`, chèn 1 dòng mới vào bảng `matches` và cộng điểm/trận vào bảng `users`.
   - Khi bấm **"Về sảnh"**, điểm số và số trận mới của người chơi được cập nhật ngay trên Sảnh Chờ và Bảng Xếp Hạng.
