package com.ems.controller;

import com.ems.dto.ApiResponse;
import com.ems.dto.LeaveApplicationRequest;
import com.ems.dto.LeaveResponseDTO;
import com.ems.dto.LeaveReviewRequest;
import com.ems.dto.LeaveStatsDTO;
import com.ems.exception.BadRequestException;
import com.ems.model.LeaveStatus;
import com.ems.security.CustomUserDetails;
import com.ems.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
@CrossOrigin(origins = "*")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> applyLeave(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LeaveApplicationRequest request) {

        if (userDetails == null || userDetails.getEmployeeId() == null) {
            throw new BadRequestException("No employee profile is linked to your account. Please contact HR or an Administrator.");
        }

        LeaveResponseDTO response = leaveService.applyLeave(userDetails.getEmployeeId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave application submitted successfully", response));
    }

    @GetMapping("/my-leaves")
    public ResponseEntity<ApiResponse<List<LeaveResponseDTO>>> getMyLeaves(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getEmployeeId() == null) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        List<LeaveResponseDTO> list = leaveService.getMyLeaves(userDetails.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<LeaveResponseDTO>>> getAllLeaves(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) Long departmentId) {

        List<LeaveResponseDTO> list = leaveService.getAllLeaves(status, departmentId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveStatsDTO>> getLeaveStats() {
        LeaveStatsDTO stats = leaveService.getLeaveStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> reviewLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LeaveReviewRequest request) {

        String reviewer = userDetails != null ? userDetails.getUsername() : null;
        LeaveResponseDTO updated = leaveService.reviewLeave(id, request, reviewer);
        return ResponseEntity.ok(ApiResponse.success("Leave request marked as " + updated.getStatus(), updated));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> approveLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) LeaveReviewRequest request) {

        String reviewer = userDetails != null ? userDetails.getUsername() : null;
        String comments = (request != null && request.getReviewComments() != null)
                ? request.getReviewComments() : "Approved";

        LeaveReviewRequest reviewReq = new LeaveReviewRequest(LeaveStatus.APPROVED, comments);
        LeaveResponseDTO updated = leaveService.reviewLeave(id, reviewReq, reviewer);
        return ResponseEntity.ok(ApiResponse.success("Leave request approved successfully", updated));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> rejectLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) LeaveReviewRequest request) {

        String reviewer = userDetails != null ? userDetails.getUsername() : null;
        String comments = (request != null && request.getReviewComments() != null)
                ? request.getReviewComments() : "Rejected";

        LeaveReviewRequest reviewReq = new LeaveReviewRequest(LeaveStatus.REJECTED, comments);
        LeaveResponseDTO updated = leaveService.reviewLeave(id, reviewReq, reviewer);
        return ResponseEntity.ok(ApiResponse.success("Leave request rejected", updated));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BadRequestException("User authentication required");
        }

        boolean isAdminOrHr = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_HR".equals(a.getAuthority()));

        leaveService.cancelLeave(id, userDetails.getEmployeeId(), isAdminOrHr);
        return ResponseEntity.ok(ApiResponse.success("Leave request cancelled successfully", null));
    }
}
