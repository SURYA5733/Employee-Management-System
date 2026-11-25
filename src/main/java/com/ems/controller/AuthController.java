package com.ems.controller;

import com.ems.dto.ApiResponse;
import com.ems.dto.AuthResponse;
import com.ems.dto.LoginRequest;
import com.ems.dto.RegisterRequest;
import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.model.Employee;
import com.ems.model.Role;
import com.ems.model.User;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.UserRepository;
import com.ems.security.CustomUserDetails;
import com.ems.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          EmployeeRepository employeeRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail().trim(),
                        loginRequest.getPassword()
                )
        );

        String jwt = tokenProvider.generateToken(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        AuthResponse authResponse = new AuthResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles,
                userDetails.getEmployeeId()
        );

        return ResponseEntity.ok(ApiResponse.success("Authentication successful", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new DuplicateResourceException("Username '" + registerRequest.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email '" + registerRequest.getEmail() + "' is already registered");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername().trim());
        user.setEmail(registerRequest.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRoles(new HashSet<>(Collections.singletonList(
                registerRequest.getRole() != null ? registerRequest.getRole() : Role.ROLE_EMPLOYEE
        )));

        if (registerRequest.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(registerRequest.getEmployeeId())
                    .orElseThrow(() -> new BadRequestException("Employee not found with ID: " + registerRequest.getEmployeeId()));
            user.setEmployee(employee);
        }

        User savedUser = userRepository.save(user);

        // Auto-login registered user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registerRequest.getUsername(), registerRequest.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        AuthResponse authResponse = new AuthResponse(
                jwt,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                roles,
                savedUser.getEmployee() != null ? savedUser.getEmployee().getId() : null
        );

        return new ResponseEntity<>(ApiResponse.success("User registered successfully", authResponse), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        AuthResponse response = new AuthResponse(
                null,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles,
                userDetails.getEmployeeId()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
