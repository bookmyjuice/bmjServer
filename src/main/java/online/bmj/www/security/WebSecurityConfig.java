package online.bmj.www.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import online.bmj.www.security.jwt.AuthEntryPointJwt;
import online.bmj.www.security.jwt.AuthTokenFilter;
import online.bmj.www.services.UserDetailsServiceImpl;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    @Autowired
    final PasswordEncoder passwordEncoder;

    @Autowired
    final UserDetailsServiceImpl userDetailsService;

    @Autowired
    final AuthEntryPointJwt unauthorizedHandler;

    public WebSecurityConfig(PasswordEncoder passwordEncoder,
                            UserDetailsServiceImpl userDetailsService,
                            AuthEntryPointJwt unauthorizedHandler) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults()) 
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/auth/**").permitAll()
            .requestMatchers("/auth/customer-created").permitAll()
            // .requestMatchers("/customer-created").permitAll()
            .requestMatchers("/api/**").authenticated()
            // .anyRequest().authenticated()
            );
        
        // http.authenticationProvider(authenticationProvider());
        // http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}


// package online.bmj.www.security;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
// import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// import online.bmj.www.security.jwt.AuthEntryPointJwt;
// import online.bmj.www.security.jwt.AuthTokenFilter;
// import online.bmj.www.services.UserDetailsServiceImpl;
// // import online.bmj.www.security.jwt.configs;

// @Configuration
// @EnableMethodSecurity
// // (securedEnabled = true,
// // jsr250Enabled = true,
// // prePostEnabled = true) // by default
// public class WebSecurityConfig {

//     @Autowired
//     private PasswordEncoder passwordEncoder;

//     @Autowired
//     private UserDetailsServiceImpl userDetailsService;

//     @Autowired
//     private AuthEntryPointJwt unauthorizedHandler;

//     // WebSecurityConfig(PasswordEncoder passwordEncoder) {
//     //     this.passwordEncoder = passwordEncoder;
//     // }
//     @Bean
//     public AuthTokenFilter authenticationJwtTokenFilter() {
//         return new AuthTokenFilter();
//     }

//     // @Autowired
//     // public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
//     //     authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
//     // }

//     @Bean
//     public DaoAuthenticationProvider authenticationProvider() {
//         DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

//         authProvider.setUserDetailsService(userDetailsService);
//         authProvider.setPasswordEncoder(passwordEncoder);

//         return authProvider;
//     }

// //  @Bean
// //  @Override
// //  public AuthenticationManager authenticationManagerBean() throws Exception {
// //    return super.authenticationManagerBean();
// //  }
//     @Bean
//     public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
//         return authConfig.getAuthenticationManager();
//     }

// //  @Override
// //  protected void configure(HttpSecurity http) throws Exception {
// //    http.cors().and().csrf().disable()
// //      .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
// //      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
// //      .authorizeRequests().antMatchers("/api/auth/**").permitAll()
// //      .antMatchers("/api/test/**").permitAll()
// //      .anyRequest().authenticated();
// //
// //    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
// //  }
//     @Bean
//     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//         http.csrf(csrf -> csrf.disable())
//                 .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
//                 .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                 .authorizeHttpRequests(auth
//                         -> auth.requestMatchers("/api/**").permitAll()
//                         .requestMatchers("/api/auth/**").permitAll()
//                         .anyRequest().authenticated()
//                 );

//         http.authenticationProvider(authenticationProvider());

//         http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

//         return http.build();
//     }
// }
