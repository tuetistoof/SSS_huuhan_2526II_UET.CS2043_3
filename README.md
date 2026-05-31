# CloudBid - Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

> **Môn học:** Lập trình nâng cao (INT2204) - Đại học Công nghệ, ĐHQGHN (UET-VNU)  
> **Nhóm thực hiện:** Nhóm 4

---

## 1. Giới thiệu Bài toán & Phạm vi Hệ thống

**CloudBid** là một hệ thống đấu giá trực tuyến thời gian thực được phát triển theo kiến trúc **Client-Server** đa luồng (Multi-threaded). Dự án mô phỏng sàn đấu giá ảo nơi người dùng có thể tham gia vào các phiên đấu giá với nhiều danh mục sản phẩm phong phú.

### Phạm vi và Đối tượng sử dụng
Hệ thống hỗ trợ phân quyền chi tiết với 3 nhóm vai trò (Roles) chính:
1. **Người đấu giá (Bidder):**
   * Đăng nhập/Đăng ký tài khoản và quản lý số dư ví cá nhân.
   * Xem danh sách các phiên đấu giá đang diễn ra, xem chi tiết phòng đấu giá.
   * Đặt giá thầu thủ công (Normal Bid) hoặc đặt cấu hình tự động đấu giá (Auto-Bid).
   * Theo dõi danh sách sản phẩm đã thắng, lịch sử thầu, và danh sách quan tâm (Watchlist).
   * Nhận thông báo thời gian thực khi bị vượt giá (Outbid), khi thắng cuộc, hoặc khi phiên kết thúc.
2. **Người bán (Seller):**
   * Đăng ký sản phẩm mới thông qua biểu mẫu nhập liệu và hệ thống định danh loại.
   * Tạo cấu hình phiên đấu giá mới (Giá khởi điểm, bước giá tối thiểu, thời gian bắt đầu, thời gian kết thúc).
   * Theo dõi tiến trình đấu giá và quản lý số dư khả dụng/số dư tạm giữ (Pending Balance).
3. **Quản trị viên (Admin):**
   * Giám sát hệ thống và người dùng.
   * Xem danh sách tất cả các phiên đấu giá và có quyền kết thúc/hủy phiên đấu giá khẩn cấp.
   * Theo dõi nhật ký hệ thống (System Logs).

---

## 2. Công nghệ Sử dụng & Yêu cầu Môi trường

Hệ thống sử dụng các công nghệ Java hiện đại, tập trung vào lập trình hướng đối tượng (OOP) thuần chất lượng cao:

* **Ngôn ngữ:** Java 25 (sử dụng các tính năng mới nhất để tối ưu hiệu năng).
* **Giao diện Client:** JavaFX 21 (sử dụng FXML cho Layout và CSS cho Styling).
* **Kết nối mạng (Networking):** Java Socket TCP/IP thuần, truyền tải dữ liệu dạng tin nhắn JSON (sử dụng thư viện `gson:2.10.1`).
* **Cơ sở dữ liệu:** MySQL 8.0, quản lý kết nối hiệu năng cao qua pool kết nối **HikariCP** (`HikariCP:4.0.3`).
* **Trình quản lý mã nguồn & build:** Maven (cấu hình dự án đa module).
* **Kiểm định code style:** Checkstyle (cấu hình Google Checks tùy chỉnh đạt chuẩn 0 lỗi).
* **Kiểm thử & Độ phủ:** JUnit 5 và JaCoCo Maven Plugin.

---

## 3. Cấu trúc Thư mục & Các Module Chính

Hệ thống được tổ chức thành 3 module Maven lồng nhau:

```text
├── common/             # Module dùng chung chứa Model, Enum, Exception và DTO
│   └── src/main/java/com/ssscloud/auction/common/
│       ├── enums/      # Các enum trạng thái (AuctionStatus, UserRole, BidType)
│       ├── exception/  # Cơ chế bắt lỗi tùy chỉnh (BaseException, DAOException, ServiceException...)
│       └── model/      # Cây kế thừa thực thể OOP (Entity -> User, Item, Auction, BidTransaction...)
│
├── server/             # Module Server xử lý Logic nghiệp vụ và Cơ sở dữ liệu
│   └── src/main/java/com/ssscloud/auction/server/
│       ├── controller/ # Định tuyến yêu cầu từ client (UserController, BidController...)
│       ├── dao/        # Tầng truy cập CSDL sử dụng SQL thuần và Hikari Connection Pool
│       ├── factory/    # Factory Method tạo các loại sản phẩm (Art, Electronic, Vehicle)
│       ├── networking/ # Socket server đa luồng (AuctionSocketServer, ClientHandler, ClientObserver)
│       └── service/    # Xử lý luồng nghiệp vụ lõi (ConcurrentBidManager, AutoBidService, AntiSnipingService...)
│
└── client/             # Module Client chứa UI JavaFX và Giao tiếp mạng
    └── src/main/java/com/ssscloud/auction/client/
        ├── controller/ # Controller điều hướng giao diện JavaFX (BiddingRoomController, Dashboard...)
        ├── networking/ # Client Socket gửi nhận và lắng nghe thông điệp từ server
        └── util/       # Các tiện ích (ThemeManager hỗ trợ Light/Dark mode, SceneManager chuyển giao diện...)
```

---

## 4. Vị trí Các File JAR (.jar)

Sau khi dự án được biên dịch thành công, các file Executable / Shaded Fat JAR sẽ nằm ở các vị trí tương ứng:

* **Server JAR:** `server/target/server-0.0.1-SNAPSHOT.jar`
* **Client JAR:** `client/target/client-0.0.1-SNAPSHOT.jar`

> 💡 **Mẹo tiện ích:** Để đơn giản hóa quá trình chạy, nhóm đã viết sẵn script đóng gói tự động. Sau khi chạy script này, hai file JAR gọn nhẹ sẽ được copy trực tiếp ra thư mục gốc của dự án với tên gọi:
> * **Server JAR:** `./server.jar`
> * **Client JAR:** `./client.jar`

---

## 5. Hướng dẫn Cài đặt & Chạy Chương trình

### Bước 1: Chuẩn bị Cơ sở dữ liệu
1. Tạo một cơ sở dữ liệu MySQL có tên là `cloud`.
2. Khởi tạo cấu trúc bảng và nạp dữ liệu mẫu bằng cách chạy file SQL tại đường dẫn:  
   `[root]/server/src/main/resources/db/init.sql`

### Bước 2: Cấu hình Kết nối CSDL và Server
1. Mở file cấu hình database của Server tại:  
   `[root]/server/src/main/resources/application.properties`
2. Chỉnh sửa URL kết nối, Username, và Password phù hợp với CSDL MySQL cục bộ của bạn:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/cloud?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

### Bước 3: Đóng gói chương trình (Biên dịch ra file JAR)
Chạy script đóng gói tự động ở thư mục gốc của dự án:
```bash
./build_jars.sh
```
*Script sẽ tự động dọn dẹp thư mục build cũ, biên dịch mã nguồn, chạy kiểm định chất lượng, đóng gói thư viện phụ thuộc và copy 2 file JAR `server.jar` và `client.jar` ra thư mục gốc.*

### Bước 4: Khởi chạy hệ thống theo thứ tự

1. **Chạy Server:**
   Mở terminal ở thư mục gốc của dự án và chạy lệnh:
   ```bash
   java -jar server.jar
   ```
   *Server sẽ bắt đầu lắng nghe kết nối tại cổng `5000` và ghi nhận hoạt động vào file `server.log` ở thư mục gốc.*

2. **Chạy Client (Có thể mở nhiều cửa sổ terminal khác nhau để chạy nhiều Client):**
   Mở một terminal mới ở thư mục gốc và chạy lệnh:
   ```bash
   java -jar client.jar
   ```
   *Bạn có thể mở nhiều terminal và chạy lệnh trên để kiểm tra tính năng thầu đồng thời (Concurrent Bidding).*

---

## 💡 Hướng dẫn chạy nhanh bằng Docker Compose (Khuyên Dùng)

Nếu máy bạn đã cài đặt Docker và Docker Compose, bạn có thể chạy toàn bộ hệ thống (gồm MySQL database tự khởi tạo + Server) bằng một lệnh duy nhất ở thư mục gốc:

```bash
docker compose up --build
```

Sau khi Server trong Docker khởi động xong, bạn chỉ cần mở máy khách client cục bộ để kết nối và trải nghiệm:
```bash
java -jar client.jar
```

---

## 6. Danh sách Chức năng Đã Hoàn Thành

| STT | Chức năng chính | Mô tả kỹ thuật | Trạng thái |
| :--- | :--- | :--- | :---: |
| 1 | **Đăng ký / Đăng nhập** | Xác thực tài khoản qua Socket, phân quyền vai trò (Admin, Bidder, Seller) | ✔ Hoàn thành |
| 2 | **Tạo phiên đấu giá** | Seller tạo phiên với cấu hình tùy biến. Áp dụng **Factory Method Pattern** để khởi tạo thông tin sản phẩm theo danh mục (Art, Electronic, Vehicle) | ✔ Hoàn thành |
| 3 | **Đấu giá trực tuyến** | Cập nhật thông tin giá thầu thời gian thực sử dụng **Observer Pattern** kết hợp Socket push tin nhắn JSON | ✔ Hoàn thành |
| 4 | **Tự động đấu giá (Auto-Bid)**| Người dùng đặt giới hạn ngân sách tối đa, hệ thống tự động nâng giá thầu dựa trên thuật toán hàng đợi ưu tiên | ✔ Hoàn thành |
| 5 | **Chống bắn tỉa (Anti-Sniping)**| Tự động cộng thêm 60 giây vào thời gian kết thúc nếu phát hiện lượt thầu hợp lệ trong 60 giây cuối cùng | ✔ Hoàn thành |
| 6 | **Xử lý Đấu giá Đồng thời** | Ngăn chặn race conditions khi nhiều người đặt giá cùng lúc bằng khóa `ReentrantLock` theo từng AuctionId tại server | ✔ Hoàn thành |
| 7 | **Hệ thống ví tiền (Wallet)** | Quản lý số dư khả dụng và số dư đóng băng tạm giữ (Locked Balance). Tự động trả tiền thầu cũ khi bị outbid và kết chuyển tiền khi hoàn tất | ✔ Hoàn thành |
| 8 | **Hệ thống thông báo** | Thông báo đẩy thời gian thực khi có biến động giá thầu, đổi vị thế thầu, thắng thầu, nhận tiền | ✔ Hoàn thành |
| 9 | **Light / Dark Mode** | Hỗ trợ chuyển đổi giao diện sáng/tối linh hoạt trực tiếp trên giao diện Client nhờ CSS Stylesheet | ✔ Hoàn thành |
| 10| **Trang giám sát Admin** | Admin xem log hoạt động hệ thống, hủy/dừng phiên đấu giá, theo dõi người dùng hoạt động | ✔ Hoàn thành |

---

## 7. Link Báo cáo & Video Demo

* **Báo cáo PDF (Tối đa 6 trang):** [👉 Link xem Báo cáo PDF](./Bao-cao-BTL-Nhom-4.pdf) *(Vui lòng cập nhật đường dẫn chính xác)*
* **Video Demo thực tế (Tối đa 3 phút):** [👉 Link xem Video Demo trên YouTube/Drive](#) *(Vui lòng cập nhật đường dẫn chính xác)*
