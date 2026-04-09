package com.bookmyjuice.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Chargebee Item with filtered categories and sizes
 * Used for GET /api/test/charge-items endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeItemDTO {
    // Basic item fields
    private String itemId;
    private String name;
    private String description;
    private String itemFamilyId;
    private String status;
    private boolean enabledInPortal;
    private boolean enabledForCheckout;

    // Metadata fields
    private String category; // Delight, Signature, Premium
    private String subcategory;
    private String imagePath;
    private String startColor;
    private String endColor;
    private Integer calories;
    private Integer popularity;
    private String servingSize;
    private String shelfLife;
    private String preparationTime;
    private String temperature;

    // Array fields
    private List<String> meals;
    private List<String> benefits;
    private List<String> allergies;
    private List<String> tags;

    // Nested objects
    private NutritionalInfo nutritionalInfo;
    private Customization customization;

    // Size-based prices (200ml, 300ml, 500ml)
    private List<ItemPriceDTO> prices;

    // Explicit setters for Lombok compatibility
    public void setCategory(String category) {
        this.category = category;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setStartColor(String startColor) {
        this.startColor = startColor;
    }

    public void setEndColor(String endColor) {
        this.endColor = endColor;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public void setPopularity(Integer popularity) {
        this.popularity = popularity;
    }

    public void setServingSize(String servingSize) {
        this.servingSize = servingSize;
    }

    public void setShelfLife(String shelfLife) {
        this.shelfLife = shelfLife;
    }

    public void setPreparationTime(String preparationTime) {
        this.preparationTime = preparationTime;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public void setMeals(List<String> meals) {
        this.meals = meals;
    }

    public void setBenefits(List<String> benefits) {
        this.benefits = benefits;
    }

    public void setAllergies(List<String> allergies) {
        this.allergies = allergies;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setNutritionalInfo(NutritionalInfo nutritionalInfo) {
        this.nutritionalInfo = nutritionalInfo;
    }

    public void setCustomization(Customization customization) {
        this.customization = customization;
    }

    public void setPrices(List<ItemPriceDTO> prices) {
        this.prices = prices;
    }

    /**
     * Nested DTO for nutritional information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutritionalInfo {
        private String protein;
        private String carbs;
        private String fiber;
        private String sugar;
        private String vitaminC;
        private String iron;
    }

    /**
     * Nested DTO for customization options
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customization {
        private List<String> sugarLevel;
        private List<String> iceLevel;
        private List<String> addOns;
    }

    /**
     * DTO for item price
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemPriceDTO {
        private String priceId;
        private String name;
        private String size; // 200ml, 300ml, 500ml
        private BigDecimal price;
        private String currencyCode;
        private String pricingModel;
        private Integer period;
        private String periodUnit;
    }
}
