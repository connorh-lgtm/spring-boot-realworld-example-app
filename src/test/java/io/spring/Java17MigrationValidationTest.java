package io.spring;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Java17MigrationValidationTest {

  @Autowired private MyBatisUserRepository userRepository;
  @Autowired private MyBatisArticleRepository articleRepository;
  @Autowired private MyBatisCommentRepository commentRepository;
  @Autowired private ArticleQueryService articleQueryService;
  @Autowired private CommentQueryService commentQueryService;

  private User testUser;
  private Article testArticle;

  @BeforeEach
  public void setUp() {
    testUser = new User("test@example.com", "testuser", "123456", "Test Bio", "");
    userRepository.save(testUser);
    
    testArticle = new Article(
        "Test Article", 
        "Test Description", 
        "Test Body", 
        Arrays.asList("java", "spring"), 
        testUser.getId()
    );
    articleRepository.save(testArticle);
  }

  @Test
  public void shouldValidateJavaTimeInstantPersistence() {
    Instant beforeSave = Instant.now().minus(1, ChronoUnit.SECONDS);
    
    Article article = new Article(
        "Time Test Article",
        "Testing java.time persistence",
        "Body content",
        Arrays.asList("test"),
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
  }

  @Test
  public void shouldValidateCommentJavaTimePersistence() {
    Comment comment = new Comment("Test comment content", testUser.getId(), testArticle.getId());
    commentRepository.save(comment);
    
    Optional<Comment> retrieved = commentRepository.findById(testArticle.getId(), comment.getId());
    assertTrue(retrieved.isPresent());
    
    Comment savedComment = retrieved.get();
    assertNotNull(savedComment.getCreatedAt());
    assertTrue(savedComment.getCreatedAt().isBefore(Instant.now().plus(1, ChronoUnit.SECONDS)));
  }

  @Test
  public void shouldValidateArticleDataJavaTimeSerialization() {
    Optional<ArticleData> articleData = articleQueryService.findById(testArticle.getId(), testUser);
    assertTrue(articleData.isPresent());
    
    ArticleData data = articleData.get();
    assertNotNull(data.getCreatedAt());
    assertNotNull(data.getUpdatedAt());
    assertTrue(data.getCreatedAt() instanceof Instant);
    assertTrue(data.getUpdatedAt() instanceof Instant);
  }

  @Test
  public void shouldValidateCommentDataJavaTimeSerialization() {
    Comment comment = new Comment("Test comment", testUser.getId(), testArticle.getId());
    commentRepository.save(comment);
    
    Optional<CommentData> commentData = commentQueryService.findById(comment.getId(), testUser);
    assertTrue(commentData.isPresent());
    
    CommentData data = commentData.get();
    assertNotNull(data.getCreatedAt());
    assertTrue(data.getCreatedAt() instanceof Instant);
  }

  @Test
  public void shouldValidateJava17RuntimeEnvironment() {
    String javaVersion = System.getProperty("java.version");
    assertTrue(javaVersion.startsWith("17"), 
        "Expected Java 17, but running on: " + javaVersion);
    
    String javaVendor = System.getProperty("java.vendor");
    assertNotNull(javaVendor);
    
    String javaHome = System.getProperty("java.home");
    assertTrue(javaHome.contains("17") || javaHome.contains("java-17"), 
        "Java home should indicate Java 17: " + javaHome);
  }

  @Test
  public void shouldValidateInstantComparison() {
    Instant now = Instant.now();
    Instant future = now.plus(1, ChronoUnit.HOURS);
    Instant past = now.minus(1, ChronoUnit.HOURS);
    
    assertTrue(future.isAfter(now));
    assertTrue(past.isBefore(now));
    assertEquals(1, ChronoUnit.HOURS.between(now, future));
    assertEquals(-1, ChronoUnit.HOURS.between(now, past));
  }

  @Test
  public void shouldValidateInstantPrecision() {
    Instant instant1 = Instant.now();
    Instant instant2 = Instant.ofEpochMilli(instant1.toEpochMilli());
    
    assertEquals(instant1.toEpochMilli(), instant2.toEpochMilli());
    
    Instant nanoInstant = Instant.ofEpochSecond(instant1.getEpochSecond(), instant1.getNano());
    assertEquals(instant1, nanoInstant);
  }
}
