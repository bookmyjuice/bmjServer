package com.bookmyjuice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.InvoiceEntity;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    List<InvoiceEntity> findByCustomerId(String customerId);
}
