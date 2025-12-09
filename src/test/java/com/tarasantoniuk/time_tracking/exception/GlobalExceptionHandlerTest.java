package com.tarasantoniuk.time_tracking.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.time_tracking.employee.dto.EmployeeRequest;
import com.tarasantoniuk.time_tracking.employee.entity.Employee;
import com.tarasantoniuk.time_tracking.employee.repository.EmployeeRepository;
import com.tarasantoniuk.time_tracking.timelog.dto.TimeLogRequest;
import com.tarasantoniuk.time_tracking.timelog.repository.TimeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("GlobalExceptionHandler Integration Tests")
class GlobalExceptionHandlerTest {

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

    @Autowired
    private TimeLogRepository timeLogRepository;

    @BeforeEach
    void setUp() {
        timeLogRepository.deleteAll();
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid request body. Please check your JSON format."));
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException with missing body message")
    void shouldHandleHttpMessageNotReadableExceptionWithMissingBody() throws Exception {
        // When & Then - POST without any content
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is required"));
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException for unique constraint")
    void shouldHandleDataIntegrityViolationForUniqueConstraint() throws Exception {
        // Given - Create an employee
        Employee employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        Employee savedEmployee = employeeRepository.save(employee);

        // Create a time log
        TimeLogRequest timeLogRequest = new TimeLogRequest();
        timeLogRequest.setEmployeeId(savedEmployee.getId());
        timeLogRequest.setCheckTime(LocalDateTime.of(2024, 12, 8, 9, 0));

        mockMvc.perform(post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timeLogRequest)))
                .andExpect(status().isCreated());

        // When & Then - Try to create duplicate time log (same employee, same time)
        mockMvc.perform(post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timeLogRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DATA_INTEGRITY_VIOLATION"));
    }

    @Nested
    @DisplayName("Unit tests for GlobalExceptionHandler")
    class GlobalExceptionHandlerUnitTests {

        private GlobalExceptionHandler handler;
        private MockHttpServletRequest mockRequest;

        @BeforeEach
        void setUp() {
            handler = new GlobalExceptionHandler();
            mockRequest = new MockHttpServletRequest();
            mockRequest.setRequestURI("/api/test");
        }

        @Test
        @DisplayName("Should handle InvalidOperationException with 422 status")
        void shouldHandleInvalidOperationException() {
            // Given
            InvalidOperationException exception = new InvalidOperationException("Operation not allowed");

            // When
            var response = handler.handleInvalidOperationException(exception, mockRequest);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(422);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(422);
            assertThat(response.getBody().getError()).isEqualTo("INVALID_OPERATION");
            assertThat(response.getBody().getMessage()).isEqualTo("Operation not allowed");
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }

        @Test
        @DisplayName("Should handle DataIntegrityViolationException with generic message")
        void shouldHandleDataIntegrityViolationWithGenericMessage() {
            // Given
            org.springframework.dao.DataIntegrityViolationException exception =
                    new org.springframework.dao.DataIntegrityViolationException("Some database error");

            // When
            var response = handler.handleDataIntegrityViolationException(exception, mockRequest);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(409);
            assertThat(response.getBody().getError()).isEqualTo("DATA_INTEGRITY_VIOLATION");
            assertThat(response.getBody().getMessage()).isEqualTo("Database constraint violation. The operation could not be completed.");
        }

        @Test
        @DisplayName("Should handle DataIntegrityViolationException with unique constraint message")
        void shouldHandleDataIntegrityViolationWithUniqueConstraintMessage() {
            // Given
            org.springframework.dao.DataIntegrityViolationException exception =
                    new org.springframework.dao.DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint");

            // When
            var response = handler.handleDataIntegrityViolationException(exception, mockRequest);

            // Then
            assertThat(response.getBody().getMessage()).isEqualTo("A record with the same unique value already exists");
        }

        @Test
        @DisplayName("Should handle DataIntegrityViolationException with foreign key message")
        void shouldHandleDataIntegrityViolationWithForeignKeyMessage() {
            // Given
            org.springframework.dao.DataIntegrityViolationException exception =
                    new org.springframework.dao.DataIntegrityViolationException("ERROR: violates foreign key constraint");

            // When
            var response = handler.handleDataIntegrityViolationException(exception, mockRequest);

            // Then
            assertThat(response.getBody().getMessage()).isEqualTo("Cannot perform operation due to existing references");
        }

        @Test
        @DisplayName("Should handle DataIntegrityViolationException with null message")
        void shouldHandleDataIntegrityViolationWithNullMessage() {
            // Given
            org.springframework.dao.DataIntegrityViolationException exception =
                    new org.springframework.dao.DataIntegrityViolationException((String) null);

            // When
            var response = handler.handleDataIntegrityViolationException(exception, mockRequest);

            // Then
            assertThat(response.getBody().getMessage()).isEqualTo("Database constraint violation. The operation could not be completed.");
        }

        @Test
        @DisplayName("Should handle generic Exception with 500 status")
        void shouldHandleGenericException() {
            // Given
            Exception exception = new RuntimeException("Unexpected error occurred");

            // When
            var response = handler.handleGenericException(exception, mockRequest);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getError()).isEqualTo("INTERNAL_SERVER_ERROR");
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }

        @Test
        @DisplayName("Should handle ResourceNotFoundException correctly")
        void shouldHandleResourceNotFoundExceptionUnit() {
            // Given
            ResourceNotFoundException exception = new ResourceNotFoundException("Employee", 123L);

            // When
            var response = handler.handleResourceNotFoundException(exception, mockRequest);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody().getError()).isEqualTo("NOT_FOUND");
            assertThat(response.getBody().getMessage()).contains("Employee");
            assertThat(response.getBody().getMessage()).contains("123");
        }

        @Test
        @DisplayName("Should handle DuplicateResourceException correctly")
        void shouldHandleDuplicateResourceExceptionUnit() {
            // Given
            DuplicateResourceException exception = new DuplicateResourceException("Employee already exists: John Doe");

            // When
            var response = handler.handleDuplicateResourceException(exception, mockRequest);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody().getError()).isEqualTo("DUPLICATE_RESOURCE");
            assertThat(response.getBody().getMessage()).isEqualTo("Employee already exists: John Doe");
        }

        @Test
        @DisplayName("Should handle HttpMessageNotReadableException with invalid JSON")
        void shouldHandleHttpMessageNotReadableExceptionWithInvalidJson() {
            // Given
            org.springframework.http.converter.HttpMessageNotReadableException exception =
                    new org.springframework.http.converter.HttpMessageNotReadableException("JSON parse error");

            // When
            var response = handler.handleHttpMessageNotReadableException(exception, mockRequest);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("BAD_REQUEST");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid request body. Please check your JSON format.");
        }

        @Test
        @DisplayName("Should handle HttpMessageNotReadableException with missing body")
        void shouldHandleHttpMessageNotReadableExceptionWithMissingBodyUnit() {
            // Given
            org.springframework.http.converter.HttpMessageNotReadableException exception =
                    new org.springframework.http.converter.HttpMessageNotReadableException("Required request body is missing");

            // When
            var response = handler.handleHttpMessageNotReadableException(exception, mockRequest);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Request body is required");
        }

        @Test
        @DisplayName("Should handle MethodArgumentTypeMismatchException")
        void shouldHandleMethodArgumentTypeMismatchExceptionUnit() {
            // Given
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception =
                    new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                            "abc", Long.class, "id", null, new IllegalArgumentException("Invalid")
                    );

            // When
            var response = handler.handleTypeMismatchException(exception, mockRequest);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("BAD_REQUEST");
            assertThat(response.getBody().getMessage()).contains("abc");
            assertThat(response.getBody().getMessage()).contains("id");
            assertThat(response.getBody().getMessage()).contains("Long");
        }
    }
}


class MockHttpServletRequest implements jakarta.servlet.http.HttpServletRequest {
    private String requestURI = "/api/test";

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    // Minimal implementation - only getRequestURI is used by the handler
    @Override public String getAuthType() { return null; }
    @Override public jakarta.servlet.http.Cookie[] getCookies() { return new jakarta.servlet.http.Cookie[0]; }
    @Override public long getDateHeader(String name) { return 0; }
    @Override public String getHeader(String name) { return null; }
    @Override public java.util.Enumeration<String> getHeaders(String name) { return null; }
    @Override public java.util.Enumeration<String> getHeaderNames() { return null; }
    @Override public int getIntHeader(String name) { return 0; }
    @Override public String getMethod() { return null; }
    @Override public String getPathInfo() { return null; }
    @Override public String getPathTranslated() { return null; }
    @Override public String getContextPath() { return null; }
    @Override public String getQueryString() { return null; }
    @Override public String getRemoteUser() { return null; }
    @Override public boolean isUserInRole(String role) { return false; }
    @Override public java.security.Principal getUserPrincipal() { return null; }
    @Override public String getRequestedSessionId() { return null; }
    @Override public StringBuffer getRequestURL() { return null; }
    @Override public String getServletPath() { return null; }
    @Override public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
    @Override public jakarta.servlet.http.HttpSession getSession() { return null; }
    @Override public String changeSessionId() { return null; }
    @Override public boolean isRequestedSessionIdValid() { return false; }
    @Override public boolean isRequestedSessionIdFromCookie() { return false; }
    @Override public boolean isRequestedSessionIdFromURL() { return false; }
    @Override public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) { return false; }
    @Override public void login(String username, String password) {}
    @Override public void logout() {}
    @Override public java.util.Collection<jakarta.servlet.http.Part> getParts() { return null; }
    @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
    @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
    @Override public Object getAttribute(String name) { return null; }
    @Override public java.util.Enumeration<String> getAttributeNames() { return null; }
    @Override public String getCharacterEncoding() { return null; }
    @Override public void setCharacterEncoding(String env) {}
    @Override public int getContentLength() { return 0; }
    @Override public long getContentLengthLong() { return 0; }
    @Override public String getContentType() { return null; }
    @Override public jakarta.servlet.ServletInputStream getInputStream() { return null; }
    @Override public String getParameter(String name) { return null; }
    @Override public java.util.Enumeration<String> getParameterNames() { return null; }
    @Override public String[] getParameterValues(String name) { return new String[0]; }
    @Override public java.util.Map<String, String[]> getParameterMap() { return null; }
    @Override public String getProtocol() { return null; }
    @Override public String getScheme() { return null; }
    @Override public String getServerName() { return null; }
    @Override public int getServerPort() { return 0; }
    @Override public java.io.BufferedReader getReader() { return null; }
    @Override public String getRemoteAddr() { return null; }
    @Override public String getRemoteHost() { return null; }
    @Override public void setAttribute(String name, Object o) {}
    @Override public void removeAttribute(String name) {}
    @Override public java.util.Locale getLocale() { return null; }
    @Override public java.util.Enumeration<java.util.Locale> getLocales() { return null; }
    @Override public boolean isSecure() { return false; }
    @Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
    @Override public int getRemotePort() { return 0; }
    @Override public String getLocalName() { return null; }
    @Override public String getLocalAddr() { return null; }
    @Override public int getLocalPort() { return 0; }
    @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
    @Override public jakarta.servlet.AsyncContext startAsync() { return null; }
    @Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) { return null; }
    @Override public boolean isAsyncStarted() { return false; }
    @Override public boolean isAsyncSupported() { return false; }
    @Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
    @Override public jakarta.servlet.DispatcherType getDispatcherType() { return null; }
    @Override public String getRequestId() { return null; }
    @Override public String getProtocolRequestId() { return null; }
    @Override public jakarta.servlet.ServletConnection getServletConnection() { return null; }
}