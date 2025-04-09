// package online.bmj.www.entities;

// import java.util.List;

// import org.hibernate.annotations.Cache;
// import org.hibernate.annotations.CacheConcurrencyStrategy;
// import org.springframework.data.redis.core.RedisHash;

// import jakarta.persistence.CascadeType;
// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.Id;
// import jakarta.persistence.OneToMany;
// import jakarta.persistence.Table;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// @Entity
// @Table(name = "customer")
// @Getter
// @Setter
// @NoArgsConstructor
// @Cache(region = "customersCache", usage = CacheConcurrencyStrategy.READ_WRITE)
// @RedisHash("customer")
// public class CustomerEntity {
//     @Id
//     private String id; // Chargebee customer ID
    
//     @Column(nullable = false)
//     private String email;
    
//     @Column(name = "phone_number")
//     private String phoneNumber;
    
//     @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
//     @Cache(region = "subscriptionCache", usage = CacheConcurrencyStrategy.READ_WRITE)
//     private List<SubscriptionEntity> subscriptions;

//     public CustomerEntity(String id, String email, String phoneNumber) {
//         this.id = id;
//         this.email = email;
//         this.phoneNumber = phoneNumber;
//     }
// }