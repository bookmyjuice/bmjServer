package online.bmj.www.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class ShopifyConfig {
    
    @Value("${shopify.store-url}")
    private String storeUrl;
    
    @Value("${shopify.storefront-access-token}")
    private String accessToken;
    
    @Value("${shopify.api-version}")
    private String apiVersion;

    @Bean
    public WebClient shopifyWebClient() {
        return WebClient.builder()
                .baseUrl(storeUrl)
                .defaultHeader("X-Shopify-Storefront-Access-Token", accessToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
    
    @Bean
    public String shopifyApiVersion() {
        return apiVersion;
    }
}