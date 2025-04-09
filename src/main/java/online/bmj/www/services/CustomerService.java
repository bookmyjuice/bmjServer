// package online.bmj.www.services;

// import java.util.Optional;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import online.bmj.www.entities.CustomerEntity;
// import online.bmj.www.repository.jpa.CustomerRepository;
// import online.bmj.www.repository.redis.CustomerCache;

// @Service
// public class CustomerService {

//     @Autowired
//     private CustomerRepository customerRepository;

//     @Autowired
//     private CustomerCache customerCache;

//     /**
//      * Fetches a customer by ID. First checks the cache, then the database.
//      */
//     public CustomerEntity getCustomerById(String customerId) {
//         CustomerEntity customer = customerCache.getCustomer(customerId);
//         if (customer == null) {
//             Optional<CustomerEntity> optionalCustomer = customerRepository.findByCustomerId(customerId);
//             if (optionalCustomer.isPresent()) {
//                 customer = optionalCustomer.get();
//                 customerCache.cacheCustomer(customer); // Cache the customer
//             }
//         }
//         return customer;
//     }

//     /**
//      * Creates a new customer and caches it.
//      */
//     public CustomerEntity createCustomer(CustomerEntity customer) {
//         CustomerEntity savedCustomer = customerRepository.save(customer);
//         customerCache.cacheCustomer(savedCustomer);
//         return savedCustomer;
//     }

//     /**
//      * Updates an existing customer and caches the updated customer.
//      */
//     public CustomerEntity updateCustomer(String customerId, CustomerEntity customerDetails) {
//         Optional<CustomerEntity> optionalCustomer = customerRepository.findByCustomerId(customerId);
//         if (!optionalCustomer.isPresent()) {
//             return null;
//         }
//         CustomerEntity customer = optionalCustomer.get();
//         customer.setEmail(customerDetails.getEmail());
//         customer.setPhoneNumber(customerDetails.getPhoneNumber());
//         // Update other fields as necessary
//         CustomerEntity updatedCustomer = customerRepository.save(customer);
//         customerCache.cacheCustomer(updatedCustomer);
//         return updatedCustomer;
//     }

//     /**
//      * Deletes a customer by ID and evicts it from the cache.
//      */
//     public void deleteCustomer(String customerId) {
//         customerRepository.deleteByCustomerId(customerId);
//         customerCache.evictCustomer(customerId);
//     }

//     /**
//      * Clears all customers from the cache.
//      */
//     public void evictAllCustomers() {
//         customerCache.evictAllCustomers();
//     }
// }