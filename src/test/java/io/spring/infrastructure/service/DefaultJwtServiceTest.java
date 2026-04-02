package io.spring.infrastructure.service;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    jwtService =
        new DefaultJwtService(
            "123123123123123123123123123123123123123123123123123123123123", 3600, "default");
  }

  @Test
  public void should_generate_and_parse_token() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    Assertions.assertNotNull(token);
    Optional<String> optional = jwtService.getSubFromToken(token);
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get(), user.getId());
  }

  @Test
  public void should_get_null_with_wrong_jwt() {
    Optional<String> optional = jwtService.getSubFromToken("123");
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_get_null_with_expired_jwt() {
    String token =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhaXNlbnNpeSIsImV4cCI6MTUwMjE2MTIwNH0.SJB-U60WzxLYNomqLo4G3v3LzFxJKuVrIud8D8Lz3-mgpo9pN1i7C8ikU_jQPJGm8HsC1CquGMI-rSuM7j6LDA";
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_accept_valid_secret() {
    Assertions.assertDoesNotThrow(
        () ->
            new DefaultJwtService(
                "this-is-a-valid-secret-that-is-at-least-32-bytes-long", 3600, "default"));
  }

  @Test
  public void should_reject_short_secret() {
    IllegalStateException exception =
        Assertions.assertThrows(
            IllegalStateException.class, () -> new DefaultJwtService("short", 3600, "default"));
    Assertions.assertTrue(
        exception.getMessage().contains("at least 32 bytes"),
        "Exception message should mention minimum key length");
  }

  @Test
  public void should_reject_dev_default_in_production() {
    IllegalStateException exception =
        Assertions.assertThrows(
            IllegalStateException.class,
            () ->
                new DefaultJwtService(
                    "dev-only-default-secret-do-not-use-in-production-nRvyYC4soFxBdZ",
                    3600,
                    "production"));
    Assertions.assertTrue(
        exception.getMessage().contains("JWT_SECRET"),
        "Exception message should mention JWT_SECRET environment variable");
  }

  @Test
  public void should_reject_dev_default_in_performance_profile() {
    IllegalStateException exception =
        Assertions.assertThrows(
            IllegalStateException.class,
            () ->
                new DefaultJwtService(
                    "dev-only-default-secret-do-not-use-in-production-nRvyYC4soFxBdZ",
                    3600,
                    "performance"));
    Assertions.assertTrue(
        exception.getMessage().contains("JWT_SECRET"),
        "Exception message should mention JWT_SECRET environment variable");
  }
}
