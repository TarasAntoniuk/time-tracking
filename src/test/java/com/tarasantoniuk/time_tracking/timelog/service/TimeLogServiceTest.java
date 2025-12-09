package com.tarasantoniuk.time_tracking.timelog.service;

import com.tarasantoniuk.time_tracking.employee.repository.EmployeeRepository;
import com.tarasantoniuk.time_tracking.exception.ResourceNotFoundException;
import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogRequest;
import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogResponse;
import com.tarasantoniuk.time_tracking.timelog.entity.TimeLog;
import com.tarasantoniuk.time_tracking.timelog.repository.TimeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
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
}