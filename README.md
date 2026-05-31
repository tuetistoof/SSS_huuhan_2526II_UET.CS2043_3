# CloudBid — Hệ Thống Đấu Giá Trực Tuyến

> **Đồ án môn Lập Trình Nâng Cao (CS2043) — Nhóm SSS (Huuhan)**  
> **Kiến trúc:** Client–Server đa luồng  
> **Công nghệ:** JavaFX 21 (Client) + Java Socket (Server) + MySQL 8.0 + Maven  
> **Tài liệu dự án:**  
> * [📄 Báo cáo BTL (OneDrive)](https://1drv.ms/w/c/a49f15581f61e2f3/IQDCZOaazyt9SasJdDgkvcAVAfDvFopFUlgUfr6WMrkBEbM?e=7jYRQ6)  
> * [🎥 Video Demo hệ thống (YouTube)](https://youtu.be/fg49XR1IxoI?si=F4GLxw9gnSC42smE)

---

## Mục lục
1. [Yêu cầu hệ thống](#1-yêu-cầu-hệ-thống)
2. [Cách chạy nhanh (dành cho thầy/cô chấm bài)](#2-cách-chạy-nhanh-dành-cho-thầycô-chấm-bài)
3. [Hướng dẫn cài đặt chi tiết](#3-hướng-dẫn-cài-đặt-chi-tiết)
4. [Hướng dẫn sử dụng chi tiết](#4-hướng-dẫn-sử-dụng-chi-tiết)
5. [Thiết lập cơ sở dữ liệu](#5-thiết-lập-cơ-sở-dữ-liệu)
6. [Tùy chỉnh cấu hình (nếu cần)](#6-tùy-chỉnh-cấu-hình-nếu-cần)
7. [Giải thích cơ chế tự động kết nối & Fallback](#7-giải-thích-cơ-chế-tự-động-kết-nối--fallback)
8. [Build từ source](#8-build-từ-source)
9. [Dành cho thành viên nhóm (dev)](#9-dành-cho-thành-viên-nhóm-dev)
10. [Tài khoản demo](#10-tài-khoản-demo)

---

## 1. Yêu cầu hệ thống

| Thành phần | Phiên bản tối thiểu | Ghi chú |
|---|---|---|
| **Java (JDK/JRE)** | 17 trở lên | Tải tại: [Adoptium](https://adoptium.net) *(Khuyên dùng JDK 21 hoặc 25)* |
| **MySQL Server** | 8.0 trở lên | Quản lý lưu trữ dữ liệu thực thể và phiên giao dịch |
| **RAM** | 512 MB trống | Đủ cho cả tiến trình Server và Client cùng hoạt động |
| **Hệ điều hành** | Windows / macOS / Linux | Đã thử nghiệm hoạt động đa nền tảng ổn định |

> **Kiểm tra phiên bản Java:** Mở Terminal (Linux/macOS) hoặc CMD/PowerShell (Windows) và chạy lệnh:  
> `java -version`

---

## 2. Cách chạy nhanh (dành cho thầy/cô chấm bài)

### Bước 1 — Chuẩn bị cơ sở dữ liệu
> *(Chỉ cần làm một lần duy nhất)*

Mở MySQL và chạy các lệnh dưới đây để tạo cơ sở dữ liệu và nạp dữ liệu mẫu ban đầu:
```bash
# Tạo cơ sở dữ liệu mới
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS cloud CHARACTER SET utf8mb4;"

# Nạp dữ liệu mẫu (nhập mật khẩu MySQL của thầy/cô khi được hỏi)
mysql -u root -p cloud < server/src/main/resources/db/init.sql
```

### Bước 2 — Cấu hình mật khẩu MySQL (nếu cần)
Mở file `client.properties` (đặt cùng thư mục với file `client.jar`) và điền mật khẩu MySQL của thầy/cô vào dòng tương ứng:
```properties
local.db.username=root
local.db.password=<mật_khẩu_mysql_của_thầy_cô>
```

### Bước 3 — Chạy ứng dụng
Đặt ba file **`client.jar`**, **`server.jar`** và **`client.properties`** trong **cùng một thư mục**, sau đó mở Terminal tại thư mục này và chạy:
```bash
java -jar client.jar
```

**Chỉ cần vậy thôi.** Ứng dụng khách sẽ tự động:
1. Thử kết nối tới máy chủ chung của nhóm (nếu đang online).
2. Nếu máy chủ nhóm offline → Tự động khởi chạy `server.jar` cục bộ ngay trên máy thầy/cô (tự truyền cấu hình database qua biến môi trường).
3. Đợi trong giây lát và hiển thị giao diện đăng nhập trực quan.

> 📋 Lịch sử log của server cục bộ sẽ được ghi tự động ra file `server-local.log` trong cùng thư mục.

---

## 3. Hướng dẫn cài đặt chi tiết

Nếu thầy/cô hoặc nhà phát triển muốn chạy hệ thống một cách thủ công và độc lập từng thành phần từ repository, hãy làm theo các bước dưới đây:

### 3.1. Clone dự án về máy tính
```bash
git clone https://github.com/tuetistoof/Coud_auction_system.git
cd Coud_auction_system
```

### 3.2. Import database thủ công
1. Đảm bảo MySQL Server đang hoạt động trên cổng mặc định `3306`.
2. Tạo database tên `cloud` thông qua MySQL Workbench, phpMyAdmin hoặc CLI:
   ```sql
   CREATE DATABASE cloud CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Import file dữ liệu mẫu có sẵn tại đường dẫn: `server/src/main/resources/db/init.sql`.

### 3.3. Cấu hình các file thuộc tính
*   **Cấu hình Server:** Mở file `server/src/main/resources/application.properties` để cấu hình khớp với tài khoản MySQL của bạn:
    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/cloud?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
    spring.datasource.username=root
    spring.datasource.password=your_password
    ```
*   **Cấu hình Client:** Mở file `client/src/main/resources/client.properties` để cấu hình địa chỉ Server mà Client sẽ tìm kiếm đầu tiên:
    ```properties
    server.host=localhost
    server.port=5000
    ```

---

## 4. Hướng dẫn sử dụng chi tiết

Hệ thống được thiết kế với cơ chế cập nhật thời gian thực đa luồng thông qua kết nối Socket TCP/IP, hỗ trợ đầy đủ các tính năng đặt thầu, tự động thầu và chống bắn tỉa.

### 4.1. Đăng nhập và Khám phá các Vai trò
Bạn có thể mở đồng thời nhiều cửa sổ Client khác nhau để mô phỏng sàn đấu giá thực tế:
1. **Người đấu giá (Bidder):**
   * Đăng nhập bằng tài khoản `bidder01` (hoặc tạo tài khoản mới).
   * Vào **Nạp tiền** ở góc phải để cộng số dư ảo của ví.
   * Chọn một sản phẩm trong trạng thái `RUNNING` để vào phòng đấu giá chi tiết.
   * **Đặt giá thầu (Normal Bid):** Nhập số tiền thầu lớn hơn giá tối thiểu yêu cầu và nhấn nút đặt thầu.
   * **Tự động đấu giá (Auto-Bid):** Thiết lập ngân sách thầu tối đa và bước giá tự động tăng. Server sẽ tự thầu hộ bạn mỗi khi có người vượt giá.
2. **Người bán (Seller):**
   * Đăng nhập bằng tài khoản `seller01`.
   * Chọn **Tạo phiên đấu giá** (Create Auction). Nhập tên sản phẩm, chọn loại sản phẩm (Art/Electronic/Vehicle) để kích hoạt **Factory Method** tương ứng.
   * Thiết lập giá khởi điểm, bước giá tối thiểu và thời gian kết thúc của sản phẩm.
3. **Quản trị viên (Admin):**
   * Đăng nhập bằng tài khoản `admin`.
   * Giám sát danh sách phiên thầu, xem log hệ thống thời gian thực hoặc hủy phiên đấu giá khẩn cấp nếu phát hiện bất thường.

### 4.2. Test tính năng cập nhật đồng bộ thời gian thực (Real-time Sync)
1. Mở song song **Client A** (`bidder01`) và **Client B** (đăng ký một tài khoản bidder mới) trên màn hình.
2. Cùng vào xem chi tiết một sản phẩm đấu giá.
3. Ở **Client A**, thực hiện đặt thầu.
4. Ngay lập tức, màn hình **Client B** sẽ tự động cập nhật số tiền thầu mới nhất, vẽ cột mốc mới trên biểu đồ biến động giá và hiển thị lịch sử lượt thầu của Client A trong danh sách mà **không cần tải lại trang**.

---

## 5. Thiết lập cơ sở dữ liệu

File schema đầy đủ (kèm dữ liệu bảng mẫu) nằm tại:
```
server/src/main/resources/db/init.sql
```

Nếu cần reset dữ liệu về trạng thái ban đầu sạch sẽ:
```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS cloud; CREATE DATABASE cloud CHARACTER SET utf8mb4;"
mysql -u root -p cloud < server/src/main/resources/db/init.sql
```

---

## 6. Tùy chỉnh cấu hình (nếu cần)

File `client.properties` (đặt cùng thư mục với `client.jar`):

```properties
# Server kết nối chính — nhóm sẽ cập nhật IP public/cloud vào đây
server.host=100.67.91.8
server.port=5000

# DB cho chế độ LOCAL (thầy/cô chỉnh ở đây nếu MySQL khác mặc định)
local.db.url=jdbc:mysql://localhost:3306/cloud?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
local.db.username=root
local.db.password=
```

**Lưu ý:** Nếu `client.properties` không tồn tại cạnh file `.jar`, các giá trị mặc định trong ứng dụng sẽ được dùng (`localhost:5000`, user `root`, không mật khẩu).

---

## 7. Giải thích cơ chế tự động kết nối & Fallback

```
Khởi động client.jar
        │
        ▼
Thử kết nối server.host:port (timeout 2 giây)
        │
   ┌────┴────┐
   │ Thành   │ Thất bại
   │ công    │
   ▼         ▼
Dùng      Tìm server.jar cùng thư mục
server         │
nhóm           ▼
           Khởi động server.jar local
           (truyền DB config qua env vars)
               │
               ▼
           Chờ tối đa 15 giây
               │
               ▼
           Kết nối localhost:5000
               │
               ▼
           Hiện UI đăng nhập
```

---

## 8. Build từ source

**Yêu cầu thêm:** Maven 3.8+ (hoặc dùng `./mvnw` đi kèm)

```bash
# Clone repo
git clone https://github.com/tuetistoof/Coud_auction_system.git
cd SSS_huuhan_2526II_UET.CS2043_3

# Build và đóng gói tất cả
./build_jars.sh

# Kết quả: server.jar và client.jar ở thư mục gốc
```

> **Windows:** chạy lệnh Maven trực tiếp trong Git Bash / CMD:
> ```cmd
> mvnw.cmd clean package -DskipTests
> copy server\target\server-0.0.1-SNAPSHOT.jar server.jar
> copy client\target\client-0.0.1-SNAPSHOT.jar client.jar
> ```

---

## 9. Dành cho thành viên nhóm (dev)

Kết nối server Tailscale chung của nhóm:

```bash
# Đảm bảo Tailscale trên máy đang chạy ổn định, sau đó chạy:
java -jar client.jar
# → client tự kết nối server.host trong client.properties (IP Tailscale của server)
```

Chạy server local độc lập khi dev offline:

```bash
# Terminal 1 — Khởi động server local
java -jar server.jar

# Terminal 2 — Khởi động client
java -jar client.jar
# → client thấy server.host Tailscale không phản hồi, tự động khởi tạo local server trên localhost
```

---

## 10. Tài khoản demo

Thầy/cô và các bạn có thể sử dụng danh sách tài khoản dưới đây để chạy thử nghiệm các vai trò trong hệ thống:

| Vai trò | Username | Mật khẩu | Ghi chú |
|---|---|---|---|
| **Admin** | `admin` | `admin123` | Có quyền dừng phiên đấu giá khẩn cấp, xem log hệ thống |
| **Người bán (Seller)** | `seller01` | `123456` | Đăng ký mặt hàng mới và tạo các phiên đấu giá |
| **Người đấu giá (Bidder)** | `bidder01` | `123456` | Xem chi tiết, nạp tiền ảo, đặt thầu tự động hoặc thủ công |

> Dữ liệu mẫu (mặt hàng, giao dịch thầu cũ) đã được nạp sẵn qua file `init.sql`.

---

*Đồ án môn Lập Trình Nâng Cao — Nhóm SSS (Huuhan) — UET, 2025–2026*
