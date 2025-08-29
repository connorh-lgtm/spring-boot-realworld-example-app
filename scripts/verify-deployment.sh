#!/bin/bash


set -e

echo "🚀 Java 17 Deployment Verification Script"
echo "=========================================="

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        exit 1
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

echo "1. Checking Java 17 Installation..."
java -version 2>&1 | grep -q "17\." 
print_status $? "Java 17 is installed"

echo ""
echo "2. Checking JAVA_HOME..."
if [ -n "$JAVA_HOME" ]; then
    $JAVA_HOME/bin/java -version 2>&1 | grep -q "17\."
    print_status $? "JAVA_HOME points to Java 17"
else
    print_warning "JAVA_HOME not set - using system Java"
fi

echo ""
echo "3. Checking Gradle Wrapper..."
./gradlew --version | grep -q "Gradle"
print_status $? "Gradle wrapper is functional"

echo ""
echo "4. Checking Build Configuration..."
./gradlew clean compileJava > /dev/null 2>&1
print_status $? "Application compiles with Java 17"

echo ""
echo "5. Running Test Suite..."
./gradlew test > /dev/null 2>&1
print_status $? "All tests pass with Java 17"

echo ""
echo "6. Checking Container Image Building..."
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app-verify > /dev/null 2>&1
print_status $? "Container image builds successfully"

echo ""
echo "7. Testing Application Startup..."
timeout 30s ./gradlew bootRun > /dev/null 2>&1 &
APP_PID=$!
sleep 15

if kill -0 $APP_PID 2>/dev/null; then
    curl -f http://localhost:8080/tags > /dev/null 2>&1
    ENDPOINT_STATUS=$?
    
    kill $APP_PID 2>/dev/null || true
    wait $APP_PID 2>/dev/null || true
    
    print_status $ENDPOINT_STATUS "Application starts and responds to requests"
else
    print_status 1 "Application failed to start"
fi

echo ""
echo "8. Checking Environment Variables..."
if [ -z "$JWT_SECRET" ]; then
    print_warning "JWT_SECRET environment variable not set (required for production)"
else
    print_status 0 "JWT_SECRET environment variable is configured"
fi

echo ""
echo "9. Checking Production Configuration Template..."
if [ -f "application-production.properties.template" ]; then
    print_status 0 "Production configuration template exists"
else
    print_status 1 "Production configuration template missing"
fi

echo ""
echo "10. Checking Deployment Documentation..."
if [ -f "JAVA_17_DEPLOYMENT_STRATEGY.md" ]; then
    print_status 0 "Deployment strategy documentation exists"
else
    print_status 1 "Deployment strategy documentation missing"
fi

echo ""
echo "🎉 Deployment verification completed successfully!"
echo ""
echo "Next Steps:"
echo "1. Review JAVA_17_DEPLOYMENT_STRATEGY.md for production deployment guidance"
echo "2. Configure production environment variables using application-production.properties.template"
echo "3. Test deployment in staging environment before production"
echo "4. Set up monitoring and logging for production deployment"
echo ""
echo "For production deployment:"
echo "  ./gradlew bootBuildImage --imageName spring-boot-realworld-example-app:java17"
echo "  docker run -p 8080:8080 -e JWT_SECRET=\$JWT_SECRET spring-boot-realworld-example-app:java17"
