package com.tarasantoniuk.time_tracking.employee.service;

import com.tarasantoniuk.time_tracking.employee.dto.EmployeeRequest;
import com.tarasantoniuk.time_tracking.employee.dto.EmployeeResponse;
import com.tarasantoniuk.time_tracking.employee.entity.Employee;
import com.tarasantoniuk.time_tracking.employee.repository.EmployeeRepository;
import com.tarasantoniuk.time_tracking.exception.DuplicateResourceException;
import com.tarasantoniuk.time_tracking.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    // Create new employee
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        // Check if employee already exists
        if (employeeRepository.existsByFirstNameAndLastName(
                request.getFirstName(), request.getLastName())) {
            throw new DuplicateResourceException(
                    "Employee",
                    "name",
                    request.getFirstName() + " " + request.getLastName()
            );
        }

        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());

        Employee saved = employeeRepository.save(employee);

        log.info("Employee created: id={}, name={} {}",
                saved.getId(), saved.getFirstName(), saved.getLastName());

        return mapToResponse(saved);
    }

    // Get employee by ID
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        return mapToResponse(employee);
    }

    // Get all employees
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAllByOrderByLastNameAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Search employees by name
    public List<EmployeeResponse> searchEmployees(String searchTerm) {
        return employeeRepository.searchByName(searchTerm)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Update employee
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());

        Employee updated = employeeRepository.save(employee);

        log.info("Employee updated: id={}, name={} {}",
                updated.getId(), updated.getFirstName(), updated.getLastName());

        return mapToResponse(updated);
    }

    // Delete employee
    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee", id);
        }

        employeeRepository.deleteById(id);
        log.info("Employee deleted: id={}", id);
    }

    // Map Entity to DTO
    private EmployeeResponse mapToResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .createdAt(employee.getCreatedAt())
                .build();
    }
}