@echo off
echo ================================================
echo   Building AI-Powered Test Automation Platform
echo ================================================
echo.

echo [1/5] Building Test Management Service...
cd test-management-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Test Management Service build failed!
    pause
    exit /b 1
)
cd ..
echo ✓ Test Management Service built successfully
echo.

echo [2/5] Building Orchestration Service...
cd orchestration-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Orchestration Service build failed!
    pause
    exit /b 1
)
cd ..
echo ✓ Orchestration Service built successfully
echo.

echo [3/5] Building Execution Service...
cd execution-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Execution Service build failed!
    pause
    exit /b 1
)
cd ..
echo ✓ Execution Service built successfully
echo.

echo [4/5] Building Reporting Service...
cd reporting-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Reporting Service build failed!
    pause
    exit /b 1
)
cd ..
echo ✓ Reporting Service built successfully
echo.

echo [5/5] Building API Gateway...
cd api-gateway
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: API Gateway build failed!
    pause
    exit /b 1
)
cd ..
echo ✓ API Gateway built successfully
echo.

echo ================================================
echo   All services built successfully! ✓
echo ================================================
echo.
echo Next steps:
echo 1. Ensure PostgreSQL is running
echo 2. Create databases:
echo    - selai_testmgmt
echo    - selai_orchestration
echo    - selai_execution
echo    - selai_rept
echo.
echo 3. Start services (in separate terminals):
echo    Terminal 1: cd api-gateway ^&^& mvn spring-boot:run
echo    Terminal 2: cd test-management-service ^&^& mvn spring-boot:run
echo    Terminal 3: cd orchestration-service ^&^& mvn spring-boot:run
echo    Terminal 4: cd execution-service ^&^& mvn spring-boot:run
echo    Terminal 5: cd reporting-service ^&^& mvn spring-boot:run
echo.
pause
