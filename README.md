# Staff Management Application

Một ứng dụng di động quản lý nhân sự toàn diện tích hợp Trợ lý ảo AI phục vụ cho doanh nghiệp. Dự án này là **Đồ án môn học Lập trình di động** tại **Trường Đại học Khoa học Tự nhiên, ĐHQG-HCM (VNU-HCM University of Science)**.

---

## 👥 Thành viên nhóm & Phân chia công việc

Dự án được phát triển bởi nhóm 3 thành viên với sự phân chia vai trò rõ ràng:

| Thành viên | MSSV | Vai trò chính | Công việc thực hiện |
| :--- | :--- | :--- | :--- |
| **Hà Gia Huy** | **22200070** | **Android Developer & QC/Tester** | - Phân tích yêu cầu hệ thống và thiết kế giao diện UI/UX theo tiêu chuẩn Material Design.<br>- Xây dựng chức năng Đăng nhập, Quản lý tài khoản, Phân quyền người dùng (Admin/Manager/Employee).<br>- Tích hợp Firebase Authentication.<br>- **Đảm nhận vai trò QC**: Lên kế hoạch kiểm thử (Test Plan), thiết kế kịch bản (Test Scenario), viết và thực thi **15+ manual test cases** xác thực độ ổn định của tính năng Đăng nhập, Phân quyền (RBAC), kiểm tra ràng buộc dữ liệu đầu vào và các luồng điều hướng giao diện. |
| **Võ Đình Quốc** | 2220133 | Android Developer | - Phát triển các tính năng nghiệp vụ: Quản lý nhân sự, Chấm công, Quản lý & giao việc (Task), Yêu cầu nghỉ phép (Leave Request), Tính lương.<br>- Xây dựng biểu đồ thống kê dữ liệu trực quan.<br>- Thiết kế cấu trúc cơ sở dữ liệu Cloud Firestore (Firestore Collections). |
| **Lê Tiến Thắng** | 2220144 | Android Developer | - Tích hợp Firebase Storage (lưu trữ hình ảnh, tài liệu) và Firebase Cloud Messaging (gửi thông báo đẩy thời gian thực).<br>- Phát triển tính năng Trò chuyện nhóm (Real-time Chat).<br>- Tích hợp hệ thống AI Chatbot Assistant (kết nối với RAG Backend) giúp giải đáp quy chế nhân sự.<br>- Kiểm thử hệ thống và viết báo cáo đồ án. |

---

## 🤖 Hệ thống AI Chatbot & RAG Backend

Chức năng trợ lý ảo AI trợ giúp giải đáp thắc mắc về nội quy, chế độ lương thưởng và quy chế của doanh nghiệp được xây dựng dựa trên mô hình **Gemini API** kết hợp kỹ thuật **RAG (Retrieval-Augmented Generation)** để đảm bảo thông tin trả về chính xác theo tài liệu nội bộ.

* **RAG Backend Repository**: Hệ thống RAG được lấy và tùy chỉnh từ dự án của thành viên **Lê Tiến Thắng**: [rag-hr-chatbot](https://github.com/kubbies03/rag-hr-chatbot).

---

## 🛠️ Công nghệ sử dụng

* **Ngôn ngữ & Công cụ**: Java, Android Studio (Gradle).
* **Database & Cloud Services**: Firebase (Authentication, Cloud Firestore, Cloud Storage, Cloud FCM).
* **Thư viện bên thứ ba**: OkHttp (gọi RAG API), Gson, MPAndroidChart (vẽ biểu đồ lương và thống kê), Glide (load hình ảnh).

---

## ⚙️ Hướng dẫn thiết lập & Cài đặt khi Clone dự án

Để bảo mật thông tin, các tệp cấu hình chứa API key và liên kết Firebase **không được đẩy lên GitHub**. Khi clone dự án về, bạn cần cấu hình lại các thành phần sau:

### 1. Cấu hình Firebase
Dự án sử dụng cơ sở dữ liệu Firebase. Để kết nối với Firebase của riêng bạn:
1. Tạo dự án mới trên [Firebase Console](https://console.firebase.google.com/).
2. Kích hoạt các dịch vụ: **Authentication**, **Cloud Firestore**, **Storage**, và **Cloud Messaging**.
3. Thêm một ứng dụng Android vào dự án Firebase với package name: `com.example.staff_management`.
4. Tải tệp `google-services.json` được cấp từ Firebase và đặt nó vào thư mục:
   ```text
   Staff_management_team9/app/google-services.json
   ```

### 2. Cấu hình API RAG Chatbot
Các khóa bảo mật và URL của Server RAG được lưu trữ cục bộ. Hãy cấu hình như sau:
1. Mở file [local.properties](file:///d:/Staff_management_team9/Staff_management_team9/local.properties) (hoặc tạo mới nếu chưa có ở thư mục gốc của dự án).
2. Thêm các dòng cấu hình sau (thay thế giá trị bằng Server URL và API Key của bạn):
   ```properties
   # URL của RAG API Backend
   RAG_BASE_URL=https://your-rag-backend-api-url.com
   
   # API Key để xác thực với RAG Backend
   RAG_API_KEY=your_rag_api_key_here
   ```
*(Lưu ý: File `local.properties` đã được liệt kê trong `.gitignore` nên sẽ không bị push lên repository của bạn).*

---

## 🔍 Kiểm thử & Đảm bảo Chất lượng (QC/Testing)

Với định hướng ứng tuyển vị trí **QC/Tester**, chức năng kiểm thử trong dự án này được thực hiện bài bản với quy trình như sau:

1. **Phân tích yêu cầu (Requirement Analysis)**: Xác định rõ ràng các điều kiện biên của các trường dữ liệu (ví dụ: định dạng email, mật khẩu tối thiểu 6 ký tự, số điện thoại hợp lệ).
2. **Thiết kế Kịch bản kiểm thử (Test Scenarios) & Viết Test Cases**:
   * Thiết kế hơn 15+ manual test cases chi tiết để bao phủ toàn bộ luồng hoạt động chính của hệ thống xác thực.
   * Tập trung kiểm thử phân quyền (RBAC - Role-Based Access Control) để đảm bảo tài khoản Employee không thể truy cập vào các màn hình quản trị của Admin/Manager.
3. **Thực thi kiểm thử (Test Execution)**:
   * **UI/UX Testing**: Kiểm tra hiển thị giao diện trên các kích thước màn hình khác nhau, đảm bảo khoảng cách hiển thị và phản hồi của nút bấm chuẩn Material Design.
   * **Boundary Value Analysis (Phân tích giá trị biên)**: Áp dụng khi nhập thông tin lương, chấm công và kiểm tra form đăng ký.
   * **API Testing**: Sử dụng Postman để kiểm tra độc lập các API kết nối với server RAG trước khi tích hợp vào ứng dụng Android.

---

## 📂 Tài liệu liên quan (Báo cáo & Slide)

Để giữ cho repository gọn nhẹ và chuyên nghiệp, các file tài liệu lớn gồm **file Báo cáo Đồ án (`.docx`)** và **file Slide thuyết trình (`.pptx`)** được lưu trữ cục bộ tại thư mục gốc của dự án trên máy phát triển và không được đẩy lên GitHub. 
* Bạn có thể tham khảo trực tiếp thông tin tổng quan, sơ đồ thực thể (ERD), và kịch bản sử dụng (Use Case) chi tiết được mô tả trực quan ngay trong mã nguồn và giao diện ứng dụng.
