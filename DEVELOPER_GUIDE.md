# Developer Guide - Java 17 Spring Boot RealWorld App

## Quick Start

### Prerequisites
- **Java 17** (required)
- **Git** for version control
- **IDE** with Java 17 support (IntelliJ IDEA, VS Code, Eclipse)

### Setup
```bash
git clone https://github.com/connorh-lgtm/spring-boot-realworld-example-app.git
cd spring-boot-realworld-example-app
./gradlew bootRun
```

### Verify Installation
```bash
curl http://localhost:8080/tags
# Should return: {"tags":[]}
```

## Development Workflow

### Running the Application
```bash
# Standard development mode
./gradlew bootRun

# With specific profile
./gradlew bootRun --args='--spring.profiles.active=development'

# With performance optimizations
./scripts/start-optimized.sh development 8080
```

### Testing
```bash
# Run all tests
./gradlew test

# Run performance benchmarks
./gradlew performanceTest

# Run specific test categories
./gradlew test --tests "*Java17*"
./gradlew test --tests "*Api*"
./gradlew test --tests "*Repository*"
```

### Code Quality
```bash
# Format code (Java 17 compatible)
./gradlew spotlessJavaApply

# Check formatting
./gradlew spotlessJavaCheck

# Build with all checks
./gradlew clean build
```

## Architecture Overview

### Domain-Driven Design Structure
```
src/main/java/io/spring/
├── api/                 # Web layer (Spring MVC)
├── core/               # Business model (entities, services)
├── application/        # High-level services (CQRS read models)
├── infrastructure/     # Technical implementation details
└── graphql/           # GraphQL resolvers (DGS framework)
```

### Key Technologies
- **Java 17** - Modern Java with performance improvements
- **Spring Boot 2.7.18** - Application framework
- **MyBatis 2.3.2** - Data persistence (Data Mapper pattern)
- **Netflix DGS 4.9.21** - GraphQL framework
- **JWT 0.12.6** - Authentication tokens
- **SQLite** - Database (easily configurable)

## Java 17 Features

### Performance Optimizations
The application leverages several Java 17 performance features:

```bash
# G1GC with string deduplication
-XX:+UseG1GC
-XX:+UseStringDeduplication
-XX:MaxGCPauseMillis=100

# Memory optimizations
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers

# Startup optimizations
-XX:+TieredCompilation
-Xshare:on
```

### Modern Java Features
- **java.time API** - Replaces legacy Joda Time
- **Enhanced String handling** - String deduplication enabled
- **Improved GC** - G1GC optimizations for better performance
- **JFR Integration** - Java Flight Recorder for production monitoring

## Configuration Profiles

### Development Profile
```properties
# Fast startup, moderate memory usage
spring.profiles.active=development
logging.level.io.spring=DEBUG
```

### Production Profile
```properties
# Optimized for throughput and stability
spring.profiles.active=production
logging.level.root=WARN
server.compression.enabled=true
```

### Performance Testing Profile
```properties
# Detailed monitoring and profiling
spring.profiles.active=performance
management.endpoints.web.exposure.include=health,metrics,prometheus
```

## Database Development

### Schema Management
```bash
# Database migrations are handled by Flyway
# Migration files: src/main/resources/db/migration/
```

### MyBatis Development
```java
// Data Mapper pattern example
@Mapper
public interface ArticleMapper {
    @Select("SELECT * FROM articles WHERE id = #{id}")
    ArticleData findById(@Param("id") String id);
}
```

### Testing with Database
```java
@SpringBootTest
@Transactional
class RepositoryTest {
    // Tests automatically rollback
}
```

## GraphQL Development

### Schema Location
```
src/main/resources/schema/schema.graphqls
```

### Adding New Resolvers
```java
@DgsComponent
public class ArticleDatafetcher {
    @DgsQuery
    public List<ArticleData> articles() {
        return articleQueryService.findRecentArticles();
    }
}
```

### Testing GraphQL
```bash
# GraphQL endpoint
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ articles { title } }"}'
```

## Security Development

### JWT Configuration
```java
// JWT secret configuration
jwt.secret=${JWT_SECRET:default-secret-key}
jwt.sessionTime=86400
```

### Adding Secured Endpoints
```java
@RestController
@PreAuthorize("hasRole('USER')")
public class SecuredController {
    // Secured endpoints
}
```

### Testing Authentication
```java
@Test
@WithMockUser
void testSecuredEndpoint() {
    // Test with authenticated user
}
```

## Performance Development

### Benchmarking
```java
@Test
@Tag("performance")
void testPerformance() {
    // Performance test implementation
}
```

### Monitoring
```bash
# Start with JFR profiling
./scripts/performance-monitoring.sh

# View performance metrics
curl http://localhost:8080/actuator/metrics
```

### JVM Tuning
```bash
# Use optimized startup script
./scripts/start-optimized.sh production 8080

# Custom JVM settings
java @config/jvm-optimization.properties[production] -jar app.jar
```

## Debugging

### Application Debugging
```bash
# Debug mode
./gradlew bootRun --debug-jvm

# Remote debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar app.jar
```

### Performance Debugging
```bash
# JFR profiling
java -XX:+FlightRecorder \
     -XX:StartFlightRecording=duration=60s,filename=profile.jfr \
     -jar app.jar

# GC logging
java -Xlog:gc*:gc.log:time,tags -jar app.jar
```

## Common Development Tasks

### Adding New API Endpoint
1. Create controller in `api/` package
2. Add service logic in `application/` package
3. Add repository methods in `infrastructure/` package
4. Write tests for all layers
5. Update API documentation

### Adding New Entity
1. Create entity in `core/` package
2. Add MyBatis mapper in `infrastructure/mybatis/`
3. Create database migration in `src/main/resources/db/migration/`
4. Add repository implementation
5. Write comprehensive tests

### Performance Optimization
1. Identify bottleneck using JFR profiling
2. Add performance test to benchmark
3. Implement optimization
4. Verify improvement with benchmarks
5. Update performance documentation

## Troubleshooting

### Common Issues

**Build Failures**
```bash
# Clean and rebuild
./gradlew clean build

# Check Java version
java -version  # Should be 17.x.x
```

**Test Failures**
```bash
# Run specific failing test
./gradlew test --tests "ClassName.methodName"

# Check test logs
cat build/reports/tests/test/index.html
```

**Performance Issues**
```bash
# Check GC performance
./scripts/performance-monitoring.sh

# Run performance benchmarks
./gradlew performanceTest
```

**Database Issues**
```bash
# Reset database
rm dev.db
./gradlew bootRun  # Will recreate with Flyway
```

### Getting Help

- **Documentation**: Check `JAVA_17_*.md` files for specific guidance
- **Performance**: See `JAVA_17_PERFORMANCE_OPTIMIZATION.md`
- **Deployment**: See `JAVA_17_DEPLOYMENT_STRATEGY.md`
- **Migration**: See `JAVA_17_MIGRATION_SUMMARY.md`

## Contributing

### Code Standards
- Follow existing code patterns and architecture
- Use Java 17 features appropriately
- Write comprehensive tests for new features
- Update documentation for significant changes
- Run `./gradlew spotlessJavaApply` before committing

### Performance Considerations
- Always run performance benchmarks for changes affecting core paths
- Use JFR profiling to validate optimizations
- Consider memory usage and GC impact
- Test with realistic data volumes

### Testing Requirements
- Unit tests for business logic
- Integration tests for API endpoints
- Performance tests for critical paths
- Database tests for repository layer
- Security tests for authentication/authorization
