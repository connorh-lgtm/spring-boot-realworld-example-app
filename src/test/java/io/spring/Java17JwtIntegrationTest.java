package io.spring;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.spring.core.user.User;
import io.spring.infrastructure.service.DefaultJwtService;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Value;

@SpringBootTest
@ActiveProfiles("test")
public class Java17JwtIntegrationTest {

  @Autowired private DefaultJwtService jwtService;
  
  @Value("${jwt.secret}")
  private String jwtSecret;

  private User testUser;

  @BeforeEach
  public void setUp() {
    testUser = new User("jwt@example.com", "jwtuser", "123456", "JWT Test User", "");
  }

  @Test
  public void shouldGenerateValidJwtTokenWithJava17() {
    String token = jwtService.toToken(testUser);
    
    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertTrue(token.contains("."), "JWT should contain dots as separators");
    
    String[] parts = token.split("\\.");
    assertEquals(3, parts.length, "JWT should have 3 parts: header.payload.signature");
    
    for (String part : parts) {
      assertFalse(part.isEmpty(), "No JWT part should be empty");
    }
  }

  @Test
  public void shouldValidateJwtTokenCorrectly() {
    String token = jwtService.toToken(testUser);
    
    Optional<String> userId = jwtService.getSubFromToken(token);
    assertTrue(userId.isPresent());
    assertEquals(testUser.getId(), userId.get());
  }

  @Test
  public void shouldHandleInvalidJwtTokensGracefully() {
    assertDoesNotThrow(() -> {
      Optional<String> result = jwtService.getSubFromToken("invalid.jwt.token");
      assertFalse(result.isPresent(), "Invalid token should return empty optional");
    });
    
    assertDoesNotThrow(() -> {
      Optional<String> result = jwtService.getSubFromToken("not-a-jwt-at-all");
      assertFalse(result.isPresent(), "Malformed token should return empty optional");
    });
    
    assertDoesNotThrow(() -> {
      Optional<String> result = jwtService.getSubFromToken("");
      assertFalse(result.isPresent(), "Empty token should return empty optional");
    });
    
    assertDoesNotThrow(() -> {
      Optional<String> result = jwtService.getSubFromToken(null);
      assertFalse(result.isPresent(), "Null token should return empty optional");
    });
  }

  @Test
  public void shouldValidateJwtTokenStructureWithUpdatedJJWT() {
    String token = jwtService.toToken(testUser);
    
    assertDoesNotThrow(() -> {
      String[] parts = token.split("\\.");
      
      String header = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
      assertTrue(header.contains("alg"), "Header should contain algorithm");
      
      String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
      assertTrue(payload.contains("sub"), "Payload should contain subject");
      assertTrue(payload.contains("exp"), "Payload should contain expiration");
    }, "JWT structure should be valid with JJWT 0.12.6");
  }

  @Test
  public void shouldValidateJwtPerformanceWithJava17() {
    int tokenCount = 100;
    
    Instant start = Instant.now();
    
    for (int i = 0; i < tokenCount; i++) {
      User user = new User("perf" + i + "@example.com", "perfuser" + i, "123456", "Perf User " + i, "");
      String token = jwtService.toToken(user);
      assertNotNull(token);
      
      Optional<String> userId = jwtService.getSubFromToken(token);
      assertTrue(userId.isPresent());
      assertEquals(user.getId(), userId.get());
    }
    
    Duration elapsed = Duration.between(start, Instant.now());
    
    assertTrue(elapsed.toMillis() < 5000, 
        "JWT operations should be performant: " + elapsed.toMillis() + "ms for " + tokenCount + " tokens");
  }

  @Test
  public void shouldHandleConcurrentJwtOperations() {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    
    try {
      CompletableFuture<Void>[] futures = IntStream.range(0, 50)
          .mapToObj(i -> CompletableFuture.runAsync(() -> {
            User user = new User("concurrent" + i + "@example.com", "concuser" + i, "123456", "Concurrent User " + i, "");
            String token = jwtService.toToken(user);
            
            assertNotNull(token);
            
            Optional<String> userId = jwtService.getSubFromToken(token);
            assertTrue(userId.isPresent());
            assertEquals(user.getId(), userId.get());
          }, executor))
          .toArray(CompletableFuture[]::new);
      
      assertDoesNotThrow(() -> {
        CompletableFuture.allOf(futures).get();
      }, "Concurrent JWT operations should not fail");
      
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void shouldValidateJwtExpirationHandling() {
    String token = jwtService.toToken(testUser);
    
    assertDoesNotThrow(() -> {
      Optional<String> userId = jwtService.getSubFromToken(token);
      assertTrue(userId.isPresent(), "Fresh token should be valid");
    });
    
    assertDoesNotThrow(() -> {
      SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
      
      String expiredToken = Jwts.builder()
          .setSubject(testUser.getId())
          .setExpiration(Date.from(Instant.now().minus(Duration.ofHours(1))))
          .setIssuedAt(Date.from(Instant.now().minus(Duration.ofHours(2))))
          .signWith(key)
          .compact();
      
      Optional<String> userId = jwtService.getSubFromToken(expiredToken);
      assertFalse(userId.isPresent(), "Expired token should be invalid");
    });
  }

  @Test
  public void shouldValidateJwtSignatureVerification() {
    String validToken = jwtService.toToken(testUser);
    
    Optional<String> validResult = jwtService.getSubFromToken(validToken);
    assertTrue(validResult.isPresent(), "Valid token should pass signature verification");
    
    assertDoesNotThrow(() -> {
      SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-for-testing-signature-validation-wrong-secret-key-for-testing".getBytes());
      
      String invalidSignatureToken = Jwts.builder()
          .setSubject(testUser.getId())
          .setExpiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
          .setIssuedAt(Date.from(Instant.now()))
          .signWith(wrongKey)
          .compact();
      
      Optional<String> invalidResult = jwtService.getSubFromToken(invalidSignatureToken);
      assertFalse(invalidResult.isPresent(), "Token with invalid signature should be rejected");
    });
  }

  @Test
  public void shouldValidateJwtClaimsWithJava17() {
    String token = jwtService.toToken(testUser);
    
    assertDoesNotThrow(() -> {
      SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
      
      Claims claims = Jwts.parser()
          .verifyWith(key)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      
      assertEquals(testUser.getId(), claims.getSubject());
      assertNotNull(claims.getExpiration(), "Token should have expiration claim");
      
      assertTrue(claims.getExpiration().after(new Date()), "Token should not be expired");
      
      long expirationTime = claims.getExpiration().getTime() - System.currentTimeMillis();
      assertTrue(expirationTime > 0, "Expiration should be in the future");
      
    }, "JWT claims should be accessible and valid with Java 17");
  }

  @Test
  public void shouldValidateJwtMemoryUsageWithJava17() {
    Runtime runtime = Runtime.getRuntime();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    
    for (int i = 0; i < 1000; i++) {
      User user = new User("memory" + i + "@example.com", "memuser" + i, "123456", "Memory User " + i, "");
      String token = jwtService.toToken(user);
      jwtService.getSubFromToken(token);
    }
    
    System.gc();
    
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = finalMemory - initialMemory;
    
    assertTrue(memoryUsed < 50 * 1024 * 1024, 
        "JWT operations should not consume excessive memory: " + memoryUsed + " bytes");
  }

  @Test
  public void shouldValidateJava17StringProcessingInJwt() {
    User userWithSpecialChars = new User(
        "special@example.com", 
        "user-with-special-chars_123", 
        "123456", 
        "User with Special Characters: àáâãäåæçèéêë", 
        ""
    );
    
    assertDoesNotThrow(() -> {
      String token = jwtService.toToken(userWithSpecialChars);
      assertNotNull(token);
      
      Optional<String> userId = jwtService.getSubFromToken(token);
      assertTrue(userId.isPresent());
      assertEquals(userWithSpecialChars.getId(), userId.get());
    }, "JWT should handle special characters correctly with Java 17");
  }
}
