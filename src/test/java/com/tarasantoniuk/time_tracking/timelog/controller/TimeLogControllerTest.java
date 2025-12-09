package com.tarasantoniuk.time_tracking.timelog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.time_tracking.employee.entity.Employee;
import com.tarasantoniuk.time_tracking.employee.repository.EmployeeRepository;
import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogRequest;
import com.tarasantoniuk.time_tracking.timelog.entity.TimeLog;
import com.tarasantoniuk.time_tracking.timelog.repository.TimeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("TimeLogController Integration Tests")
class TimeLogControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TimeLogRepository timeLogRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Long employeeId;

    @BeforeEach
    void setUp() {
        timeLogRepository.deleteAll();
        employeeRepository.deleteAll();

        Employee employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employeeId = employeeRepository.save(employee).getId();
    }

    @Test
    @DisplayName("POST /api/time-logs - Should create time log successfully")
    void shouldCreateTimeLogSuccessfully() throws Exception {
        // Given
        TimeLogRequest request = new TimeLogRequest(
                employeeId,
                LocalDateTime.of(2024, 12, 8, 9, 0)
        );

        // When & Then
        mockMvc.perform(post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.checkTime").value("2024-12-08T09:00:00"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/time-logs - Should return 404 when employee not found")
    void shouldReturn404WhenEmployeeNotFound() throws Exception {
        // Given
        TimeLogRequest request = new TimeLogRequest(
                999L,
                LocalDateTime.of(2024, 12, 8, 9, 0)
        );

        // When & Then
        mockMvc.perform(post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("Employee not found")));
    }

    @Test
    @DisplayName("POST /api/time-logs - Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Given - Missing employeeId
        TimeLogRequest request = new TimeLogRequest(null, LocalDateTime.now());

        // When & Then
        mockMvc.perform(post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/time-logs - Should get employee logs for period")
    void shouldGetEmployeeLogsForPeriod() throws Exception {
        // Given
        TimeLog log1 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0));
        TimeLog log2 = createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 17, 0));
        timeLogRepository.save(log1);
        timeLogRepository.save(log2);

        // When & Then
        mockMvc.perform(get("/api/time-logs")
                        .param("employee_id", employeeId.toString())
                        .param("from", "2024-12-01T00:00:00")
                        .param("to", "2024-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].employeeId").value(employeeId))
                .andExpect(jsonPath("$[1].employeeId").value(employeeId));
    }

    @Test
    @DisplayName("GET /api/time-logs - Should return empty list when no logs in period")
    void shouldReturnEmptyListWhenNoLogsInPeriod() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/time-logs")
                        .param("employee_id", employeeId.toString())
                        .param("from", "2024-12-01T00:00:00")
                        .param("to", "2024-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/time-logs/timesheet - Should calculate timesheet")
    void shouldCalculateTimesheet() throws Exception {
        // Given - Create paired check-ins and check-outs
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 17, 0)));
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 9, 9, 0)));
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 9, 17, 0)));

        // When & Then
        mockMvc.perform(get("/api/time-logs/timesheet")
                        .param("employee_id", employeeId.toString())
                        .param("from", "2024-12-01T00:00:00")
                        .param("to", "2024-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.totalHours").exists())
                .andExpect(jsonPath("$.dailyRecords", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/time-logs/present - Should get present employees")
    void shouldGetPresentEmployees() throws Exception {
        // Given - Employee checked in at 9:00, not checked out
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));

        // When & Then - Check at 10:00 (employee should be present)
        mockMvc.perform(get("/api/time-logs/present")
                        .param("time", "2024-12-08T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time").value("2024-12-08T10:00:00"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.employees", hasSize(1)))
                .andExpect(jsonPath("$.employees[0].id").value(employeeId))
                .andExpect(jsonPath("$.employees[0].firstName").value("John"))
                .andExpect(jsonPath("$.employees[0].lastName").value("Doe"));
    }

    @Test
    @DisplayName("GET /api/time-logs/present - Should return empty when no one present")
    void shouldReturnEmptyWhenNoOnePresent() throws Exception {
        // Given - Employee checked in and checked out
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 17, 0)));

        // When & Then - Check at 18:00 (employee should NOT be present)
        mockMvc.perform(get("/api/time-logs/present")
                        .param("time", "2024-12-08T18:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.employees", hasSize(0)));
    }

    @Test
    @DisplayName("Should handle multiple check-ins and check-outs on same day")
    void shouldHandleMultipleCheckInsAndCheckOuts() throws Exception {
        // Given - Multiple entries
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 12, 0)));
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 13, 0)));
        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 17, 0)));

        // When & Then
        mockMvc.perform(get("/api/time-logs")
                        .param("employee_id", employeeId.toString())
                        .param("from", "2024-12-08T00:00:00")
                        .param("to", "2024-12-08T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @DisplayName("Should handle midnight check time")
    void shouldHandleMidnightCheckTime() throws Exception {
        // Given
        TimeLogRequest request = new TimeLogRequest(
                employeeId,
                LocalDateTime.of(2024, 12, 8, 0, 0)
        );

        // When & Then
        mockMvc.perform(post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkTime").value("2024-12-08T00:00:00"));
    }

    @Test
    @DisplayName("Should handle multiple employees separately")
    void shouldHandleMultipleEmployeesSeparately() throws Exception {
        // Given
        Employee employee2 = new Employee();
        employee2.setFirstName("Jane");
        employee2.setLastName("Smith");
        Long employee2Id = employeeRepository.save(employee2).getId();

        timeLogRepository.save(createTimeLog(employeeId, LocalDateTime.of(2024, 12, 8, 9, 0)));
        timeLogRepository.save(createTimeLog(employee2Id, LocalDateTime.of(2024, 12, 8, 9, 0)));

        // When & Then - Both should be present
        mockMvc.perform(get("/api/time-logs/present")
                        .param("time", "2024-12-08T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.employees", hasSize(2)));
    }

    // Helper method
    private TimeLog createTimeLog(Long empId, LocalDateTime checkTime) {
        TimeLog log = new TimeLog();
        log.setEmployeeId(empId);
        log.setCheckTime(checkTime);
        return log;
    }
}