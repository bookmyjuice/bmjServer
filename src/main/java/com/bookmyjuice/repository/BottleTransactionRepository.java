package com.bookmyjuice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.BottleTransactionEntity;

@Repository
public interface BottleTransactionRepository extends JpaRepository<BottleTransactionEntity, Long> {

    /**
     * Find all bottle transactions for a given customer, ordered by most recent first.
     */
    List<BottleTransactionEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    /**
     * Find all transactions related to a specific order.
     */
    List<BottleTransactionEntity> findByOrderIdOrderByCreatedAtDesc(String orderId);

    /**
     * Aggregate bottle counts per customer per bottle type.
     * Returns Object[] with: [customerId, bottleType, action, SUM(quantity)]
     * Caller groups by action to compute ledger.
     */
    @Query("SELECT b.customerId, b.bottleType, b.action, SUM(b.quantity) " +
           "FROM BottleTransactionEntity b " +
           "WHERE b.customerId = :customerId " +
           "GROUP BY b.customerId, b.bottleType, b.action")
    List<Object[]> aggregateByCustomer(@Param("customerId") String customerId);

    /**
     * Get all unique customers who have bottle transactions.
     */
    @Query("SELECT DISTINCT b.customerId FROM BottleTransactionEntity b")
    List<String> findAllCustomerIds();
}
