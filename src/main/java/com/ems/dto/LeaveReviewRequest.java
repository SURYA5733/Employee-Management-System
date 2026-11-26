package com.ems.dto;

import com.ems.model.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LeaveReviewRequest {

    @NotNull(message = "Review status is required")
    private LeaveStatus status;

    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String reviewComments;

    public LeaveReviewRequest() {
    }

    public LeaveReviewRequest(LeaveStatus status, String reviewComments) {
        this.status = status;
        this.reviewComments = reviewComments;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(String reviewComments) {
        this.reviewComments = reviewComments;
    }
}
