# PoseTrainer

Ứng dụng Android hỗ trợ luyện tập bằng camera. PoseTrainer dùng MediaPipe để nhận diện tư thế, phân tích động tác và phản hồi trong buổi tập; đồng thời quản lý bài tập, lịch tập và tiến độ qua Firebase.

## Chức năng

- Theo dõi tư thế với CameraX và MediaPipe Pose Landmarker; đếm lượt và phản hồi khi tập.
- Thư viện bài tập, bài tập tùy chỉnh, buổi tập và lịch sử luyện tập.
- Kế hoạch tập qua Firebase Cloud Function `generatePlan` (cần backend tương ứng).
- Mục tiêu, lịch nhắc tập, tiến độ, chuỗi ngày tập và thành tích.
- Tài khoản, hồ sơ, bài đăng cộng đồng và thông báo.

## Công nghệ

Android (Java, Kotlin, XML), Gradle, CameraX, MediaPipe Tasks Vision, Firebase Authentication/Firestore/Storage/Functions/Messaging, Glide và ExoPlayer. Cấu hình SDK: `minSdk 24`, `targetSdk 34`, `compileSdk 34`.

## Chạy dự án

1. Mở thư mục gốc bằng Android Studio. Cài Android SDK 34 và JDK tương thích Android Gradle Plugin 8.13.
2. Cấu hình Firebase và đặt `google-services.json` tại `app/google-services.json`. Triển khai Cloud Functions tương ứng nếu dùng chức năng tạo kế hoạch.
3. Cấu hình `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` và `RELEASE_KEY_PASSWORD` trong `gradle.properties` theo keystore của bạn. Cả build debug và release hiện dùng cấu hình ký này.
4. Chạy trên thiết bị Android có camera và cấp quyền được yêu cầu. Build bằng `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).

Mô hình MediaPipe có trong `app/src/main/assets`; tác vụ `preBuild` có thể tải mô hình còn thiếu.

## Cấu trúc

| Thư mục trong `app/src/main` | Vai trò |
| --- | --- |
| `java/.../UI` | Activity, Fragment, adapter và giao diện camera |
| `java/.../Core` | MediaPipe và bộ phân tích động tác |
| `java/.../DAL`, `java/.../Domain` | Truy cập và mô hình dữ liệu |
| `java/.../Service` | Firebase, thông báo và lịch nhắc |
| `res` | Layout và tài nguyên Android |

Các dịch vụ Firebase và Cloud Functions cần được cấu hình riêng để sử dụng đầy đủ chức năng. Không công khai mật khẩu keystore.
