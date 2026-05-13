package com.bookmyjuice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.ServiceAreaEntity;

@Repository
public interface ServiceAreaRepository extends JpaRepository<ServiceAreaEntity, Long> {

    Optional<ServiceAreaEntity> findByPincode(String pincode);

    List<ServiceAreaEntity> findByCity(String city);

    List<ServiceAreaEntity> findByIsServicedTrue();
}
