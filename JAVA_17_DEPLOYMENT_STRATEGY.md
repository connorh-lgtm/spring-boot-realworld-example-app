# Java 17 Deployment Strategy

## Overview
This document provides comprehensive guidance for deploying the Spring Boot RealWorld Example Application with Java 17 in production environments.

## Pre-Deployment Checklist

### Environment Requirements
- [ ] Java 17 JDK/JRE installed on target environment
- [ ] Environment variables configured (see Configuration section)
- [ ] Database connectivity verified
- [ ] SSL/TLS certificates updated if needed
- [ ] Monitoring and logging systems ready

### Configuration Verification
- [ ] JWT secrets externalized (not hardcoded)
- [ ] Database connection strings configured for production
- [ ] Logging levels appropriate for production
- [ ] Performance settings optimized for target environment

## Production Configuration

### Environment Variables
Create the following environment variables for production deployment:

```bash
# Required - JWT Configuration
export JWT_SECRET="your-production-jwt-secret-minimum-512-bits-required-for-hmac-sha256"
export JWT_SESSION_TIME="86400"

# Database Configuration
export SPRING_DATASOURCE_URL="jdbc:sqlite:/path/to/production/database.db"
export SPRING_DATASOURCE_USERNAME=""
export SPRING_DATASOURCE_PASSWORD=""

# Application Configuration
export SPRING_PROFILES_ACTIVE="production"
export SERVER_PORT="8080"

# Logging Configuration
export LOGGING_LEVEL_ROOT="INFO"
export LOGGING_LEVEL_IO_SPRING="INFO"
```

### JVM Optimization for Java 17
Recommended JVM settings for production deployment:

```bash
# Memory Settings
-Xms512m
-Xmx2g
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m

# Garbage Collection (G1GC - recommended for Java 17)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m

# Performance Optimizations
-XX:+UseStringDeduplication
-XX:+OptimizeStringConcat
-XX:+UseCompressedOops

# Monitoring and Debugging
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/app/
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps

# Java 17 Specific Optimizations
--enable-preview
-XX:+UnlockExperimentalVMOptions
-XX:+UseZGC  # Alternative to G1GC for large heaps
```

## Container Deployment

### Using Spring Boot's Built-in Container Support
The application uses Spring Boot's `bootBuildImage` task which automatically creates production-ready container images with Java 17:

```bash
# Build container image
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app:java17

# Run container with environment variables
docker run -d \
  --name realworld-app \
  -p 8080:8080 \
  -e JWT_SECRET="your-production-jwt-secret" \
  -e SPRING_PROFILES_ACTIVE="production" \
  -e SPRING_DATASOURCE_URL="jdbc:sqlite:/app/data/production.db" \
  -v /host/data:/app/data \
  spring-boot-realworld-example-app:java17
```

### Container Image Features
- Based on Eclipse Temurin Java 17 runtime
- Optimized for production workloads
- Includes security updates and minimal attack surface
- Supports health checks and graceful shutdown

### Docker Compose Example
```yaml
version: '3.8'
services:
  realworld-app:
    image: spring-boot-realworld-example-app:java17
    ports:
      - "8080:8080"
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - SPRING_PROFILES_ACTIVE=production
      - SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/production.db
    volumes:
      - ./data:/app/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/tags"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped
```

## Migration Strategy

### Step-by-Step Production Migration

#### Phase 1: Pre-Migration (1-2 hours)
1. **Backup Current System**
   ```bash
   # Backup database
   cp production.db production.db.backup.$(date +%Y%m%d_%H%M%S)
   
   # Backup configuration
   tar -czf config-backup-$(date +%Y%m%d_%H%M%S).tar.gz /path/to/config/
   ```

2. **Verify Java 17 Installation**
   ```bash
   java -version  # Should show Java 17
   echo $JAVA_HOME  # Should point to Java 17 installation
   ```

3. **Test Application in Staging**
   ```bash
   # Deploy to staging environment first
   ./gradlew bootRun --args='--spring.profiles.active=staging'
   ```

#### Phase 2: Deployment (30 minutes)
1. **Stop Current Application**
   ```bash
   # Graceful shutdown
   kill -TERM $APP_PID
   # Wait for graceful shutdown, then force if needed
   sleep 30 && kill -KILL $APP_PID 2>/dev/null || true
   ```

2. **Deploy New Version**
   ```bash
   # Using JAR deployment
   java -jar spring-boot-realworld-example-app-java17.jar
   
   # OR using container deployment
   docker-compose up -d
   ```

3. **Verify Deployment**
   ```bash
   # Check application health
   curl -f http://localhost:8080/tags
   
   # Check logs for errors
   tail -f /var/log/app/application.log
   ```

#### Phase 3: Post-Migration Validation (30 minutes)
1. **Functional Testing**
   ```bash
   # Test key endpoints
   curl -X GET http://localhost:8080/tags
   curl -X GET http://localhost:8080/articles
   
   # Test authentication flow
   # (Use your existing test scripts)
   ```

2. **Performance Monitoring**
   - Monitor memory usage
   - Check garbage collection metrics
   - Verify response times
   - Monitor error rates

### Rollback Procedures

#### Quick Rollback (5 minutes)
```bash
# Stop Java 17 application
docker-compose down  # or kill Java process

# Restore Java 11 version
docker-compose -f docker-compose.java11.yml up -d

# Verify rollback
curl -f http://localhost:8080/tags
```

#### Full Rollback (15 minutes)
```bash
# Stop application
docker-compose down

# Restore database backup
cp production.db.backup.YYYYMMDD_HHMMSS production.db

# Restore configuration
tar -xzf config-backup-YYYYMMDD_HHMMSS.tar.gz -C /

# Start Java 11 version
docker-compose -f docker-compose.java11.yml up -d
```

## Performance Monitoring

### Key Metrics to Monitor
- **JVM Metrics**: Heap usage, GC frequency, GC pause times
- **Application Metrics**: Response times, throughput, error rates
- **System Metrics**: CPU usage, memory usage, disk I/O

### Java 17 Specific Monitoring
```bash
# Enable JFR (Java Flight Recorder)
-XX:+FlightRecorder
-XX:StartFlightRecording=duration=60s,filename=app-profile.jfr

# Enable JVM metrics endpoint
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.metrics.enabled=true
```

### Expected Performance Improvements
- **Startup Time**: 10-15% faster startup compared to Java 11
- **Memory Usage**: 5-10% reduction in memory footprint
- **Throughput**: 5-15% improvement in request throughput
- **GC Performance**: Reduced pause times with G1GC improvements

## Security Considerations

### JWT Secret Management
```bash
# Generate secure JWT secret (minimum 512 bits for HMAC-SHA256)
openssl rand -base64 64

# Store in environment variable or secrets management system
export JWT_SECRET="$(openssl rand -base64 64)"
```

### Container Security
- Use non-root user in container
- Scan images for vulnerabilities
- Keep base images updated
- Limit container capabilities

### Network Security
- Use HTTPS in production
- Configure proper CORS settings
- Implement rate limiting
- Use reverse proxy (nginx/Apache)

## Troubleshooting

### Common Issues

#### Application Won't Start
```bash
# Check Java version
java -version

# Check environment variables
env | grep -E "(JWT_|SPRING_)"

# Check logs
tail -f logs/spring.log
```

#### Performance Issues
```bash
# Check GC logs
-XX:+PrintGC -XX:+PrintGCDetails

# Monitor JVM metrics
jstat -gc $PID 1s

# Check for memory leaks
jmap -histo $PID
```

#### Container Issues
```bash
# Check container logs
docker logs realworld-app

# Check container resource usage
docker stats realworld-app

# Debug container
docker exec -it realworld-app /bin/sh
```

## Support and Maintenance

### Regular Maintenance Tasks
- Monitor application logs daily
- Review performance metrics weekly
- Update dependencies monthly
- Security patches as needed

### Escalation Procedures
1. **Application Issues**: Check logs, restart if needed
2. **Performance Issues**: Review metrics, adjust JVM settings
3. **Security Issues**: Apply patches immediately, review access logs
4. **Data Issues**: Restore from backup, investigate root cause

## Additional Resources
- [Spring Boot Production Deployment Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- [Java 17 Performance Tuning](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [Container Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/container-images.html)
