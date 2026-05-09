package com.bookmyjuice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.Product;
import com.bookmyjuice.services.ProductSyncService;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Autowired
    private ProductSyncService productSyncService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productSyncService.getAllProducts();
    }
}
