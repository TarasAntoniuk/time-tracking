package com.tarasantoniuk.time_tracking.employee.dto;

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
public class PresentEmployeesResponse {
    private LocalDateTime time;
    private Integer count;
    private List<EmployeeResponse> employees;
}