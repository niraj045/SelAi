# 🧠 AI-Powered No-Code Test Automation Platform
## Java Backend Implementation

---

## 📦 What's Been Built

A complete microservices-based backend for an AI-powered test automation platform built with **Spring Boot 3.2.1** and **Java 21**.

### Services Implemented:

1. **Test Management Service** (Port 8081) - ✅ COMPLETE
   - Project CRUD operations
   - Test suite management
   - Search and filtering

2. **Orchestration Service** (Port 8082) - ✅ COMPLETE
   - Test run lifecycle management
   - Coordinates AI Engine and Execution Service
   - Async processing with Spring @Async

3. **Execution Service** (Port 8083) - ✅ COMPLETE
   - Selenium WebDriver execution
   - Screenshot capture
   - Step-by-step test execution
   - Browser management (Chrome, Firefox, Edge)
   - Async execution callbacks to orchestration

4. **Reporting Service** (Port 8084) - ✅ COMPLETE
   - Persistent report summaries
   - HTML, Markdown, and PDF rendering
   - Report download endpoints

5. **API Gateway** (Port 8080) - ✅ COMPLETE
   - Routes all backend services
   - Global CORS configuration
   - Optional bearer-token gate for deployed environments

---

## 🔧 Prerequisites

Before running the services, ensure you have:

```bash
# 1. Java 21
java --version
# Should show: openjdk 21.x.x or Oracle Java 21.x.x

# 2. Maven 3.8+
mvn --version
# Should show: Apache Maven 3.8.x or higher

# 3. PostgreSQL 14+
psql --version
# Should show: psql (PostgreSQL) 14.x or higher

# 4. Chrome Browser (for Selenium)
# Chrome will be used for test execution
# WebDriverManager will auto-download chromedriver
```

---

## 🗄️ Database Setup

### Step 1: Create PostgreSQL Databases

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create databases for each service
CREATE DATABASE selai_testmgmt;
CREATE DATABASE selai_orchestration;
CREATE DATABASE selai_execution;
CREATE DATABASE selai_reporting;

-- Verify databases created
\l

-- Exit
\q
```

### Step 2: Configure Database Credentials

If your PostgreSQL credentials are different from the defaults, update `application.properties` in each service:

```properties
# Default credentials (change if needed)
spring.datasource.username=postgres
spring.datasource.password=postgres
```

---

## 🚀 Building the Services

### Option 1: Build All Services

```bash
# From the project root
cd d:\ns-backend-selAi

# Build Test Management Service
cd test-management-service
mvn clean install
cd ..

# Build Orchestration Service
cd orchestration-service
mvn clean install
cd ..

# Build Execution Service
cd execution-service
mvn clean install
cd ..
```

### Option 2: Build a Single Service

```bash
cd test-management-service
mvn clean install
```

**Note:** First build may take 5-10 minutes as Maven downloads all dependencies.

---

## ▶️ Running the Services

### Start Services in Order:

```powershell
# 1. Test Management Service (Terminal 1)
cd d:\ns-backend-selAi\test-management-service
mvn spring-boot:run

# 2. Orchestration Service (Terminal 2)
cd d:\ns-backend-selAi\orchestration-service
mvn spring-boot:run

# 3. Execution Service (Terminal 3)
cd d:\ns-backend-selAi\execution-service
mvn spring-boot:run
```

### Verification:

Check if services are running:

```bash
# Test Management Service
curl http://localhost:8081/api/projects

# Orchestration Service  
curl http://localhost:8082/api/test-runs

# Expected: Empty array [] or proper JSON response
```

---

## 🧪 Testing the Complete Flow

### Step 1: Create a Project

```bash
curl -X POST http://localhost:8081/api/projects ^
  -H "Content-Type: application/json" ^
  -d "{\"name\": \"Test Project\", \"url\": \"https://example.com\", \"browserType\": \"chrome\", \"testType\": \"smoke\"}"
```

**Expected Response:**
```json
{
  "id": 1,
  "name": "Test Project",
  "url": "https://example.com",
  "browserType": "chrome",
  "testType": "smoke",
  "isActive": true,
  "createdAt": "2026-02-02T11:42:54",
  ...
}
```

###Step 2: Start a Test Run

```bash
curl -X POST http://localhost:8082/api/test-runs ^
  -H "Content-Type: application/json" ^
  -d "{\"projectId\": 1, \"url\": \"https://example.com\", \"browser\": \"chrome\", \"testType\": \"smoke\"}"
```

**Expected Response:**
```json
{
  "id": 1,
  "projectId": 1,
  "status": "PENDING",
  "browser": "chrome",
  "startedAt": "2026-02-02T11:43:00",
  "totalTests": 0,
  "passedTests": 0,
  "failedTests": 0
}
```

### Step 3: Check Test Run Status

```bash
curl http://localhost:8082/api/test-runs/1
```

**Status will progress:** PENDING → RUNNING → PASSED/FAILED

### Step 4: Get All Test Runs for a Project

```bash
curl http://localhost:8082/api/test-runs/project/1
```

---

## 🌐 API Endpoints Reference

### Test Management Service (8081)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create new project |
| GET | `/api/projects` | Get all projects |
| GET | `/api/projects/{id}` | Get project by ID |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project (soft) |
| GET | `/api/projects/search?q=term` | Search projects |
| GET | `/api/projects/by-type/{type}` | Get by test type |

### Orchestration Service (8082)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/test-runs` | Start new test run |
| GET | `/api/test-runs/{id}` | Get test run status |
| GET | `/api/test-runs/project/{id}` | Get all runs for project |
| POST | `/api/test-runs/{id}/stop` | Stop running test |
| POST | `/api/test-runs/{id}/results` | Update results (callback) |

### Execution Service (8083)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/execute` | Execute test cases |
| GET | `/api/executions/test-run/{id}` | Get executions for test run |

---

## 📊 Database Schemas

### Test Management Service

```sql
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    description TEXT,
    browser_type VARCHAR(50),
    test_type VARCHAR(50),
    created_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Orchestration Service

```sql
CREATE TABLE test_runs (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,  -- PENDING, RUNNING, PASSED, FAILED, STOPPED
    browser VARCHAR(50),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    total_tests INT,
    passed_tests INT,
    failed_tests INT,
    error_message TEXT
);
```

### Execution Service

```sql
CREATE TABLE test_executions (
    id BIGSERIAL PRIMARY KEY,
    test_run_id BIGINT NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    test_description TEXT,
    status VARCHAR(50) NOT NULL,  -- PENDING, RUNNING, PASSED, FAILED, SKIPPED
    error_message TEXT,
    screenshot_path VARCHAR(500),
    execution_time_ms BIGINT,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🐛 Troubleshooting

### Issue: Port Already in Use

```bash
# Windows: Find and kill process using port
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### Issue: Database Connection Failed

```bash
# Check if PostgreSQL is running
# Windows: Check Services → PostgreSQL

# Test connection
psql -U postgres -d selai_testmgmt
```

### Issue: Maven Build Fails

```bash
# Clear Maven cache and rebuild
mvn clean
mvn dependency:purge-local-repository
mvn clean install
```

### Issue: Java Version Mismatch

```bash
# Check Java version
java --version

# Set JAVA_HOME (Windows)
# System Properties → Environment Variables
# Add: JAVA_HOME=C:\Program Files\Java\jdk-21
```

---

## 📁 Project Structure

```
ns-backend-selAi/
├── test-management-service/
│   ├── src/main/java/com/ns/selai/testmanagement/
│   │   ├── model/Project.java
│   │   ├── repository/ProjectRepository.java
│   │   ├── service/ProjectService.java
│   │   ├── controller/ProjectController.java
│   │   ├── dto/ProjectDTO.java
│   │   └── exception/
│   └── src/main/resources/application.properties
│
├── orchestration-service/
│   ├── src/main/java/com/ns/selai/orchestration/
│   │   ├── model/TestRun.java
│   │   ├── repository/TestRunRepository.java
│   │   ├── service/TestOrchestrationService.java
│   │   ├── controller/TestRunController.java
│   │   ├── client/
│   │   │   ├── AiEngineClient.java
│   │   │   └── ExecutionServiceClient.java
│   │   ├── dto/
│   │   └── config/AsyncConfig.java
│   └── src/main/resources/application.properties
│
├── execution-service/
│   ├── src/main/java/com/ns/selai/execution/
│   │   ├── model/TestExecution.java
│   │   ├── selenium/
│   │   │   ├── BrowserManager.java
│   │   │   ├── StepExecutor.java
│   │   │   └── ScreenshotService.java
│   │   ├── service/
│   │   └── controller/
│   └── src/main/resources/application.properties
│
├── IMPLEMENTATION_PLAN.md        - Complete architecture documentation
├── IMPLEMENTATION_STATUS.md      - Current progress status
└── README.md                     - This file
```

---

## 🔐 Security Notes

**Current Implementation:**
- ⚠️ CORS is set to allow all origins (`*`)
- ⚠️ No authentication/authorization
- ⚠️ Database credentials in plain text

**Production Recommendations:**
1. Configure CORS properly
2. Implement JWT-based authentication
3. Use environment variables for sensitive data
4. Add rate limiting
5. Enable HTTPS

---

## 🎯 Next Steps

1. **Python AI Engine**
   - FastAPI service
   - DOM analysis
   - Test case generation
   - Selector healing

2. **Frontend**
   - React dashboard
   - Project management UI
   - Test run and report screens

3. **Production hardening**
   - Replace the development bearer token with JWT validation
   - Move credentials to environment variables or secrets
   - Add rate limiting and tenant isolation
   - Test run monitoring
   - Report viewer

---

## 📝 Configuration Files

### Test Management Service - `application.properties`
```properties
server.port=8081
spring.application.name=test-management-service
spring.datasource.url=jdbc:postgresql://localhost:5432/selai_testmgmt
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
```

### Orchestration Service - `application.properties`
```properties
server.port=8082
spring.application.name=orchestration-service
spring.datasource.url=jdbc:postgresql://localhost:5432/selai_orchestration
spring.datasource.username=postgres
spring.datasource.password=postgres
ai.engine.base-url=http://localhost:5000
execution.service.base-url=http://localhost:8083
```

### Execution Service - `application.properties`
```properties
server.port=8083
spring.application.name=execution-service
spring.datasource.url=jdbc:postgresql://localhost:5432/selai_execution
spring.datasource.username=postgres
spring.datasource.password=postgres
screenshot.storage.path=./screenshots
```

---

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review logs in console output
3. Check `IMPLEMENTATION_STATUS.md` for component status

---

## 📜 License

This project is part of the SelAi AI-Powered No-Code Test Automation Platform.

---

**Built with ❤️ using Spring Boot, Selenium, and AI**
