package com.bezkoder.springjwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.chargebee.Environment;
import com.chargebee.exceptions.OperationFailedException;

import jakarta.annotation.PostConstruct;


@Configuration
public class ChargeBeeConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChargeBeeConfig.class);

    // @Value("${chargebee.site}")
    private final String chargebeeSite="bookmyjuice-test";

    // @Value("${chargebee.api-key}")
    private final String chargebeeApiKey= "test_fMwLpyDFENxTWox6zsbpaYNAoL3yiY9v";  


    @PostConstruct
    public void initChargeBee() {
        try {
            // Configure ChargeBee environment using externalized values
            Environment.configure(chargebeeSite, chargebeeApiKey /*, apiVersion if needed*/);
            logger.info("ChargeBee configured successfully for site: {}", chargebeeSite);

        } catch (OperationFailedException e) {
            logger.error("Failed to initialize ChargeBee configuration", e);
            throw new RuntimeException("ChargeBee initialization failed", e);
        }
    }

}

