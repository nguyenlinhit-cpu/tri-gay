# SupperMario iOS native

Ứng dụng iPhone/iPad dùng **Swift, UIKit, Core Graphics và AVFoundation**.
Không có WebView, JavaScript, framework đa nền tảng hoặc dependency bên thứ ba.
Hỗ trợ iOS 15 trở lên, màn hình ngang, safe area và nhiều ngón tay.

GameCore chạy mô phỏng cố định 120 bước/giây, vẽ theo CADisplayLink 60 Hz.
Có 5 màn, ba nhân vật, ba độ khó, xương rồng, chim thả đá, súng/đá/kiếm,
ống dịch chuyển, nền di chuyển, vật phẩm, boss hai giai đoạn và cổng khóa.
Tự tạm dừng khi rời ứng dụng; nhấn Tiếp tục để chơi tiếp.
Dữ liệu màn và hệ số độ khó xuất từ mã Kotlin Android bằng trình xuất có giới hạn cú pháp.
Hình ảnh dùng lại từ tài nguyên Android; hiệu ứng âm thanh PCM tạo riêng cho iOS.

## Build bằng GitHub Actions

Mở workflow [iOS Native](https://github.com/nguyenlinhit-cpu/tri-gay/actions/workflows/ios-native.yml)
và chọn **Run workflow**. Workflow cũng tự chạy khi mã iOS hoặc dữ liệu màn thay đổi trên `main`.

Runner macOS chạy unit test cho Swift, build và kiểm tra giao diện trên iPhone Simulator,
sau đó biên dịch bản arm64 dành cho thiết bị. Artifact gồm:

- `SupperMario-simulator.zip`: app chạy bằng Xcode Simulator trên Mac.
- `SupperMario-unsigned.ipa`: bản arm64 **chưa ký**, dành cho bước ký sau.
- Checksum SHA-256 và ảnh menu iPhone.
- `iOS-test-results`: kết quả XCTest và ảnh từng màn.

**IPA chưa ký không cài trực tiếp lên iPhone và không phải bản TestFlight/App Store.**
Workflow build không cần tài khoản Apple, chứng chỉ hay khóa bí mật.

## Build trên Mac

Cài Xcode và chọn Command Line Tools của Xcode, rồi chạy từ thư mục gốc repo:

```sh
python3 tools/prepare_ios.py
swift ios/GenerateIcon.swift
swift test --package-path ios/GameCore
open ios/SupperMario.xcodeproj
```

Chọn scheme `SupperMario`, một iPhone Simulator và Run.
Project Xcode được lưu trong repo; `tools/generate_xcode_project.py` tạo lại project khi thêm file Swift.
Các tài nguyên và icon được sinh trước build, không cần cài package bổ sung.

## Ký và phân phối khi có tài khoản Apple

1. Mở project bằng Xcode trên Mac và thêm Apple Account trong Xcode Settings.
2. Trong target SupperMario → Signing & Capabilities, chọn Team và Bundle Identifier riêng.
3. Chọn iPhone đã kết nối để cài bản development, hoặc dùng Product → Archive.
4. Để TestFlight/App Store, đăng ký Apple Developer Program, tạo app trong App Store Connect,
   chọn Distribute App trên archive đã ký và làm các bước metadata/beta review của Apple.

Không lưu certificate, provisioning profile hay App Store Connect API key vào Git.
Khóa ký của Android không dùng được cho iOS.
Hướng dẫn của Apple: https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases

## Kiểm tra

```sh
python3 tools/prepare_ios.py --check
swift test --package-path ios/GameCore --parallel
```

Các test kiểm tra dữ liệu của 15 biến thể màn, bước mô phỏng theo tốc độ màn hình,
nhảy giữ/nhả, chặn tường ống, rơi xuống sông, chống nhảy giữa không trung,
đáp đảo đá, dịch chuyển, khóa cổng và kiếm dự phòng, boss và nhịp xương rồng.
UI test mở cả năm màn, thao tác di chuyển/nhảy và kiểm tra tạm dừng/tiếp tục.
Chưa thay thế kiểm tra trên iPhone/iPad thật và cân bằng độ khó thực tế.
