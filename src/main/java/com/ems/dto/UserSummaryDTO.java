package com.ems.dto;

import com.ems.model.Role;
import com.ems.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserSummaryDTO {

    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private boolean enabled;
    private Long employeeId;
    private String employeeName;

    public UserSummaryDTO() {
    }

    public static UserSummaryDTO fromEntity(User user) {
        if (user == null) return null;
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRoles(user.getRoles().stream().map(Role::name).collect(Collectors.toList()));
        dto.setEnabled(user.isEnabled());
        if (user.getEmployee() != null) {
            dto.setEmployeeId(user.getEmployee().getId());
            dto.setEmployeeName(user.getEmployee().getFullName());
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
}
