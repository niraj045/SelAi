# Quick Start - Test the API

## 1. Create a Project
```bash
curl -X POST http://localhost:8081/api/projects ^
  -H "Content-Type: application/json" ^
  -d "{\"name\": \"My First Project\", \"url\": \"https://www.google.com\", \"description\": \"Test Google homepage\", \"browserType\": \"chrome\", \"testType\": \"smoke\"}"
```

## 2. Get All Projects
```bash
curl http://localhost:8081/api/projects
```

## 3. Start a Test Run
```bash
curl -X POST http://localhost:8082/api/test-runs ^
  -H "Content-Type: application/json" ^
  -d "{\"projectId\": 1, \"url\": \"https://www.google.com\", \"browser\": \"chrome\", \"testType\": \"smoke\"}"
```

## 4. Check Test Run Status
```bash
curl http://localhost:8082/api/test-runs/1
```

## 5. Get All Test Runs for Project
```bash
curl http://localhost:8082/api/test-runs/project/1
```

## Example: Complete Flow

```bash
# Step 1: Create Project
curl -X POST http://localhost:8081/api/projects -H "Content-Type: application/json" -d "{\"name\": \"Login Test\", \"url\": \"https://the-internet.herokuapp.com/login\", \"browserType\": \"chrome\", \"testType\": \"functional\"}"

# Step 2: Start Test Run  
curl -X POST http://localhost:8082/api/test-runs -H "Content-Type: application/json" -d "{\"projectId\": 1, \"url\": \"https://the-internet.herokuapp.com/login\", \"browser\": \"chrome\"}"

# Step 3: Monitor Status
curl http://localhost:8082/api/test-runs/1

# Step 4: Search Projects
curl "http://localhost:8081/api/projects/search?q=Login"

# Step 5: Update Project
curl -X PUT http://localhost:8081/api/projects/1 -H "Content-Type: application/json" -d "{\"description\": \"Updated description\"}"

# Step 6: Delete Project (soft delete)
curl -X DELETE http://localhost:8081/api/projects/1
```

## Using Postman

Import these as a Postman collection:

###POST Create Project
`POST http://localhost:8081/api/projects`
```json
{
  "name": "Test Project",
  "url": "https://example.com",
  "description": "My test project",
  "browserType": "chrome",
  "testType": "smoke",
  "createdBy": "user123"
}
```

### POST Start Test Run
`POST http://localhost:8082/api/test-runs`
```json
{
  "projectId": 1,
  "url": "https://example.com",
  "browser": "chrome",
  "testType": "smoke",
  "userId": "user123"
}
```

### GET Test Run Status
`GET http://localhost:8082/api/test-runs/1`

---

## Expected Responses

### Project Created
```json
{
  "id": 1,
  "name": "Test Project",
  "url": "https://example.com",
  "description": "My test project",
  "browserType": "chrome",
  "testType": "smoke",
  "createdBy": "user123",
  "isActive": true,
  "createdAt": "2026-02-02T11:42:54.123",
  "updatedAt": "2026-02-02T11:42:54.123"
}
```

### Test Run Started
```json
{
  "id": 1,
  "projectId": 1,
  "status": "PENDING",
  "browser": "chrome",
  "startedAt": "2026-02-02T11:43:00.456",
  "completedAt": null,
  "totalTests": 0,
  "passedTests": 0,
  "failedTests": 0,
  "errorMessage": null
}
```

### Test Run Completed
```json
{
  "id": 1,
  "projectId": 1,
  "status": "PASSED",
  "browser": "chrome",
  "startedAt": "2026-02-02T11:43:00.456",
  "completedAt": "2026-02-02T11:45:30.789",
  "totalTests": 5,
  "passedTests": 5,
  "failedTests": 0,
  "errorMessage": null
}
```
