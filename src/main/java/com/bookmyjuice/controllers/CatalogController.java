package com.bookmyjuice.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.dto.ChargeItemDTO;
import com.bookmyjuice.services.ItemService;

/**
 * Production catalog endpoint (V1 launch freeze).
 *
 * This is the production replacement for the non-production
 * {@code GET /api/test/charge-items} route (see {@link TestController}).
 *
 * It is pure plumbing: it delegates to the exact same Chargebee-backed
 * {@link ItemService#getChargeItemsWithCategoriesAndSizes()} method that the
 * {@code /api/test/charge-items} route uses, so the response contract
 * (categories Delight/Signature/Premium, sizes 200/300/500ml) is identical.
 *
 * No new business functionality is introduced.
 *
 * Mapped under both {@code /api/catalog} and {@code /api/v1/catalog} so that
 * versioned and unversioned clients resolve the same handler.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping({ "/api/catalog", "/api/v1/catalog" })
public class CatalogController {

    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);

    @Autowired
    private ItemService itemService;

    /**
     * GET /api/catalog/charge-items (alias: /api/v1/catalog/charge-items)
     *
     * Fetch Chargebee items, filtered by Delight/Signature/Premium categories
     * and 200/300/500ml sizes.
     *
     * Success body is a JSON array of {@link ChargeItemDTO}.
     */
    @GetMapping("/charge-items")
    @ResponseBody
    public ResponseEntity<?> getChargeItemsWithCategoriesAndSizes() {
        try {
            List<ChargeItemDTO> items = itemService.getChargeItemsWithCategoriesAndSizes();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Error fetching charge items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error",
                            e.getMessage() == null ? "Failed to fetch charge items" : e.getMessage()));
        }
    }
}
