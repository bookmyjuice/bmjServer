// package com.bezkoder.springjwt;

// import org.springframework.context.annotation.Configuration;
// import org.springframework.lang.NonNull;
// import org.springframework.web.servlet.config.annotation.CorsRegistry;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Configuration
// public class WebConfig implements WebMvcConfigurer {

//     @Override
//     public void addCorsMappings(@NonNull CorsRegistry registry) {
//         registry.addMapping("/api/**")
//                 .allowedOrigins("*")
//                 .allowedMethods("POST", "GET", "OPTIONS").allowedHeaders("Authrization", "Content-Type", "Accept")
//                 .exposedHeaders("Authrization", "Content-Type", "Accept")
//                 .allowCredentials(true);
//     }
// };