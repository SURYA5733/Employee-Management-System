package com.ems.util;

import com.ems.model.*;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(DepartmentRepository departmentRepository,
                           EmployeeRepository employeeRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // Upgrade legacy column in MySQL if it was created as a restricted ENUM
        try {
            jdbcTemplate.execute("ALTER TABLE user_roles MODIFY COLUMN role VARCHAR(50) NOT NULL");
        } catch (Exception ignored) {
            // Safe to ignore on H2 or if already VARCHAR
        }

        // Seed default departments & employees if empty
        if (departmentRepository.count() == 0) {
            seedDepartmentsAndEmployees();
        }

        // Ensure all default role accounts exist (Admin, HR, Manager, Employee)
        seedUsers();
    }

    private void seedDepartmentsAndEmployees() {
        Department engineering = new Department("Engineering", "ENG", "Software engineering, DevOps, and infrastructure", "Tech Wing - Floor 4");
        Department hr = new Department("Human Resources", "HR", "People operations, talent acquisition, and culture", "Main Building - Floor 2");
        Department finance = new Department("Finance", "FIN", "Corporate accounting, financial planning, and payroll", "Executive Tower - Floor 3");
        Department marketing = new Department("Marketing", "MKT", "Brand strategy, demand generation, and design", "Creative Hub - Floor 1");
        Department product = new Department("Product Management", "PRD", "Product strategy, roadmap, and user research", "Innovation Lab - Floor 4");

        List<Department> savedDepts = departmentRepository.saveAll(Arrays.asList(engineering, hr, finance, marketing, product));

        Department deptEng = savedDepts.get(0);
        Department deptHr = savedDepts.get(1);
        Department deptFin = savedDepts.get(2);
        Department deptMkt = savedDepts.get(3);
        Department deptPrd = savedDepts.get(4);

        List<Employee> employees = Arrays.asList(
                new Employee(null, "Alexander", "Wright", "alex.wright@company.com", "+1 (555) 234-5678",
                        "Principal Software Engineer", deptEng, new BigDecimal("135000.00"),
                        LocalDate.of(2021, 3, 15), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "742 Evergreen Terrace, Seattle, WA"),

                new Employee(null, "Sophia", "Chen", "sophia.chen@company.com", "+1 (555) 345-6789",
                        "Senior Backend Developer", deptEng, new BigDecimal("115000.00"),
                        LocalDate.of(2022, 6, 1), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "1204 Pine St, San Francisco, CA"),

                new Employee(null, "Liam", "Johnson", "liam.johnson@company.com", "+1 (555) 456-7890",
                        "Frontend Engineer", deptEng, new BigDecimal("95000.00"),
                        LocalDate.of(2023, 1, 10), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "582 Market Street, Austin, TX"),

                new Employee(null, "Emily", "Davis", "emily.davis@company.com", "+1 (555) 567-8901",
                        "DevOps Specialist", deptEng, new BigDecimal("108000.00"),
                        LocalDate.of(2022, 11, 20), EmploymentType.CONTRACT, EmployeeStatus.ACTIVE,
                        "330 Broadway, New York, NY"),

                new Employee(null, "Michael", "Brown", "michael.brown@company.com", "+1 (555) 678-9012",
                        "HR Director", deptHr, new BigDecimal("110000.00"),
                        LocalDate.of(2020, 5, 4), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "890 Oak Ave, Chicago, IL"),

                new Employee(null, "Olivia", "Martinez", "olivia.martinez@company.com", "+1 (555) 789-0123",
                        "Technical Recruiter", deptHr, new BigDecimal("72000.00"),
                        LocalDate.of(2023, 4, 17), EmploymentType.FULL_TIME, EmployeeStatus.ON_LEAVE,
                        "415 Elm St, Denver, CO"),

                new Employee(null, "Daniel", "Wilson", "daniel.wilson@company.com", "+1 (555) 890-1234",
                        "Finance Manager", deptFin, new BigDecimal("120000.00"),
                        LocalDate.of(2019, 8, 12), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "102 Wall St, Boston, MA"),

                new Employee(null, "Ava", "Taylor", "ava.taylor@company.com", "+1 (555) 901-2345",
                        "Senior Financial Analyst", deptFin, new BigDecimal("88000.00"),
                        LocalDate.of(2022, 2, 28), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "670 Lake Shore Dr, Chicago, IL"),

                new Employee(null, "Lucas", "Anderson", "lucas.anderson@company.com", "+1 (555) 012-3456",
                        "Marketing Director", deptMkt, new BigDecimal("118000.00"),
                        LocalDate.of(2021, 9, 1), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "204 Sunset Blvd, Los Angeles, CA"),

                new Employee(null, "Isabella", "Thomas", "isabella.thomas@company.com", "+1 (555) 123-4567",
                        "Content & SEO Strategist", deptMkt, new BigDecimal("68000.00"),
                        LocalDate.of(2023, 7, 5), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "15 Ocean Drive, Miami, FL"),

                new Employee(null, "Ethan", "Jackson", "ethan.jackson@company.com", "+1 (555) 234-9876",
                        "VP of Product", deptPrd, new BigDecimal("145000.00"),
                        LocalDate.of(2020, 1, 15), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "500 Grand Canal, San Jose, CA"),

                new Employee(null, "Mia", "White", "mia.white@company.com", "+1 (555) 345-8765",
                        "Senior Product Designer", deptPrd, new BigDecimal("98000.00"),
                        LocalDate.of(2022, 8, 15), EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        "220 King St, Portland, OR"),

                new Employee(null, "Noah", "Harris", "noah.harris@company.com", "+1 (555) 456-7654",
                        "Junior Software Developer", deptEng, new BigDecimal("65000.00"),
                        LocalDate.of(2024, 2, 1), EmploymentType.INTERN, EmployeeStatus.ACTIVE,
                        "88 University Way, Seattle, WA")
        );

        employeeRepository.saveAll(employees);
    }

    private void seedUsers() {
        // 1. System Admin
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User(
                    "admin",
                    "admin@company.com",
                    passwordEncoder.encode("admin123"),
                    new HashSet<>(Collections.singletonList(Role.ROLE_ADMIN))
            );
            userRepository.save(admin);
        }

        // 2. HR Specialist / Manager
        if (!userRepository.existsByUsername("hr")) {
            User hrUser = new User(
                    "hr",
                    "hr@company.com",
                    passwordEncoder.encode("hr123"),
                    new HashSet<>(Collections.singletonList(Role.ROLE_HR))
            );
            employeeRepository.findAll().stream()
                    .filter(e -> e.getJobTitle().toLowerCase().contains("hr") || e.getJobTitle().toLowerCase().contains("recruiter"))
                    .findFirst()
                    .ifPresent(hrUser::setEmployee);
            userRepository.save(hrUser);
        }

        // 3. Department Manager
        if (!userRepository.existsByUsername("manager")) {
            User manager = new User(
                    "manager",
                    "manager@company.com",
                    passwordEncoder.encode("manager123"),
                    new HashSet<>(Collections.singletonList(Role.ROLE_MANAGER))
            );
            userRepository.save(manager);
        }

        // 4. Regular Employee User
        if (!userRepository.existsByUsername("user")) {
            User employeeUser = new User(
                    "user",
                    "user@company.com",
                    passwordEncoder.encode("user123"),
                    new HashSet<>(Collections.singletonList(Role.ROLE_EMPLOYEE))
            );
            employeeRepository.findAll().stream().findFirst().ifPresent(employeeUser::setEmployee);
            userRepository.save(employeeUser);
        }
    }
}
