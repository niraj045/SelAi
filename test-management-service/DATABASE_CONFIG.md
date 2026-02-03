# Test Management Service - Database Configuration

## Quick Start

### Development Mode (H2 Database)
For development and testing, use the embedded H2 database:

```bash
# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or set environment variable
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

The application will:
- Use H2 in-memory database
- Create tables automatically (DDL auto-creation)
- Be available at: http://localhost:8081
- H2 Console available at: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - User: `sa`
  - Password: (empty)

### Production Mode (PostgreSQL)
For production, ensure PostgreSQL is running and use default profile:

```bash
# Make sure PostgreSQL is running with correct credentials
# Default configuration expects:
# - Database: selai_testmgmt
# - Username: postgres
# - Password: postgres123123

mvn spring-boot:run
```

### Testing
Tests automatically use H2 database configuration:

```bash
mvn test
```

## Database Profiles

| Profile | Database | Purpose | Configuration File |
|---------|----------|---------|-------------------|
| `dev` | H2 (in-memory) | Development | `application-dev.properties` |
| `default` | PostgreSQL | Production | `application.properties` |
| `test` | H2 (in-memory) | Unit Testing | `test/resources/application.properties` |

## Switching Between Databases

### Option 1: Use Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=dev
```

### Option 2: Use JVM System Property
```bash
java -Dspring.profiles.active=dev -jar target/test-management-service-0.0.1-SNAPSHOT.jar
```

### Option 3: Use Maven
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## PostgreSQL Setup (Production)

If you need to set up PostgreSQL:

```bash
# Create database
createdb selai_testmgmt

# Set password for postgres user
sudo -u postgres psql
ALTER USER postgres PASSWORD 'postgres123123';
```

Or update the credentials in `application.properties` to match your PostgreSQL setup.