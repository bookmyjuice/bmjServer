package com.bookmyjuice.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.bookmyjuice.models.OneTimePrice;
import com.bookmyjuice.models.Product;
import com.bookmyjuice.models.SubscriptionPlan;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for product endpoints matching the Flutter contract:
 * GET /api/products, /api/products/{id}, /api/products/family/{family},
 * /api/products/search, /api/products/featured
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private String id;
    private String name;
    private String family;
    private String description;
    private String imageUrl;
    private String chargebeeItemId;
    private List<PriceResponse> prices;
    private boolean isFeatured;
    private boolean isAvailable;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceResponse {
        @JsonProperty("itemPriceId")
        private String itemPriceId;
        private String currencyCode;
        private BigDecimal unitAmount;
        private String period;
        private String periodUnit;
    }

    /**
     * Converts a Product entity to this response DTO.
     */
    public static ProductResponse fromEntity(Product product) {
        List<PriceResponse> prices = new ArrayList<>();

        // Add one-time price if present
        if (product.getOneTimePrice() != null) {
            OneTimePrice otp = product.getOneTimePrice();
            prices.add(PriceResponse.builder()
                    .itemPriceId(otp.getId())
                    .currencyCode(otp.getCurrency())
                    .unitAmount(otp.getPrice() != null ? BigDecimal.valueOf(otp.getPrice()) : BigDecimal.ZERO)
                    .period(null)
                    .periodUnit(null)
                    .build());
        }

        // Add subscription plan prices if present
        if (product.getSubscriptionPlans() != null && !product.getSubscriptionPlans().isEmpty()) {
            prices.addAll(product.getSubscriptionPlans().stream()
                    .map(sp -> PriceResponse.builder()
                            .itemPriceId(sp.getId())
                            .currencyCode(sp.getCurrency())
                            .unitAmount(sp.getPrice() != null ? BigDecimal.valueOf(sp.getPrice()) : BigDecimal.ZERO)
                            .period(sp.getBillingPeriod())
                            .periodUnit(sp.getBillingPeriod())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Determine family from category (map bmjServer categories to spec families)
        String family = mapCategoryToFamily(product.getCategory());

        // Determine featured status from metadata
        boolean featured = isFeaturedProduct(product);

        // Product is available if it has at least one price
        boolean available = !prices.isEmpty();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .family(family)
                .description(extractDescription(product))
                .imageUrl(product.getImageUrl())
                .chargebeeItemId(product.getId())
                .prices(prices)
                .isFeatured(featured)
                .isAvailable(available)
                .build();
    }

    private static String mapCategoryToFamily(String category) {
        if (category == null) return "juice";
        String lower = category.toLowerCase();
        if (lower.contains("smoothie")) return "smoothie";
        if (lower.contains("detox") || lower.contains("cleanse")) return "detox";
        return "juice";
    }

    private static boolean isFeaturedProduct(Product product) {
        if (product.getMetadata() == null) return false;
        try {
            return product.getMetadata().toLowerCase().contains("\"featured\":true")
                    || product.getMetadata().toLowerCase().contains("\"featured\": true");
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractDescription(Product product) {
        if (product.getMetadata() == null) return "";
        try {
            // Try to extract description from metadata JSON
            String meta = product.getMetadata();
            int descIdx = meta.indexOf("\"description\"");
            if (descIdx >= 0) {
                int colonIdx = meta.indexOf(":", descIdx);
                int startQuote = meta.indexOf("\"", colonIdx + 1);
                int endQuote = meta.indexOf("\"", startQuote + 1);
                if (startQuote >= 0 && endQuote > startQuote) {
                    return meta.substring(startQuote + 1, endQuote);
                }
            }
        } catch (Exception e) {
            // Fall through
        }
        return "";
    }
}