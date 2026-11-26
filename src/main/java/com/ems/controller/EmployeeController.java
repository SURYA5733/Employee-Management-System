package com.ems.controller;

import com.ems.dto.ApiResponse;
import com.ems.dto.EmployeeRequestDTO;
import com.ems.dto.EmployeeResponseDTO;
import com.ems.exception.ResourceNotFoundException;
import com.ems.model.EmployeeStatus;
import com.ems.model.EmploymentType;
import com.ems.security.CustomUserDetails;
import com.ems.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    private boolean isRegularEmployeeOnly(CustomUserDetails userDetails) {
        if (userDetails == null) return false;
        boolean hasPrivilegedRole = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_HR") ||
                               a.getAuthority().equals("ROLE_MANAGER"));
        return !hasPrivilegedRole;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmployeeResponseDTO>>> getEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) EmploymentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Enforce employee isolation: Employees can ONLY view their own account
        if (isRegularEmployeeOnly(userDetails)) {
            if (userDetails.getEmployeeId() != null) {
                EmployeeResponseDTO ownRecord = employeeService.getEmployeeById(userDetails.getEmployeeId());
                Page<EmployeeResponseDTO> singlePage = new PageImpl<>(
                        Collections.singletonList(ownRecord),
                        PageRequest.of(0, 1),
                        1
                );
                return ResponseEntity.ok(ApiResponse.success(singlePage));
            } else {
                return ResponseEntity.ok(ApiResponse.success(Page.empty()));
            }
        }

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EmployeeResponseDTO> employees = employeeService.getEmployees(keyword, departmentId, status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getAllEmployees(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (isRegularEmployeeOnly(userDetails)) {
            if (userDetails.getEmployeeId() != null) {
                EmployeeResponseDTO ownRecord = employeeService.getEmployeeById(userDetails.getEmployeeId());
                return ResponseEntity.ok(ApiResponse.success(Collections.singletonList(ownRecord)));
            } else {
                return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
            }
        }

        List<EmployeeResponseDTO> list = employeeService.getAllEmployeesList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null || userDetails.getEmployeeId() == null) {
            throw new ResourceNotFoundException("No employee profile linked to current user account");
        }
        EmployeeResponseDTO employee = employeeService.getEmployeeById(userDetails.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getEmployeeById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Regular employees can ONLY view their own ID
        if (isRegularEmployeeOnly(userDetails)) {
            if (userDetails.getEmployeeId() == null || !userDetails.getEmployeeId().equals(id)) {
                throw new AccessDeniedException("Access Denied: Employees can only view their own profile.");
            }
        }

        EmployeeResponseDTO employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> createEmployee(
            @Valid @RequestBody EmployeeRequestDTO requestDTO) {
        EmployeeResponseDTO created = employeeService.createEmployee(requestDTO);
        return new ResponseEntity<>(ApiResponse.success("Employee created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDTO requestDTO) {
        EmployeeResponseDTO updated = employeeService.updateEmployee(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> updateEmployeeStatus(
            @PathVariable Long id,
            @RequestParam EmployeeStatus status) {
        EmployeeResponseDTO updated = employeeService.updateEmployeeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Employee status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted successfully", null));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<byte[]> exportEmployeesCsv() {
        byte[] csvData = employeeService.exportEmployeesToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
