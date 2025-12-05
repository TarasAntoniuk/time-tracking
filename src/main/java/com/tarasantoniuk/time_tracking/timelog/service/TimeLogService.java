package com.tarasantoniuk.time_tracking.timelog.service;

import com.tarasantoniuk.time_tracking.timelog.dto.DailyWorkResponse;
import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogRequest;
import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogResponse;
import com.tarasantoniuk.time_tracking.timelog.dto.TimesheetResponse;
import com.tarasantoniuk.time_tracking.timelog.entity.TimeLog;
import com.tarasantoniuk.time_tracking.timelog.repository.TimeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;

    // Create a check-in record
    @Transactional
    public TimeLogResponse createTimeLog(TimeLogRequest request) {
        TimeLog timeLog = new TimeLog();
        timeLog.setEmployeeId(request.getEmployeeId());
        timeLog.setCheckTime(request.getCheckTime());

        TimeLog saved = timeLogRepository.save(timeLog);

        log.info("TimeLog created: employeeId={}, checkTime={}",
                saved.getEmployeeId(), saved.getCheckTime());

        return mapToResponse(saved);
    }

    // Calculate worked hours per day using SQL with HH:MM format
    public TimesheetResponse calculateTimesheetSQL(Long employeeId, LocalDateTime from, LocalDateTime to) {
        log.info("Calculating timesheet using SQL for employee {}", employeeId);

        List<TimeLogRepository.DailyWorkProjection> projections = timeLogRepository.calculateTimesheetNative(
                employeeId, from, to
        );

        List<DailyWorkResponse> dailyRecords = new ArrayList<>();
        int totalMinutes = 0;

        // Employee data from first projection (same for all days)
        String firstName = null;
        String lastName = null;

        for (TimeLogRepository.DailyWorkProjection proj : projections) {
            // Get employee name from first record
            if (firstName == null) {
                firstName = proj.getFirstName();
                lastName = proj.getLastName();
            }

            String hoursWorked = proj.getHoursWorked(); // Already in HH:MM format

            DailyWorkResponse daily = DailyWorkResponse.builder()
                    .date(proj.getDate().toLocalDate())
                    .hoursWorked(hoursWorked)
                    .firstEntry(proj.getFirstEntry() != null ? proj.getFirstEntry().toLocalDateTime().toLocalTime() : null)
                    .lastExit(proj.getLastExit() != null ? proj.getLastExit().toLocalDateTime().toLocalTime() : null)
                    .totalEntries(proj.getTotalEntries())
                    .build();

            dailyRecords.add(daily);

            // Sum up total minutes
            if (hoursWorked != null && !hoursWorked.isEmpty()) {
                String[] parts = hoursWorked.split(":");
                totalMinutes += Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            }
        }

        // Convert total minutes to HH:MM format
        int totalHours = totalMinutes / 60;
        int remainingMinutes = totalMinutes % 60;
        String totalHoursFormatted = String.format("%d:%02d", totalHours, remainingMinutes);

        log.info("Timesheet calculated (SQL) for employee {} ({}  {}): {} days, {} total hours",
                employeeId, firstName, lastName, dailyRecords.size(), totalHoursFormatted);

        return TimesheetResponse.builder()
                .employeeId(employeeId)
                .firstName(firstName)
                .lastName(lastName)
                .from(from)
                .to(to)
                .totalHours(totalHoursFormatted)
                .dailyRecords(dailyRecords)
                .build();
    }

    // Get list of employees who were present at a specific time
    public List<Long> getPresentEmployees(LocalDateTime atTime) {
        List<Long> presentIds = timeLogRepository.findPresentEmployees(atTime);
        log.info("Present employees at {}: {}", atTime, presentIds);
        return presentIds;
    }

    // Get all check-ins for an employee within a period
    public List<TimeLogResponse> getEmployeeLogs(Long employeeId, LocalDateTime from, LocalDateTime to) {
        List<TimeLog> logs = timeLogRepository.findByEmployeeAndPeriod(employeeId, from, to);
        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Map Entity to DTO
    private TimeLogResponse mapToResponse(TimeLog timeLog) {
        return TimeLogResponse.builder()
                .id(timeLog.getId())
                .employeeId(timeLog.getEmployeeId())
                .checkTime(timeLog.getCheckTime())
                .createdAt(timeLog.getCreatedAt())
                .build();
    }
}