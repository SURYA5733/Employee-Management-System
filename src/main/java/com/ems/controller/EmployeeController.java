package com.ems.controller;

import com.ems.dto.ApiResponse;
import com.ems.dto.EmployeeRequestDTO;
import com.ems.dto.EmployeeResponseDTO;
import com.ems.model.EmployeeStatus;
import com.ems.model.EmploymentType;
import com.ems.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
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
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EmployeeResponseDTO> employees = employeeService.getEmployees(keyword, departmentId, status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getAllEmployees() {
        List<EmployeeResponseDTO> list = employeeService.getAllEmployeesList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getEmployeeById(@PathVariable Long id) {
        EmployeeResponseDTO employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> createEmployee(
            @Valid @RequestBody EmployeeRequestDTO requestDTO) {
        EmployeeResponseDTO created = employeeService.createEmployee(requestDTO);
        return new ResponseEntity<>(ApiResponse.success("Employee created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDTO requestDTO) {
        EmployeeResponseDTO updated = employeeService.updateEmployee(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportEmployeesCsv() {
        byte[] csvData = employeeService.exportEmployeesToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
