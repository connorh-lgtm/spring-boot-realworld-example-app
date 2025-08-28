package io.spring;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.user.User;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Java17PerformanceValidationTest {

  @Autowired private MyBatisUserRepository userRepository;
  @Autowired private MyBatisArticleRepository articleRepository;

  private User testUser;

  @BeforeEach
  public void setUp() {
    testUser = new User("perf@example.com", "perfuser", "123456", "Performance Test", "");
    userRepository.save(testUser);
  }

  @Test
  public void shouldValidateInstantCreationPerformance() {
    int iterations = 10000;
    
    Instant start = Instant.now();
    
    for (int i = 0; i < iterations; i++) {
      Instant.now();
    }
    
    Duration elapsed = Duration.between(start, Instant.now());
    
    assertTrue(elapsed.toMillis() < 1000, 
        "Instant.now() performance degraded: " + elapsed.toMillis() + "ms for " + iterations + " iterations");
  }

  @Test
  public void shouldValidateStreamProcessingWithInstant() {
    List<Article> articles = IntStream.range(0, 100)
        .mapToObj(i -> new Article(
            "Article " + i,
            "Description " + i,
            "Body " + i,
            Arrays.asList("tag" + i),
            testUser.getId()
        ))
        .toList();

    Instant start = Instant.now();
    
    List<Instant> creationTimes = articles.stream()
        .map(Article::getCreatedAt)
        .sorted()
        .toList();
    
    Duration elapsed = Duration.between(start, Instant.now());
    
    assertEquals(100, creationTimes.size());
    assertTrue(elapsed.toMillis() < 100, 
        "Stream processing with Instant too slow: " + elapsed.toMillis() + "ms");
  }

  @Test
  public void shouldValidateBulkArticleCreationPerformance() {
    int articleCount = 50;
    
    Instant start = Instant.now();
    
    for (int i = 0; i < articleCount; i++) {
      Article article = new Article(
          "Bulk Article " + i,
          "Description " + i,
          "Body content " + i,
          Arrays.asList("bulk", "test"),
          testUser.getId()
      );
      articleRepository.save(article);
    }
    
    Duration elapsed = Duration.between(start, Instant.now());
    
    assertTrue(elapsed.toSeconds() < 5, 
        "Bulk article creation too slow: " + elapsed.toSeconds() + " seconds for " + articleCount + " articles");
  }

  @Test
  public void shouldValidateMemoryUsageWithInstant() {
    Runtime runtime = Runtime.getRuntime();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    
    List<Instant> instants = IntStream.range(0, 1000)
        .mapToObj(i -> Instant.now())
        .toList();
    
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = finalMemory - initialMemory;
    
    assertEquals(1000, instants.size());
    assertTrue(memoryUsed < 1024 * 1024, 
        "Excessive memory usage for Instant objects: " + memoryUsed + " bytes");
  }

  @Test
  public void shouldValidateJava17StringProcessingPerformance() {
    String testString = "Java 17 Performance Test String with Multiple Words";
    int iterations = 10000;
    
    Instant start = Instant.now();
    
    for (int i = 0; i < iterations; i++) {
      String processed = testString.toLowerCase()
          .replace(" ", "-")
          .substring(0, Math.min(20, testString.length()));
      assertNotNull(processed);
    }
    
    Duration elapsed = Duration.between(start, Instant.now());
    
    assertTrue(elapsed.toMillis() < 500, 
        "String processing performance degraded: " + elapsed.toMillis() + "ms");
  }
}
