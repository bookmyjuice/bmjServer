package com.bookmyjuice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.models.entities.SubscriptionEntity;



@Repository
public interface SubscriptionEntityRepository extends JpaRepository<SubscriptionEntity, String> {
    // SubscriptionEntity findByCustomerId(String userId); // Assuming you have a userId field in Subscription
    // boolean existsByCustomerId(String userId); // Assuming you have a userId field in Subscription
    // boolean existsBy(String id); // Assuming you have a userId field in Subscription
    // boolean existsByFamilyId(String familyId); // Assuming you have a familyId field in Subscription
    boolean existsByCustomerId(String Id);
    List<SubscriptionEntity> findByCustomer(CustomerEntity customer);

    /**
     * Load all subscriptions that point at a raw customer key, without going through a
     * {@link CustomerEntity} instance.
     *
     * <p>Used by the identity resolver's transitional mismatch check (and later by the
     * reconciler): it can answer "does this user's data live under a different key?"
     * without first proving that key maps to an entity row.
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.customer.id = :customerId")
    List<SubscriptionEntity> findByCustomerIdValue(@Param("customerId") String customerId);

    /**
     * Guarded variant of {@link #findByCustomer(CustomerEntity)}.
     *
     * <p>A null customer used to reach this repository and produce an empty list, which
     * the API then reported as "no subscriptions". Failing loudly here makes that class of
     * defect impossible to reintroduce: an unresolved identity can no longer masquerade as
     * an empty result set.
     *
     * @throws IllegalArgumentException when the customer, or its id, is missing
     */
    default List<SubscriptionEntity> findByCustomerGuarded(CustomerEntity customer) {
        if (customer == null || customer.getId() == null || customer.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "customer identity must not be null or blank when loading subscriptions");
        }
        return findByCustomer(customer);
    }
}

