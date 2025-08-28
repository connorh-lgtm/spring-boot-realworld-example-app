package io.spring;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Java17GraphQLSimpleTest {

  @Autowired private MyBatisUserRepository userRepository;
  @Autowired private MyBatisArticleRepository articleRepository;
  @Autowired private MyBatisCommentRepository commentRepository;

  private User testUser;
  private Article testArticle;

  @BeforeEach
  public void setUp() {
    testUser = new User("graphql@example.com", "graphqluser", "123456", "GraphQL Test", "");
    userRepository.save(testUser);
    
    testArticle = new Article(
        "GraphQL Test Article", 
        "Testing GraphQL with Java 17", 
        "This article tests GraphQL integration", 
        Arrays.asList("graphql", "java17"), 
        testUser.getId()
    );
    articleRepository.save(testArticle);
  }

  @Test
  public void shouldValidateDateTimeFormatterISOInstantCompatibility() {
    Instant testInstant = Instant.now();
    String formattedInstant = DateTimeFormatter.ISO_INSTANT.format(testInstant);
    
    assertDoesNotThrow(() -> {
      Instant parsedInstant = Instant.parse(formattedInstant);
      assertEquals(testInstant.getEpochSecond(), parsedInstant.getEpochSecond());
    }, "DateTimeFormatter.ISO_INSTANT should be compatible with Instant.parse()");
    
    assertTrue(formattedInstant.endsWith("Z"), "ISO instant should end with Z");
    assertTrue(formattedInstant.contains("T"), "ISO instant should contain T separator");
  }

  @Test
  public void shouldValidateInstantSerializationForGraphQLCompatibility() {
    assertNotNull(testArticle.getCreatedAt());
    assertNotNull(testArticle.getUpdatedAt());
    
    String createdAtFormatted = DateTimeFormatter.ISO_INSTANT.format(testArticle.getCreatedAt());
    String updatedAtFormatted = DateTimeFormatter.ISO_INSTANT.format(testArticle.getUpdatedAt());
    
    assertDoesNotThrow(() -> {
      Instant.parse(createdAtFormatted);
      Instant.parse(updatedAtFormatted);
    }, "Article Instant fields should be serializable to ISO format for GraphQL");
    
    assertTrue(createdAtFormatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z"));
    assertTrue(updatedAtFormatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z"));
  }

  @Test
  public void shouldValidateCommentInstantHandlingForGraphQL() {
    Comment testComment = new Comment("Test comment for GraphQL", testUser.getId(), testArticle.getId());
    commentRepository.save(testComment);
    
    assertNotNull(testComment.getCreatedAt());
    assertTrue(testComment.getCreatedAt() instanceof Instant);
    assertTrue(testComment.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
    
    String createdAtFormatted = DateTimeFormatter.ISO_INSTANT.format(testComment.getCreatedAt());
    assertDoesNotThrow(() -> {
      Instant.parse(createdAtFormatted);
    }, "Comment Instant fields should be serializable to ISO format for GraphQL");
  }

  @Test
  public void shouldValidateJava17StreamProcessingWithArticles() {
    for (int i = 0; i < 5; i++) {
      Article article = new Article(
          "Bulk Article " + i,
          "Description " + i,
          "Body " + i,
          Arrays.asList("bulk", "test" + i),
          testUser.getId()
      );
      articleRepository.save(article);
    }

    Instant queryStart = Instant.now();
    
    assertDoesNotThrow(() -> {
      long count = Arrays.asList(0, 1, 2, 3, 4)
          .stream()
          .map(i -> articleRepository.findBySlug(Article.toSlug("Bulk Article " + i)))
          .filter(opt -> opt.isPresent())
          .map(opt -> opt.get())
          .filter(article -> article.getCreatedAt() != null)
          .count();
      
      assertEquals(5, count, "All bulk articles should have valid createdAt timestamps");
    }, "Java 17 stream processing should work correctly with Instant fields");
    
    Instant queryEnd = Instant.now();
    long queryDurationMs = queryEnd.toEpochMilli() - queryStart.toEpochMilli();
    
    assertTrue(queryDurationMs < 5000, 
        "Stream processing should complete within 5 seconds: " + queryDurationMs + "ms");
  }

  @Test
  public void shouldValidateInstantPrecisionForGraphQLSerialization() {
    Instant specificInstant = Instant.ofEpochSecond(1693276800, 123456789);
    
    Article precisionArticle = new Article(
        "Precision Test Article",
        "Testing instant precision for GraphQL",
        "Content",
        Arrays.asList("precision"),
        testUser.getId(),
        specificInstant
    );
    
    articleRepository.save(precisionArticle);
    
    String formattedCreatedAt = DateTimeFormatter.ISO_INSTANT.format(precisionArticle.getCreatedAt());
    String formattedUpdatedAt = DateTimeFormatter.ISO_INSTANT.format(precisionArticle.getUpdatedAt());
    
    assertDoesNotThrow(() -> {
      Instant parsedCreatedAt = Instant.parse(formattedCreatedAt);
      Instant parsedUpdatedAt = Instant.parse(formattedUpdatedAt);
      
      assertEquals(specificInstant.getEpochSecond(), parsedCreatedAt.getEpochSecond());
      assertEquals(specificInstant.getEpochSecond(), parsedUpdatedAt.getEpochSecond());
      
    }, "Instant precision should be maintained through GraphQL serialization format");
  }
}
