package com.ems.service;

import com.ems.dto.LeaveApplicationRequest;
import com.ems.dto.LeaveResponseDTO;
import com.ems.dto.LeaveReviewRequest;
import com.ems.dto.LeaveStatsDTO;
import com.ems.model.LeaveStatus;

import java.util.List;

public interface LeaveService {

    LeaveResponseDTO applyLeave(Long employeeId, LeaveApplicationRequest request);

    List<LeaveResponseDTO> getMyLeaves(Long employeeId);

    List<LeaveResponseDTO> getAllLeaves(LeaveStatus status, Long departmentId);

    LeaveResponseDTO reviewLeave(Long leaveId, LeaveReviewRequest request, String reviewerUsername);

    void cancelLeave(Long leaveId, Long employeeId, boolean isAdminOrHr);

    LeaveStatsDTO getLeaveStats();
}
