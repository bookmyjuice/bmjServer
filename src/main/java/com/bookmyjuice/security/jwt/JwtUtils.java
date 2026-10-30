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

import io.jsonwebtoken.Claims;
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

  /** Purpose claim that distinguishes a signup token from an access token. */
  private static final String SIGNUP_TOKEN_PURPOSE = "phone-signup";

  /** Signup tokens are deliberately short-lived: they only bridge OTP to address. */
  private static final long SIGNUP_TOKEN_TTL_MS = 15 * 60 * 1000L;

  /** Purpose claim that distinguishes an email-proof token from every other token. */
  private static final String EMAIL_TOKEN_PURPOSE = "email-verify";

  /** Email proofs bridge the email OTP step to account creation; keep it short. */
  private static final long EMAIL_TOKEN_TTL_MS = 15 * 60 * 1000L;

  /**
   * Mint the short-lived proof that a phone number was verified (A-1).
   * Consumed by /auth/phone-signup and /auth/google-signup.
   */
  public String generatePhoneVerificationToken(String phone) {
    return Jwts.builder()
        .setSubject(phone)
        .claim("purpose", SIGNUP_TOKEN_PURPOSE)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + SIGNUP_TOKEN_TTL_MS))
        .signWith(key(), SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Read the verified phone from a signup token.
   *
   * @throws IllegalArgumentException when the token is expired, malformed, or
   *         is not a phone-signup token (e.g. a normal access token)
   */
  public String getPhoneFromVerificationToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(key())
        .build()
        .parseClaimsJws(token)
        .getBody();
    if (!SIGNUP_TOKEN_PURPOSE.equals(claims.get("purpose", String.class))) {
      throw new IllegalArgumentException("Not a phone verification token");
    }
    return claims.getSubject();
  }

  /**
   * Mint the short-lived proof that an email address passed the email OTP check.
   * Consumed by /auth/phone-signup: an account cannot be created from a phone
   * verification alone any more (email is mandatory at signup).
   */
  public String generateEmailVerificationToken(String email) {
    return Jwts.builder()
        .setSubject(email)
        .claim("purpose", EMAIL_TOKEN_PURPOSE)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + EMAIL_TOKEN_TTL_MS))
        .signWith(key(), SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Read the verified email from an email-proof token.
   *
   * @throws IllegalArgumentException when the token is expired, malformed, or is
   *         not an email-verify token (e.g. a phone signup or access token)
   */
  public String getEmailFromVerificationToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(key())
        .build()
        .parseClaimsJws(token)
        .getBody();
    if (!EMAIL_TOKEN_PURPOSE.equals(claims.get("purpose", String.class))) {
      throw new IllegalArgumentException("Not an email verification token");
    }
    return claims.getSubject();
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
