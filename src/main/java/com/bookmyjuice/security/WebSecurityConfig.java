package com.bookmyjuice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bookmyjuice.security.jwt.AuthEntryPointJwt;
import com.bookmyjuice.security.jwt.AuthTokenFilter;
import com.bookmyjuice.security.jwt.RouteExistenceFilter;
import com.bookmyjuice.services.UserDetailsServiceImpl;

@Configuration
@EnableMethodSecurity
// (securedEnabled = true,
// jsr250Enabled = true,
// prePostEnabled = true) // by default
public class WebSecurityConfig { // extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);
  @Autowired
  UserDetailsServiceImpl userDetailsService;

  @Autowired
  private AuthEntryPointJwt unauthorizedHandler;

  @Autowired
  private RouteExistenceFilter routeExistenceFilter;

  @Autowired
  private RateLimitingFilter rateLimitingFilter;

  @Value("${WEBHOOK_USERNAME:}")
  private String webhookUsername;

  @Value("${WEBHOOK_PASSWORD:}")
  private String webhookPassword;

  /**
   * Escape hatch for local runs and tests that do not exercise the webhook endpoints.
   * Never set this in a deployed environment: it re-opens the fail-open behaviour.
   */
  @Value("${webhook.auth.optional:false}")
  private boolean webhookAuthOptional;

  /**
   * Refuse to start when the webhook credentials are missing.
   *
   * <p>Previously both values defaulted to empty, so Spring happily built an in-memory user
   * with a blank password and {@code /api/webhooks/**} accepted {@code Basic <anything>:} —
   * the endpoint failed open. A deployment cannot silently lose its webhook authentication
   * any more; it either has credentials or it does not start.
   */
  @jakarta.annotation.PostConstruct
  void validateWebhookCredentials() {
    boolean missingUser = webhookUsername == null || webhookUsername.isBlank();
    boolean missingPassword = webhookPassword == null || webhookPassword.isBlank();

    if (!missingUser && !missingPassword) {
      logger.info("Webhook Basic Auth configured for user '{}'", webhookUsername);
      return;
    }

    String missing = (missingUser ? " WEBHOOK_USERNAME" : "") + (missingPassword ? " WEBHOOK_PASSWORD" : "");
    if (webhookAuthOptional) {
      logger.warn("Webhook credentials are not set ({}); continuing because "
          + "webhook.auth.optional=true. /api/webhooks/** must not be exposed in this mode.", missing.trim());
      return;
    }
    throw new IllegalStateException(
        "Missing required webhook credentials:" + missing
            + ". Set both environment variables (see deploy/bmj-server.env.example). "
            + "Refusing to start rather than exposing /api/webhooks/** without authentication. "
            + "For local runs only, set webhook.auth.optional=true.");
  }

  @Bean
  public AuthTokenFilter authenticationJwtTokenFilter() {
    return new AuthTokenFilter();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());

    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return new ProviderManager(authenticationProvider());
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/error").permitAll()
            .requestMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()
            .requestMatchers("/api/health").permitAll()
            .requestMatchers("/api/subscriptions/pricing/**").permitAll()
            .requestMatchers("/api/catalog/**", "/api/v1/catalog/**").permitAll()
            .requestMatchers("/api/test/**").permitAll()
            // Require auth only for /api/ routes (known routes are authenticated, unknown /api/ routes return 401)
            .requestMatchers("/api/**").authenticated()
            // All non-API routes fall through to default servlet which returns 404
            .anyRequest().permitAll());

    // B-09 FIX: Route existence filter runs FIRST - before any security filters.
    // For unknown /api/ routes, it bypasses the security chain so the
    // DispatcherServlet returns a proper 404 instead of 401.
    // Uses SecurityContextHolderAwareRequestFilter as reference since it's the
    // earliest well-known Spring Security filter with a registered order.
    http.addFilterBefore(routeExistenceFilter, org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.class);
    // Add rate limiting filter before authentication filters
    http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  @Order(1)
  public SecurityFilterChain webhookFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/webhooks/**")
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults());

    http.authenticationProvider(webhookAuthenticationProvider());
    return http.build();
  }

  @Bean
  public DaoAuthenticationProvider webhookAuthenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setPasswordEncoder(passwordEncoder());
    provider.setUserDetailsService(webhookUserDetailsService());
    return provider;
  }

  @Bean
  public UserDetailsService webhookUserDetailsService() {
    String encoded = passwordEncoder().encode(webhookPassword);
    InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager(
        User.withUsername(webhookUsername)
            .password(encoded)
            .roles("WEBHOOK")
            .build());
    return manager;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOriginPattern("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("GET");
    config.addAllowedMethod("POST");
    config.addAllowedMethod("PUT");
    config.addAllowedMethod("DELETE");
    config.addAllowedMethod("OPTIONS");
    config.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
