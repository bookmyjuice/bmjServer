// package online.bmj.www.entities;

// import java.time.LocalDateTime;
// import java.util.Map;
// import java.util.UUID;

// import org.hibernate.annotations.CreationTimestamp;
// import org.hibernate.annotations.JdbcTypeCode;
// import org.hibernate.annotations.UpdateTimestamp;
// import org.hibernate.type.SqlTypes;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.EnumType;
// import jakarta.persistence.Enumerated;
// import jakarta.persistence.FetchType;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.JoinColumn;
// import jakarta.persistence.ManyToOne;
// import jakarta.persistence.Table;
// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// @Entity
// @Table(name = "subscriptions")
// @Data
// @NoArgsConstructor
// @AllArgsConstructor
// @Getter
// @Setter
// public class SubscriptionEntity {

//     @Id
//     @GeneratedValue(strategy = GenerationType.UUID)
//     private UUID id;

//     @Column(name = "chargebee_id", nullable = false, unique = true)
//     private String chargebeeId;

//     @Column(name = "plan_id", nullable = false)
//     private String planId;

//     @Column(name = "plan_name", nullable = false)
//     private String planName;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "customer_id", nullable = false)
//     private CustomerEntity customer;

//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false)
//     private SubscriptionStatus status;

//     @Column(name = "start_date", nullable = false)
//     private LocalDateTime startDate;

//     @Column(name = "next_billing_date", nullable = false)
//     private LocalDateTime nextBillingDate;

//     @Column(name = "billing_period_unit", nullable = false)
//     private String billingPeriodUnit;

//     @Column(name = "billing_period")
//     private Integer billingPeriod;

//     @JdbcTypeCode(SqlTypes.JSON)
//     @Column(columnDefinition = "jsonb")
//     private Map<String, Object> metadata;

//     @CreationTimestamp
//     @Column(name = "created_at", updatable = false)
//     private LocalDateTime createdAt;

//     @UpdateTimestamp
//     @Column(name = "updated_at")
//     private LocalDateTime updatedAt;

//     public enum SubscriptionStatus {
//         ACTIVE, 
//         FUTURE, 
//         IN_TRIAL, 
//         NON_RENEWING, 
//         CANCELLED, 
//         PAUSED,
//         EXPIRED
//     }
// }