# Time Tracking API

Spring Boot REST API for employee time tracking with automated timesheet generation.

## 📋 Prerequisites

- Java 21+
- PostgreSQL 17+
- Maven 3.8+
- Docker (optional)

## 🚀 Quick Start
```bash
# Clone
git clone https://github.com/TarasAntoniuk/time-tracking.git
cd time-tracking

# Configure database (application.properties)
spring.datasource.url=jdbc:postgresql://localhost:5432/time_tracking
spring.datasource.username=your_user
spring.datasource.password=your_password

# Run
mvn spring-boot:run
```

**API:** `http://localhost:8081`  
**Swagger:** `http://localhost:8081/swagger-ui.html`  
**🌐 Live Demo:** `https://tarasantoniuk.com/projects/timetracking/index.html`

## ✨ Features

- Employee CRUD with duplicate detection
- Check-in/check-out tracking
- Timesheet generation (HH:MM format)
- **Break time exclusion** - only actual work hours counted (first check-in to last check-out minus breaks)
- Real-time presence check
- 97% line and branch test coverage
- Automated CI/CD deployment

## 🔧 Tech Stack

**Java 21** | **Spring Boot 3.5.8** | **PostgreSQL 17** | **Maven** | **JUnit 5** | **Testcontainers**

## 📊 API Examples

### Create Employee
```bash
POST /api/employees
{"firstName": "John", "lastName": "Doe"}
```

### Check-in
```bash
POST /api/time-logs
{"employeeId": 1, "checkTime": "2024-11-01T09:00:00"}
```

### Get Timesheet
```bash
GET /api/time-logs/timesheet/1?from=2024-11-01T00:00:00&to=2024-11-30T23:59:59
```

Response:
```json
{
  "totalHours": "184:30",
  "dailyRecords": [
    {
      "date": "2024-11-01",
      "hoursWorked": "08:30",
      "firstEntry": "09:00:00",
      "lastExit": "18:30:00"
    }
  ]
}
```

**Note:** Hours worked = (last check-out - first check-in) - break time. Breaks between check-out and next check-in are automatically excluded.

### Get Present Employees
```bash
GET /api/time-logs/present?checkTime=2024-11-01T14:00:00
```

## 🧪 Testing
```bash
mvn test              # Run tests
mvn verify            # Run with coverage check
```

**85+ tests** with **97% line and branch coverage** - comprehensive test suite covering all major functionality.

## 🚢 Deployment

### Docker
```bash
mvn clean package
docker-compose up -d
```

See `docker/docker-compose.yml` for configuration.

### Manual
```bash
mvn clean package
java -jar target/time-tracking-0.0.1-SNAPSHOT.jar
```

Automated deployment via GitHub Actions on push to `master`.

## 📄 License

This project is licensed under the MIT License.

## Contact

**Taras Antoniuk**  
📧 [bronya2004@gmail.com](mailto:bronya2004@gmail.com)  
🔗 [LinkedIn](https://www.linkedin.com/in/taras-antoniuk-7a550816a/)  
💻 [HackerRank](https://www.hackerrank.com/profile/bronya2004)

---

**Last Updated**: December 2024
⭐ Star if helpful! 📫 Questions? Open an issue.