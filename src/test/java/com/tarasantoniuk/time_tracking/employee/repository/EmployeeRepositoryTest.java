package com.tarasantoniuk.time_tracking.employee.repository;

import com.tarasantoniuk.time_tracking.employee.entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("EmployeeRepository Integration Tests")
class EmployeeRepositoryTest {

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
    private EmployeeRepository employeeRepository;

    private Employee employee1;
    private Employee employee2;
    private Employee employee3;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();

        employee1 = new Employee();
        employee1.setFirstName("John");
        employee1.setLastName("Doe");

        employee2 = new Employee();
        employee2.setFirstName("Jane");
        employee2.setLastName("Smith");

        employee3 = new Employee();
        employee3.setFirstName("Bob");
        employee3.setLastName("Johnson");
    }

    @Test
    @DisplayName("Should save and find employee by id")
    void shouldSaveAndFindEmployeeById() {
        // Given
        Employee saved = employeeRepository.save(employee1);

        // When
        Optional<Employee> found = employeeRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getLastName()).isEqualTo("Doe");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find employee by first name and last name")
    void shouldFindEmployeeByFirstNameAndLastName() {
        // Given
        employeeRepository.save(employee1);

        // When
        Optional<Employee> found = employeeRepository.findByFirstNameAndLastName("John", "Doe");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should return empty when employee not found by name")
    void shouldReturnEmptyWhenEmployeeNotFoundByName() {
        // When
        Optional<Employee> found = employeeRepository.findByFirstNameAndLastName("NonExistent", "Person");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if employee exists by first name and last name")
    void shouldCheckIfEmployeeExistsByName() {
        // Given
        employeeRepository.save(employee1);

        // When
        boolean exists = employeeRepository.existsByFirstNameAndLastName("John", "Doe");
        boolean notExists = employeeRepository.existsByFirstNameAndLastName("NonExistent", "Person");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should search employees by first name")
    void shouldSearchEmployeesByFirstName() {
        // Given
        employeeRepository.save(employee1); // John Doe
        employeeRepository.save(employee2); // Jane Smith
        employeeRepository.save(employee3); // Bob Johnson

        // When
        List<Employee> results = employeeRepository.searchByName("John");

        // Then
        assertThat(results).hasSize(2); // John Doe and Bob Johnson
        assertThat(results).extracting(Employee::getFirstName)
                .containsExactlyInAnyOrder("John", "Bob");
    }

    @Test
    @DisplayName("Should search employees by last name")
    void shouldSearchEmployeesByLastName() {
        // Given
        employeeRepository.save(employee1); // John Doe
        employeeRepository.save(employee2); // Jane Smith
        employeeRepository.save(employee3); // Bob Johnson

        // When
        List<Employee> results = employeeRepository.searchByName("Smith");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Should search employees case-insensitively")
    void shouldSearchEmployeesCaseInsensitively() {
        // Given
        employeeRepository.save(employee1); // John Doe

        // When
        List<Employee> results = employeeRepository.searchByName("john");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return empty list when search finds no matches")
    void shouldReturnEmptyListWhenSearchFindsNoMatches() {
        // Given
        employeeRepository.save(employee1);

        // When
        List<Employee> results = employeeRepository.searchByName("xyz");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should find all employees ordered by last name ascending")
    void shouldFindAllEmployeesOrderedByLastName() {
        // Given
        employeeRepository.save(employee2); // Smith
        employeeRepository.save(employee1); // Doe
        employeeRepository.save(employee3); // Johnson

        // When
        List<Employee> results = employeeRepository.findAllByOrderByLastNameAsc();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(Employee::getLastName)
                .containsExactly("Doe", "Johnson", "Smith");
    }

    @Test
    @DisplayName("Should delete employee by id")
    void shouldDeleteEmployeeById() {
        // Given
        Employee saved = employeeRepository.save(employee1);
        Long id = saved.getId();

        // When
        employeeRepository.deleteById(id);

        // Then
        Optional<Employee> found = employeeRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should update employee")
    void shouldUpdateEmployee() {
        // Given
        Employee saved = employeeRepository.save(employee1);

        // When
        saved.setFirstName("Johnny");
        saved.setLastName("Doeson");
        Employee updated = employeeRepository.save(saved);

        // Then
        assertThat(updated.getFirstName()).isEqualTo("Johnny");
        assertThat(updated.getLastName()).isEqualTo("Doeson");
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle special characters in names")
    void shouldHandleSpecialCharactersInNames() {
        // Given
        Employee specialEmployee = new Employee();
        specialEmployee.setFirstName("Jean-Pierre");
        specialEmployee.setLastName("O'Brien");

        // When
        Employee saved = employeeRepository.save(specialEmployee);
        Optional<Employee> found = employeeRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Jean-Pierre");
        assertThat(found.get().getLastName()).isEqualTo("O'Brien");
    }

    @Test
    @DisplayName("Should auto-generate ID on save")
    void shouldAutoGenerateIdOnSave() {
        // When
        Employee saved = employeeRepository.save(employee1);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Should set createdAt timestamp automatically")
    void shouldSetCreatedAtTimestampAutomatically() {
        // When
        Employee saved = employeeRepository.save(employee1);

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}