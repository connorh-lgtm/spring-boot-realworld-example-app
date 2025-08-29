#!/bin/bash

set -e

echo "🚀 Java 17 Optimized Application Startup Script"
echo "==============================================="

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        exit 1
    fi
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

PROFILE=${1:-production}
PORT=${2:-8080}
JAR_FILE="build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar"

echo ""
echo "Configuration:"
echo "  Profile: ${PROFILE}"
echo "  Port: ${PORT}"
echo "  JAR: ${JAR_FILE}"
echo ""

if [ ! -f "${JAR_FILE}" ]; then
    print_status 1 "JAR file not found: ${JAR_FILE}"
fi

case "${PROFILE}" in
    "development")
        print_info "Using development optimization profile"
        JVM_OPTS="-Xms256m -Xmx1g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
        JVM_OPTS="${JVM_OPTS} -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=8m"
        JVM_OPTS="${JVM_OPTS} -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseCompressedOops"
        JVM_OPTS="${JVM_OPTS} -XX:+FlightRecorder -XX:+UnlockDiagnosticVMOptions"
        SPRING_PROFILE="default"
        ;;
    
    "production")
        print_info "Using production optimization profile"
        JVM_OPTS="-Xms512m -Xmx2g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
        JVM_OPTS="${JVM_OPTS} -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:G1HeapRegionSize=16m"
        JVM_OPTS="${JVM_OPTS} -XX:+UseStringDeduplication -XX:+OptimizeStringConcat"
        JVM_OPTS="${JVM_OPTS} -XX:+UseCompressedOops -XX:+UseCompressedClassPointers"
        JVM_OPTS="${JVM_OPTS} -XX:+TieredCompilation -Xshare:on"
        JVM_OPTS="${JVM_OPTS} -XX:+FlightRecorder -XX:+HeapDumpOnOutOfMemoryError"
        JVM_OPTS="${JVM_OPTS} -XX:HeapDumpPath=./logs/ -Xlog:gc*:gc.log:time,tags"
        SPRING_PROFILE="performance"
        ;;
    
    "performance-testing")
        print_info "Using performance testing optimization profile"
        JVM_OPTS="-Xms512m -Xmx2g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
        JVM_OPTS="${JVM_OPTS} -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:G1HeapRegionSize=16m"
        JVM_OPTS="${JVM_OPTS} -XX:+UseStringDeduplication -XX:+UseCompressedOops"
        JVM_OPTS="${JVM_OPTS} -XX:+UseCompressedClassPointers -XX:+TieredCompilation"
        JVM_OPTS="${JVM_OPTS} -XX:+FlightRecorder -XX:StartFlightRecording=duration=600s,filename=performance-test.jfr"
        JVM_OPTS="${JVM_OPTS} -XX:+UnlockDiagnosticVMOptions -XX:+HeapDumpOnOutOfMemoryError"
        JVM_OPTS="${JVM_OPTS} -Xlog:gc*:gc-performance.log:time,tags"
        SPRING_PROFILE="performance"
        ;;
    
    "low-latency")
        print_info "Using low-latency optimization profile (ZGC)"
        JVM_OPTS="-Xms1g -Xmx4g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
        JVM_OPTS="${JVM_OPTS} -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
        JVM_OPTS="${JVM_OPTS} -XX:+UseCompressedOops -XX:+UseCompressedClassPointers"
        JVM_OPTS="${JVM_OPTS} -XX:+TieredCompilation -XX:+OptimizeStringConcat"
        JVM_OPTS="${JVM_OPTS} -XX:+FlightRecorder -Xlog:gc*:gc-lowlatency.log:time,tags"
        SPRING_PROFILE="performance"
        ;;
    
    "memory-constrained")
        print_info "Using memory-constrained optimization profile"
        JVM_OPTS="-Xms128m -Xmx512m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m"
        JVM_OPTS="${JVM_OPTS} -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=4m"
        JVM_OPTS="${JVM_OPTS} -XX:+UseStringDeduplication -XX:+UseCompressedOops"
        JVM_OPTS="${JVM_OPTS} -XX:+UseCompressedClassPointers -XX:+OptimizeStringConcat"
        JVM_OPTS="${JVM_OPTS} -XX:+FlightRecorder -Xlog:gc:gc-memory.log"
        SPRING_PROFILE="default"
        ;;
    
    *)
        print_status 1 "Unknown profile: ${PROFILE}"
        echo "Available profiles: development, production, performance-testing, low-latency, memory-constrained"
        exit 1
        ;;
esac

mkdir -p logs

echo ""
echo "JVM Options:"
echo "${JVM_OPTS}" | tr ' ' '\n' | sed 's/^/  /'
echo ""

print_info "Checking Java version..."
java -version 2>&1 | head -1
if ! java -version 2>&1 | grep -q "17\."; then
    print_warning "Java 17 not detected. Performance optimizations may not work as expected."
fi

print_info "Starting application with optimized settings..."
echo ""

JAVA_CMD="java ${JVM_OPTS} -jar ${JAR_FILE}"
JAVA_CMD="${JAVA_CMD} --spring.profiles.active=${SPRING_PROFILE}"
JAVA_CMD="${JAVA_CMD} --server.port=${PORT}"

if [ "${PROFILE}" = "production" ]; then
    if [ -n "${JWT_SECRET}" ]; then
        JAVA_CMD="${JAVA_CMD} --jwt.secret=${JWT_SECRET}"
    else
        print_warning "JWT_SECRET environment variable not set for production"
    fi
fi

echo "Executing: ${JAVA_CMD}"
echo ""

exec ${JAVA_CMD}
