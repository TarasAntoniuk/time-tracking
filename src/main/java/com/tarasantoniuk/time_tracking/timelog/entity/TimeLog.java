package com.tarasantoniuk.time_tracking.timelog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "time_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_check_time",
                        columnNames = {"employee_id", "check_time"}
                )
        },
        indexes = {
                @Index(name = "idx_employee_check_time", columnList = "employee_id, check_time")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "time_log_seq")
    @SequenceGenerator(name = "time_log_seq", sequenceName = "time_logs_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "check_time", nullable = false)
    private LocalDateTime checkTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}