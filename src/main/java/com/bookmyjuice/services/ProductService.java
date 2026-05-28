package com.bookmyjuice.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.dto.response.ProductResponse;
import com.bookmyjuice.models.Product;
import com.bookmyjuice.repository.ProductRepository;

/**
 * Service layer for product endpoints matching the Flutter contract.
 * Provides all CRUD, search, filtering, and featured product operations.
 */
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * Get all products.
     */
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a single product by ID.
     */
    public ProductResponse getProductById(String id) {
        return productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .orElse(null);
    }

    /**
     * Get products by family (juice, smoothie, detox).
     * Matches against the category field in the DB, then maps to family in response.
     */
    public List<ProductResponse> getProductsByFamily(String family) {
        if (family == null || family.isBlank()) {
            return getAllProducts();
        }

        String lowerFamily = family.toLowerCase().trim();

        return productRepository.findAll().stream()
                .filter(p -> {
                    String category = p.getCategory();
                    if (category == null) return "juice".equals(lowerFamily);
                    String lowerCat = category.toLowerCase();
                    switch (lowerFamily) {
                        case "juice":
                            return !lowerCat.contains("smoothie") && !lowerCat.contains("detox")
                                    && !lowerCat.contains("cleanse");
                        case "smoothie":
                            return lowerCat.contains("smoothie");
                        case "detox":
                            return lowerCat.contains("detox") || lowerCat.contains("cleanse");
                        default:
                            return lowerCat.contains(lowerFamily);
                    }
                })
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search products by query string.
     * Matches against name, description (from metadata), and category.
     */
    public List<ProductResponse> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return getAllProducts();
        }

        String lowerQuery = query.toLowerCase().trim();

        return productRepository.findAll().stream()
                .filter(p -> {
                    // Match by name
                    if (p.getName() != null && p.getName().toLowerCase().contains(lowerQuery)) {
                        return true;
                    }
                    // Match by category
                    if (p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerQuery)) {
                        return true;
                    }
                    // Match by metadata (description)
                    if (p.getMetadata() != null && p.getMetadata().toLowerCase().contains(lowerQuery)) {
                        return true;
                    }
                    // Match by ID
                    if (p.getId() != null && p.getId().toLowerCase().contains(lowerQuery)) {
                        return true;
                    }
                    return false;
                })
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get featured products only.
     */
    public List<ProductResponse> getFeaturedProducts() {
        return productRepository.findAll().stream()
                .filter(p -> {
                    if (p.getMetadata() == null) return false;
                    String meta = p.getMetadata().toLowerCase();
                    return meta.contains("\"featured\":true") || meta.contains("\"featured\": true");
                })
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
}