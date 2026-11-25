package com.ems.service.impl;

import com.ems.dto.EmployeeRequestDTO;
import com.ems.dto.EmployeeResponseDTO;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.model.Department;
import com.ems.model.Employee;
import com.ems.model.EmployeeStatus;
import com.ems.model.EmploymentType;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> getEmployees(String keyword, Long departmentId,
                                                  EmployeeStatus status, EmploymentType type,
                                                  Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return employeeRepository.searchEmployees(cleanKeyword, departmentId, status, type, pageable)
                .map(EmployeeResponseDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> getAllEmployeesList() {
        return employeeRepository.findAll().stream()
                .map(EmployeeResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        return EmployeeResponseDTO.fromEntity(employee);
    }

    @Override
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO req) {
        if (employeeRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new DuplicateResourceException("An employee with email '" + req.getEmail() + "' already exists");
        }

        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));

        Employee employee = new Employee();
        mapRequestToEntity(req, employee, department);

        Employee saved = employeeRepository.save(employee);
        return EmployeeResponseDTO.fromEntity(saved);
    }

    @Override
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO req) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(req.getEmail(), id)) {
            throw new DuplicateResourceException("An employee with email '" + req.getEmail() + "' already exists");
        }

        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));

        mapRequestToEntity(req, employee, department);

        Employee updated = employeeRepository.save(employee);
        return EmployeeResponseDTO.fromEntity(updated);
    }

    @Override
    public EmployeeResponseDTO updateEmployeeStatus(Long id, EmployeeStatus status) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        employee.setStatus(status);
        Employee updated = employeeRepository.save(employee);
        return EmployeeResponseDTO.fromEntity(updated);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        employeeRepository.delete(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportEmployeesToCsv() {
        List<Employee> employees = employeeRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("ID,First Name,Last Name,Email,Phone,Job Title,Department,Salary,Hire Date,Type,Status,Address");
            for (Employee emp : employees) {
                writer.println(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%s,%s,%s,\"%s\"",
                        emp.getId(),
                        escapeCsv(emp.getFirstName()),
                        escapeCsv(emp.getLastName()),
                        escapeCsv(emp.getEmail()),
                        escapeCsv(emp.getPhone() != null ? emp.getPhone() : ""),
                        escapeCsv(emp.getJobTitle()),
                        escapeCsv(emp.getDepartment() != null ? emp.getDepartment().getName() : ""),
                        emp.getSalary() != null ? emp.getSalary() : 0.0,
                        emp.getHireDate() != null ? emp.getHireDate().toString() : "",
                        emp.getEmploymentType(),
                        emp.getStatus(),
                        escapeCsv(emp.getAddress() != null ? emp.getAddress() : "")
                ));
            }
        }
        return out.toByteArray();
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }

    private void mapRequestToEntity(EmployeeRequestDTO req, Employee emp, Department dept) {
        emp.setFirstName(req.getFirstName().trim());
        emp.setLastName(req.getLastName().trim());
        emp.setEmail(req.getEmail().trim().toLowerCase());
        emp.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        emp.setJobTitle(req.getJobTitle().trim());
        emp.setDepartment(dept);
        emp.setSalary(req.getSalary());
        emp.setHireDate(req.getHireDate());
        emp.setEmploymentType(req.getEmploymentType() != null ? req.getEmploymentType() : EmploymentType.FULL_TIME);
        emp.setStatus(req.getStatus() != null ? req.getStatus() : EmployeeStatus.ACTIVE);
        emp.setAddress(req.getAddress() != null ? req.getAddress().trim() : null);
    }
}
