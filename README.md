# Staff Management Application

A comprehensive mobile staff management application integrated with a RAG-based AI Assistant. This project was developed as a **Mobile Programming Course Project** at the **VNU-HCM University of Science (University of Science, VNU-HCM)**.

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

When cloning this repository, you need to manually add the following configuration files and credentials to run the project locally:

### 1. Firebase Configuration
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

---

## 📝 Disclaimers & Credits (Team Contributions)

This project was built collaboratively by:
* **Ha Gia Huy (Student ID: 22200070)** - Android Developer & QC/Tester: Requirement analysis, Material UI/UX design, Auth, Account Management, Role-Based Access Control (RBAC), and lead manual tester (authored & executed 15+ test cases for authentication stability, input constraints, and role navigation).
* **Vo Dinh Quoc (Student ID: 2220133)** - Android Developer: Employee Management, Attendance, Tasks, Leave Requests, Salary modules, and Cloud Firestore design.
* **Le Tien Thang (Student ID: 2220144)** - Android Developer: Firebase Storage, FCM integration, real-time Group Chat, Gemini RAG Integration, and reporting.
