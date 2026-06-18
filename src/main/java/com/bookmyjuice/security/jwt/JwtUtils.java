package com.bookmyjuice.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.bookmyjuice.services.UserDetailsImpl;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  @Value("${bezkoder.app.jwtSecret}")
  private String jwtSecret;

  @Value("${bezkoder.app.jwtExpirationMs}")
  private long jwtExpirationMs;

  public String generateJwtToken(Authentication authentication, int tokenVersion) {
    Object principal = authentication.getPrincipal();
    String username;

    // FIX: Dynamically evaluate principal type to support both login pathways
    if (principal instanceof UserDetailsImpl userDetailsImpl) {
      username = userDetailsImpl.getUsername();
    } else if (principal instanceof String string) {
      username = string;
    } else {
      throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass().getName());
    }

    return Jwts.builder()
        .setSubject(username)
        .claim("tokenVersion", tokenVersion)
        .setIssuedAt(new Date())
        .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
        .signWith(key(), SignatureAlgorithm.HS256)
        .compact();
  }

  public String generateJwtToken(Authentication authentication) {
    return generateJwtToken(authentication, 1);
  }

  public int getTokenVersionFromJwtToken(String token) {
    try {
      Object version = Jwts.parserBuilder().setSigningKey(key()).build()
          .parseClaimsJws(token).getBody().get("tokenVersion");
      if (version instanceof Integer) {
        return (Integer) version;
      } else if (version instanceof Number) {
        return ((Number) version).intValue();
      }
    } catch (Exception e) {
      logger.warn("Could not extract token version: {}", e.getMessage());
    }
    return 0;
  }

  private Key key() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public String getUserNameFromJwtToken(String token) {
    return Jwts.parserBuilder().setSigningKey(key()).build()
        .parseClaimsJws(token).getBody().getSubject();
  }

  public boolean validateJwtToken(String authToken) {
    if (authToken == null || authToken.isEmpty()) {
      return false;
    }
    try {
      // FIX: Use parseClaimsJws instead of plain parse to correctly validate signed
      // tokens
      Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
      return true;
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    }

    return false;
  }

  public static String getUserIdFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
      UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
      return userDetails.getId().toString();
    }
    return null;
  }
}
