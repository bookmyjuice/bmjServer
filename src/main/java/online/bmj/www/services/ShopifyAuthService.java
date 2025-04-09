// package online.bmj.www.services;

// import java.util.List;
// import java.util.Map;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatusCode;
// import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient;

// import online.bmj.www.DTOs.ShopifySignupRequest;
// import reactor.core.publisher.Mono;

// @Service
// public class ShopifyAuthService {
    
//     private static final Logger logger = LoggerFactory.getLogger(ShopifyAuthService.class);
    
//     @Autowired
//     private WebClient shopifyWebClient;
    
//     @Autowired
//     private String shopifyApiVersion;

//     public String authenticateCustomer(String email, String password) {
//         String query;
//         query = String.format("""
//                                   mutation {
//                                       customerAccessTokenCreate(input: {
//                                           email: "%s",
//                                           password: "%s"
//                                       }) {
//                                           customerAccessToken {
//                                               accessToken
//                                               expiresAt
//                                           }
//                                           userErrors {
//                                               field
//                                               message
//                                           }
//                                       }
//                                   }
//                               """, email, password);

//         return shopifyWebClient.post().uri("/api/{version}/graphql.json", shopifyApiVersion).bodyValue(Map.of("query", query)).retrieve().onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class).map(body -> new RuntimeException("Shopify API error: " + body))
//                 )
//                 .bodyToMono(Map.class)
//                 .map(response -> extractAccessToken(response))
//                 .onErrorResume(e -> {
//                     logger.error("Shopify authentication failed: {}", e.getMessage());
//                     return Mono.empty();
//                 })
//                 .block();
//     }

//     private String extractAccessToken(Map<String, Object> response) {
//         Map<String, Object> data = (Map<String, Object>) response.get("data");
//         Map<String, Object> tokenData = (Map<String, Object>) data.get("customerAccessTokenCreate");
//         List<Map<String, String>> errors = (List<Map<String, String>>) tokenData.get("userErrors");
        
//         if (!errors.isEmpty()) {
//             throw new RuntimeException("Shopify auth error: " + errors.get(0).get("message"));
//         }
        
//         Map<String, String> token = (Map<String, String>) tokenData.get("customerAccessToken");
//         return token.get("accessToken");
//     }

//     public Map<String, Object> getCustomerDetails(String accessToken) {
//         String query = String.format("""
//             query {
//                 customer(customerAccessToken: "%s") {
//                     id
//                     email
//                     firstName
//                     lastName
//                     phone
//                     addresses(first: 1) {
//                         edges {
//                             node {
//                                 address1
//                                 city
//                                 country
//                                 zip
//                             }
//                         }
//                     }
//                 }
//             }
//         """, accessToken);

//         return shopifyWebClient.post()
//                 .uri("/api/{version}/graphql.json", shopifyApiVersion)
//                 .bodyValue(Map.of("query", query))
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .map(response -> {
//                     Map<String, Object> data = (Map<String, Object>) response.get("data");
//                     return (Map<String, Object>) data.get("customer");
//                 })
//                 .block();
//     }

//     public Map<String, Object> createCustomer(ShopifySignupRequest request) {
//         String mutation = String.format("""
//             mutation {
//                 customerCreate(input: {
//                     email: "%s",
//                     password: "%s",
//                     firstName: "%s",
//                     lastName: "%s",
//                     phone: "%s",
//                     addresses: [{
//                         address1: "%s",
//                         city: "%s",
//                         country: "%s",
//                         zip: "%s"
//                     }]
//                 }) {
//                     customer {
//                         id
//                         email
//                     }
//                     userErrors {
//                         field
//                         message
//                     }
//                 }
//             }
//         """, request.getEmail(), request.getPassword(), request.getFirstName(),
//                 request.getLastName(), request.getPhone(), request.getAddress(),
//                 request.getCity(), request.getCountry(), request.getZip());

//         return shopifyWebClient.post()
//                 .uri("/api/{version}/graphql.json", shopifyApiVersion)
//                 .bodyValue(Map.of("query", mutation))
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .map(response -> {
//                     Map<String, Object> data = (Map<String, Object>) response.get("data");
//                     return (Map<String, Object>) data.get("customerCreate");
//                 })
//                 .block();
//     }
// }