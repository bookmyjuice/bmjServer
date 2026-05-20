package com.bookmyjuice.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.DeliverySlotEntity;

@Repository
public interface DeliverySlotRepository extends JpaRepository<DeliverySlotEntity, Long> {

    List<DeliverySlotEntity> findByServiceAreaIdAndSlotDateAndIsActiveTrue(Long serviceAreaId, LocalDate slotDate);

    List<DeliverySlotEntity> findByServiceAreaIdAndSlotDateBetweenAndIsActiveTrue(Long serviceAreaId, LocalDate start, LocalDate end);

    List<DeliverySlotEntity> findBySlotDateAfterAndIsActiveTrue(LocalDate date);
}
