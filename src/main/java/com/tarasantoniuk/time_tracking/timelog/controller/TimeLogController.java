package com.tarasantoniuk.time_tracking.timelog.controller;

import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogRequest;
import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogResponse;
import com.tarasantoniuk.time_tracking.timelog.dto.TimesheetResponse;
import com.tarasantoniuk.time_tracking.timelog.service.TimeLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/time-logs")
@RequiredArgsConstructor
@Tag(name = "Time Logs", description = "API for managing employee check-in/check-out records")
public class TimeLogController {

    private final TimeLogService timeLogService;

    @Operation(
            summary = "Create time log",
            description = "Record an employee check-in or check-out at the entrance"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Time log created successfully",
                    content = @Content(schema = @Schema(implementation = TimeLogResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data"
            )
    })
    @PostMapping
    public ResponseEntity<TimeLogResponse> createTimeLog(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Time log data",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TimeLogRequest.class))
            )
            @Valid @RequestBody TimeLogRequest request
    ) {
        TimeLogResponse response = timeLogService.createTimeLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get employee timesheet",
            description = "Calculate worked hours per day for an employee within a time period. " +
                    "Returns daily breakdown with first entry, last exit, and hours worked each day."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Timesheet calculated successfully",
                    content = @Content(schema = @Schema(implementation = TimesheetResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid employee ID or date format"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found"
            )
    })
    @GetMapping("/timesheet")
    public ResponseEntity<TimesheetResponse> getTimesheet(
            @Parameter(
                    description = "Employee ID",
                    required = true,
                    example = "1"
            )
            @RequestParam("employee_id") Long employeeId,

            @Parameter(
                    description = "Start date and time (ISO 8601 format)",
                    required = true,
                    example = "2024-11-01T00:00:00"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(
                    description = "End date and time (ISO 8601 format)",
                    required = true,
                    example = "2024-11-30T23:59:59"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        TimesheetResponse response = timeLogService.calculateTimesheetSQL(employeeId, from, to);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get present employees",
            description = "Get list of employee IDs who were present in the building at a specific time. " +
                    "Logic: odd number of check-ins means employee is currently inside."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of present employees retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid time format"
            )
    })
    @GetMapping("/present")
    public ResponseEntity<Map<String, Object>> getPresentEmployees(
            @Parameter(
                    description = "Time to check presence (ISO 8601 format)",
                    required = true,
                    example = "2024-11-15T14:30:00"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime time
    ) {
        List<Long> employeeIds = timeLogService.getPresentEmployees(time);

        return ResponseEntity.ok(Map.of(
                "time", time,
                "employee_ids", employeeIds,
                "count", employeeIds.size()
        ));
    }

    @Operation(
            summary = "Get employee time logs",
            description = "Get all check-in and check-out records for an employee within a time period"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Time logs retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid employee ID or date format"
            )
    })
    @GetMapping
    public ResponseEntity<List<TimeLogResponse>> getEmployeeLogs(
            @Parameter(
                    description = "Employee ID",
                    required = true,
                    example = "1"
            )
            @RequestParam("employee_id") Long employeeId,

            @Parameter(
                    description = "Start date and time (ISO 8601 format)",
                    required = true,
                    example = "2024-11-01T00:00:00"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(
                    description = "End date and time (ISO 8601 format)",
                    required = true,
                    example = "2024-11-30T23:59:59"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        List<TimeLogResponse> logs = timeLogService.getEmployeeLogs(employeeId, from, to);
        return ResponseEntity.ok(logs);
    }
}