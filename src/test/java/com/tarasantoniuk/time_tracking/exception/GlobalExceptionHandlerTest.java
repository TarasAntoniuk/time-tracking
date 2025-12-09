package com.tarasantoniuk.time_tracking.exception;

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
@DisplayName("GlobalExceptionHandler Integration Tests")
class GlobalExceptionHandlerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
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
    @DisplayName("Should handle ResourceNotFoundException with 404 status")
    void shouldHandleResourceNotFoundException() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/employees/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("Employee not found")))
                .andExpect(jsonPath("$.path").value("/api/employees/999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle DuplicateResourceException with 409 status")
    void shouldHandleDuplicateResourceException() throws Exception {
        // Given - Create employee first
        Employee employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employeeRepository.save(employee);

        // When & Then - Try to create duplicate
        EmployeeRequest request = new EmployeeRequest("John", "Doe");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value(containsString("Employee already exists")))
                .andExpect(jsonPath("$.path").value("/api/employees"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with 400 status and validation errors")
    void shouldHandleValidationException() throws Exception {
        // Given - Invalid request (empty firstName, short lastName)
        EmployeeRequest request = new EmployeeRequest("", "D");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.path").value("/api/employees"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.validationErrors[*].field").exists())
                .andExpect(jsonPath("$.validationErrors[*].message").exists());
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException with 400 status")
    void shouldHandleTypeMismatchException() throws Exception {
        // When & Then - Pass string instead of Long for ID
        mockMvc.perform(get("/api/employees/{id}", "invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid value")))
                .andExpect(jsonPath("$.message").value(containsString("invalid-id")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should validate firstName is not blank")
    void shouldValidateFirstNameNotBlank() throws Exception {
        // Given
        EmployeeRequest request = new EmployeeRequest("", "Doe");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[?(@.field == 'firstName')]").exists());
    }

    @Test
    @DisplayName("Should validate lastName is not blank")
    void shouldValidateLastNameNotBlank() throws Exception {
        // Given
        EmployeeRequest request = new EmployeeRequest("John", "");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[?(@.field == 'lastName')]").exists());
    }

    @Test
    @DisplayName("Should validate firstName length")
    void shouldValidateFirstNameLength() throws Exception {
        // Given - First name too short (less than 2 chars)
        EmployeeRequest request = new EmployeeRequest("J", "Doe");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[?(@.field == 'firstName')]").exists());
    }

    @Test
    @DisplayName("Should validate lastName length")
    void shouldValidateLastNameLength() throws Exception {
        // Given - Last name too short (less than 2 chars)
        EmployeeRequest request = new EmployeeRequest("John", "D");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[?(@.field == 'lastName')]").exists());
    }

    @Test
    @DisplayName("Should handle multiple validation errors at once")
    void shouldHandleMultipleValidationErrors() throws Exception {
        // Given - Both fields invalid
        EmployeeRequest request = new EmployeeRequest("", "D");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Should include rejectedValue in validation errors")
    void shouldIncludeRejectedValueInValidationErrors() throws Exception {
        // Given
        EmployeeRequest request = new EmployeeRequest("X", "Doe");

        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[?(@.field == 'firstName')].rejectedValue").value("X"));
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException for update operation")
    void shouldHandleResourceNotFoundOnUpdate() throws Exception {
        // Given
        EmployeeRequest request = new EmployeeRequest("John", "Doe");

        // When & Then
        mockMvc.perform(put("/api/employees/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException for delete operation")
    void shouldHandleResourceNotFoundOnDelete() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/employees/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Should return proper error format with all required fields")
    void shouldReturnProperErrorFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/employees/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    @DisplayName("Should handle null request body")
    void shouldHandleNullRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle invalid JSON format")
    void shouldHandleInvalidJsonFormat() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
}