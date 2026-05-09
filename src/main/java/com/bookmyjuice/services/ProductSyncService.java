package com.bookmyjuice.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.OneTimePrice;
import com.bookmyjuice.models.Product;
import com.bookmyjuice.models.SubscriptionPlan;
import com.bookmyjuice.repository.OneTimePriceRepository;
import com.bookmyjuice.repository.ProductRepository;
import com.bookmyjuice.repository.SubscriptionPlanRepository;
import com.chargebee.ListResult;
import com.chargebee.models.Item;
import com.chargebee.models.ItemPrice;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProductSyncService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProductSyncService.class);

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OneTimePriceRepository oneTimePriceRepository;
    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    /**
     * Syncs products and prices from Chargebee to local database.
     * Uses the same pagination pattern as ChargebeeSyncService.
     */
    @Transactional
    public void syncAllProducts() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        int newProducts = 0, updatedProducts = 0, newPrices = 0;

        // 1. Sync Items from Chargebee
        logger.info("🔄 Starting product sync from Chargebee...");
        ListResult itemResults = Item.list().limit(100).request();
        do {
            logger.info("Processing {} items from Chargebee", itemResults.size());

            for (ListResult.Entry entry : itemResults) {
                Item chargebeeItem = entry.item();

                // Only sync "charge" type items (one-time products)
                if (!"charge".equals(chargebeeItem.type().toString())) {
                    continue;
                }

                // Extract category and image_url from metadata
                String category = "";
                String imageUrl = "";
                Object metadataObj = chargebeeItem.metadata();
                if (metadataObj != null) {
                    try {
                        Map<String, Object> metadata = convertChargebeeMetadata(metadataObj);
                        if (metadata.containsKey("category")) {
                            category = metadata.get("category").toString();
                        }
                        if (metadata.containsKey("image_url")) {
                            imageUrl = metadata.get("image_url").toString();
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to parse metadata for item {}: {}", chargebeeItem.id(), e.getMessage());
                    }
                }

                // Check if product exists
                if (productRepository.existsById(chargebeeItem.id())) {
                    Product existingProduct = productRepository.findById(chargebeeItem.id()).orElse(null);
                    if (existingProduct != null) {
                        existingProduct.setName(chargebeeItem.name());
                        existingProduct.setCategory(category);
                        existingProduct.setImageUrl(imageUrl);
                        existingProduct.setItemType(chargebeeItem.type().toString());
                        productRepository.save(existingProduct);
                        updatedProducts++;
                    }
                } else {
                    Product product = new Product();
                    product.setId(chargebeeItem.id());
                    product.setName(chargebeeItem.name());
                    product.setCategory(category);
                    product.setImageUrl(imageUrl);
                    product.setItemType(chargebeeItem.type().toString());
                    productRepository.save(product);
                    newProducts++;
                }
            }

            // Pagination
            if (itemResults.nextOffset() != null) {
                itemResults = Item.list().limit(100).offset(itemResults.nextOffset()).request();
            } else {
                break;
            }
        } while (true);

        // 2. Sync ItemPrices from Chargebee
        logger.info("🔄 Starting price sync from Chargebee...");
        ListResult priceResults = ItemPrice.list().limit(100).request();
        do {
            logger.info("Processing {} prices from Chargebee", priceResults.size());

            for (ListResult.Entry entry : priceResults) {
                ItemPrice chargebeePrice = entry.itemPrice();
                String itemId = chargebeePrice.itemId();

                // Find parent product
                Product product = productRepository.findById(itemId).orElse(null);
                if (product == null) {
                    logger.warn("Parent product not found for price: {} (Item ID: {})", chargebeePrice.id(), itemId);
                    continue;
                }

                if ("charge".equals(product.getItemType())) {
                    // One-time price
                    OneTimePrice otp;
                    if (oneTimePriceRepository.existsById(chargebeePrice.id())) {
                        otp = oneTimePriceRepository.findById(chargebeePrice.id()).orElse(null);
                    } else {
                        otp = new OneTimePrice();
                        otp.setId(chargebeePrice.id());
                    }

                    if (otp != null) {
                        otp.setProduct(product);
                        otp.setSize(extractSize(chargebeePrice.name()));
                        otp.setPrice(chargebeePrice.price());
                        otp.setCurrency(chargebeePrice.currencyCode());
                        oneTimePriceRepository.save(otp);
                        newPrices++;
                    }
                } else if ("plan".equals(product.getItemType())) {
                    // Subscription plan
                    SubscriptionPlan sp;
                    if (subscriptionPlanRepository.existsById(chargebeePrice.id())) {
                        sp = subscriptionPlanRepository.findById(chargebeePrice.id()).orElse(null);
                    } else {
                        sp = new SubscriptionPlan();
                        sp.setId(chargebeePrice.id());
                    }

                    if (sp != null) {
                        sp.setProduct(product);
                        sp.setSize(extractSize(chargebeePrice.name()));
                        sp.setFrequency(extractFrequency(chargebeePrice.name()));
                        sp.setPrice(chargebeePrice.price());
                        sp.setCurrency(chargebeePrice.currencyCode());
                        sp.setBillingPeriod(chargebeePrice.periodUnit() != null
                                ? chargebeePrice.periodUnit().toString()
                                : "month");
                        subscriptionPlanRepository.save(sp);
                        newPrices++;
                    }
                }
            }

            // Pagination
            if (priceResults.nextOffset() != null) {
                priceResults = ItemPrice.list().limit(100).offset(priceResults.nextOffset()).request();
            } else {
                break;
            }
        } while (true);

        logger.info("✅ Product sync complete - New products: {}, Updated: {}, Prices: {}",
                newProducts, updatedProducts, newPrices);
    }

    /**
     * Converts Chargebee metadata Object to a clean Map.
     * Handles the Chargebee JSONObject wrapper class.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertChargebeeMetadata(Object metadataObj) throws Exception {
        if (metadataObj == null) {
            return new HashMap<>();
        }

        // Chargebee SDK wraps metadata in com.chargebee.org.json.JSONObject
        if (metadataObj.getClass().getName().contains("chargebee.org.json.JSONObject")) {
            String jsonStr = metadataObj.toString();
            return new ObjectMapper().readValue(jsonStr, Map.class);
        }

        if (metadataObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) metadataObj;
            // Remove Chargebee wrapper keys
            map.remove("empty");
            map.remove("mapType");
            return map;
        }

        // Fallback: serialize and parse
        String jsonStr = new ObjectMapper().writeValueAsString(metadataObj);
        return new ObjectMapper().readValue(jsonStr, Map.class);
    }

    private String extractSize(String name) {
        if (name == null)
            return "standard";
        if (name.contains("200ml"))
            return "200ml";
        if (name.contains("300ml"))
            return "300ml";
        if (name.contains("500ml"))
            return "500ml";
        return "standard";
    }

    private String extractFrequency(String name) {
        if (name == null)
            return "unknown";
        String lower = name.toLowerCase();
        if (lower.contains("weekly"))
            return "weekly";
        if (lower.contains("monthly"))
            return "monthly";
        return "unknown";
    }

    public Map<String, Object> getCatalog() {
        return Map.of(
                "products", productRepository.findAll(),
                "one_time_prices", oneTimePriceRepository.findAll(),
                "subscription_plans", subscriptionPlanRepository.findAll());
    }

    /**
     * Returns all products from the local database.
     * Used by ProductController for GET /api/v1/products
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
