package com.tarasantoniuk.time_tracking.timelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyWorkResponse {

    private LocalDate date;
    private String hoursWorked;    // Changed from Double to String for HH:MM format (e.g. "08:30")
    private LocalTime firstEntry;  // First check-in time
    private LocalTime lastExit;    // Last check-out time (null if still working)
    private Integer totalEntries;  // Number of check-ins/outs
}