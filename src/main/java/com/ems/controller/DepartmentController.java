package com.ems.controller;

import com.ems.dto.ApiResponse;
import com.ems.dto.DepartmentDTO;
import com.ems.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAllDepartments() {
        List<DepartmentDTO> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.success(departments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getDepartmentById(@PathVariable Long id) {
        DepartmentDTO department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success(department));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> createDepartment(
            @Valid @RequestBody DepartmentDTO departmentDTO) {
        DepartmentDTO created = departmentService.createDepartment(departmentDTO);
        return new ResponseEntity<>(ApiResponse.success("Department created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentDTO departmentDTO) {
        DepartmentDTO updated = departmentService.updateDepartment(id, departmentDTO);
        return ResponseEntity.ok(ApiResponse.success("Department updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.success("Department deleted successfully", null));
    }
}
