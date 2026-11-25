package com.ems.service.impl;

import com.ems.dto.DepartmentDTO;
import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.model.Department;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                 EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return mapToDTO(department);
    }

    @Override
    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        if (departmentRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Department with name '" + dto.getName() + "' already exists");
        }
        if (departmentRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw new DuplicateResourceException("Department code '" + dto.getCode() + "' is already in use");
        }

        Department department = new Department(
                dto.getName().trim(),
                dto.getCode().trim().toUpperCase(),
                dto.getDescription() != null ? dto.getDescription().trim() : null,
                dto.getLocation() != null ? dto.getLocation().trim() : null
        );

        Department saved = departmentRepository.save(department);
        return mapToDTO(saved);
    }

    @Override
    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            throw new DuplicateResourceException("Department with name '" + dto.getName() + "' already exists");
        }
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(dto.getCode(), id)) {
            throw new DuplicateResourceException("Department code '" + dto.getCode() + "' is already in use");
        }

        department.setName(dto.getName().trim());
        department.setCode(dto.getCode().trim().toUpperCase());
        department.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        department.setLocation(dto.getLocation() != null ? dto.getLocation().trim() : null);

        Department updated = departmentRepository.save(department);
        return mapToDTO(updated);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        long count = employeeRepository.countByDepartmentId(id);
        if (count > 0) {
            throw new BadRequestException("Cannot delete department '" + department.getName() +
                    "' because it has " + count + " assigned employee(s). Reassign them first.");
        }

        departmentRepository.delete(department);
    }

    private DepartmentDTO mapToDTO(Department dept) {
        long employeeCount = employeeRepository.countByDepartmentId(dept.getId());
        return new DepartmentDTO(
                dept.getId(),
                dept.getName(),
                dept.getCode(),
                dept.getDescription(),
                dept.getLocation(),
                employeeCount
        );
    }
}
