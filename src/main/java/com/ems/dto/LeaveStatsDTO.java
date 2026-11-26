package com.ems.dto;

public class LeaveStatsDTO {

    private long totalPending;
    private long totalApproved;
    private long totalRejected;
    private long currentlyOnLeave;

    public LeaveStatsDTO() {
    }

    public LeaveStatsDTO(long totalPending, long totalApproved, long totalRejected, long currentlyOnLeave) {
        this.totalPending = totalPending;
        this.totalApproved = totalApproved;
        this.totalRejected = totalRejected;
        this.currentlyOnLeave = currentlyOnLeave;
    }

    public long getTotalPending() {
        return totalPending;
    }

    public void setTotalPending(long totalPending) {
        this.totalPending = totalPending;
    }

    public long getTotalApproved() {
        return totalApproved;
    }

    public void setTotalApproved(long totalApproved) {
        this.totalApproved = totalApproved;
    }

    public long getTotalRejected() {
        return totalRejected;
    }

    public void setTotalRejected(long totalRejected) {
        this.totalRejected = totalRejected;
    }

    public long getCurrentlyOnLeave() {
        return currentlyOnLeave;
    }

    public void setCurrentlyOnLeave(long currentlyOnLeave) {
        this.currentlyOnLeave = currentlyOnLeave;
    }
}
