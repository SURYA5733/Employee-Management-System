package com.ems.repository;

import com.ems.model.Employee;
import com.ems.model.EmployeeStatus;
import com.ems.model.EmploymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByStatus(EmployeeStatus status);

    long countByStatus(EmployeeStatus status);

    long countByDepartmentId(Long departmentId);

    @Query("SELECT e FROM Employee e WHERE " +
           "(:deptId IS NULL OR e.department.id = :deptId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:type IS NULL OR e.employmentType = :type) AND " +
           "(:keyword IS NULL OR " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Employee> searchEmployees(
            @Param("keyword") String keyword,
            @Param("deptId") Long deptId,
            @Param("status") EmployeeStatus status,
            @Param("type") EmploymentType type,
            Pageable pageable
    );

    @Query("SELECT AVG(e.salary) FROM Employee e WHERE e.status = 'ACTIVE'")
    BigDecimal calculateAverageActiveSalary();

    @Query("SELECT SUM(e.salary) FROM Employee e WHERE e.status = 'ACTIVE'")
    BigDecimal calculateTotalActivePayroll();

    @Query("SELECT e.department.name, COUNT(e) FROM Employee e GROUP BY e.department.name")
    List<Object[]> countEmployeesByDepartment();

    @Query("SELECT e.status, COUNT(e) FROM Employee e GROUP BY e.status")
    List<Object[]> countEmployeesByStatusGroup();
}
