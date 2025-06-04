package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.ItemPriceEntity;

@Repository
public interface ItemPriceRepository extends JpaRepository<ItemPriceEntity, String> {
    // Add custom queries if needed
}
