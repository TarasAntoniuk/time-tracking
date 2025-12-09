package com.tarasantoniuk.time_tracking.employee.service;

import com.tarasantoniuk.time_tracking.employee.dto.EmployeeRequest;
import com.tarasantoniuk.time_tracking.employee.dto.EmployeeResponse;
import com.tarasantoniuk.time_tracking.employee.entity.Employee;
import com.tarasantoniuk.time_tracking.employee.repository.EmployeeRepository;
import com.tarasantoniuk.time_tracking.exception.DuplicateResourceException;
import com.tarasantoniuk.time_tracking.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService Unit Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;
    private EmployeeRequest testRequest;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setCreatedAt(LocalDateTime.now());

        testRequest = new EmployeeRequest();
        testRequest.setFirstName("John");
        testRequest.setLastName("Doe");
    }

    @Test
    @DisplayName("Should create employee successfully")
    void shouldCreateEmployeeSuccessfully() {
        // Given
        when(employeeRepository.existsByFirstNameAndLastName(anyString(), anyString()))
                .thenReturn(false);
        when(employeeRepository.save(any(Employee.class)))
                .thenReturn(testEmployee);

        // When
        EmployeeResponse response = employeeService.createEmployee(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");

        verify(employeeRepository).existsByFirstNameAndLastName("John", "Doe");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when employee already exists")
    void shouldThrowExceptionWhenEmployeeAlreadyExists() {
        // Given
        when(employeeRepository.existsByFirstNameAndLastName(anyString(), anyString()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(testRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Employee already exists");

        verify(employeeRepository).existsByFirstNameAndLastName("John", "Doe");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get employee by id successfully")
    void shouldGetEmployeeByIdSuccessfully() {
        // Given
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));

        // When
        EmployeeResponse response = employeeService.getEmployeeById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");

        verify(employeeRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when employee not found")
    void shouldThrowExceptionWhenEmployeeNotFound() {
        // Given
        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.getEmployeeById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");

        verify(employeeRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get all employees successfully")
    void shouldGetAllEmployeesSuccessfully() {
        // Given
        Employee employee2 = new Employee();
        employee2.setId(2L);
        employee2.setFirstName("Jane");
        employee2.setLastName("Smith");
        employee2.setCreatedAt(LocalDateTime.now());

        List<Employee> employees = Arrays.asList(testEmployee, employee2);
        when(employeeRepository.findAllByOrderByLastNameAsc())
                .thenReturn(employees);

        // When
        List<EmployeeResponse> responses = employeeService.getAllEmployees();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getFirstName()).isEqualTo("John");
        assertThat(responses.get(1).getFirstName()).isEqualTo("Jane");

        verify(employeeRepository).findAllByOrderByLastNameAsc();
    }

    @Test
    @DisplayName("Should return empty list when no employees exist")
    void shouldReturnEmptyListWhenNoEmployees() {
        // Given
        when(employeeRepository.findAllByOrderByLastNameAsc())
                .thenReturn(Arrays.asList());

        // When
        List<EmployeeResponse> responses = employeeService.getAllEmployees();

        // Then
        assertThat(responses).isEmpty();

        verify(employeeRepository).findAllByOrderByLastNameAsc();
    }

    @Test
    @DisplayName("Should search employees by name successfully")
    void shouldSearchEmployeesByNameSuccessfully() {
        // Given
        when(employeeRepository.searchByName("John"))
                .thenReturn(Arrays.asList(testEmployee));

        // When
        List<EmployeeResponse> responses = employeeService.searchEmployees("John");

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFirstName()).isEqualTo("John");

        verify(employeeRepository).searchByName("John");
    }

    @Test
    @DisplayName("Should update employee successfully")
    void shouldUpdateEmployeeSuccessfully() {
        // Given
        EmployeeRequest updateRequest = new EmployeeRequest();
        updateRequest.setFirstName("Johnny");
        updateRequest.setLastName("Doe");

        Employee updatedEmployee = new Employee();
        updatedEmployee.setId(1L);
        updatedEmployee.setFirstName("Johnny");
        updatedEmployee.setLastName("Doe");
        updatedEmployee.setCreatedAt(testEmployee.getCreatedAt());

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class)))
                .thenReturn(updatedEmployee);

        // When
        EmployeeResponse response = employeeService.updateEmployee(1L, updateRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("Johnny");
        assertThat(response.getLastName()).isEqualTo("Doe");

        verify(employeeRepository).findById(1L);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent employee")
    void shouldThrowExceptionWhenUpdatingNonExistentEmployee() {
        // Given
        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.updateEmployee(999L, testRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");

        verify(employeeRepository).findById(999L);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete employee successfully")
    void shouldDeleteEmployeeSuccessfully() {
        // Given
        when(employeeRepository.existsById(1L))
                .thenReturn(true);
        doNothing().when(employeeRepository).deleteById(1L);

        // When
        employeeService.deleteEmployee(1L);

        // Then
        verify(employeeRepository).existsById(1L);
        verify(employeeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent employee")
    void shouldThrowExceptionWhenDeletingNonExistentEmployee() {
        // Given
        when(employeeRepository.existsById(999L))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> employeeService.deleteEmployee(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");

        verify(employeeRepository).existsById(999L);
        verify(employeeRepository, never()).deleteById(any());
    }
}