package com.ems.repository;

import com.ems.model.LeaveRequest;
import com.ems.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByAppliedAtDesc(Long employeeId);

    List<LeaveRequest> findByStatusOrderByAppliedAtDesc(LeaveStatus status);

    List<LeaveRequest> findAllByOrderByAppliedAtDesc();

    long countByStatus(LeaveStatus status);

    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.status = 'APPROVED' AND :today BETWEEN l.startDate AND l.endDate")
    long countEmployeesCurrentlyOnLeave(@Param("today") LocalDate today);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LeaveRequest l " +
           "WHERE l.employee.id = :employeeId " +
           "AND l.status != 'REJECTED' AND l.status != 'CANCELLED' " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate " +
           "AND (:excludeId IS NULL OR l.id != :excludeId)")
    boolean hasOverlappingLeave(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId
    );
}
