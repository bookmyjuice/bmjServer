package com.bookmyjuice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.CustomerEntity;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {

    /**
     * Customers whose phone matches exactly. Used by the identity linker to bridge a BMJ
     * user to an existing Chargebee customer on login (login self-heal).
     */
    List<CustomerEntity> findByPhone(String phone);

    /**
     * Customers whose email matches, case-insensitively.
     */
    List<CustomerEntity> findByEmailIgnoreCase(String email);
}
