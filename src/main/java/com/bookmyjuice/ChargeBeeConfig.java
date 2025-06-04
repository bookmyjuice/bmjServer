package com.bookmyjuice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.chargebee.Environment;
import com.chargebee.exceptions.OperationFailedException;

import jakarta.annotation.PostConstruct;


@Configuration
public class ChargeBeeConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChargeBeeConfig.class);

    @Value("${chargebee.site}")
    private String chargebeeSite;

    @Value("${chargebee.apiKey}")
    private String chargebeeApiKey;


    @PostConstruct
    public void initChargeBee() {
        try {
            // Configure ChargeBee environment using externalized values
            Environment.configure(chargebeeSite, chargebeeApiKey);
            logger.info("ChargeBee configured successfully for site: {}", chargebeeSite);

        } catch (OperationFailedException e) {
            logger.error("Failed to initialize ChargeBee configuration", e);
            throw new RuntimeException("ChargeBee initialization failed", e);
        }
    }

}

