package com.bookmyjuice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.dto.response.ProductResponse;
import com.bookmyjuice.models.Product;
import com.bookmyjuice.services.ProductService;
import com.bookmyjuice.services.ProductSyncService;

/**
 * Product endpoints matching the Flutter ProductsBloc contract.
 * Exposed at both /api/products and /api/v1/products for backward compatibility.
 *
 * GET  /api/products              → all products
 * GET  /api/products/{id}         → single product
 * GET  /api/products/family/{family}  → by family (juice|smoothie|detox)
 * GET  /api/products/search?q=    → search by query
 * GET  /api/products/featured     → featured products only
 */
@RestController
@RequestMapping({"/api/products", "/api/v1/products"})
public class ProductController {

    @Autowired
    private ProductSyncService productSyncService;

    @Autowired
    private ProductService productService;

    /**
     * GET /api/products — all products (uses new ProductService returning ProductResponse DTOs).
     * Also available at /api/v1/products (legacy).
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(value = "family", required = false) String family) {
        if (family != null && !family.isBlank()) {
            return ResponseEntity.ok(productService.getProductsByFamily(family));
        }
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * GET /api/products/{id} — single product by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        ProductResponse product = productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    /**
     * GET /api/products/family/{family} — products by family.
     * Valid families: juice, smoothie, detox
     */
    @GetMapping("/family/{family}")
    public ResponseEntity<List<ProductResponse>> getProductsByFamily(@PathVariable String family) {
        List<ProductResponse> products = productService.getProductsByFamily(family);
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/search?q={query} — search products.
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam("q") String query) {
        List<ProductResponse> products = productService.searchProducts(query);
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/featured — featured products only.
     */
    @GetMapping("/featured")
    public ResponseEntity<List<ProductResponse>> getFeaturedProducts() {
        List<ProductResponse> products = productService.getFeaturedProducts();
        return ResponseEntity.ok(products);
    }

    // ──────────────── Legacy v1 passthrough (return Product entities) ────────────────

    /**
     * Legacy endpoint returning raw Product entities.
     * Kept for backward compatibility with existing sync consumers.
     */
    @GetMapping("/legacy")
    public List<Product> getAllProductsLegacy() {
        return productSyncService.getAllProducts();
    }
}