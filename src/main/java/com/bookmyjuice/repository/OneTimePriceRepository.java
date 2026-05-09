package com.bookmyjuice.repository;

import com.bookmyjuice.models.OneTimePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OneTimePriceRepository extends JpaRepository<OneTimePrice, String> {
}
