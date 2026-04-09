package com.bookmyjuice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.ProductFamilyEntity;

@Repository
public interface ProductFamilyRepository extends JpaRepository<ProductFamilyEntity, String> {
    
    Optional<ProductFamilyEntity> findByName(String name);
    
    boolean existsByName(String name);
}
