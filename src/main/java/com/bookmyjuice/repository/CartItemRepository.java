package com.bookmyjuice.repository;

import com.bookmyjuice.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Optional: Add helper methods later if needed (e.g., findByPriceId)
}
