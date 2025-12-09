package com.tarasantoniuk.time_tracking.timelog.service;

import com.tarasantoniuk.time_tracking.employee.dto.EmployeeResponse;
import com.tarasantoniuk.time_tracking.employee.dto.PresentEmployeesResponse;
import com.tarasantoniuk.time_tracking.employee.repository.EmployeeRepository;
import com.tarasantoniuk.time_tracking.exception.ResourceNotFoundException;
import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogRequest;
import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogResponse;
import com.tarasantoniuk.time_tracking.timelog.dto.TimesheetResponse;
import com.tarasantoniuk.time_tracking.timelog.entity.TimeLog;
import com.tarasantoniuk.time_tracking.timelog.repository.TimeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeLogService Unit Tests")
class TimeLogServiceTest {

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private TimeLogService timeLogService;

    private TimeLog testTimeLog;
    private TimeLogRequest testRequest;
    private LocalDateTime testCheckTime;

    @BeforeEach
    void setUp() {
        testCheckTime = LocalDateTime.of(2024, 12, 8, 9, 0);

        testTimeLog = new TimeLog();
        testTimeLog.setId(1L);
        testTimeLog.setEmployeeId(1L);
        testTimeLog.setCheckTime(testCheckTime);
        testTimeLog.setCreatedAt(LocalDateTime.now());

        testRequest = new TimeLogRequest();
        testRequest.setEmployeeId(1L);
        testRequest.setCheckTime(testCheckTime);
    }

    @Test
    @DisplayName("Should create time log successfully when employee exists")
    void shouldCreateTimeLogSuccessfullyWhenEmployeeExists() {
        // Given
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(testTimeLog);

        // When
        TimeLogResponse response = timeLogService.createTimeLog(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmployeeId()).isEqualTo(1L);
        assertThat(response.getCheckTime()).isEqualTo(testCheckTime);

        verify(employeeRepository).existsById(1L);
        verify(timeLogRepository).save(any(TimeLog.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when employee does not exist")
    void shouldThrowExceptionWhenEmployeeDoesNotExist() {
        // Given
        when(employeeRepository.existsById(999L)).thenReturn(false);

        TimeLogRequest invalidRequest = new TimeLogRequest();
        invalidRequest.setEmployeeId(999L);
        invalidRequest.setCheckTime(testCheckTime);

        // When & Then
        assertThatThrownBy(() -> timeLogService.createTimeLog(invalidRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found")
                .hasMessageContaining("999");

        verify(employeeRepository).existsById(999L);
        verify(timeLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get employee logs for a period successfully")
    void shouldGetEmployeeLogsForPeriodSuccessfully() {
        // Given
        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        TimeLog log1 = new TimeLog();
        log1.setId(1L);
        log1.setEmployeeId(1L);
        log1.setCheckTime(LocalDateTime.of(2024, 12, 8, 9, 0));
        log1.setCreatedAt(LocalDateTime.now());

        TimeLog log2 = new TimeLog();
        log2.setId(2L);
        log2.setEmployeeId(1L);
        log2.setCheckTime(LocalDateTime.of(2024, 12, 8, 17, 0));
        log2.setCreatedAt(LocalDateTime.now());

        List<TimeLog> logs = Arrays.asList(log1, log2);
        when(timeLogRepository.findByEmployeeAndPeriod(1L, from, to))
                .thenReturn(logs);

        // When
        List<TimeLogResponse> responses = timeLogService.getEmployeeLogs(1L, from, to);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getEmployeeId()).isEqualTo(1L);
        assertThat(responses.get(1).getEmployeeId()).isEqualTo(1L);

        verify(timeLogRepository).findByEmployeeAndPeriod(1L, from, to);
    }

    @Test
    @DisplayName("Should return empty list when no logs exist for period")
    void shouldReturnEmptyListWhenNoLogsExistForPeriod() {
        // Given
        LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        when(timeLogRepository.findByEmployeeAndPeriod(1L, from, to))
                .thenReturn(Arrays.asList());

        // When
        List<TimeLogResponse> responses = timeLogService.getEmployeeLogs(1L, from, to);

        // Then
        assertThat(responses).isEmpty();

        verify(timeLogRepository).findByEmployeeAndPeriod(1L, from, to);
    }

    @Test
    @DisplayName("Should handle multiple check-ins for same employee on same day")
    void shouldHandleMultipleCheckInsForSameDay() {
        // Given
        when(employeeRepository.existsById(1L)).thenReturn(true);

        TimeLog morningLog = new TimeLog();
        morningLog.setId(1L);
        morningLog.setEmployeeId(1L);
        morningLog.setCheckTime(LocalDateTime.of(2024, 12, 8, 9, 0));

        TimeLog eveningLog = new TimeLog();
        eveningLog.setId(2L);
        eveningLog.setEmployeeId(1L);
        eveningLog.setCheckTime(LocalDateTime.of(2024, 12, 8, 17, 0));

        when(timeLogRepository.save(any(TimeLog.class)))
                .thenReturn(morningLog)
                .thenReturn(eveningLog);

        // When
        TimeLogRequest morningRequest = new TimeLogRequest();
        morningRequest.setEmployeeId(1L);
        morningRequest.setCheckTime(LocalDateTime.of(2024, 12, 8, 9, 0));

        TimeLogRequest eveningRequest = new TimeLogRequest();
        eveningRequest.setEmployeeId(1L);
        eveningRequest.setCheckTime(LocalDateTime.of(2024, 12, 8, 17, 0));

        TimeLogResponse response1 = timeLogService.createTimeLog(morningRequest);
        TimeLogResponse response2 = timeLogService.createTimeLog(eveningRequest);

        // Then
        assertThat(response1.getCheckTime().getHour()).isEqualTo(9);
        assertThat(response2.getCheckTime().getHour()).isEqualTo(17);

        verify(timeLogRepository, times(2)).save(any(TimeLog.class));
    }

    @Test
    @DisplayName("Should verify employee existence before saving time log")
    void shouldVerifyEmployeeExistenceBeforeSaving() {
        // Given
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(testTimeLog);

        // When
        timeLogService.createTimeLog(testRequest);

        // Then
        verify(employeeRepository).existsById(1L);
        verify(timeLogRepository).save(any(TimeLog.class));
    }

    @Test
    @DisplayName("Should not save time log if employee check fails")
    void shouldNotSaveTimeLogIfEmployeeCheckFails() {
        // Given
        when(employeeRepository.existsById(999L)).thenReturn(false);

        TimeLogRequest invalidRequest = new TimeLogRequest();
        invalidRequest.setEmployeeId(999L);
        invalidRequest.setCheckTime(testCheckTime);

        // When & Then
        assertThatThrownBy(() -> timeLogService.createTimeLog(invalidRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(timeLogRepository, never()).save(any(TimeLog.class));
    }

    @Nested
    @DisplayName("calculateTimesheetSQL Tests")
    class CalculateTimesheetSQLTests {

        @Test
        @DisplayName("Should calculate timesheet with multiple days")
        void shouldCalculateTimesheetWithMultipleDays() {
            // Given
            LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

            TimeLogRepository.DailyWorkProjection day1 = createProjection(
                    1L, "John", "Doe",
                    Date.valueOf(LocalDate.of(2024, 12, 8)),
                    Timestamp.valueOf(LocalDateTime.of(2024, 12, 8, 9, 0)),
                    Timestamp.valueOf(LocalDateTime.of(2024, 12, 8, 17, 0)),
                    "08:00", 2
            );

            TimeLogRepository.DailyWorkProjection day2 = createProjection(
                    1L, "John", "Doe",
                    Date.valueOf(LocalDate.of(2024, 12, 9)),
                    Timestamp.valueOf(LocalDateTime.of(2024, 12, 9, 9, 30)),
                    Timestamp.valueOf(LocalDateTime.of(2024, 12, 9, 18, 0)),
                    "08:30", 2
            );

            when(timeLogRepository.calculateTimesheetNative(1L, from, to))
                    .thenReturn(Arrays.asList(day1, day2));

            // When
            TimesheetResponse response = timeLogService.calculateTimesheetSQL(1L, from, to);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmployeeId()).isEqualTo(1L);
            assertThat(response.getFirstName()).isEqualTo("John");
            assertThat(response.getLastName()).isEqualTo("Doe");
            assertThat(response.getTotalHours()).isEqualTo("16:30"); // 8:00 + 8:30
            assertThat(response.getDailyRecords()).hasSize(2);
            assertThat(response.getDailyRecords().get(0).getHoursWorked()).isEqualTo("08:00");
            assertThat(response.getDailyRecords().get(1).getHoursWorked()).isEqualTo("08:30");

            verify(timeLogRepository).calculateTimesheetNative(1L, from, to);
        }

        @Test
        @DisplayName("Should return empty timesheet when no logs exist")
        void shouldReturnEmptyTimesheetWhenNoLogsExist() {
            // Given
            LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

            when(timeLogRepository.calculateTimesheetNative(1L, from, to))
                    .thenReturn(Collections.emptyList());

            // When
            TimesheetResponse response = timeLogService.calculateTimesheetSQL(1L, from, to);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmployeeId()).isEqualTo(1L);
            assertThat(response.getFirstName()).isNull();
            assertThat(response.getLastName()).isNull();
            assertThat(response.getTotalHours()).isEqualTo("0:00");
            assertThat(response.getDailyRecords()).isEmpty();
        }

        @Test
        @DisplayName("Should handle single day timesheet")
        void shouldHandleSingleDayTimesheet() {
            // Given
            LocalDateTime from = LocalDateTime.of(2024, 12, 8, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 12, 8, 23, 59);

            TimeLogRepository.DailyWorkProjection day = createProjection(
                    1L, "Jane", "Smith",
                    Date.valueOf(LocalDate.of(2024, 12, 8)),
                    Timestamp.valueOf(LocalDateTime.of(2024, 12, 8, 8, 0)),
                    Timestamp.valueOf(LocalDateTime.of(2024, 12, 8, 16, 30)),
                    "08:30", 2
            );

            when(timeLogRepository.calculateTimesheetNative(1L, from, to))
                    .thenReturn(Collections.singletonList(day));

            // When
            TimesheetResponse response = timeLogService.calculateTimesheetSQL(1L, from, to);

            // Then
            assertThat(response.getTotalHours()).isEqualTo("8:30");
            assertThat(response.getDailyRecords()).hasSize(1);
            assertThat(response.getDailyRecords().get(0).getDate()).isEqualTo(LocalDate.of(2024, 12, 8));
        }

        @Test
        @DisplayName("Should accumulate hours correctly over multiple days")
        void shouldAccumulateHoursCorrectlyOverMultipleDays() {
            // Given
            LocalDateTime from = LocalDateTime.of(2024, 12, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

            // 5 days of work: 8h + 7h30 + 9h + 8h30 + 10h = 43h
            List<TimeLogRepository.DailyWorkProjection> projections = Arrays.asList(
                    createProjection(1L, "John", "Doe", Date.valueOf(LocalDate.of(2024, 12, 2)), null, null, "08:00", 2),
                    createProjection(1L, "John", "Doe", Date.valueOf(LocalDate.of(2024, 12, 3)), null, null, "07:30", 2),
                    createProjection(1L, "John", "Doe", Date.valueOf(LocalDate.of(2024, 12, 4)), null, null, "09:00", 2),
                    createProjection(1L, "John", "Doe", Date.valueOf(LocalDate.of(2024, 12, 5)), null, null, "08:30", 2),
                    createProjection(1L, "John", "Doe", Date.valueOf(LocalDate.of(2024, 12, 6)), null, null, "10:00", 2)
            );

            when(timeLogRepository.calculateTimesheetNative(1L, from, to))
                    .thenReturn(projections);

            // When
            TimesheetResponse response = timeLogService.calculateTimesheetSQL(1L, from, to);

            // Then
            assertThat(response.getTotalHours()).isEqualTo("43:00");
            assertThat(response.getDailyRecords()).hasSize(5);
        }

        @Test
        @DisplayName("Should handle multiple check-ins per day")
        void shouldHandleMultipleCheckInsPerDay() {
            // Given
            LocalDateTime from = LocalDateTime.of(2024, 12, 8, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 12, 8, 23, 59);

            // 4 entries (2 pairs): 9:00-12:00 (3h) + 13:00-17:00 (4h) = 7h
            TimeLogRepository.DailyWorkProjection day = createProjection(
                    1L, "John", "Doe",
                    Date.valueOf(LocalDate.of(2024, 12, 8)),
                    Timestamp.valueOf(LocalDateTime.of(2024, 12, 8, 9, 0)),
                    Timestamp.valueOf(LocalDateTime.of(2024, 12, 8, 17, 0)),
                    "07:00", 4
            );

            when(timeLogRepository.calculateTimesheetNative(1L, from, to))
                    .thenReturn(Collections.singletonList(day));

            // When
            TimesheetResponse response = timeLogService.calculateTimesheetSQL(1L, from, to);

            // Then
            assertThat(response.getDailyRecords().get(0).getTotalEntries()).isEqualTo(4);
            assertThat(response.getDailyRecords().get(0).getHoursWorked()).isEqualTo("07:00");
        }

        @Test
        @DisplayName("Should handle null first entry and last exit")
        void shouldHandleNullFirstEntryAndLastExit() {
            // Given
            LocalDateTime from = LocalDateTime.of(2024, 12, 8, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 12, 8, 23, 59);

            TimeLogRepository.DailyWorkProjection day = createProjection(
                    1L, "John", "Doe",
                    Date.valueOf(LocalDate.of(2024, 12, 8)),
                    null, null, // null timestamps
                    "04:00", 2
            );

            when(timeLogRepository.calculateTimesheetNative(1L, from, to))
                    .thenReturn(Collections.singletonList(day));

            // When
            TimesheetResponse response = timeLogService.calculateTimesheetSQL(1L, from, to);

            // Then
            assertThat(response.getDailyRecords().get(0).getFirstEntry()).isNull();
            assertThat(response.getDailyRecords().get(0).getLastExit()).isNull();
        }

        @Test
        @DisplayName("Should handle empty hours worked string")
        void shouldHandleEmptyHoursWorkedString() {
            // Given
            LocalDateTime from = LocalDateTime.of(2024, 12, 8, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 12, 8, 23, 59);

            TimeLogRepository.DailyWorkProjection day = createProjection(
                    1L, "John", "Doe",
                    Date.valueOf(LocalDate.of(2024, 12, 8)),
                    null, null,
                    "", // empty hours string
                    0
            );

            when(timeLogRepository.calculateTimesheetNative(1L, from, to))
                    .thenReturn(Collections.singletonList(day));

            // When
            TimesheetResponse response = timeLogService.calculateTimesheetSQL(1L, from, to);

            // Then
            assertThat(response.getTotalHours()).isEqualTo("0:00");
        }

        @Test
        @DisplayName("Should handle null hours worked string")
        void shouldHandleNullHoursWorkedString() {
            // Given
            LocalDateTime from = LocalDateTime.of(2024, 12, 8, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 12, 8, 23, 59);

            TimeLogRepository.DailyWorkProjection day = createProjection(
                    1L, "John", "Doe",
                    Date.valueOf(LocalDate.of(2024, 12, 8)),
                    null, null,
                    null, // null hours string
                    0
            );

            when(timeLogRepository.calculateTimesheetNative(1L, from, to))
                    .thenReturn(Collections.singletonList(day));

            // When
            TimesheetResponse response = timeLogService.calculateTimesheetSQL(1L, from, to);

            // Then
            assertThat(response.getTotalHours()).isEqualTo("0:00");
        }

        private TimeLogRepository.DailyWorkProjection createProjection(
                Long employeeId, String firstName, String lastName,
                Date date, Timestamp firstEntry, Timestamp lastExit,
                String hoursWorked, Integer totalEntries) {

            return new TimeLogRepository.DailyWorkProjection() {
                @Override
                public Long getEmployeeId() { return employeeId; }
                @Override
                public String getFirstName() { return firstName; }
                @Override
                public String getLastName() { return lastName; }
                @Override
                public Date getDate() { return date; }
                @Override
                public Timestamp getFirstEntry() { return firstEntry; }
                @Override
                public Timestamp getLastExit() { return lastExit; }
                @Override
                public String getHoursWorked() { return hoursWorked; }
                @Override
                public Integer getTotalEntries() { return totalEntries; }
            };
        }
    }

    @Nested
    @DisplayName("getPresentEmployeesWithDetails Tests")
    class GetPresentEmployeesWithDetailsTests {

        @Test
        @DisplayName("Should return present employees with details")
        void shouldReturnPresentEmployeesWithDetails() {
            // Given
            LocalDateTime atTime = LocalDateTime.of(2024, 12, 8, 10, 0);

            TimeLogRepository.PresentEmployeeProjection emp1 = createPresentProjection(
                    1L, "John", "Doe", Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 10, 0))
            );
            TimeLogRepository.PresentEmployeeProjection emp2 = createPresentProjection(
                    2L, "Jane", "Smith", Timestamp.valueOf(LocalDateTime.of(2024, 2, 15, 14, 30))
            );

            when(timeLogRepository.findPresentEmployeesWithDetails(atTime))
                    .thenReturn(Arrays.asList(emp1, emp2));

            // When
            PresentEmployeesResponse response = timeLogService.getPresentEmployeesWithDetails(atTime);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTime()).isEqualTo(atTime);
            assertThat(response.getCount()).isEqualTo(2);
            assertThat(response.getEmployees()).hasSize(2);
            assertThat(response.getEmployees().get(0).getFirstName()).isEqualTo("John");
            assertThat(response.getEmployees().get(1).getFirstName()).isEqualTo("Jane");

            verify(timeLogRepository).findPresentEmployeesWithDetails(atTime);
        }

        @Test
        @DisplayName("Should return empty list when no employees present")
        void shouldReturnEmptyListWhenNoEmployeesPresent() {
            // Given
            LocalDateTime atTime = LocalDateTime.of(2024, 12, 8, 6, 0); // Early morning, no one present

            when(timeLogRepository.findPresentEmployeesWithDetails(atTime))
                    .thenReturn(Collections.emptyList());

            // When
            PresentEmployeesResponse response = timeLogService.getPresentEmployeesWithDetails(atTime);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTime()).isEqualTo(atTime);
            assertThat(response.getCount()).isEqualTo(0);
            assertThat(response.getEmployees()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null createdAt in projection")
        void shouldHandleNullCreatedAtInProjection() {
            // Given
            LocalDateTime atTime = LocalDateTime.of(2024, 12, 8, 10, 0);

            TimeLogRepository.PresentEmployeeProjection emp = createPresentProjection(
                    1L, "John", "Doe", null // null createdAt
            );

            when(timeLogRepository.findPresentEmployeesWithDetails(atTime))
                    .thenReturn(Collections.singletonList(emp));

            // When
            PresentEmployeesResponse response = timeLogService.getPresentEmployeesWithDetails(atTime);

            // Then
            assertThat(response.getEmployees()).hasSize(1);
            assertThat(response.getEmployees().get(0).getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("Should return single employee when only one present")
        void shouldReturnSingleEmployeeWhenOnlyOnePresent() {
            // Given
            LocalDateTime atTime = LocalDateTime.of(2024, 12, 8, 18, 0);

            TimeLogRepository.PresentEmployeeProjection emp = createPresentProjection(
                    1L, "John", "Doe", Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 10, 0))
            );

            when(timeLogRepository.findPresentEmployeesWithDetails(atTime))
                    .thenReturn(Collections.singletonList(emp));

            // When
            PresentEmployeesResponse response = timeLogService.getPresentEmployeesWithDetails(atTime);

            // Then
            assertThat(response.getCount()).isEqualTo(1);
            assertThat(response.getEmployees()).hasSize(1);
            assertThat(response.getEmployees().get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should map all employee fields correctly")
        void shouldMapAllEmployeeFieldsCorrectly() {
            // Given
            LocalDateTime atTime = LocalDateTime.of(2024, 12, 8, 10, 0);
            LocalDateTime createdAt = LocalDateTime.of(2024, 6, 15, 9, 30);

            TimeLogRepository.PresentEmployeeProjection emp = createPresentProjection(
                    42L, "Alice", "Wonder", Timestamp.valueOf(createdAt)
            );

            when(timeLogRepository.findPresentEmployeesWithDetails(atTime))
                    .thenReturn(Collections.singletonList(emp));

            // When
            PresentEmployeesResponse response = timeLogService.getPresentEmployeesWithDetails(atTime);

            // Then
            EmployeeResponse employee = response.getEmployees().get(0);
            assertThat(employee.getId()).isEqualTo(42L);
            assertThat(employee.getFirstName()).isEqualTo("Alice");
            assertThat(employee.getLastName()).isEqualTo("Wonder");
            assertThat(employee.getCreatedAt()).isEqualTo(createdAt);
        }

        private TimeLogRepository.PresentEmployeeProjection createPresentProjection(
                Long employeeId, String firstName, String lastName, Timestamp createdAt) {

            return new TimeLogRepository.PresentEmployeeProjection() {
                @Override
                public Long getEmployeeId() { return employeeId; }
                @Override
                public String getFirstName() { return firstName; }
                @Override
                public String getLastName() { return lastName; }
                @Override
                public Timestamp getCreatedAt() { return createdAt; }
            };
        }
    }
}