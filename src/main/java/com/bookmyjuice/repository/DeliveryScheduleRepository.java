package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.DeliveryScheduleEntity;

/**
 * Lookup of the day-wise delivery schedule captured at subscription checkout.
 * The primary key is the subscription id reserved before the Chargebee hosted
 * page was created.
 */
@Repository
public interface DeliveryScheduleRepository extends JpaRepository<DeliveryScheduleEntity, String> {
}
