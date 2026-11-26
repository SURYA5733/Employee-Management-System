package com.ems.controller;

import com.ems.dto.*;
import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
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
import org.springframework.security.access.prepost.PreAuthorize;
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

    private void ensureDemoUserExists(String usernameOrEmail) {
        if (usernameOrEmail == null) return;
        String identifier = usernameOrEmail.trim().toLowerCase();

        if (identifier.equals("admin") || identifier.equals("admin@company.com")) {
            userRepository.findByUsernameIgnoreCase("admin").ifPresentOrElse(
                    admin -> {
                        admin.setPassword(passwordEncoder.encode("admin123"));
                        admin.setEnabled(true);
                        admin.setRoles(new HashSet<>(Collections.singletonList(Role.ROLE_ADMIN)));
                        userRepository.save(admin);
                    },
                    () -> {
                        User admin = new User("admin", "admin@company.com", passwordEncoder.encode("admin123"),
                                new HashSet<>(Collections.singletonList(Role.ROLE_ADMIN)));
                        userRepository.save(admin);
                    }
            );
        } else if (identifier.equals("hr") || identifier.equals("hr@company.com")) {
            userRepository.findByUsernameIgnoreCase("hr").ifPresentOrElse(
                    hr -> {
                        hr.setPassword(passwordEncoder.encode("hr123"));
                        hr.setEnabled(true);
                        hr.setRoles(new HashSet<>(Collections.singletonList(Role.ROLE_HR)));
                        userRepository.save(hr);
                    },
                    () -> {
                        User hrUser = new User("hr", "hr@company.com", passwordEncoder.encode("hr123"),
                                new HashSet<>(Collections.singletonList(Role.ROLE_HR)));
                        employeeRepository.findAll().stream()
                                .filter(e -> e.getJobTitle().toLowerCase().contains("hr") || e.getJobTitle().toLowerCase().contains("recruiter"))
                                .findFirst()
                                .ifPresent(hrUser::setEmployee);
                        userRepository.save(hrUser);
                    }
            );
        } else if (identifier.equals("manager") || identifier.equals("manager@company.com")) {
            userRepository.findByUsernameIgnoreCase("manager").ifPresentOrElse(
                    mgr -> {
                        mgr.setPassword(passwordEncoder.encode("manager123"));
                        mgr.setEnabled(true);
                        mgr.setRoles(new HashSet<>(Collections.singletonList(Role.ROLE_MANAGER)));
                        userRepository.save(mgr);
                    },
                    () -> {
                        User manager = new User("manager", "manager@company.com", passwordEncoder.encode("manager123"),
                                new HashSet<>(Collections.singletonList(Role.ROLE_MANAGER)));
                        userRepository.save(manager);
                    }
            );
        } else if (identifier.equals("user") || identifier.equals("user@company.com")) {
            userRepository.findByUsernameIgnoreCase("user").ifPresentOrElse(
                    usr -> {
                        usr.setPassword(passwordEncoder.encode("user123"));
                        usr.setEnabled(true);
                        usr.setRoles(new HashSet<>(Collections.singletonList(Role.ROLE_EMPLOYEE)));
                        userRepository.save(usr);
                    },
                    () -> {
                        User employeeUser = new User("user", "user@company.com", passwordEncoder.encode("user123"),
                                new HashSet<>(Collections.singletonList(Role.ROLE_EMPLOYEE)));
                        employeeRepository.findAll().stream().findFirst().ifPresent(employeeUser::setEmployee);
                        userRepository.save(employeeUser);
                    }
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String identifier = loginRequest.getUsernameOrEmail().trim();

        // Self-heal demo accounts if missing or password mismatch
        ensureDemoUserExists(identifier);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        identifier,
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
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            @AuthenticationPrincipal CustomUserDetails creatorDetails
    ) {
        // Enforce HR restriction: HR cannot create Admin accounts
        if (creatorDetails != null) {
            boolean isHr = creatorDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_HR"));
            boolean isAdmin = creatorDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (isHr && !isAdmin && registerRequest.getRole() == Role.ROLE_ADMIN) {
                throw new BadRequestException("HR users are not authorized to create Admin accounts.");
            }
        } else {
            // Unauthenticated public registration defaults to EMPLOYEE
            if (registerRequest.getRole() == Role.ROLE_ADMIN || registerRequest.getRole() == Role.ROLE_HR) {
                registerRequest.setRole(Role.ROLE_EMPLOYEE);
            }
        }

        if (userRepository.existsByUsernameIgnoreCase(registerRequest.getUsername())) {
            throw new DuplicateResourceException("Username '" + registerRequest.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmailIgnoreCase(registerRequest.getEmail())) {
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

        // If an Admin/HR created this account on behalf of another user, return success without logging into it
        if (creatorDetails != null) {
            AuthResponse response = new AuthResponse(
                    null,
                    savedUser.getId(),
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    savedUser.getRoles().stream().map(Role::name).collect(Collectors.toList()),
                    savedUser.getEmployee() != null ? savedUser.getEmployee().getId() : null
            );
            return new ResponseEntity<>(ApiResponse.success("User account created successfully", response), HttpStatus.CREATED);
        }

        // Auto-login registered user for self-registration
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

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getAllUserAccounts() {
        List<UserSummaryDTO> users = userRepository.findAll().stream()
                .map(UserSummaryDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> toggleUserStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails callerDetails
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        boolean isCallerAdmin = callerDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (user.getRoles().contains(Role.ROLE_ADMIN) && !isCallerAdmin) {
            throw new BadRequestException("HR users cannot disable or modify Admin accounts.");
        }

        user.setEnabled(!user.isEnabled());
        User updated = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("User status updated", UserSummaryDTO.fromEntity(updated)));
    }
}
