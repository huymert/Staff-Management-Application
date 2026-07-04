package com.example.staff_management.models;

/*
 * User:
 * - Dùng để: lưu thông tin người dùng trong hệ thống quản lý nhân sự.
 * - Kiểm tra/xử lý: map trực tiếp với document Users trên Firestore.
 * - Sau đó: các màn hình sẽ lấy dữ liệu này để hiển thị profile, role, lương và trạng thái làm việc.
 */
public class User {
    private String uid;
    private String fullName;
    private String email;
    private String role;
    private String status;
    private String employmentStatus;
    private String presenceStatus;
    private String avatarUrl;
    private double baseSalary;
    private double productivityScore;
    private String birthDate;
    private String gender;
    private String phoneNumber;
    private String address;
    private String department;
    private String position;
    private String joinDate;
    private String employeeCode;
    private String citizenId;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String fcmToken;
    private boolean biometricRegistered;

    public User() {}

    public User(String uid, String fullName, String email, String role, String employmentStatus, double baseSalary) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = employmentStatus;
        this.employmentStatus = employmentStatus;
        this.presenceStatus = "offline";
        this.baseSalary = baseSalary;
        this.productivityScore = 0;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return getEmploymentStatus(); }
    public void setStatus(String status) { setEmploymentStatus(status); }

    public String getEmploymentStatus() {
        if (employmentStatus != null && !employmentStatus.isEmpty()) {
            return employmentStatus;
        }
        if (status != null && !status.isEmpty() && !"online".equalsIgnoreCase(status) && !"offline".equalsIgnoreCase(status)) {
            return status;
        }
        return "";
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
        this.status = employmentStatus;
    }

    public String getPresenceStatus() {
        if (presenceStatus != null && !presenceStatus.isEmpty()) {
            return presenceStatus;
        }
        if ("online".equalsIgnoreCase(status) || "offline".equalsIgnoreCase(status)) {
            return status;
        }
        return "offline";
    }

    public void setPresenceStatus(String presenceStatus) { this.presenceStatus = presenceStatus; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }
    public double getProductivityScore() { return productivityScore; }
    public void setProductivityScore(double productivityScore) { this.productivityScore = productivityScore; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public boolean isBiometricRegistered() { return biometricRegistered; }
    public void setBiometricRegistered(boolean biometricRegistered) { this.biometricRegistered = biometricRegistered; }
}
