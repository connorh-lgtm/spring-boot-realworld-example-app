package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class ArticleTest {

  @Test
  public void should_get_right_slug() {
    Article article = new Article("a new   title", "desc", "body", Arrays.asList("java"), "123");
    assertTrue(article.getSlug().startsWith("a-new-title-"));
  }

  @Test
  public void should_get_right_slug_with_number_in_title() {
    Article article = new Article("a new title 2", "desc", "body", Arrays.asList("java"), "123");
    assertTrue(article.getSlug().startsWith("a-new-title-2-"));
  }

  @Test
  public void should_get_lower_case_slug() {
    Article article = new Article("A NEW TITLE", "desc", "body", Arrays.asList("java"), "123");
    assertTrue(article.getSlug().startsWith("a-new-title-"));
  }

  @Test
  public void should_handle_other_language() {
    Article article = new Article("中文：标题", "desc", "body", Arrays.asList("java"), "123");
    assertTrue(article.getSlug().startsWith("中文-标题-"));
  }

  @Test
  public void should_handle_commas() {
    Article article = new Article("what?the.hell,w", "desc", "body", Arrays.asList("java"), "123");
    assertTrue(article.getSlug().startsWith("what-the-hell-w-"));
  }

  @Test
  public void should_produce_unique_slugs_for_same_title() {
    Article article1 = new Article("same title", "desc", "body", Arrays.asList("java"), "123");
    Article article2 = new Article("same title", "desc", "body", Arrays.asList("java"), "123");
    assertTrue(!article1.getSlug().equals(article2.getSlug()),
        "Two articles with the same title should have different slugs");
  }

  @Test
  public void should_preserve_base_slug_without_id() {
    assertThat(Article.toSlug("A New Title"), is("a-new-title"));
  }
}
