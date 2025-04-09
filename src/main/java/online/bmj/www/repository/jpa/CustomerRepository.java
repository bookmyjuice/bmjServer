// package online.bmj.www.repository.jpa;

// import java.util.Optional;

// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// import online.bmj.www.entities.CustomerEntity;

// @Repository
// public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {

//     /**
//      * Find a customer by Chargebee customer ID.
//      * 
//      * @param customerId the Chargebee-provided unique customer ID
//      * @return Optional<CustomerEntity>
//      */
//     Optional<CustomerEntity> findByCustomerId(String customerId);

//     /**
//      * Find a customer by email.
//      * 
//      * @param email the customer's email
//      * @return Optional<CustomerEntity>
//      */
//     Optional<CustomerEntity> findByEmail(String email);

//     /**
//      * Check if a customer exists by customer ID.
//      * 
//      * @param customerId the Chargebee customer ID
//      * @return boolean - true if the customer exists, false otherwise
//      */
//     boolean existsByCustomerId(String customerId);

//     /**
//      * Delete a customer by Chargebee customer ID.
//      * 
//      * @param customerId the Chargebee customer ID
//      */
//     void deleteByCustomerId(String customerId);
//     void addCustomer(CustomerEntity customer);
//     void updateCustomer(CustomerEntity customer);
//     void deleteCustomer(CustomerEntity customer);
//     void getCustomer(String customerId);
// }
