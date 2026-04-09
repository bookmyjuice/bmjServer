package com.bookmyjuice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.bookmyjuice.security.jwt.JwtUtils.getUserIdFromSecurityContext;
import com.bookmyjuice.services.OrderService;
import com.bookmyjuice.services.UserDetailsImpl;

@RestController
@RequestMapping("/api/test")
public class OrdersController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderService.fetchOrders());
    }

    @GetMapping("/ordersByCustomerId")
    public ResponseEntity<?> getOrdersByCustomerId() {
        return ResponseEntity.ok(orderService.fetchOrdersByCustomerId(getUserIdFromSecurityContext()));
    }
}