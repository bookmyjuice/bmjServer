// package online.bmj.www.repository.redis;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.CachePut;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.stereotype.Component;

// import online.bmj.www.entities.CustomerEntity;
// import online.bmj.www.repository.jpa.CustomerRepository;

// @Component
// public class CustomerCache {

//     @Autowired
//     CustomerRepository customerRepository;

//     /**
//      * Fetches a customer from cache. If not found, retrieves from DB and caches it.
//      */
//     @Cacheable(value = "customer", key = "#customerId")
//     public CustomerEntity getCustomer(String customerId) {
//         return customerRepository.findByCustomerId(customerId).orElse(null);
//     }

//     /**
//      * Adds or updates a customer in the cache.
//      */
//     @CachePut(value = "customer", key = "#customer.id")
//     public CustomerEntity cacheCustomer(CustomerEntity customer) {
//         return customer;
//     }

//     /**
//      * Removes a customer from the cache when deleted.
//      */
//     @CacheEvict(value = "customer", key = "#customerId")
//     public void evictCustomer(String customerId) {
//         // No return value
//     }

//     /**
//      * Clears all customers from the cache.
//      */
//     @CacheEvict(value = "customer", allEntries = true)
//     public void evictAllCustomers() {
//         // No return value
//     }
// }