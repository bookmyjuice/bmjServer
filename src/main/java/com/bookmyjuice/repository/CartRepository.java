package com.bookmyjuice.repository;

import com.bookmyjuice.models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    // Find the specific cart for the logged-in user
    Optional<Cart> findByUserId(Long userId);
}
