#!/bin/bash

set -e

echo "🔍 Java 17 Performance Monitoring and Profiling Script"
echo "======================================================"

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

mkdir -p logs/performance
mkdir -p logs/jfr
mkdir -p logs/gc
mkdir -p logs/heap-dumps

echo ""
echo "1. Java Flight Recorder (JFR) Profiling"
echo "========================================"

start_jfr_profiling() {
    local duration=${1:-60}
    local filename="logs/jfr/app-profile-$(date +%Y%m%d_%H%M%S).jfr"
    
    print_info "Starting JFR profiling for ${duration} seconds..."
    print_info "Output file: ${filename}"
    
    java -XX:+FlightRecorder \
         -XX:StartFlightRecording=duration=${duration}s,filename=${filename} \
         -XX:FlightRecorderOptions=settings=profile \
         -jar build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar \
         --spring.profiles.active=performance > logs/performance/jfr-startup.log 2>&1 &
    
    local app_pid=$!
    echo "Application PID: ${app_pid}"
    
    sleep 10
    
    if curl -f http://localhost:8080/tags > /dev/null 2>&1; then
        print_status 0 "Application started successfully"
    else
        print_status 1 "Application failed to start"
    fi
    
    print_info "Waiting for profiling to complete..."
    sleep $((duration - 10))
    
    kill $app_pid 2>/dev/null || true
    wait $app_pid 2>/dev/null || true
    
    print_status 0 "JFR profiling completed: ${filename}"
    
    if command -v jfr &> /dev/null; then
        print_info "Analyzing JFR file..."
        jfr summary ${filename} > logs/performance/jfr-summary-$(date +%Y%m%d_%H%M%S).txt
        print_status 0 "JFR analysis completed"
    else
        print_warning "JFR command not available. Install JDK tools for analysis."
    fi
}

echo ""
echo "2. GC Performance Monitoring"
echo "============================"

start_gc_monitoring() {
    local duration=${1:-60}
    local gc_log="logs/gc/gc-performance-$(date +%Y%m%d_%H%M%S).log"
    
    print_info "Starting GC monitoring for ${duration} seconds..."
    print_info "GC log file: ${gc_log}"
    
    java -Xlog:gc*:${gc_log}:time,tags \
         -XX:+UseG1GC \
         -XX:+UseStringDeduplication \
         -XX:MaxGCPauseMillis=100 \
         -jar build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar \
         --spring.profiles.active=performance > logs/performance/gc-startup.log 2>&1 &
    
    local app_pid=$!
    echo "Application PID: ${app_pid}"
    
    sleep 10
    
    print_info "Generating load to trigger garbage collection..."
    for i in {1..100}; do
        curl -s http://localhost:8080/tags > /dev/null 2>&1 || true
        sleep 0.1
    done
    
    sleep $((duration - 20))
    
    kill $app_pid 2>/dev/null || true
    wait $app_pid 2>/dev/null || true
    
    print_status 0 "GC monitoring completed: ${gc_log}"
    
    print_info "Analyzing GC performance..."
    analyze_gc_log ${gc_log}
}

analyze_gc_log() {
    local gc_log=$1
    local analysis_file="logs/performance/gc-analysis-$(date +%Y%m%d_%H%M%S).txt"
    
    {
        echo "GC Performance Analysis"
        echo "======================"
        echo "Log file: ${gc_log}"
        echo "Analysis time: $(date)"
        echo ""
        
        echo "GC Events Summary:"
        grep -c "GC(" ${gc_log} 2>/dev/null || echo "0"
        echo ""
        
        echo "Pause Times (ms):"
        grep "GC(" ${gc_log} | grep -o '[0-9]\+\.[0-9]\+ms' | sort -n | tail -10
        echo ""
        
        echo "Memory Usage Patterns:"
        grep "GC(" ${gc_log} | grep -o '[0-9]\+M->[0-9]\+M' | tail -10
        echo ""
        
    } > ${analysis_file}
    
    print_status 0 "GC analysis completed: ${analysis_file}"
}

echo ""
echo "3. Memory Usage Monitoring"
echo "========================="

start_memory_monitoring() {
    local duration=${1:-60}
    local memory_log="logs/performance/memory-$(date +%Y%m%d_%H%M%S).log"
    
    print_info "Starting memory monitoring for ${duration} seconds..."
    
    java -XX:+HeapDumpOnOutOfMemoryError \
         -XX:HeapDumpPath=logs/heap-dumps/ \
         -Xms256m -Xmx1g \
         -jar build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar \
         --spring.profiles.active=performance > logs/performance/memory-startup.log 2>&1 &
    
    local app_pid=$!
    echo "Application PID: ${app_pid}"
    
    sleep 10
    
    {
        echo "Memory Usage Monitoring"
        echo "======================"
        echo "PID: ${app_pid}"
        echo "Start time: $(date)"
        echo ""
    } > ${memory_log}
    
    for i in $(seq 1 $((duration / 5))); do
        if ps -p ${app_pid} > /dev/null 2>&1; then
            {
                echo "Time: $(date)"
                ps -p ${app_pid} -o pid,vsz,rss,pmem,pcpu,time
                echo ""
            } >> ${memory_log}
        fi
        sleep 5
    done
    
    kill $app_pid 2>/dev/null || true
    wait $app_pid 2>/dev/null || true
    
    print_status 0 "Memory monitoring completed: ${memory_log}"
}

echo ""
echo "4. Performance Benchmarking"
echo "==========================="

run_performance_benchmark() {
    print_info "Running performance benchmark tests..."
    
    ./gradlew test --tests "*PerformanceBenchmark*" > logs/performance/benchmark-$(date +%Y%m%d_%H%M%S).log 2>&1
    
    if [ $? -eq 0 ]; then
        print_status 0 "Performance benchmarks completed successfully"
    else
        print_status 1 "Performance benchmarks failed"
    fi
}

echo ""
echo "5. Startup Time Analysis"
echo "======================="

measure_startup_time() {
    local iterations=${1:-5}
    local startup_log="logs/performance/startup-times-$(date +%Y%m%d_%H%M%S).log"
    
    print_info "Measuring startup time over ${iterations} iterations..."
    
    {
        echo "Startup Time Analysis"
        echo "===================="
        echo "Iterations: ${iterations}"
        echo "Date: $(date)"
        echo ""
    } > ${startup_log}
    
    local total_time=0
    
    for i in $(seq 1 ${iterations}); do
        print_info "Iteration ${i}/${iterations}"
        
        local start_time=$(date +%s%N)
        
        java -jar build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar \
             --spring.profiles.active=performance \
             --server.port=808${i} > /dev/null 2>&1 &
        
        local app_pid=$!
        
        local ready=false
        local timeout=30
        local elapsed=0
        
        while [ ${elapsed} -lt ${timeout} ] && [ "${ready}" = "false" ]; do
            if curl -f http://localhost:808${i}/tags > /dev/null 2>&1; then
                ready=true
            else
                sleep 1
                elapsed=$((elapsed + 1))
            fi
        done
        
        local end_time=$(date +%s%N)
        local startup_time=$(( (end_time - start_time) / 1000000 ))
        
        if [ "${ready}" = "true" ]; then
            echo "Iteration ${i}: ${startup_time}ms" >> ${startup_log}
            total_time=$((total_time + startup_time))
            print_status 0 "Startup time: ${startup_time}ms"
        else
            echo "Iteration ${i}: TIMEOUT" >> ${startup_log}
            print_status 1 "Startup timeout"
        fi
        
        kill $app_pid 2>/dev/null || true
        wait $app_pid 2>/dev/null || true
        
        sleep 2
    done
    
    local avg_time=$((total_time / iterations))
    echo "" >> ${startup_log}
    echo "Average startup time: ${avg_time}ms" >> ${startup_log}
    
    print_status 0 "Average startup time: ${avg_time}ms"
    print_status 0 "Startup analysis completed: ${startup_log}"
}

echo ""
echo "Performance Monitoring Options:"
echo "1. JFR Profiling (60s)"
echo "2. GC Monitoring (60s)"
echo "3. Memory Monitoring (60s)"
echo "4. Performance Benchmarks"
echo "5. Startup Time Analysis (5 iterations)"
echo "6. Full Performance Suite"
echo ""

case "${1:-6}" in
    1)
        start_jfr_profiling ${2:-60}
        ;;
    2)
        start_gc_monitoring ${2:-60}
        ;;
    3)
        start_memory_monitoring ${2:-60}
        ;;
    4)
        run_performance_benchmark
        ;;
    5)
        measure_startup_time ${2:-5}
        ;;
    6)
        print_info "Running full performance monitoring suite..."
        run_performance_benchmark
        measure_startup_time 3
        start_gc_monitoring 30
        start_memory_monitoring 30
        print_status 0 "Full performance suite completed"
        ;;
    *)
        echo "Usage: $0 [1-6] [duration/iterations]"
        echo "  1: JFR Profiling"
        echo "  2: GC Monitoring"
        echo "  3: Memory Monitoring"
        echo "  4: Performance Benchmarks"
        echo "  5: Startup Time Analysis"
        echo "  6: Full Performance Suite (default)"
        exit 1
        ;;
esac

echo ""
echo "📊 Performance monitoring completed!"
echo "Check logs/performance/ for detailed results."
