package com.ems.service;

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
import com.ems.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Department department;
    private Employee employee;

    @BeforeEach
    void setUp() {
        department = new Department(1L, "Engineering", "ENG", "Engineering Dept", "Floor 4");
        employee = new Employee(1L, "John", "Doe", "john.doe@example.com", "1234567890",
                "Software Engineer", department, new BigDecimal("85000.00"),
                LocalDate.now(), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE, "123 Main St");
    }

    @Test
    void getEmployeeById_success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponseDTO result = employeeService.getEmployeeById(1L);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("Engineering", result.getDepartmentName());
    }

    @Test
    void getEmployeeById_notFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(99L));
    }

    @Test
    void createEmployee_duplicateEmail() {
        EmployeeRequestDTO request = new EmployeeRequestDTO();
        request.setEmail("john.doe@example.com");

        when(employeeRepository.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    void createEmployee_success() {
        EmployeeRequestDTO request = new EmployeeRequestDTO();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setEmail("jane.smith@example.com");
        request.setJobTitle("QA Engineer");
        request.setDepartmentId(1L);
        request.setSalary(new BigDecimal("75000.00"));
        request.setHireDate(LocalDate.now());

        when(employeeRepository.existsByEmailIgnoreCase("jane.smith@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        EmployeeResponseDTO response = employeeService.createEmployee(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Jane", response.getFirstName());
        assertEquals("jane.smith@example.com", response.getEmail());
    }
}
