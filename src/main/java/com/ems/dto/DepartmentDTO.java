package com.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DepartmentDTO {

    private Long id;

    @NotBlank(message = "Department name is required")
    @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Department code is required")
    @Size(min = 2, max = 20, message = "Department code must be between 2 and 20 characters")
    private String code;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    private Long employeeCount;

    public DepartmentDTO() {
    }

    public DepartmentDTO(Long id, String name, String code, String description, String location, Long employeeCount) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.location = location;
        this.employeeCount = employeeCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Long employeeCount) {
        this.employeeCount = employeeCount;
    }
}
