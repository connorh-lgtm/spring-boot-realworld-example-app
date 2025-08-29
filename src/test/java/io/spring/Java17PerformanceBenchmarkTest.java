package io.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.root=WARN",
    "logging.level.io.spring=WARN"
})
@Tag("performance")
public class Java17PerformanceBenchmarkTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void testApplicationStartupTime() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long startupTime = runtimeBean.getUptime();
        
        assertThat(startupTime).isLessThan(10000);
        
        System.out.println("Application startup time: " + startupTime + "ms");
    }

    @Test
    public void testMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        assertThat(heapUsed).isLessThan(heapMax);
        assertThat(heapUsed).isGreaterThan(0);
        
        System.out.println("Heap memory used: " + (heapUsed / 1024 / 1024) + "MB");
        System.out.println("Heap memory max: " + (heapMax / 1024 / 1024) + "MB");
        System.out.println("Non-heap memory used: " + (nonHeapUsed / 1024 / 1024) + "MB");
        
        assertThat(heapUsed).isLessThan(500 * 1024 * 1024);
    }

    @Test
    public void testGarbageCollectionPerformance() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        long totalCollections = 0;
        long totalCollectionTime = 0;
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long collections = gcBean.getCollectionCount();
            long collectionTime = gcBean.getCollectionTime();
            
            if (collections > 0) {
                totalCollections += collections;
                totalCollectionTime += collectionTime;
                
                System.out.println(gcBean.getName() + " - Collections: " + collections + 
                                 ", Time: " + collectionTime + "ms");
            }
        }
        
        System.out.println("Total GC collections: " + totalCollections);
        System.out.println("Total GC time: " + totalCollectionTime + "ms");
        
        assertThat(totalCollectionTime).isLessThan(1000);
    }

    @Test
    public void testResponseTimePerformance() {
        String url = "http://localhost:" + port + "/tags";
        
        for (int i = 0; i < 5; i++) {
            restTemplate.getForEntity(url, String.class);
        }
        
        Instant start = Instant.now();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        Instant end = Instant.now();
        
        Duration responseTime = Duration.between(start, end);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(responseTime.toMillis()).isLessThan(1000);
        
        System.out.println("Response time: " + responseTime.toMillis() + "ms");
    }

    @Test
    public void testThroughputPerformance() {
        String url = "http://localhost:" + port + "/tags";
        int numberOfRequests = 100;
        
        for (int i = 0; i < 10; i++) {
            restTemplate.getForEntity(url, String.class);
        }
        
        Instant start = Instant.now();
        
        for (int i = 0; i < numberOfRequests; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
        
        Instant end = Instant.now();
        Duration totalTime = Duration.between(start, end);
        
        double requestsPerSecond = numberOfRequests / (totalTime.toMillis() / 1000.0);
        
        System.out.println("Throughput: " + String.format("%.2f", requestsPerSecond) + " requests/second");
        System.out.println("Total time for " + numberOfRequests + " requests: " + totalTime.toMillis() + "ms");
        
        assertThat(requestsPerSecond).isGreaterThan(50.0);
    }

    @Test
    public void testJava17Features() {
        String javaVersion = System.getProperty("java.version");
        assertThat(javaVersion).startsWith("17");
        
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        boolean hasG1GC = gcBeans.stream()
            .anyMatch(gc -> gc.getName().contains("G1"));
        
        System.out.println("Java version: " + javaVersion);
        System.out.println("Available GC algorithms: ");
        gcBeans.forEach(gc -> System.out.println("  - " + gc.getName()));
        
        assertThat(hasG1GC).isTrue();
    }

    @Test
    public void testStringDeduplicationCompatibility() {
        String str1 = new String("test-string-deduplication");
        String str2 = new String("test-string-deduplication");
        
        assertThat(str1).isEqualTo(str2);
        assertThat(str1).isNotSameAs(str2);
        
        String intern1 = str1.intern();
        String intern2 = str2.intern();
        assertThat(intern1).isSameAs(intern2);
        
        System.out.println("String deduplication test passed");
    }
}
