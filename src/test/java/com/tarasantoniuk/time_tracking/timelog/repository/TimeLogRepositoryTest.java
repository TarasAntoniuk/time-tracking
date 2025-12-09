package com.tarasantoniuk.time_tracking.timelog.repository;

import com.tarasantoniuk.time_tracking.employee.entity.Employee;
import com.tarasantoniuk.time_tracking.employee.repository.EmployeeRepository;
import com.tarasantoniuk.time_tracking.timelog.entity.TimeLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("TimeLogRepository Integration Tests")
class TimeLogRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TimeLogRepository timeLogRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Long employeeId;

    @BeforeEach
    void setUp() {
        timeLogRepository.deleteAll();
        employeeRepository.deleteAll();

        // Create test employee
        Employee employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employeeId = employeeRepository.save(employee).getId();
    }

    @Test
    @DisplayName("Should save and find time log by id")
    void shouldSaveAndFindTimeLogById() {
        // Given
        TimeLog timeLog = new TimeLog();
        timeLog.setEmployeeId(employeeId);
        timeLog.setCheckTime(LocalDateTime.of(2024, 12, 8, 9, 0));

        // When
        TimeLog saved = timeLogRepository.save(timeLog);
        TimeLog found = timeLogRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getEmployeeId()).isEqualTo(employeeId);
        assertThat(found.getCheckTime()).isEqualTo(LocalDateTime.of(2024, 12, 8, 9, 0));
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find time logs for employee in period")
    void shouldFindTimeLogsForEmployeeInPeriod() {
        // Given
        TimeLog log1 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 1, 9, 0));
        TimeLog log2 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 5, 9, 0));
        TimeLog log3 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 10, 9, 0));
        TimeLog log4 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 15, 9, 0));

        timeLogRepository.saveAll(List.of(log1, log2, log3, log4));

        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 10, 23, 59);

        // When
        List<TimeLog> results = timeLogRepository.findByEmployeeAndPeriod(employeeId, from, to);

        // Then
        assertThat(results).hasSize(3); // log1, log2, log3
        assertThat(results).extracting(TimeLog::getCheckTime)
                .containsExactly(
                        LocalDateTime.of(2024, 12, 1, 9, 0),
                        LocalDateTime.of(2024, 12, 5, 9, 0),
                        LocalDateTime.of(2024, 12, 10, 9, 0)
                );
    }

    @Test
    @DisplayName("Should return empty list when no logs in period")
    void shouldReturnEmptyListWhenNoLogsInPeriod() {
        // Given
        TimeLog log = createTimeLog(employeeId, LocalDateTime.of(2024, 11, 15, 9, 0));
        timeLogRepository.save(log);

        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        // When
        List<TimeLog> results = timeLogRepository.findByEmployeeAndPeriod(employeeId, from, to);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should order time logs by check time")
    void shouldOrderTimeLogsByCheckTime() {
        // Given
        TimeLog log1 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 17, 0));
        TimeLog log2 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0));
        TimeLog log3 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 12, 0));

        timeLogRepository.saveAll(List.of(log1, log2, log3));

        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        // When
        List<TimeLog> results = timeLogRepository.findByEmployeeAndPeriod(employeeId, from, to);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(TimeLog::getCheckTime)
                .containsExactly(
                        LocalDateTime.of(2024, 12, 8, 9, 0),
                        LocalDateTime.of(2024, 12, 8, 12, 0),
                        LocalDateTime.of(2024, 12, 8, 17, 0)
                );
    }

    @Test
    @DisplayName("Should handle multiple employees separately")
    void shouldHandleMultipleEmployeesSeparately() {
        // Given
        Employee employee2 = new Employee();
        employee2.setFirstName("Jane");
        employee2.setLastName("Smith");
        Long employee2Id = employeeRepository.save(employee2).getId();

        TimeLog log1 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0));
        TimeLog log2 = createTimeLog(employee2Id, LocalDateTime.of(2024, 12, 8, 9, 0));

        timeLogRepository.saveAll(List.of(log1, log2));

        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        // When
        List<TimeLog> employee1Logs = timeLogRepository.findByEmployeeAndPeriod(employeeId, from, to);
        List<TimeLog> employee2Logs = timeLogRepository.findByEmployeeAndPeriod(employee2Id, from, to);

        // Then
        assertThat(employee1Logs).hasSize(1);
        assertThat(employee2Logs).hasSize(1);
        assertThat(employee1Logs.get(0).getEmployeeId()).isEqualTo(employeeId);
        assertThat(employee2Logs.get(0).getEmployeeId()).isEqualTo(employee2Id);
    }

    @Test
    @DisplayName("Should calculate timesheet with native query")
    void shouldCalculateTimesheetWithNativeQuery() {
        // Given - Create paired check-ins/check-outs
        timeLogRepository.saveAll(List.of(
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)),  // Check-in
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 17, 0)), // Check-out
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 9, 9, 0)),  // Check-in
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 9, 17, 0))  // Check-out
        ));

        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        // When
        List<TimeLogRepository.DailyWorkProjection> results =
                timeLogRepository.calculateTimesheetNative(employeeId, from, to);

        // Then
        assertThat(results).hasSize(2); // Two days
        assertThat(results.get(0).getFirstName()).isEqualTo("John");
        assertThat(results.get(0).getLastName()).isEqualTo("Doe");
        assertThat(results.get(0).getHoursWorked()).isEqualTo("08:00");
        assertThat(results.get(0).getTotalEntries()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find present employees at specific time")
    void shouldFindPresentEmployeesAtSpecificTime() {
        // Given
        Employee employee2 = new Employee();
        employee2.setFirstName("Jane");
        employee2.setLastName("Smith");
        Long employee2Id = employeeRepository.save(employee2).getId();

        // Employee 1: checked in at 9:00, not checked out yet
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));

        // Employee 2: checked in at 9:00, checked out at 17:00
        timeLogRepository.saveAll(List.of(
                createTimeLog(employee2Id, LocalDateTime.of(2024, 12, 8, 9, 0)),
                createTimeLog(employee2Id, LocalDateTime.of(2024, 12, 8, 17, 0))
        ));

        // When - Check at 10:00 (employee1 present, employee2 present)
        List<TimeLogRepository.PresentEmployeeProjection> presentAt10 =
                timeLogRepository.findPresentEmployeesWithDetails(LocalDateTime.of(2024, 12, 8, 10, 0));

        // When - Check at 18:00 (employee1 present, employee2 NOT present)
        List<TimeLogRepository.PresentEmployeeProjection> presentAt18 =
                timeLogRepository.findPresentEmployeesWithDetails(LocalDateTime.of(2024, 12, 8, 18, 0));

        // Then
        assertThat(presentAt10).hasSize(2); // Both present at 10:00
        assertThat(presentAt18).hasSize(1); // Only employee1 present at 18:00
        assertThat(presentAt18.get(0).getEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("Should handle midnight check times")
    void shouldHandleMidnightCheckTimes() {
        // Given
        TimeLog midnight = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 0, 0));
        timeLogRepository.save(midnight);

        LocalDateTime from = LocalDateTime.of(2024, 12, 8, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 8, 23, 59);

        // When
        List<TimeLog> results = timeLogRepository.findByEmployeeAndPeriod(employeeId, from, to);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCheckTime().getHour()).isEqualTo(0);
        assertThat(results.get(0).getCheckTime().getMinute()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should auto-generate ID on save")
    void shouldAutoGenerateIdOnSave() {
        // Given
        TimeLog timeLog = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0));

        // When
        TimeLog saved = timeLogRepository.save(timeLog);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Should set createdAt timestamp automatically")
    void shouldSetCreatedAtTimestampAutomatically() {
        // Given
        TimeLog timeLog = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0));

        // When
        TimeLog saved = timeLogRepository.save(timeLog);

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete time log by id")
    void shouldDeleteTimeLogById() {
        // Given
        TimeLog saved = timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));
        Long id = saved.getId();

        // When
        timeLogRepository.deleteById(id);

        // Then
        assertThat(timeLogRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("Should calculate timesheet with multiple check-ins and check-outs on same day")
    void shouldCalculateTimesheetWithMultipleCheckInsAndCheckOutsOnSameDay() {
        // Given - 2 work sessions on the same day: 9:00-12:00 and 13:00-17:00
        timeLogRepository.saveAll(List.of(
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)),   // Check-in
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 12, 0)),  // Check-out
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 13, 0)),  // Check-in
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 17, 0))   // Check-out
        ));

        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        // When
        List<TimeLogRepository.DailyWorkProjection> results =
                timeLogRepository.calculateTimesheetNative(employeeId, from, to);

        // Then
        assertThat(results).hasSize(1); // One day
        assertThat(results.get(0).getHoursWorked()).isEqualTo("07:00"); // 3h + 4h = 7h
        assertThat(results.get(0).getTotalEntries()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should return empty timesheet for employee with no logs in period")
    void shouldReturnEmptyTimesheetForEmployeeWithNoLogsInPeriod() {
        // Given - log outside the query period
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 11, 15, 9, 0)));

        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        // When
        List<TimeLogRepository.DailyWorkProjection> results =
                timeLogRepository.calculateTimesheetNative(employeeId, from, to);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle timesheet boundary dates correctly")
    void shouldHandleTimesheetBoundaryDatesCorrectly() {
        // Given - logs exactly at the boundary
        timeLogRepository.saveAll(List.of(
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 1, 0, 0)),   // Exactly at from
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 1, 8, 0)),   // 8 hours later
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 31, 9, 0)),  // Near the end
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 31, 17, 0))  // 8 hours later
        ));

        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 1, 1, 0, 0);  // Exclusive end

        // When
        List<TimeLogRepository.DailyWorkProjection> results =
                timeLogRepository.calculateTimesheetNative(employeeId, from, to);

        // Then
        assertThat(results).hasSize(2); // Two days
    }

    @Test
    @DisplayName("Should handle odd number of check-ins (missing check-out)")
    void shouldHandleOddNumberOfCheckIns() {
        // Given - only check-in, no check-out (still working)
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));

        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        // When
        List<TimeLogRepository.DailyWorkProjection> results =
                timeLogRepository.calculateTimesheetNative(employeeId, from, to);

        // Then - unpaired check-in returns a record but with null hoursWorked
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getHoursWorked()).isNull();
        assertThat(results.get(0).getFirstEntry()).isNotNull();
        assertThat(results.get(0).getLastExit()).isNull();
    }

    @Test
    @DisplayName("Should return empty present employees when no one checked in")
    void shouldReturnEmptyPresentEmployeesWhenNoOneCheckedIn() {
        // Given - no time logs at all

        // When
        List<TimeLogRepository.PresentEmployeeProjection> results =
                timeLogRepository.findPresentEmployeesWithDetails(LocalDateTime.of(2024, 12, 8, 10, 0));

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should not include employee who checked out before query time")
    void shouldNotIncludeEmployeeWhoCheckedOutBeforeQueryTime() {
        // Given - employee checked in at 9:00, checked out at 12:00
        timeLogRepository.saveAll(List.of(
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)),  // Check-in
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 12, 0))  // Check-out
        ));

        // When - query at 14:00 (after check-out)
        List<TimeLogRepository.PresentEmployeeProjection> results =
                timeLogRepository.findPresentEmployeesWithDetails(LocalDateTime.of(2024, 12, 8, 14, 0));

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should include employee who is currently checked in")
    void shouldIncludeEmployeeWhoIsCurrentlyCheckedIn() {
        // Given - employee checked in at 9:00, no check-out yet
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));

        // When - query at 14:00 (still working)
        List<TimeLogRepository.PresentEmployeeProjection> results =
                timeLogRepository.findPresentEmployeesWithDetails(LocalDateTime.of(2024, 12, 8, 14, 0));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("Should handle employee with multiple check-in/check-out cycles")
    void shouldHandleEmployeeWithMultipleCheckInCheckOutCycles() {
        // Given - morning session: 9:00-12:00, afternoon session: 13:00-?
        timeLogRepository.saveAll(List.of(
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)),   // Check-in
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 12, 0)),  // Check-out
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 13, 0))   // Check-in (back from lunch)
        ));

        // When - query at 14:00 (during afternoon session)
        List<TimeLogRepository.PresentEmployeeProjection> results =
                timeLogRepository.findPresentEmployeesWithDetails(LocalDateTime.of(2024, 12, 8, 14, 0));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("Should not include employee when query time is before first check-in")
    void shouldNotIncludeEmployeeWhenQueryTimeIsBeforeFirstCheckIn() {
        // Given - employee checks in at 9:00
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));

        // When - query at 8:00 (before check-in)
        List<TimeLogRepository.PresentEmployeeProjection> results =
                timeLogRepository.findPresentEmployeesWithDetails(LocalDateTime.of(2024, 12, 8, 8, 0));

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle cross-day work correctly for present employees")
    void shouldHandleCrossDayWorkCorrectlyForPresentEmployees() {
        // Given - employee checked in yesterday and hasn't checked out
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 7, 22, 0))); // Night shift

        // When - query the next morning
        List<TimeLogRepository.PresentEmployeeProjection> results =
                timeLogRepository.findPresentEmployeesWithDetails(LocalDateTime.of(2024, 12, 8, 6, 0));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("Should return projection with all employee fields populated")
    void shouldReturnProjectionWithAllEmployeeFieldsPopulated() {
        // Given
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));

        // When
        List<TimeLogRepository.PresentEmployeeProjection> results =
                timeLogRepository.findPresentEmployeesWithDetails(LocalDateTime.of(2024, 12, 8, 10, 0));

        // Then
        assertThat(results).hasSize(1);
        TimeLogRepository.PresentEmployeeProjection projection = results.get(0);
        assertThat(projection.getEmployeeId()).isEqualTo(employeeId);
        assertThat(projection.getFirstName()).isEqualTo("John");
        assertThat(projection.getLastName()).isEqualTo("Doe");
        assertThat(projection.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate timesheet with exact period boundaries including start time")
    void shouldCalculateTimesheetWithExactPeriodBoundariesIncludingStartTime() {
        // Given - log exactly at the from boundary
        timeLogRepository.saveAll(List.of(
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0, 0)),
                createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 17, 0, 0))
        ));

        LocalDateTime from = LocalDateTime.of(2024, 12, 8, 9, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 8, 23, 59, 59);

        // When
        List<TimeLogRepository.DailyWorkProjection> results =
                timeLogRepository.calculateTimesheetNative(employeeId, from, to);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getHoursWorked()).isEqualTo("08:00");
    }

    // Helper method
    private TimeLog createTimeLog(Long empId, LocalDateTime checkTime) {
        TimeLog log = new TimeLog();
        log.setEmployeeId(empId);
        log.setCheckTime(checkTime);
        return log;
    }
}