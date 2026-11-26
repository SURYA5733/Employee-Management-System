package com.ems.service;

import com.ems.dto.LeaveApplicationRequest;
import com.ems.dto.LeaveResponseDTO;
import com.ems.dto.LeaveReviewRequest;
import com.ems.dto.LeaveStatsDTO;
import com.ems.exception.BadRequestException;
import com.ems.model.*;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.LeaveRepository;
import com.ems.repository.UserRepository;
import com.ems.service.impl.LeaveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    private Department department;
    private Employee employee;

    @BeforeEach
    void setUp() {
        department = new Department(1L, "Engineering", "ENG", "Engineering Dept", "Floor 4");
        employee = new Employee(1L, "Sarah", "Connor", "s.connor@company.com", "1234567890",
                "Senior Software Engineer", department, new BigDecimal("95000.00"),
                LocalDate.now().minusYears(1), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE, "123 Main St");
    }

    @Test
    void applyLeave_Success() {
        LeaveApplicationRequest req = new LeaveApplicationRequest();
        req.setLeaveType(LeaveType.CASUAL);
        req.setStartDate(LocalDate.now().plusDays(2));
        req.setEndDate(LocalDate.now().plusDays(4));
        req.setReason("Attending family event");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.hasOverlappingLeave(eq(1L), any(LocalDate.class), any(LocalDate.class), isNull()))
                .thenReturn(false);
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        LeaveResponseDTO result = leaveService.applyLeave(1L, req);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getEmployeeId());
        assertEquals(3, result.getDurationDays());
        assertEquals(LeaveStatus.PENDING, result.getStatus());
        verify(leaveRepository, times(1)).save(any(LeaveRequest.class));
    }

    @Test
    void applyLeave_ThrowsOnOverlap() {
        LeaveApplicationRequest req = new LeaveApplicationRequest();
        req.setLeaveType(LeaveType.SICK);
        req.setStartDate(LocalDate.now().plusDays(1));
        req.setEndDate(LocalDate.now().plusDays(3));
        req.setReason("Dentist appointment");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.hasOverlappingLeave(eq(1L), any(LocalDate.class), any(LocalDate.class), isNull()))
                .thenReturn(true);

        assertThrows(BadRequestException.class, () -> leaveService.applyLeave(1L, req));
        verify(leaveRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void reviewLeave_ApproveSuccess_AndSetsEmployeeOnLeave() {
        LocalDate today = LocalDate.now();
        LeaveRequest leave = new LeaveRequest(employee, LeaveType.ANNUAL, today, today.plusDays(3), "Summer vacation");
        leave.setId(5L);
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById(5L)).thenReturn(Optional.of(leave));
        when(userRepository.findByUsername("hr")).thenReturn(Optional.empty());
        when(leaveRepository.save(any(LeaveRequest.class))).thenReturn(leave);

        LeaveReviewRequest reviewReq = new LeaveReviewRequest(LeaveStatus.APPROVED, "Approved by HR");
        LeaveResponseDTO result = leaveService.reviewLeave(5L, reviewReq, "hr");

        assertEquals(LeaveStatus.APPROVED, result.getStatus());
        assertEquals(EmployeeStatus.ON_LEAVE, employee.getStatus());
        verify(employeeRepository, times(1)).save(employee);
        verify(leaveRepository, times(1)).save(leave);
    }

    @Test
    void cancelLeave_Success() {
        LeaveRequest leave = new LeaveRequest(employee, LeaveType.CASUAL, LocalDate.now().plusDays(5), LocalDate.now().plusDays(6), "Personal day");
        leave.setId(8L);
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById(8L)).thenReturn(Optional.of(leave));

        leaveService.cancelLeave(8L, 1L, false);

        assertEquals(LeaveStatus.CANCELLED, leave.getStatus());
        verify(leaveRepository, times(1)).save(leave);
    }

    @Test
    void getLeaveStats_ReturnsAggregatedCounts() {
        when(leaveRepository.countByStatus(LeaveStatus.PENDING)).thenReturn(3L);
        when(leaveRepository.countByStatus(LeaveStatus.APPROVED)).thenReturn(8L);
        when(leaveRepository.countByStatus(LeaveStatus.REJECTED)).thenReturn(1L);
        when(leaveRepository.countEmployeesCurrentlyOnLeave(any(LocalDate.class))).thenReturn(2L);

        LeaveStatsDTO stats = leaveService.getLeaveStats();

        assertEquals(3L, stats.getTotalPending());
        assertEquals(8L, stats.getTotalApproved());
        assertEquals(1L, stats.getTotalRejected());
        assertEquals(2L, stats.getCurrentlyOnLeave());
    }
}
