package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bookmyjuice.models.entities.AttachedItemEntity;

@Repository
public interface AttachedItemRepository extends JpaRepository<AttachedItemEntity, String> {
    // Add custom queries if needed
}
