// package online.bmj.www.repository.jpa;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import java.util.UUID;

// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Modifying;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.lang.NonNull;
// import org.springframework.stereotype.Repository;

// import online.bmj.www.entities.SubscriptionEntity;

// @Repository
// public interface SubscriptionsRepository extends JpaRepository<SubscriptionEntity, UUID> {

//     @Cacheable(value = "subscriptions", key = "#chargebeeId")
//     Optional<SubscriptionEntity> findByChargebeeId(String chargebeeId);

//     @Cacheable(value = "subscriptions", key = "#subscriptionId")
//     @Override
//     Optional<SubscriptionEntity> findById(@NonNull UUID subscriptionId); // Changed method name to 'findById'

//     @Cacheable(value = "customerSubscriptions", key = "#customerId")
//     List<SubscriptionEntity> findByCustomerIdAndStatus(UUID customerId, String status); // Changed method name to 'findByCustomerIdAndStatus'

//     @Query("SELECT s FROM SubscriptionEntity s WHERE s.nextBillingDate BETWEEN :start AND :end")
//     List<SubscriptionEntity> findSubscriptionsDueForRenewal(LocalDateTime start, LocalDateTime end);

//     @CacheEvict(value = {"subscriptions", "customerSubscriptions"}, allEntries = true)
//     @Modifying
//     @Query("UPDATE SubscriptionEntity s SET s.status = :status WHERE s.chargebeeId = :chargebeeId")
//     int updateStatus(String chargebeeId, String status);

//     @CacheEvict(value = {"subscriptions", "customerSubscriptions"}, key = "#chargebeeId")
//     @Modifying
//     @Query("DELETE FROM SubscriptionEntity s WHERE s.chargebeeId = :chargebeeId")
//     int deleteByChargebeeId(String chargebeeId);

//     @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SubscriptionEntity s WHERE s.chargebeeId = :chargebeeId")
//     boolean existsByChargebeeId(String chargebeeId);

//     @CacheEvict(value = {"subscriptions", "customerSubscriptions"}, allEntries = true)
//     @Modifying
//     @Query("UPDATE SubscriptionEntity s SET " +
//            "s.planId = :planId, " +
//            "s.planName = :planName, " +
//            "s.nextBillingDate = :nextBillingDate, " +
//            "s.billingPeriodUnit = :billingPeriodUnit, " +
//            "s.billingPeriod = :billingPeriod, " +
//            "s.metadata = :metadata " +
//            "WHERE s.chargebeeId = :chargebeeId")
//     int updateSubscriptionDetails(
//         String chargebeeId,
//         String planId,
//         String planName,
//         LocalDateTime nextBillingDate,
//         String billingPeriodUnit,
//         Integer billingPeriod,
//         Map<String, Object> metadata
//     );
// }