// package online.bmj.www.Config;

// import java.time.Duration;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import io.github.resilience4j.ratelimiter.RateLimiter;
// import io.github.resilience4j.ratelimiter.RateLimiterConfig;

// /**
//  * Configuration class for Resilience4j rate limiters.
//  * This rate limiter is used for Chargebee API integrations.
//  */
// @Configuration
// public class RateLimiterConfigs {

//     private static final Logger logger = LoggerFactory.getLogger(RateLimiterConfigs.class);

//     @Value("${chargebee.ratelimiter.limit-refresh-period}")
//     private int limitRefreshPeriodSeconds;

//     @Value("${chargebee.ratelimiter.limit-for-period}")
//     private int limitForPeriod;

//     @Value("${chargebee.ratelimiter.timeout-duration}")
//     private int timeoutDurationSeconds;

//     /**
//      * Configures a rate limiter for Chargebee API calls.
//      *
//      * @return a RateLimiter configured for Chargebee with custom settings
//      */
//     @Bean
//     public RateLimiter chargebeeRateLimiter() {
//         RateLimiterConfig config = RateLimiterConfig.custom()
//             .limitRefreshPeriod(Duration.ofSeconds(limitRefreshPeriodSeconds))
//             .limitForPeriod(limitForPeriod)  // e.g., 450 requests per period
//             .timeoutDuration(Duration.ofSeconds(timeoutDurationSeconds))
//             .build();
            
//         logger.info("Initializing Chargebee RateLimiter: refresh period {} sec, limit {} per period, timeout {} sec",
//                     limitRefreshPeriodSeconds, limitForPeriod, timeoutDurationSeconds);
                    
//         return RateLimiter.of("chargebee", config);
//     }
// }
