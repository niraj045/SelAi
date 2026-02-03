# How to Test the AI-Powered Test Automation Platform

This guide provides steps to test the integrated microservices after they have been successfully built and started.

## Service Overview

| Service | Port | Description |
|---------|------|-------------|
| **API Gateway** | 8080 | Single entry point for all API calls |
| **Test Management** | 8081 | Manage projects and test suites |
| **Orchestration** | 8082 | Workflow brain; interacts with AI Engine & Execution |
| **Execution** | 8083 | Selenium/WebDriver test executor |
| **Reporting** | 8084 | Test results and statistics |

---

## 1. Verify Connectivity
Ensure all services are up by checking the API Gateway's reachability.

```bash
# Check if API Gateway is running
curl -I http://localhost:8080/api/projects
```

---

## 2. End-to-End Workflow Test

Follow these steps to perform a full test of the system.

### Step A: Create a Project
First, define a project with a target URL to test.

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Demo E-commerce Project",
    "url": "https://www.example.com",
    "description": "Functional testing of example.com",
    "browserType": "chrome",
    "testType": "smoke"
  }'
```
*Note the `id` returned in the response (e.g., `1`).*

### Step B: Start a Test Run
Trigger the orchestration flow which will call the AI Engine (Python) to generate tests and then execute them.

```bash
curl -X POST http://localhost:8080/api/test-runs \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": 1,
    "url": "https://www.example.com",
    "browser": "chrome",
    "testType": "smoke"
  }'
```
*Note the `id` of the test run returned.*

### Step C: Monitor Test Run Status
Check the status of your test run as it progresses from `PENDING` to `RUNNING` to `PASSED/FAILED`.

```bash
# Replace {id} with the ID from Step B
curl http://localhost:8080/api/test-runs/{id}
```

### Step D: View Reports
Once the test run is complete, you can view the execution reports.

```bash
curl http://localhost:8080/api/reports/test-runs/1
```

---

## 3. Advanced Testing (Individual Services)

If you need to debug a specific service, you can call it directly by bypasssing the Gateway:

- **Test Management**: `http://localhost:8081/api/projects`
- **Orchestration**: `http://localhost:8082/api/test-runs`
- **Reporting**: `http://localhost:8084/api/reports`

---

## 4. Troubleshooting

- **Check Logs**: If a test run is stuck in `PENDING`, check the logs of the `orchestration-service` and `execution-service`.
- **Database**: Ensure PostgreSQL is running and the `selai_*` databases are created as per `build-all.bat`.
- **Selenium**: Ensure a compatible Chrome/Firefox browser is installed on the system for the `execution-service` to use.
