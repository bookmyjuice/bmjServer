// package online.bmj.www.Config;

// import org.redisson.Redisson;
// import org.redisson.api.RedissonClient;
// import org.redisson.config.Config;
// import org.redisson.spring.cache.RedissonSpringCacheManager;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.cache.CacheManager;
// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

// @Configuration
// @EnableCaching
// @EnableRedisRepositories(basePackages = "online.bmj.www.repository.redis")// Scan JPA repos// Scan Redis rep
// public class CacheConfig {

//     private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

//     @Value("${redis.address}")
//     private String redisAddress;

//     @Bean(destroyMethod = "shutdown")
//     public RedissonClient redissonClient() {
//         Config config = new Config();
//         config.useSingleServer().setAddress(redisAddress);
//         try {
//             RedissonClient client = Redisson.create(config);
//             logger.info("Successfully connected to Redis at {}", redisAddress);
//             return client;
//         } catch (Exception ex) {
//             logger.error("Failed to create Redisson client for Redis at {}: {}", redisAddress, ex.getMessage());
//             throw ex;
//         }
//     }

//     @Bean
//     public CacheManager cacheManager(RedissonClient redissonClient) {
//         return new RedissonSpringCacheManager(redissonClient);
//     }
// }