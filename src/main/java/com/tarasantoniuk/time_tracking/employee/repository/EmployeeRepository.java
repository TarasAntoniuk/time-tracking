package com.tarasantoniuk.time_tracking.employee.repository;

import com.tarasantoniuk.time_tracking.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Find employee by first name and last name
    Optional<Employee> findByFirstNameAndLastName(String firstName, String lastName);

    // Search employees by name (case-insensitive)
    @Query("SELECT e FROM Employee e WHERE " +
            "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Employee> searchByName(@Param("search") String search);

    // Get all employees ordered by last name
    List<Employee> findAllByOrderByLastNameAsc();

    // Check if employee exists by name
    boolean existsByFirstNameAndLastName(String firstName, String lastName);
}