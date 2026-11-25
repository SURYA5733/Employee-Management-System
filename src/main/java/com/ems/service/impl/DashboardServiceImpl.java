package com.ems.service.impl;

import com.ems.dto.DashboardStatsDTO;
import com.ems.model.EmployeeStatus;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public DashboardServiceImpl(EmployeeRepository employeeRepository,
                                DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        long total = employeeRepository.count();
        long active = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long onLeave = employeeRepository.countByStatus(EmployeeStatus.ON_LEAVE);
        long terminated = employeeRepository.countByStatus(EmployeeStatus.TERMINATED);
        long totalDepts = departmentRepository.count();

        stats.setTotalEmployees(total);
        stats.setActiveEmployees(active);
        stats.setOnLeaveEmployees(onLeave);
        stats.setTerminatedEmployees(terminated);
        stats.setTotalDepartments(totalDepts);

        BigDecimal avgSalary = employeeRepository.calculateAverageActiveSalary();
        BigDecimal totalPayroll = employeeRepository.calculateTotalActivePayroll();

        stats.setAverageSalary(avgSalary != null ? avgSalary.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        stats.setTotalPayroll(totalPayroll != null ? totalPayroll.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        Map<String, Long> deptMap = new LinkedHashMap<>();
        List<Object[]> deptCounts = employeeRepository.countEmployeesByDepartment();
        for (Object[] row : deptCounts) {
            String deptName = (String) row[0];
            Long count = (Long) row[1];
            deptMap.put(deptName, count);
        }
        stats.setDepartmentDistribution(deptMap);

        Map<String, Long> statusMap = new LinkedHashMap<>();
        List<Object[]> statusCounts = employeeRepository.countEmployeesByStatusGroup();
        for (Object[] row : statusCounts) {
            EmployeeStatus st = (EmployeeStatus) row[0];
            Long count = (Long) row[1];
            statusMap.put(st.name(), count);
        }
        stats.setStatusDistribution(statusMap);

        return stats;
    }
}
