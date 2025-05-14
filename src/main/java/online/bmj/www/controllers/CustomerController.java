// package online.bmj.www.controllers;

// import java.util.Optional;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.PutMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import online.bmj.www.entities.CustomerEntity;
// import online.bmj.www.services.CustomerService;

// @RestController
// @RequestMapping("/api/customers")
// public class CustomerController {

//     @Autowired
//     private CustomerService customerService;

//     @GetMapping("/{customerId}")
//     public ResponseEntity<?> getCustomer(@PathVariable String customerId) {
//         // CustomerEntity customer = customerService.getCustomerById(customerId);
//         // if (customer == null) {
//         //     return ResponseEntity.notFound().build();
//         // }
//         return ResponseEntity.ok(customerService.getCustomerById(customerId));
//     }

//     @PostMapping
//     public ResponseEntity<?> createCustomer(@RequestBody CustomerEntity customer) {
//         // CustomerEntity savedCustomer = customerService.createCustomer(customer);
//         return ResponseEntity.ok(customerService.createCustomer(customer));
//     }

//     @PutMapping("/{customerId}")
//     public ResponseEntity<?> updateCustomer(@PathVariable String customerId, @RequestBody CustomerEntity customerDetails) {
//         // CustomerEntity updatedCustomer = customerService.updateCustomer(customerId, customerDetails);
//         // if (updatedCustomer == null) {
//         //     return ResponseEntity.notFound().build();
//         // }
//         return ResponseEntity.ok(customerService.updateCustomer(customerId, customerDetails));
//     }

//     @DeleteMapping("/{customerId}")
//     public ResponseEntity<?> deleteCustomer(@PathVariable String customerId) {
//         customerService.deleteCustomer(customerId);
//         return ResponseEntity.noContent().build();
//     }

//     @DeleteMapping("/cache")
//     public ResponseEntity<?> evictAllCustomersCache() {
//         customerService.evictAllCustomers();
//         return ResponseEntity.noContent().build();
//     }
// }