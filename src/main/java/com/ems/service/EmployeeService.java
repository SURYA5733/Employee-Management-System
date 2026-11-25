package com.ems.service;

import com.ems.dto.EmployeeRequestDTO;
import com.ems.dto.EmployeeResponseDTO;
import com.ems.model.EmployeeStatus;
import com.ems.model.EmploymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {

    Page<EmployeeResponseDTO> getEmployees(String keyword, Long departmentId,
                                           EmployeeStatus status, EmploymentType type,
                                           Pageable pageable);

    List<EmployeeResponseDTO> getAllEmployeesList();

    EmployeeResponseDTO getEmployeeById(Long id);

    EmployeeResponseDTO createEmployee(EmployeeRequestDTO requestDTO);

    EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO requestDTO);

    EmployeeResponseDTO updateEmployeeStatus(Long id, EmployeeStatus status);

    void deleteEmployee(Long id);

    byte[] exportEmployeesToCsv();
}
