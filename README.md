# SupperMario

Game Android Kotlin (Android 7.0 trở lên) và bản iOS native Swift (iOS 15 trở lên).

[Bản iOS native và hướng dẫn build/ký](ios/README.md) ·
[Build iOS trên GitHub Actions](https://github.com/nguyenlinhit-cpu/tri-gay/actions/workflows/ios-native.yml)

Bản iOS dùng UIKit/Core Graphics; bản arm64 chưa ký cần được ký bằng Apple trước khi cài lên iPhone.

[Tải APK cài thử](https://github.com/nguyenlinhit-cpu/tri-gay/releases/latest)

Năm màn chơi có độ khó tăng dần: Vườn gai, Đảo đá, Cầu chuyển động,
Pháo đài ống và Thành trì cuối. Có xương rồng trồi từ ống, chim thả đá,
ba loại vũ khí và boss hai giai đoạn. Phải hạ boss mới mở cổng đích.
Ba chế độ Dễ/Vừa/Khó thay đổi tốc độ quái, nhịp tấn công, thời gian và máu boss.

[Xem sơ đồ 5 màn](design/levels/routes.svg) ·
[Chi tiết thiết kế màn và độ khó](design/levels/README.md)

Điều khiển bằng các nút cảm ứng hoặc bàn phím: **A/D** di chuyển,
**Space** nhảy, **J/F** tấn công khi có vũ khí.

Môi trường Nix dành cho Linux x86_64, khóa phiên bản nixpkgs trong
`flake.lock`: JDK 25, Android SDK 37.0, Build Tools 37.0.0. Gradle 9.6.0
được khóa phiên bản và SHA-256 trong Gradle Wrapper. Không cần cài Android Studio.
Nix cần bật `nix-command` và `flakes`. SDK sử dụng giấy phép Android của Google,
được chấp nhận trong cấu hình flake. Lần đầu chạy cần mạng để tải công cụ
và dependency Gradle; các lần sau dùng bộ nhớ đệm.

Xuất APK đã tối ưu, chạy unit test và Android Lint:

```sh
nix run path:.
```

Kết quả: `dist/SupperMario.apk` và `dist/SupperMario.apk.sha256`.
APK này dùng cấu hình release có tối ưu R8 và khóa debug cục bộ để cài thử.
Khóa được Gradle tạo trong `.android/`; giữ lại khóa này nếu muốn các bản
build tiếp theo cập nhật được bản đã cài. Khi phát hành lên cửa hàng, cần
cấu hình khóa ký phát hành riêng; `assembleRelease` thông thường không
tự sử dụng khóa debug.

Vào môi trường phát triển:

```sh
nix develop path:.
bash ./gradlew testDebugUnitTest
bash ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Sau khi thêm `flake.nix` và `flake.lock` vào Git, có thể dùng `nix run .`
và `nix develop .` để Nix chỉ lấy các file thuộc Git, tránh sao chép cache.
Cache Gradle ở `.gradle-home/`, dữ liệu Android ở `.android/`; các thư mục
này và sản phẩm build được bỏ qua trong Git.

Các tối ưu: bật tối ưu release, cache Gradle và giới hạn 4 worker; tải sprite
ở kích thước gốc để tránh phóng ảnh theo mật độ màn hình; chỉ chạy luồng game
khi Activity đang hoạt động và Surface sẵn sàng; giải phóng SoundPool khi
Activity bị hủy và luôn trả Canvas sau khi vẽ.
