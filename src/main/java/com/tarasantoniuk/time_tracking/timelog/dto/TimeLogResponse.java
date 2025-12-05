package com.tarasantoniuk.time_tracking.timelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeLogResponse {

    private Long id;
    private Long employeeId;
    private LocalDateTime checkTime;
    private LocalDateTime createdAt;
}