package com.ems.dto;

import java.util.List;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private List<String> roles;
    private Long employeeId;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long userId, String username, String email, List<String> roles, Long employeeId) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.employeeId = employeeId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
}
