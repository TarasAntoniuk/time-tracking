package com.tarasantoniuk.time_tracking.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.time_tracking.employee.dto.EmployeeRequest;
import com.tarasantoniuk.time_tracking.employee.entity.Employee;
import com.tarasantoniuk.time_tracking.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("EmployeeController Integration Tests")
class EmployeeControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/employees - Should create employee successfully")
    void shouldCreateEmployeeSuccessfully() throws Exception {
        // Given
        EmployeeRequest request = new EmployeeRequest("John", "Doe");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/employees - Should return 409 when employee already exists")
    void shouldReturn409WhenEmployeeAlreadyExists() throws Exception {
        // Given
        Employee existing = new Employee();
        existing.setFirstName("John");
        existing.setLastName("Doe");
        employeeRepository.save(existing);

        EmployeeRequest request = new EmployeeRequest("John", "Doe");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value(containsString("Employee already exists")));
    }

    @Test
    @DisplayName("POST /api/employees - Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Given - Empty first name and too short last name
        EmployeeRequest request = new EmployeeRequest("", "D");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("GET /api/employees/{id} - Should return employee by id")
    void shouldReturnEmployeeById() throws Exception {
        // Given
        Employee employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        Employee saved = employeeRepository.save(employee);

        // When & Then
        mockMvc.perform(get("/api/employees/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("GET /api/employees/{id} - Should return 404 when employee not found")
    void shouldReturn404WhenEmployeeNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/employees/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("Employee not found")));
    }

    @Test
    @DisplayName("GET /api/employees - Should return all employees")
    void shouldReturnAllEmployees() throws Exception {
        // Given
        Employee emp1 = new Employee();
        emp1.setFirstName("John");
        emp1.setLastName("Doe");

        Employee emp2 = new Employee();
        emp2.setFirstName("Jane");
        emp2.setLastName("Smith");

        employeeRepository.save(emp1);
        employeeRepository.save(emp2);

        // When & Then
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].firstName", containsInAnyOrder("John", "Jane")));
    }

    @Test
    @DisplayName("GET /api/employees?search=name - Should search employees by name")
    void shouldSearchEmployeesByName() throws Exception {
        // Given
        Employee emp1 = new Employee();
        emp1.setFirstName("John");
        emp1.setLastName("Doe");

        Employee emp2 = new Employee();
        emp2.setFirstName("Jane");
        emp2.setLastName("Smith");

        employeeRepository.save(emp1);
        employeeRepository.save(emp2);

        // When & Then
        mockMvc.perform(get("/api/employees").param("search", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    @DisplayName("PUT /api/employees/{id} - Should update employee successfully")
    void shouldUpdateEmployeeSuccessfully() throws Exception {
        // Given
        Employee employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        Employee saved = employeeRepository.save(employee);

        EmployeeRequest updateRequest = new EmployeeRequest("Johnny", "Doeson");

        // When & Then
        mockMvc.perform(put("/api/employees/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Doeson"));
    }

    @Test
    @DisplayName("PUT /api/employees/{id} - Should return 404 when updating non-existent employee")
    void shouldReturn404WhenUpdatingNonExistentEmployee() throws Exception {
        // Given
        EmployeeRequest updateRequest = new EmployeeRequest("Johnny", "Doeson");

        // When & Then
        mockMvc.perform(put("/api/employees/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} - Should delete employee successfully")
    void shouldDeleteEmployeeSuccessfully() throws Exception {
        // Given
        Employee employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        Employee saved = employeeRepository.save(employee);

        // When & Then
        mockMvc.perform(delete("/api/employees/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/employees/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} - Should return 404 when deleting non-existent employee")
    void shouldReturn404WhenDeletingNonExistentEmployee() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/employees/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/employees - Should return empty list when no employees exist")
    void shouldReturnEmptyListWhenNoEmployeesExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should handle special characters in employee names")
    void shouldHandleSpecialCharactersInNames() throws Exception {
        // Given
        EmployeeRequest request = new EmployeeRequest("Jean-Pierre", "O'Brien");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Jean-Pierre"))
                .andExpect(jsonPath("$.lastName").value("O'Brien"));
    }

    @Test
    @DisplayName("GET /api/employees - Should return all employees when search is null")
    void shouldReturnAllEmployeesWhenSearchIsNull() throws Exception {
        // Given
        Employee emp1 = new Employee();
        emp1.setFirstName("John");
        emp1.setLastName("Doe");

        Employee emp2 = new Employee();
        emp2.setFirstName("Jane");
        emp2.setLastName("Smith");

        employeeRepository.save(emp1);
        employeeRepository.save(emp2);

        // When & Then - Don't pass search parameter
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/employees?search= - Should return all employees when search is empty")
    void shouldReturnAllEmployeesWhenSearchIsEmpty() throws Exception {
        // Given
        Employee emp1 = new Employee();
        emp1.setFirstName("John");
        emp1.setLastName("Doe");

        employeeRepository.save(emp1);

        // When & Then - Empty search parameter
        mockMvc.perform(get("/api/employees").param("search", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/employees?search=   - Should return all employees when search is only spaces")
    void shouldReturnAllEmployeesWhenSearchIsOnlySpaces() throws Exception {
        // Given
        Employee emp1 = new Employee();
        emp1.setFirstName("John");
        emp1.setLastName("Doe");

        employeeRepository.save(emp1);

        // When & Then - Search with only spaces
        mockMvc.perform(get("/api/employees").param("search", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}