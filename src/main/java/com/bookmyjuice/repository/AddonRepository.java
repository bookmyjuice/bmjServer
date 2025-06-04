package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.AddonEntity;

@Repository
public interface AddonRepository extends JpaRepository<AddonEntity, String> {
    // Add custom queries if needed
}
