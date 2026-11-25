package com.ems.dto;

import com.ems.model.Employee;
import com.ems.model.EmployeeStatus;
import com.ems.model.EmploymentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String jobTitle;
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private BigDecimal salary;
    private LocalDate hireDate;
    private EmploymentType employmentType;
    private EmployeeStatus status;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmployeeResponseDTO() {
    }

    public static EmployeeResponseDTO fromEntity(Employee emp) {
        if (emp == null) return null;
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setId(emp.getId());
        dto.setFirstName(emp.getFirstName());
        dto.setLastName(emp.getLastName());
        dto.setFullName(emp.getFullName());
        dto.setEmail(emp.getEmail());
        dto.setPhone(emp.getPhone());
        dto.setJobTitle(emp.getJobTitle());
        if (emp.getDepartment() != null) {
            dto.setDepartmentId(emp.getDepartment().getId());
            dto.setDepartmentName(emp.getDepartment().getName());
            dto.setDepartmentCode(emp.getDepartment().getCode());
        }
        dto.setSalary(emp.getSalary());
        dto.setHireDate(emp.getHireDate());
        dto.setEmploymentType(emp.getEmploymentType());
        dto.setStatus(emp.getStatus());
        dto.setAddress(emp.getAddress());
        dto.setCreatedAt(emp.getCreatedAt());
        dto.setUpdatedAt(emp.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
