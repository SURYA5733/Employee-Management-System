package com.ems.service.impl;

import com.ems.dto.LeaveApplicationRequest;
import com.ems.dto.LeaveResponseDTO;
import com.ems.dto.LeaveReviewRequest;
import com.ems.dto.LeaveStatsDTO;
import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.model.*;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.LeaveRepository;
import com.ems.repository.UserRepository;
import com.ems.service.LeaveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public LeaveServiceImpl(LeaveRepository leaveRepository,
                            EmployeeRepository employeeRepository,
                            UserRepository userRepository) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public LeaveResponseDTO applyLeave(Long employeeId, LeaveApplicationRequest request) {
        if (employeeId == null) {
            throw new BadRequestException("Employee ID is required to apply for leave");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be earlier than start date");
        }

        boolean hasOverlap = leaveRepository.hasOverlappingLeave(
                employeeId,
                request.getStartDate(),
                request.getEndDate(),
                null
        );

        if (hasOverlap) {
            throw new BadRequestException("An existing or pending leave request already overlaps with these selected dates");
        }

        LeaveRequest leaveRequest = new LeaveRequest(
                employee,
                request.getLeaveType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getReason()
        );

        LeaveRequest saved = leaveRepository.save(leaveRequest);
        return LeaveResponseDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDTO> getMyLeaves(Long employeeId) {
        if (employeeId == null) {
            throw new BadRequestException("Employee ID cannot be null");
        }
        return leaveRepository.findByEmployeeIdOrderByAppliedAtDesc(employeeId)
                .stream()
                .map(LeaveResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDTO> getAllLeaves(LeaveStatus status, Long departmentId) {
        List<LeaveRequest> list;
        if (status != null) {
            list = leaveRepository.findByStatusOrderByAppliedAtDesc(status);
        } else {
            list = leaveRepository.findAllByOrderByAppliedAtDesc();
        }

        if (departmentId != null) {
            list = list.stream()
                    .filter(l -> l.getEmployee() != null
                            && l.getEmployee().getDepartment() != null
                            && departmentId.equals(l.getEmployee().getDepartment().getId()))
                    .collect(Collectors.toList());
        }

        return list.stream()
                .map(LeaveResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public LeaveResponseDTO reviewLeave(Long leaveId, LeaveReviewRequest request, String reviewerUsername) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + leaveId));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only PENDING leave requests can be reviewed. Current status: " + leave.getStatus());
        }

        if (request.getStatus() != LeaveStatus.APPROVED && request.getStatus() != LeaveStatus.REJECTED) {
            throw new BadRequestException("Review status must be either APPROVED or REJECTED");
        }

        User reviewer = null;
        if (reviewerUsername != null) {
            reviewer = userRepository.findByUsername(reviewerUsername).orElse(null);
        }

        leave.setStatus(request.getStatus());
        leave.setReviewComments(request.getReviewComments());
        leave.setReviewedBy(reviewer);
        leave.setReviewedAt(LocalDateTime.now());

        // If approved and today falls in the leave span, update employee status to ON_LEAVE
        if (request.getStatus() == LeaveStatus.APPROVED) {
            LocalDate today = LocalDate.now();
            if (!today.isBefore(leave.getStartDate()) && !today.isAfter(leave.getEndDate())) {
                Employee employee = leave.getEmployee();
                if (employee != null && employee.getStatus() != EmployeeStatus.TERMINATED) {
                    employee.setStatus(EmployeeStatus.ON_LEAVE);
                    employeeRepository.save(employee);
                }
            }
        }

        LeaveRequest updated = leaveRepository.save(leave);
        return LeaveResponseDTO.fromEntity(updated);
    }

    @Override
    public void cancelLeave(Long leaveId, Long employeeId, boolean isAdminOrHr) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + leaveId));

        if (!isAdminOrHr) {
            if (leave.getEmployee() == null || !leave.getEmployee().getId().equals(employeeId)) {
                throw new BadRequestException("You are not authorized to cancel this leave application");
            }
        }

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only PENDING leave requests can be cancelled. Current status: " + leave.getStatus());
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        leaveRepository.save(leave);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveStatsDTO getLeaveStats() {
        long pending = leaveRepository.countByStatus(LeaveStatus.PENDING);
        long approved = leaveRepository.countByStatus(LeaveStatus.APPROVED);
        long rejected = leaveRepository.countByStatus(LeaveStatus.REJECTED);
        long currentlyOnLeave = leaveRepository.countEmployeesCurrentlyOnLeave(LocalDate.now());

        return new LeaveStatsDTO(pending, approved, rejected, currentlyOnLeave);
    }
}
