package io.spring.infrastructure.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultJwtService implements JwtService {
  private final SecretKey signingKey;
  private int sessionTime;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.sessionTime}") int sessionTime,
      @Value("${spring.profiles.active:default}") String activeProfile) {
    this.sessionTime = sessionTime;
    if (secret.contains("dev-only-default")
        && (activeProfile.contains("production") || activeProfile.contains("performance"))) {
      throw new IllegalStateException(
          "JWT_SECRET environment variable must be set for production. "
              + "Do not use the default development secret in production.");
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException(
          "JWT secret must be at least 32 bytes for HMAC-SHA256 security.");
    }
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String toToken(User user) {
    return Jwts.builder()
        .subject(user.getId())
        .expiration(expireTimeFromNow())
        .signWith(signingKey)
        .compact();
  }

  @Override
  public Optional<String> getSubFromToken(String token) {
    try {
      Jws<Claims> claimsJws =
          Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
      return Optional.ofNullable(claimsJws.getPayload().getSubject());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private Date expireTimeFromNow() {
    return new Date(System.currentTimeMillis() + sessionTime * 1000L);
  }
}
