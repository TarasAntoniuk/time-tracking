package com.tarasantoniuk.time_tracking.timelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetResponse {

    private Long employeeId;
    private String firstName;      // Employee first name
    private String lastName;       // Employee last name
    private LocalDateTime from;
    private LocalDateTime to;
    private String totalHours;     // Changed from Double to String for HH:MM format (e.g. "184:30")
    private List<DailyWorkResponse> dailyRecords;
}