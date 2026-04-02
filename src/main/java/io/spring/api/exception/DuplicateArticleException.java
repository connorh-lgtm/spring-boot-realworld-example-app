package io.spring.api.exception;

@SuppressWarnings("serial")
public class DuplicateArticleException extends RuntimeException {
  public DuplicateArticleException(String message, Throwable cause) {
    super(message, cause);
  }
}
