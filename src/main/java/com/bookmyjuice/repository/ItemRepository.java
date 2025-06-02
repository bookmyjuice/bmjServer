package com.bookmyjuice.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.ItemEntity;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, String> {
}