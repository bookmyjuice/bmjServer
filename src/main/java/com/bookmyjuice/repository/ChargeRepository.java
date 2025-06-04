package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bookmyjuice.models.entities.ChargeEntity;


@Repository
public interface ChargeRepository extends JpaRepository<ChargeEntity, String> {
    // Add custom queries if needed
}
