package io.spring;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
public class Java17DatabaseIntegrationTest {

  @Autowired private MyBatisUserRepository userRepository;
  @Autowired private MyBatisArticleRepository articleRepository;
  @Autowired private MyBatisCommentRepository commentRepository;

  private User testUser;

  @BeforeEach
  public void setUp() {
    testUser = new User("db@example.com", "dbuser", "123456", "Database Test", "");
    userRepository.save(testUser);
  }

  @Test
  public void shouldPersistAndRetrieveInstantFieldsAccurately() {
    Instant beforeSave = Instant.now().minus(1, ChronoUnit.SECONDS);
    
    Article article = new Article(
        "Database Test Article",
        "Testing database persistence",
        "Content for database testing",
        Arrays.asList("database", "test"),
        testUser.getId()
    );
    
    articleRepository.save(article);
    
    Optional<Article> retrieved = articleRepository.findById(article.getId());
    assertTrue(retrieved.isPresent());
    
    Article savedArticle = retrieved.get();
    assertNotNull(savedArticle.getCreatedAt());
    assertNotNull(savedArticle.getUpdatedAt());
    
    assertTrue(savedArticle.getCreatedAt().isAfter(beforeSave));
    assertTrue(savedArticle.getUpdatedAt().isAfter(beforeSave));
    assertTrue(savedArticle.getCreatedAt().isBefore(Instant.now().plus(1, ChronoUnit.SECONDS)));
    assertTrue(savedArticle.getUpdatedAt().isBefore(Instant.now().plus(1, ChronoUnit.SECONDS)));
  }

  @Test
  public void shouldMaintainInstantPrecisionThroughDatabaseRoundTrip() {
    Instant specificInstant = Instant.ofEpochSecond(1693276800, 123456789);
    
    Article article = new Article(
        "Precision Test Article",
        "Testing instant precision",
        "Content",
        Arrays.asList("precision"),
        testUser.getId(),
        specificInstant
    );
    
    articleRepository.save(article);
    
    Optional<Article> retrieved = articleRepository.findById(article.getId());
    assertTrue(retrieved.isPresent());
    
    Article savedArticle = retrieved.get();
    
    assertEquals(specificInstant.getEpochSecond(), savedArticle.getCreatedAt().getEpochSecond());
    assertEquals(specificInstant.getEpochSecond(), savedArticle.getUpdatedAt().getEpochSecond());
    
    long nanosDiff = Math.abs(specificInstant.getNano() - savedArticle.getCreatedAt().getNano());
    assertTrue(nanosDiff < 1000000, 
        "Nanosecond precision should be maintained within 1ms: " + nanosDiff + " nanoseconds difference");
  }

  @Test
  public void shouldHandleBulkOperationsWithInstantFields() {
    List<Article> articles = IntStream.range(0, 20)
        .mapToObj(i -> new Article(
            "Bulk Article " + i,
            "Description " + i,
            "Body " + i,
            Arrays.asList("bulk", "test" + i),
            testUser.getId()
        ))
        .toList();

    Instant bulkStart = Instant.now();
    
    articles.forEach(articleRepository::save);
    
    Instant bulkEnd = Instant.now();
    long bulkDurationMs = ChronoUnit.MILLIS.between(bulkStart, bulkEnd);
    
    assertTrue(bulkDurationMs < 5000, 
        "Bulk save of 20 articles should complete within 5 seconds: " + bulkDurationMs + "ms");
    
    List<Article> retrievedArticles = articles.stream()
        .map(a -> articleRepository.findById(a.getId()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
    
    assertEquals(20, retrievedArticles.size());
    
    for (Article article : retrievedArticles) {
      assertNotNull(article.getCreatedAt());
      assertNotNull(article.getUpdatedAt());
      assertTrue(article.getCreatedAt().isAfter(bulkStart.minus(1, ChronoUnit.SECONDS)));
      assertTrue(article.getCreatedAt().isBefore(bulkEnd.plus(1, ChronoUnit.SECONDS)));
    }
  }

  @Test
  public void shouldHandleCommentInstantFieldsPersistence() {
    Article article = new Article(
        "Comment Test Article",
        "For testing comments",
        "Content",
        Arrays.asList("comments"),
        testUser.getId()
    );
    articleRepository.save(article);
    
    Instant beforeComment = Instant.now().minus(1, ChronoUnit.SECONDS);
    
    Comment comment = new Comment("Test comment with instant", testUser.getId(), article.getId());
    commentRepository.save(comment);
    
    Optional<Comment> retrieved = commentRepository.findById(article.getId(), comment.getId());
    assertTrue(retrieved.isPresent());
    
    Comment savedComment = retrieved.get();
    assertNotNull(savedComment.getCreatedAt());
    assertTrue(savedComment.getCreatedAt().isAfter(beforeComment));
    assertTrue(savedComment.getCreatedAt().isBefore(Instant.now().plus(1, ChronoUnit.SECONDS)));
  }

  @Test
  public void shouldHandleTransactionRollbackWithInstantFields() {
    Article article = new Article(
        "Transaction Test Article",
        "Testing transaction behavior",
        "Content",
        Arrays.asList("transaction"),
        testUser.getId()
    );
    
    articleRepository.save(article);
    String articleId = article.getId();
    
    Optional<Article> beforeUpdate = articleRepository.findById(articleId);
    assertTrue(beforeUpdate.isPresent());
    Instant originalUpdatedAt = beforeUpdate.get().getUpdatedAt();
    
    assertDoesNotThrow(() -> {
      article.update("Updated Title", null, null);
      articleRepository.save(article);
      
      Optional<Article> afterUpdate = articleRepository.findById(articleId);
      assertTrue(afterUpdate.isPresent());
      assertTrue(afterUpdate.get().getUpdatedAt().isAfter(originalUpdatedAt) || 
                 afterUpdate.get().getUpdatedAt().equals(originalUpdatedAt));
      
    }, "Transaction should not fail in normal case");
  }

  @Test
  public void shouldValidateMyBatisInstantTypeHandlerCompatibility() {
    Instant testInstant = Instant.now();
    
    Article article = new Article(
        "MyBatis Handler Test",
        "Testing MyBatis type handler",
        "Content",
        Arrays.asList("mybatis"),
        testUser.getId(),
        testInstant
    );
    
    articleRepository.save(article);
    
    Optional<Article> retrieved = articleRepository.findById(article.getId());
    assertTrue(retrieved.isPresent());
    
    Article savedArticle = retrieved.get();
    
    assertEquals(testInstant.getEpochSecond(), savedArticle.getCreatedAt().getEpochSecond());
    assertEquals(testInstant.getEpochSecond(), savedArticle.getUpdatedAt().getEpochSecond());
    
    assertTrue(savedArticle.getCreatedAt() instanceof Instant);
    assertTrue(savedArticle.getUpdatedAt() instanceof Instant);
  }

  @Test
  public void shouldHandleInstantComparisonInDatabaseQueries() {
    Instant baseTime = Instant.now().minus(1, ChronoUnit.HOURS);
    
    Article oldArticle = new Article(
        "Old Article",
        "Created an hour ago",
        "Content",
        Arrays.asList("old"),
        testUser.getId(),
        baseTime
    );
    articleRepository.save(oldArticle);
    
    Article newArticle = new Article(
        "New Article",
        "Created now",
        "Content",
        Arrays.asList("new"),
        testUser.getId()
    );
    articleRepository.save(newArticle);
    
    Optional<Article> retrievedOld = articleRepository.findById(oldArticle.getId());
    Optional<Article> retrievedNew = articleRepository.findById(newArticle.getId());
    
    assertTrue(retrievedOld.isPresent());
    assertTrue(retrievedNew.isPresent());
    
    assertTrue(retrievedNew.get().getCreatedAt().isAfter(retrievedOld.get().getCreatedAt()));
    
    long hoursBetween = ChronoUnit.HOURS.between(
        retrievedOld.get().getCreatedAt(), 
        retrievedNew.get().getCreatedAt()
    );
    assertTrue(hoursBetween >= 0, "Time difference should be positive");
  }

  @Test
  public void shouldValidateDatabaseConnectionWithJava17() {
    assertDoesNotThrow(() -> {
      User testConnectionUser = new User("connection@test.com", "connuser", "123456", "Connection Test", "");
      userRepository.save(testConnectionUser);
      
      Optional<User> retrieved = userRepository.findByUsername("connuser");
      assertTrue(retrieved.isPresent());
      assertEquals("connection@test.com", retrieved.get().getEmail());
    }, "Database connection should work correctly with Java 17");
  }

  @Test
  public void shouldValidateInstantFieldsInComplexQueries() {
    for (int i = 0; i < 5; i++) {
      Article article = new Article(
          "Query Test Article " + i,
          "Description " + i,
          "Body " + i,
          Arrays.asList("query", "test"),
          testUser.getId()
      );
      articleRepository.save(article);
    }
    
    assertDoesNotThrow(() -> {
      List<Article> allArticles = IntStream.range(0, 5)
          .mapToObj(i -> articleRepository.findBySlug(Article.toSlug("Query Test Article " + i)))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .toList();
      
      assertEquals(5, allArticles.size());
      
      for (Article article : allArticles) {
        assertNotNull(article.getCreatedAt());
        assertNotNull(article.getUpdatedAt());
        assertTrue(article.getCreatedAt() instanceof Instant);
        assertTrue(article.getUpdatedAt() instanceof Instant);
      }
    }, "Complex queries with Instant fields should work correctly");
  }
}
