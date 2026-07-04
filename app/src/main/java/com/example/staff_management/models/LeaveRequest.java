package com.example.staff_management.models;

import java.util.Date;

/*
 * LeaveRequest:
 * - Dùng để: lưu thông tin một đơn xin nghỉ phép.
 * - Kiểm tra/xử lý: map trực tiếp với document LeaveRequests trên Firestore.
 * - Sau đó: app dùng dữ liệu này để hiển thị, duyệt và cập nhật trạng thái đơn.
 */
public class LeaveRequest {
    private String id;
    private String userId;
    private String userName;
    private String department;
    private String leaveType;
    private String reason;
    private Date startDate;
    private Date endDate;
    private long totalDays;
    private String status;
    private String approverId;
    private String approverName;
    private String rejectionReason;
    private Date createdAt;
    private Date updatedAt;

    public LeaveRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public long getTotalDays() { return totalDays; }
    public void setTotalDays(long totalDays) { this.totalDays = totalDays; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
