# Staff Management Application

A comprehensive mobile staff management application integrated with a RAG-based AI Assistant. This project was developed as a **Mobile Programming Course Project** at the **VNU-HCM University of Science (University of Science, VNU-HCM)**.

---

## 👥 Team Members & Contributions

The project was collaboratively developed by a team of three members with clear division of roles:

| Member | Student ID | Primary Role | Contributions & Deliverables |
| :--- | :--- | :--- | :--- |
| **Ha Gia Huy** | **22200070** | **Android Developer & QC/Tester** | - Conducted system requirement analysis and designed UI/UX screens complying with Material Design guidelines.<br>- Built Authentication, Account Management, and Role-Based Access Control (RBAC) for Admin, Manager, and Employee roles.<br>- Integrated Firebase Authentication.<br>- **QA/Testing Lead**: Formulated the test plan, designed test scenarios, and authored & executed **15+ comprehensive manual test cases** validating authentication stability, input field constraints, and role-based interface navigation flows. |
| **Vo Dinh Quoc** | 2220133 | Android Developer | - Developed core business logic modules: Employee Management (CRUD), Attendance tracking, Task assignment (Task Management), Leave Requests, and Salary Calculation.<br>- Designed and implemented analytical data charts.<br>- Modeled the Cloud Firestore database schema (Firestore Collections). |
| **Le Tien Thang** | 2220144 | Android Developer | - Integrated Firebase Storage (media upload) and Firebase Cloud Messaging (FCM) for real-time push notifications.<br>- Developed the real-time Group Chat feature.<br>- Integrated the AI Chatbot Assistant powered by Gemini API and a custom RAG backend.<br>- Performed system-wide integration testing and compiled the project report. |

---

## 🤖 AI Chatbot & RAG Backend

The HR AI Chatbot helps answer company policies, salary calculations, and internal guidelines. It utilizes the **Gemini API** coupled with **Retrieval-Augmented Generation (RAG)** to provide accurate answers based on internal documents.

* **RAG Backend Repository**: The RAG system was retrieved and customized from the repository of our team member **Le Tien Thang**: [rag-hr-chatbot](https://github.com/kubbies03/rag-hr-chatbot).

---

## 🛠️ Technology Stack

* **Language & IDE**: Java, Android Studio (Gradle).
* **Database & Cloud Services**: Firebase (Authentication, Cloud Firestore, Cloud Storage, Cloud Messaging).
* **Libraries**: OkHttp (REST API client for RAG), Gson (JSON parsing), MPAndroidChart (data visualization), Glide (image loading).

---

## ⚙️ Project Setup & Installation (Post-Clone Guide)

For security reasons, sensitive configuration files containing API keys and Firebase credentials **are git-ignored**. You must configure these manually after cloning the repository:

### 1. Firebase Configuration
This application relies on Firebase services. To connect it to your Firebase instance:
1. Create a project on the [Firebase Console](https://console.firebase.google.com/).
2. Enable **Authentication**, **Cloud Firestore**, **Cloud Storage**, and **Cloud Messaging**.
3. Register an Android application in the Firebase project with the package name: `com.example.staff_management`.
4. Download the `google-services.json` file and place it in the application directory:
   ```text
   Staff_management_team9/app/google-services.json
   ```

### 2. RAG API Configuration
The API keys and server endpoint details are stored locally.
1. Open (or create) the `local.properties` file in the root directory of the project:
   ```text
   Staff_management_team9/local.properties
   ```
2. Append the following lines (replace values with your own API endpoints and keys):
   ```properties
   # URL of your RAG API server
   RAG_BASE_URL=https://your-rag-backend-api-url.com
   
   # API authentication token
   RAG_API_KEY=your_rag_api_key_here
   ```
*(Note: `local.properties` is already added to `.gitignore` and will never be pushed to your repository).*

---

## 🔍 Quality Assurance & Testing (QC Showcase)

To align with professional **QC/Tester** standards, a structured testing workflow was implemented during development:

1. **Requirement Analysis**: Outlined data validation limits (e.g., email patterns, password constraints with minimum 6 characters, phone number formats).
2. **Test Design**:
   * Authored 15+ comprehensive test cases covering authentication flows and input validation bounds.
   * Conducted extensive Role-Based Access Control (RBAC) tests to verify that Employee accounts are strictly restricted from accessing Admin/Manager dashboard routes.
3. **Execution & Bug Tracking**:
   * **UI/UX Testing**: Inspected responsive layouts across different screen densities and verified visual feedback matching Material Design.
   * **Boundary Value Analysis (BVA)**: Applied to salary input fields, attendance reports, and user registration forms.
   * **API Testing**: Utilized Postman to test backend RAG endpoints independently prior to integrating them with the Android front-end.

---

## 📂 Project Documentation (Reports & Slides)

To keep this codebase repository lightweight, the large project report (`.docx`) and presentation slides (`.pptx`) are kept locally in the parent directory of this workspace and are excluded from the repository. Detailed schema, entity-relationship diagrams (ERDs), and use-case scenarios are instead documented within the source files and application interface flows.
