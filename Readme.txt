@ -0,0 +1,187 @@
# Online Auction System

## Mô tả bài toán

Online Auction System là hệ thống đấu giá trực tuyến dạng desktop client-server. Người dùng có thể đăng ký, đăng nhập, xem phiên đấu giá, tạo sản phẩm, tạo phiên đấu giá, đặt giá, cấu hình auto-bid và theo dõi lịch sử giao dịch.

Phạm vi hệ thống tập trung vào quy trình đấu giá: quản lý người dùng, sản phẩm, phiên đấu giá, đặt giá realtime qua socket và lưu trữ dữ liệu bằng MySQL. Dự án không xử lý thanh toán thật, vận chuyển, định danh người dùng ngoài đời hoặc các nghiệp vụ thương mại điện tử ngoài phạm vi đấu giá.

## Công nghệ sử dụng

- Java 17
- JavaFX 21.0.2 cho giao diện desktop client
- Maven cho build, test và chạy ứng dụng
- MySQL 8.x / Aiven MySQL cho cơ sở dữ liệu
- JDBC + HikariCP cho kết nối database
- TCP Socket + Java Serialization cho giao tiếp client-server
- Gemini API cho chức năng chatbot hỗ trợ người dùng
- JUnit / Spring Boot Test cho kiểm thử
- GitHub Actions cho CI

## Môi trường và yêu cầu cài đặt

Cài đặt các thành phần sau trước khi chạy:

- JDK 17 hoặc mới hơn
- Maven 3.8 hoặc mới hơn
- MySQL client/server nếu dùng database cục bộ
- Kết nối internet nếu dùng database Aiven hoặc Gemini API

Kiểm tra môi trường:

```bash
java -version
mvn -version
```

Nếu `mvn` không được nhận diện trên Windows, cần cài Maven và thêm thư mục `bin` của Maven vào biến môi trường `PATH`.

## Cấu trúc thư mục

```text
.
|-- pom.xml
|-- gemini.properties
|-- src
|   |-- main
|   |   |-- java/com/auction
|   |   |   |-- client        # JavaFX app, controller, scene manager, network client
|   |   |   |-- server        # Socket server, service, DAO, database config
|   |   |   `-- shared        # Model dùng chung và protocol SocketMessage
|   |   `-- resources
|   |       |-- fxml          # Màn hình JavaFX
|   |       |-- css           # Giao diện
|   |       |-- img           # Hình ảnh tĩnh
|   |       `-- schema.sql    # Script tạo bảng MySQL
|   `-- test/java             # Unit test
`-- .github/workflows/ci.yml  # Pipeline build/test
```

Các module chính:

- `com.auction.server.network`: khởi động server socket và xử lý kết nối client.
- `com.auction.server.service`: nghiệp vụ đăng nhập, người dùng, sản phẩm, phiên đấu giá, bid và auto-bid.
- `com.auction.server.dao`: truy cập dữ liệu MySQL.
- `com.auction.client`: ứng dụng JavaFX và quản lý scene.
- `com.auction.client.controller`: controller cho các màn hình đăng nhập, đăng ký, dashboard, đấu giá, lịch sử, nạp tiền, admin và chatbot.
- `com.auction.shared`: model dùng chung giữa client/server và lớp `SocketMessage`.

## Cấu hình database

Mã nguồn hiện cấu hình database trong:

```text
src/main/java/com/auction/server/config/DBConnection.java
```

Nếu dùng database khác, cập nhật JDBC URL, username và password trong file trên. Không nên đưa credential thật lên repository công khai.

Tạo schema nếu database chưa có bảng:

```bash
mysql -u your_username -p defaultdb --execute="source src/main/resources/schema.sql"
```

Trên Windows, Linux và macOS có thể dùng cùng lệnh trên nếu đã cài MySQL client và đang đứng ở thư mục gốc dự án.

## Cấu hình Gemini chatbot

Chatbot đọc API key theo thứ tự sau:

1. JVM system property `gemini.api.key`
2. Biến môi trường `GEMINI_API_KEY`
3. File `gemini.properties` ở thư mục gốc dự án

Khuyến nghị dùng biến môi trường thay vì lưu key thật trong repository.

Windows PowerShell:

```powershell
$env:GEMINI_API_KEY="AIzaSyDkDxtc46I8D_yFS0zWEmlH1t1Q5NxJFW8"
```

Linux/macOS:

```bash
export GEMINI_API_KEY="AIzaSyDkDxtc46I8D_yFS0zWEmlH1t1Q5NxJFW8"
```

## Build và test

Các lệnh Maven dưới đây dùng được trên Windows, Linux và macOS sau khi Maven đã được cài đặt:

```bash
mvn clean test
mvn clean package
```

## Chạy chương trình

Ứng dụng cần chạy server trước, sau đó mới chạy client.

### 1. Chạy Server

Server mặc định lắng nghe port `9090`.

Windows PowerShell:

```powershell
mvn exec:java "-Dexec.mainClass=com.auction.server.network.AuctionServer" "-Dexec.args=9090"
```

Windows CMD:

```cmd
mvn exec:java -Dexec.mainClass=com.auction.server.network.AuctionServer -Dexec.args=9090
```

Linux/macOS:

```bash
mvn exec:java -Dexec.mainClass=com.auction.server.network.AuctionServer -Dexec.args=9090
```

Nếu muốn dùng port khác, thay `9090` bằng port mong muốn. Khi chạy client trên máy khác, cần cập nhật `SERVER_HOST` và `SERVER_PORT` trong các controller client đang hardcode kết nối tới server.

### 2. Chạy Client

Mở terminal thứ hai trong thư mục gốc dự án và chạy:

```bash
mvn javafx:run
```

Client JavaFX sẽ mở màn hình đăng nhập và tự kết nối tới `localhost:9090`. Nếu chưa có tài khoản, dùng màn hình đăng ký để tạo tài khoản bidder.

## Thứ tự chạy đầy đủ

1. Cài JDK 17, Maven và MySQL client nếu cần.
2. Cấu hình database trong `DBConnection.java`.
3. Chạy `schema.sql` nếu database chưa có bảng.
4. Mở terminal 1 và chạy server bằng lệnh ở mục `Chạy Server`.
5. Mở terminal 2 và chạy client bằng `mvn javafx:run`.
6. Đăng ký tài khoản hoặc đăng nhập.
7. Tạo sản phẩm, tạo phiên đấu giá, bắt đầu phiên và đặt giá từ client.

## Chức năng đã hoàn thành

- Đăng ký, đăng nhập và đăng xuất người dùng.
- Kết nối client-server qua TCP Socket.
- Quản lý người dùng và nạp tiền tài khoản.
- Quản lý sản phẩm đấu giá theo nhóm electronics, vehicle, art và other.
- Tạo, xem, bắt đầu, kết thúc, hủy và xóa phiên đấu giá.
- Đặt giá thủ công cho phiên đấu giá.
- Cấu hình và hủy auto-bid.
- Cơ chế anti-sniping với thời gian gia hạn cuối phiên.
- Tự động kiểm tra và kết thúc phiên hết hạn bằng scheduler server.
- Broadcast cập nhật bid và kết thúc phiên tới các client đang theo dõi.
- Xem lịch sử đấu giá và danh sách lượt bid của người dùng.
- Giao diện JavaFX cho login, register, dashboard, auctions, seller, my bids, my sell, history, deposit, admin và chatbot.
- Chatbot Gemini có thể đọc ngữ cảnh dữ liệu đấu giá để hỗ trợ người dùng.
- Unit test cho model và service chính.
- CI GitHub Actions chạy test và build package.

## Báo cáo và video demo

- Báo cáo PDF: [TODO - cập nhật link báo cáo PDF](#)
- Video demo: [TODO - cập nhật link video demo](#)
