package com.ems.dto;

import com.ems.model.EmployeeStatus;
import com.ems.model.EmploymentType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 60, message = "First name must be between 2 and 60 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 60, message = "Last name must be between 1 and 60 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Pattern(regexp = "^$|^[+0-9\\-\\s()]{7,20}$", message = "Phone number is invalid")
    private String phone;

    @NotBlank(message = "Job title is required")
    @Size(max = 100, message = "Job title cannot exceed 100 characters")
    private String jobTitle;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.01", message = "Salary must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Salary format is invalid")
    private BigDecimal salary;

    @NotNull(message = "Hire date is required")
    @PastOrPresent(message = "Hire date cannot be in the future")
    private LocalDate hireDate;

    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    public EmployeeRequestDTO() {
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
}
