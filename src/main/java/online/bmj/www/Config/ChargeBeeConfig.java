package online.bmj.www.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.chargebee.Environment;
import com.chargebee.exceptions.OperationFailedException;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class for initializing ChargeBee.
 * This class sets up the ChargeBee environment using external configuration values.
 */
@Configuration
// @EnableJpaRepositories(basePackages = "online.bmj.www.repository.jpa") // JPA repos
// @EnableRedisRepositories(basePackages = "online.bmj.www.repository.redis") // Redis repos
public class ChargeBeeConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChargeBeeConfig.class);

    // @Value("${chargebee.site}")
    private final String chargebeeSite="bookmyjuice-test";

    // @Value("${chargebee.api-key}")
    private final String chargebeeApiKey= "test_fMwLpyDFENxTWox6zsbpaYNAoL3yiY9v";

    // @Value("${chargebee.api-version:2023-10-09}")
    // private String apiVersion;

    /**
     * Initializes the ChargeBee configuration during bean creation.
     */
    // chargebeeSite="lush-j-test";
    // chargebeeApiKey="your_chargebee_api_key";
   


    @PostConstruct
    public void initChargeBee() {
        try {
            // Configure ChargeBee environment using externalized values
            Environment.configure(chargebeeSite, chargebeeApiKey /*, apiVersion if needed*/);
            logger.info("ChargeBee configured successfully for site: {}", chargebeeSite);

            // Optionally, verify the configuration with a lightweight test call
            // verifyConfiguration();

        } catch (OperationFailedException e) {
            logger.error("Failed to initialize ChargeBee configuration", e);
            throw new RuntimeException("ChargeBee initialization failed", e);
        }
    }

    /**
     * Optionally verify the ChargeBee configuration by making a test API call.
     * Uncomment and adjust the method if you want to perform this check.
     */
    /*
    private void verifyConfiguration() {
        try {
            // For example, list plans to verify credentials
            ChargeBee.listPlans()
                .limit(1)
                .request();
            logger.debug("ChargeBee configuration verified successfully");
        } catch (Exception e) {
            logger.error("ChargeBee configuration verification failed", e);
            throw new RuntimeException("ChargeBee verification failed", e);
        }
    }
    */
}
