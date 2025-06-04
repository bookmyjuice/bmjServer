package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.PlanEntity;

@Repository
public interface PlanRepository extends JpaRepository<PlanEntity, String> {
    // Add custom queries if needed
}
