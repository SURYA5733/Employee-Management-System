package com.ems.dto;

import com.ems.model.LeaveRequest;
import com.ems.model.LeaveStatus;
import com.ems.model.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveResponseDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private String departmentName;
    private String jobTitle;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int durationDays;
    private String reason;
    private LeaveStatus status;
    private String reviewedByUsername;
    private String reviewComments;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;

    public LeaveResponseDTO() {
    }

    public static LeaveResponseDTO fromEntity(LeaveRequest entity) {
        if (entity == null) return null;
        LeaveResponseDTO dto = new LeaveResponseDTO();
        dto.setId(entity.getId());
        if (entity.getEmployee() != null) {
            dto.setEmployeeId(entity.getEmployee().getId());
            dto.setEmployeeName(entity.getEmployee().getFullName());
            dto.setEmployeeEmail(entity.getEmployee().getEmail());
            if (entity.getEmployee().getDepartment() != null) {
                dto.setDepartmentName(entity.getEmployee().getDepartment().getName());
            }
            dto.setJobTitle(entity.getEmployee().getJobTitle());
        }
        dto.setLeaveType(entity.getLeaveType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setDurationDays(entity.getDurationDays());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        if (entity.getReviewedBy() != null) {
            dto.setReviewedByUsername(entity.getReviewedBy().getUsername());
        }
        dto.setReviewComments(entity.getReviewComments());
        dto.setAppliedAt(entity.getAppliedAt());
        dto.setReviewedAt(entity.getReviewedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getReviewedByUsername() {
        return reviewedByUsername;
    }

    public void setReviewedByUsername(String reviewedByUsername) {
        this.reviewedByUsername = reviewedByUsername;
    }

    public String getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(String reviewComments) {
        this.reviewComments = reviewComments;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
