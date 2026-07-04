package com.example.staff_management.models;

import java.io.Serializable;

/*
 * Task:
 * - Dùng để: lưu thông tin một công việc được giao cho nhân viên.
 * - Kiểm tra/xử lý: map với collection Tasks trên Firestore và truyền qua Intent khi cần.
 * - Sau đó: app dùng dữ liệu này để hiển thị trạng thái, mức ưu tiên và hạn hoàn thành.
 */
public class Task implements Serializable {
    private String id;
    private String title;
    private String objective;
    private String requirements;
    private String employeeId;
    private String employeeName;
    private String adminId;
    private String status;
    private String priority;
    private long timestamp;
    private long deadline;

    public Task() {}

    public Task(String id, String title, String objective, String requirements,
                String employeeId, String employeeName, String adminId,
                String status, long timestamp) {
        this.id = id;
        this.title = title;
        this.objective = objective;
        this.requirements = requirements;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.adminId = adminId;
        this.status = status;
        this.timestamp = timestamp;
        this.priority = "Trung bình";
        this.deadline = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public long getDeadline() { return deadline; }
    public void setDeadline(long deadline) { this.deadline = deadline; }
}
