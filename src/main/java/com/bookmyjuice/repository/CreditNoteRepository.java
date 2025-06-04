package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.CreditNoteEntity;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNoteEntity, String> {
    // Add custom queries if needed
}
