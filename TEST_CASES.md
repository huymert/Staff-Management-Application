# Manual Test Case Suite - Staff Management Application

This document contains the manual test cases designed and executed to verify the functionality, security, and usability of the **Staff Management Application**, focusing on **Authentication**, **Role-Based Access Control (RBAC)**, and **Input Data Constraints**.

---

## 📊 Test Case Summary

* **Total Test Cases**: 16
* **Target Modules**: Authentication (6), Role-Based Access Control (6), Input Validation (4)
* **Author**: Ha Gia Huy (QC/Tester)

---

## 🔑 Module 1: Authentication & Password Reset (TC-001 to TC-006)

### TC-001: Verify Login with valid Admin credentials
* **Description**: Ensure that an Admin user can successfully log in using registered credentials and gets redirected to the correct dashboard.
* **Pre-conditions**: An Admin account exists in Firebase Auth & Firestore with role `"admin"`.
* **Test Steps**:
  1. Open the application.
  2. Input registered Admin email (`admin@company.com`) and password.
  3. Tap the "Login" button.
* **Expected Result**: User is successfully authenticated, and the app redirects to `AdminDashboardActivity`.
* **Status**: Pass

### TC-002: Verify Login with valid Manager credentials
* **Description**: Ensure that a Manager user can successfully log in and gets redirected to the Manager dashboard.
* **Pre-conditions**: A Manager account exists in Firebase Auth & Firestore with role `"manager"`.
* **Test Steps**:
  1. Open the application.
  2. Input registered Manager email and password.
  3. Tap the "Login" button.
* **Expected Result**: User is successfully authenticated, and the app redirects to `ManagerDashboardActivity`.
* **Status**: Pass

### TC-003: Verify Login with valid Employee credentials
* **Description**: Ensure that an Employee user can successfully log in and gets redirected to the Employee dashboard.
* **Pre-conditions**: An Employee account exists in Firebase Auth & Firestore with role `"employee"`.
* **Test Steps**:
  1. Open the application.
  2. Input registered Employee email and password.
  3. Tap the "Login" button.
* **Expected Result**: User is successfully authenticated, and the app redirects to `EmployeeDashboardActivity`.
* **Status**: Pass

### TC-004: Verify Login fails with unregistered email
* **Description**: Verify that the system prevents login attempts with an unregistered email.
* **Pre-conditions**: None.
* **Test Steps**:
  1. Open the application.
  2. Input an unregistered email (`nonexistent@company.com`) and any password.
  3. Tap the "Login" button.
* **Expected Result**: Authentication fails. An error toast/message is displayed: "Authentication failed" or equivalent. User remains on the Login screen.
* **Status**: Pass

### TC-005: Verify Login fails with incorrect password
* **Description**: Verify that the system prevents login attempts when the password is incorrect.
* **Pre-conditions**: A registered account exists.
* **Test Steps**:
  1. Open the application.
  2. Input a registered email and an incorrect password.
  3. Tap the "Login" button.
* **Expected Result**: Authentication fails. An error message is shown, and the user remains on the Login screen.
* **Status**: Pass

### TC-006: Verify password reset link transmission
* **Description**: Verify that the password reset function sends a recovery email for registered addresses.
* **Pre-conditions**: A registered account exists.
* **Test Steps**:
  1. Open the application.
  2. Tap "Forgot Password".
  3. Enter the registered email address.
  4. Tap the "Reset Password" button.
* **Expected Result**: A success dialog or toast is shown: "Reset link sent to your email", and a Firebase password reset email is sent to the address.
* **Status**: Pass

---

## 🛡️ Module 2: Role-Based Access Control - RBAC (TC-007 to TC-012)

### TC-007: Verify Admin navigation access
* **Description**: Verify that the Admin user can view and access Admin-only features (e.g., adding staff, changing global roles).
* **Pre-conditions**: Logged in as Admin.
* **Test Steps**:
  1. Navigate the Admin Dashboard.
  2. Check for the presence of the "Add Staff" option and role management features.
* **Expected Result**: All Admin-specific buttons are visible and functional.
* **Status**: Pass

### TC-008: Verify Employee restricted access to Admin Dashboard
* **Description**: Ensure an Employee cannot access Admin-only activities or navigation screens.
* **Pre-conditions**: Logged in as Employee.
* **Test Steps**:
  1. Try to access the Admin Dashboard layout or navigate to `AdminDashboardActivity` directly.
* **Expected Result**: Navigation is blocked, or the dashboard redirects/displays an unauthorized screen. No Admin options are visible in the Employee menu.
* **Status**: Pass

### TC-009: Verify Employee restricted access to Manager Dashboard
* **Description**: Ensure an Employee cannot access Manager-only features (e.g., approving leaves, viewing other employees' salaries).
* **Pre-conditions**: Logged in as Employee.
* **Test Steps**:
  1. Try to open the leave approval or salary calculation activity.
* **Expected Result**: Screen is hidden, or access is blocked.
* **Status**: Pass

### TC-010: Verify Manager dashboard access
* **Description**: Verify that a Manager can access Manager features (Task assignment, attendance reports, leave approvals).
* **Pre-conditions**: Logged in as Manager.
* **Test Steps**:
  1. Check the Manager Dashboard.
  2. Tap the "Assign Task" button.
  3. Tap the "Leave Requests" menu.
* **Expected Result**: Manager dashboard loads correctly, allowing task creation and leave request reviews.
* **Status**: Pass

### TC-011: Verify Employee dashboard access
* **Description**: Verify that an Employee can access their own profile features (Check-in, personal tasks, creating leave request).
* **Pre-conditions**: Logged in as Employee.
* **Test Steps**:
  1. Open the Employee Dashboard.
  2. Tap "Check In".
  3. Tap "Request Leave".
* **Expected Result**: Employee dashboard loads correctly, permitting personal attendance check-ins and personal leave applications.
* **Status**: Pass

### TC-012: Verify real-time database synchronization across different roles
* **Description**: Verify that actions taken by one role (e.g., Manager assigning a task) update the database instantly and reflect on another role's dashboard (e.g., Employee task list).
* **Pre-conditions**: Two devices/emulators active: Device A logged in as Manager, Device B logged in as Employee.
* **Test Steps**:
  1. On Device A (Manager), assign a new task to the Employee on Device B.
  2. On Device B (Employee), view the task board.
* **Expected Result**: The assigned task appears in the Employee's task list in real-time without reloading the screen.
* **Status**: Pass

---

## ✍️ Module 3: Input Field Validation & Boundary Constraints (TC-013 to TC-016)

### TC-013: Verify input constraints when adding a new staff profile
* **Description**: Verify that the "Add Staff" form rejects invalid formats (e.g., invalid email structure, blank fields).
* **Pre-conditions**: Logged in as Admin/Manager.
* **Test Steps**:
  1. Open the "Add Staff" form.
  2. Leave the name blank, enter an invalid email (`invalidemail.com`), and enter a negative salary.
  3. Tap "Submit".
* **Expected Result**: System rejects the submission, highlighting the invalid fields with error messages (e.g., "Enter a valid email", "Fields cannot be empty").
* **Status**: Pass

### TC-014: Verify boundary validation for basic salary input
* **Description**: Ensure the basic salary input field rejects invalid numeric bounds (e.g., negative numbers, excessively large values).
* **Pre-conditions**: Logged in as Admin/Manager.
* **Test Steps**:
  1. Open the "Add Staff" or "Calculate Salary" screen.
  2. Input `-5000` into the basic salary field. Tap "Submit".
  3. Input `1000000000` (overflow test). Tap "Submit".
* **Expected Result**: The system prevents submission and displays an error message for negative/invalid inputs.
* **Status**: Pass

### TC-015: Verify boundary check for Leave Days request
* **Description**: Verify that the leave day requested must be a positive integer within valid limits.
* **Pre-conditions**: Logged in as Employee.
* **Test Steps**:
  1. Open the "Request Leave" form.
  2. Enter `0` or a negative number in the "Number of Days" field.
  3. Tap "Request".
* **Expected Result**: Validation fails, and the user is prompted to enter a valid number of days (>= 1).
* **Status**: Pass

### TC-016: Verify name field length constraint validation
* **Description**: Verify that the employee name field does not accept excessively long strings or special characters.
* **Pre-conditions**: Logged in as Admin.
* **Test Steps**:
  1. Open "Add Staff" screen.
  2. Input a string of 100+ random characters in the "Name" field.
  3. Tap "Submit".
* **Expected Result**: Submission fails with a field limit warning, or the input is truncated to a reasonable character limit (e.g., 50 characters).
* **Status**: Pass
