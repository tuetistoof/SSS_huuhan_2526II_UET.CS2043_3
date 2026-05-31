# CloudBid — Hệ thống Đấu giá Trực tuyến

> **Đồ án môn CS2043 — Lập Trình Nâng Cao**  
> **Nhóm:** SSS (Huuhan) — UET, 2025–2026  
> **Kiến trúc:** Client–Server | JavaFX + Spring Boot + TCP Socket + MySQL

---

## Mô tả bài toán & Phạm vi hệ thống

**CloudBid** là nền tảng đấu giá trực tuyến thời gian thực, xây dựng trên kết nối TCP Socket thuần và trao đổi gói tin JSON. Hệ thống giải quyết các bài toán: đồng bộ dữ liệu đa client, quản lý thầu đồng thời (concurrency), tự động nâng giá (auto-bid) và chống bắn tỉa (anti-sniping).

Hệ thống hỗ trợ 3 vai trò người dùng:

- **Bidder (Người đấu giá):** Đăng ký/đăng nhập, nạp tiền vào ví ảo, vào phòng thầu để đặt thầu thủ công hoặc bật Auto-Bid, theo dõi Watchlist, nhận push notification khi bị vượt giá hoặc thắng thầu.
- **Seller (Người bán):** Tạo phiên đấu giá mới theo danh mục (Art / Electronic / Vehicle), thiết lập giá khởi điểm, bước giá, thời gian kết thúc và theo dõi số dư tạm giữ.
- **Admin (Quản trị viên):** Giám sát toàn bộ tài khoản và phiên đấu giá, có quyền dừng/hủy phiên thầu khẩn cấp.

---

## Công nghệ sử dụng, môi trường chạy & Yêu cầu cài đặt

| Thành phần | Yêu cầu tối thiểu | Chi tiết |
|---|---|---|
| **Java SDK** | 17+ (khuyên dùng 21) | JavaFX 21 (UI) + Java TCP Socket đa luồng |
| **Database** | MySQL 8.0+ | Connection pool HikariCP |
| **Build tool** | Maven 3.8+ | Dự án có sẵn Maven Wrapper (`mvnw`) |
| **Hệ điều hành** | Windows / macOS / Linux | Hỗ trợ Docker |
| **Docker** *(tuỳ chọn)* | Docker Desktop / Engine | Triển khai nhanh không cần cài đặt thủ công |

Kiểm tra Java: `java -version`. Nếu chưa có, tải tại [https://adoptium.net](https://adoptium.net).

---

## Cấu trúc thư mục & Các module chính

```
SSS_huuhan_2526II_UET.CS2043_3/
├── common/          # Thực thể OOP dùng chung: Entity, Enum, Exception
├── server/          # Logic nghiệp vụ: BidManager, AutoBidService, DAO, Socket Server
├── client/          # Giao diện JavaFX (FXML + CSS), Client Socket
├── server.jar       # Fat JAR server (chạy trực tiếp)
├── client.jar       # Fat JAR client (chạy trực tiếp)
├── client.properties# Cấu hình host/port server và DB (đặt cùng thư mục .jar)
├── docker-compose.yml
└── build_jars.sh    # Script build tự động
```

**3 module Maven độc lập:**

- **`common`** — Định nghĩa các thực thể cốt lõi (`User`, `Item`, `Auction`), Enum trạng thái và Exception phân lớp.
- **`server`** — Xử lý logic trung tâm (`ConcurrentBidManager`, `AutoBidService`, `AntiSnipingService`), tầng DAO (SQL thuần) và Socket Server đa luồng.
- **`client`** — Giao diện JavaFX (FXML + CSS, hỗ trợ Light/Dark Mode) và Client Socket lắng nghe dữ liệu đẩy từ Server.

---

## Vị trí các file JAR

Sau khi build, Fat JAR thực thi được đặt tại thư mục gốc:

| File | Đường dẫn |
|---|---|
| **Server JAR** | `./server.jar` |
| **Client JAR** | `./client.jar` |

Đường dẫn gốc trong `target/` (trước khi copy):
- `server/target/server-0.0.1-SNAPSHOT.jar`
- `client/target/client-0.0.1-SNAPSHOT.jar`

---

## Hướng dẫn chạy Server và Client

> **Tổng quan:** Hệ thống gồm 2 thành phần chạy độc lập — **Server** (xử lý logic, kết nối DB) và **Client** (giao diện JavaFX). Server phải khởi động **trước**, sau đó mới chạy Client. Để chạy nhiều client đồng thời, mở thêm Terminal và chạy lại lệnh `java -jar client.jar` ở mỗi Terminal.

---

### Cách 1 — Chạy trực tiếp bằng file JAR có sẵn *(Dành cho người chấm bài)*

Đây là cách nhanh nhất, không cần cài Maven hay build lại.

#### Bước 1 — Kiểm tra Java

Mở Terminal/CMD và chạy:

```bash
java -version
```

Yêu cầu **Java 17 trở lên**. Nếu chưa có, tải tại [https://adoptium.net](https://adoptium.net) và cài đặt, sau đó thử lại.

#### Bước 2 — Chuẩn bị thư mục chạy

Đặt 3 file sau vào **cùng một thư mục** trên máy (ví dụ: `D:\cloudbid\`):

```
cloudbid/
├── server.jar
├── client.jar
└── client.properties     ← tạo file này nếu chưa có (xem Bước 3)
```

#### Bước 3 — Tạo / chỉnh file `client.properties`

Tạo file `client.properties` trong cùng thư mục với nội dung sau, **sửa lại mật khẩu MySQL** cho khớp với máy của bạn:

```properties
# Địa chỉ server (để localhost khi chạy local)
server.host=localhost
server.port=5000

# Thông tin kết nối MySQL cho chế độ local
local.db.url=jdbc:mysql://localhost:3306/cloud?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
local.db.username=root
local.db.password=YOUR_MYSQL_PASSWORD
```

> Nếu bạn không đặt file này, ứng dụng dùng mặc định: `localhost`, user `root`, không mật khẩu.

#### Bước 4 — Khởi tạo database MySQL

Đảm bảo MySQL đang chạy trên máy (cổng `3306`), sau đó chạy 2 lệnh sau trong Terminal:

```bash
# Tạo database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS cloud CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Nạp schema và dữ liệu mẫu (chạy từ thư mục gốc dự án)
mysql -u root -p cloud < server/src/main/resources/db/init.sql
```

> Lệnh trên sẽ hỏi mật khẩu MySQL của bạn. Nếu dùng MySQL Workbench hoặc DBeaver, bạn có thể import file `server/src/main/resources/db/init.sql` trực tiếp qua giao diện đồ họa.

#### Bước 5 — Cấu hình Server (nếu build lại từ source)

Mở file `server/src/main/resources/application.properties`, đảm bảo thông tin DB khớp với máy:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cloud?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

#### Bước 6 — Chạy Server

Mở **Terminal 1**, điều hướng vào thư mục chứa JAR và chạy:

```bash
java -jar server.jar
```

Server khởi động thành công khi Terminal hiển thị dòng tương tự:

```
Socket Server started on port 5000
Waiting for connections...
```

> Giữ Terminal này **mở trong suốt quá trình chạy**. Đừng đóng lại.

#### Bước 7 — Chạy Client

Mở **Terminal 2** mới (giữ Terminal 1 của Server vẫn chạy), điều hướng vào **cùng thư mục** và chạy:

```bash
java -jar client.jar
```

Sau 3–5 giây, cửa sổ đăng nhập JavaFX sẽ hiện ra.

#### Bước 8 — Chạy nhiều Client đồng thời

Để mô phỏng nhiều người đấu giá cùng lúc, mở thêm Terminal mới và chạy lại lệnh trên ở **mỗi Terminal**:

```bash
# Terminal 3
java -jar client.jar

# Terminal 4
java -jar client.jar
```

Mỗi lần chạy sẽ mở thêm một cửa sổ Client độc lập, tất cả kết nối vào cùng một Server.

---

### Cách 2 — Chạy bằng Docker Compose *(Không cần cài Java/MySQL trên máy)*

Yêu cầu: đã cài **Docker Desktop** (Windows/macOS) hoặc **Docker Engine** (Linux) và đang chạy.

#### Bước 1 — Tạo file `.env` (cấu hình mật khẩu DB)

Tạo file `.env` ở thư mục gốc dự án:

```env
DB_USERNAME=root
DB_PASSWORD=YOUR_MYSQL_PASSWORD
```

#### Bước 2 — Khởi động MySQL + Server trong Docker

Mở Terminal tại thư mục gốc dự án, chạy:

```bash
docker compose up --build
```

Docker sẽ tự động: tạo container MySQL → nạp `init.sql` → build Server → khởi động Server.  
Chờ đến khi Terminal hiển thị `Socket Server started on port 5000` thì tiếp tục.

#### Bước 3 — Chạy Client trên máy thật

Mở Terminal mới (máy thật, không phải trong Docker), chạy:

```bash
java -jar client.jar
```

> Client vẫn chạy trực tiếp trên máy thật (không qua Docker) vì JavaFX cần hiển thị giao diện đồ họa.

#### Dừng Docker khi xong

```bash
docker compose down
```

---

### Cách 3 — Build từ source rồi chạy *(Dành cho nhà phát triển)*

Yêu cầu: **Maven 3.8+** (hoặc dùng Maven Wrapper `mvnw` có sẵn trong dự án).

#### Linux / macOS

```bash
# Clone dự án
git clone https://github.com/tuetistoof/Coud_auction_system.git
cd SSS_huuhan_2526II_UET.CS2043_3

# Cấp quyền và build (tự động copy JAR ra thư mục gốc)
chmod +x build_jars.sh
./build_jars.sh
```

Sau khi build xong, thư mục gốc sẽ có `server.jar` và `client.jar`. Tiếp tục từ **Bước 4** của Cách 1.

#### Windows

```cmd
:: Clone dự án
git clone https://github.com/tuetistoof/Coud_auction_system.git
cd SSS_huuhan_2526II_UET.CS2043_3

:: Build và copy JAR
mvnw.cmd clean package -DskipTests
copy server\target\server-0.0.1-SNAPSHOT.jar server.jar
copy client\target\client-0.0.1-SNAPSHOT.jar client.jar
```

Tiếp tục từ **Bước 4** của Cách 1.

---

### Xử lý sự cố thường gặp

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Connection refused` khi chạy Client | Server chưa khởi động | Chạy `server.jar` trước, chờ thấy `port 5000` |
| `Access denied for user 'root'` | Sai mật khẩu MySQL | Sửa lại `client.properties` và `application.properties` |
| `Unknown database 'cloud'` | Chưa tạo DB | Chạy lại lệnh ở Bước 4 |
| Cửa sổ JavaFX không hiện | Java thiếu JavaFX module | Dùng đúng bản JDK có JavaFX (Azul Zulu FX hoặc Liberica Full JDK) |
| Server tắt ngay sau khi chạy | DB chưa sẵn sàng | Kiểm tra MySQL đang chạy và cấu hình đúng port `3306` |

---

## Danh sách chức năng đã hoàn thành

| STT | Tính năng | Giải pháp kỹ thuật | Trạng thái |
|:---:|---|---|:---:|
| 1 | Đăng ký & Đăng nhập | Socket JSON, phân quyền vai trò | ✅ |
| 2 | Tạo phiên đấu giá | Factory Method Pattern tạo `Item` theo danh mục | ✅ |
| 3 | Đấu giá trực tiếp (realtime) | Observer Pattern, đồng bộ phòng thầu đa client | ✅ |
| 4 | Tự động thầu (Auto-Bid) | Hàng đợi ưu tiên, ngân sách tối đa | ✅ |
| 5 | Chống bắn tỉa (Anti-Sniping) | Gia hạn tự động +60 giây khi có thầu sát giờ chót | ✅ |
| 6 | Xử lý thầu đồng thời | `ReentrantLock` per-auction, chống race condition | ✅ |
| 7 | Hệ thống ví tiền (Wallet) | Đóng băng số dư, tự hoàn trả khi bị outbid | ✅ |
| 8 | Push notification thời gian thực | Server đẩy thông báo khi bị vượt giá / thắng thầu | ✅ |
| 9 | Giao diện Light / Dark Mode | Chuyển đổi CSS Stylesheet động | ✅ |
| 10 | Trang quản trị (Admin) | Xem log, giám sát và dừng khẩn cấp phiên thầu | ✅ |

---


## Báo cáo PDF & Video Demo

- 📄 **Báo cáo PDF:** [Tải báo cáo BTL (OneDrive)](https://1drv.ms/w/c/a49f15581f61e2f3/IQDCZOaazyt9SasJdDgkvcAVAfDvFopFUlgUfr6WMrkBEbM?e=7jYRQ6)
- 🎥 **Video Demo:** [Xem trên YouTube](https://youtu.be/qp2XtEzeNdc?si=OyxiGee9npSjzNDM)

---

*Đồ án môn Lập Trình Nâng Cao (CS2043) — Nhóm SSS (Huuhan) — UET, 2025–2026*