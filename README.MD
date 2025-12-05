# Time Tracking API

Spring Boot REST API for employee time tracking with timesheet generation.

## 🚀 Quick Start

```bash
# Clone
git clone https://github.com/TarasAntoniuk/time-tracking.git
cd time-tracking

# Configure database in application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/time_tracking
spring.datasource.username=your_user
spring.datasource.password=your_password

# Run
mvn spring-boot:run
```

API available at `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

## 📋 Main Features

- Employee management (CRUD)
- Check-in/check-out tracking
- Timesheet generation with daily breakdown
- Hours format: HH:MM (e.g., "08:30")
- SQL-optimized with PostgreSQL window functions

## 🔧 Tech Stack

Java 17 | Spring Boot 3 | PostgreSQL | Maven | Lombok

## 📊 API Examples

### Create check-in
```bash
POST /api/time-logs
{
  "employeeId": 1,
  "checkTime": "2024-11-01T09:00:00"
}
```

### Get timesheet
```bash
GET /api/time-logs/timesheet/1?from=2024-11-01T00:00:00&to=2024-11-30T23:59:59
```

Response:
```json
{
  "employeeId": 1,
  "firstName": "Carlos",
  "lastName": "García",
  "totalHours": "184:30",
  "dailyRecords": [
    {
      "date": "2024-11-01",
      "hoursWorked": "08:30",
      "firstEntry": "09:06:00",
      "lastExit": "18:39:00"
    }
  ]
}
```

## 📦 Deployment

```bash
mvn clean package
java -jar target/time-tracking-api-0.0.1-SNAPSHOT.jar
```

## 👤 Author

Taras Antoniuk