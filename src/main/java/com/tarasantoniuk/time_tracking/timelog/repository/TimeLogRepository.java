package com.tarasantoniuk.time_tracking.timelog.repository;

import com.tarasantoniuk.time_tracking.timelog.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    // Original method - find all logs for employee in period
    @Query("SELECT t FROM TimeLog t WHERE t.employeeId = :employeeId " +
            "AND t.checkTime BETWEEN :from AND :to ORDER BY t.checkTime")
    List<TimeLog> findByEmployeeAndPeriod(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // SQL-based timesheet calculation with HH:MM format + Employee data
    @Query(value = """
        WITH ordered AS (
            SELECT
                tl.employee_id,
                tl.check_time AS event_time,
                ROW_NUMBER() OVER (
                    PARTITION BY tl.employee_id, DATE(tl.check_time)
                    ORDER BY tl.check_time
                ) AS rn
            FROM time_logs tl
            WHERE tl.employee_id = :employeeId 
              AND tl.check_time >= :from 
              AND tl.check_time < :to
        ),
        paired AS (
            SELECT
                employee_id,
                DATE(event_time) AS work_date,
                (rn + 1) / 2 AS pair_number,
                MAX(CASE WHEN rn % 2 = 1 THEN event_time END) AS time_in,
                MAX(CASE WHEN rn % 2 = 0 THEN event_time END) AS time_out,
                MAX(CASE WHEN rn % 2 = 0 THEN event_time END) -
                MAX(CASE WHEN rn % 2 = 1 THEN event_time END) AS duration_interval
            FROM ordered
            GROUP BY employee_id, DATE(event_time), (rn + 1) / 2
        ),
        daily_summary AS (
            SELECT
                employee_id,
                work_date,
                MIN(time_in) AS first_entry,
                MAX(time_out) AS last_exit,
                SUM(duration_interval) AS total_duration,
                COUNT(*) * 2 AS total_entries
            FROM paired
            WHERE time_in IS NOT NULL
            GROUP BY employee_id, work_date
        )
        SELECT 
            ds.employee_id,
            e.first_name,
            e.last_name,
            ds.work_date AS date,
            ds.first_entry,
            ds.last_exit,
            TO_CHAR(ds.total_duration, 'HH24:MI') AS hours_worked,
            ds.total_entries
        FROM daily_summary ds
        JOIN employees e ON e.id = ds.employee_id
        ORDER BY ds.work_date
        """, nativeQuery = true)
    List<DailyWorkProjection> calculateTimesheetNative(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // Find employees who were present at a specific time
    @Query(value = """
        SELECT DISTINCT employee_id 
        FROM (
            SELECT 
                employee_id,
                check_time,
                ROW_NUMBER() OVER (PARTITION BY employee_id ORDER BY check_time) as rn
            FROM time_logs
            WHERE check_time <= :atTime
        ) t
        WHERE rn % 2 = 1
        AND NOT EXISTS (
            SELECT 1 
            FROM time_logs t2 
            WHERE t2.employee_id = t.employee_id 
              AND t2.check_time > t.check_time 
              AND t2.check_time <= :atTime
            HAVING COUNT(*) % 2 = 1
        )
        """, nativeQuery = true)
    List<Long> findPresentEmployees(@Param("atTime") LocalDateTime atTime);

    // Interface for projection with employee data
    interface DailyWorkProjection {
        Long getEmployeeId();
        String getFirstName();
        String getLastName();
        java.sql.Date getDate();
        java.sql.Timestamp getFirstEntry();
        java.sql.Timestamp getLastExit();
        String getHoursWorked();  // HH:MM format
        Integer getTotalEntries();
    }
}