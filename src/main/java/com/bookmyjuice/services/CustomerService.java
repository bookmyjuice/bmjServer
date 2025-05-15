package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.CustomerEntity;
import com.bookmyjuice.models.CustomerMapper;
import com.bookmyjuice.repository.CustomerRepository;
import com.chargebee.models.Event;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public String getCustomerDetails() {
        return "Customer details";
    }

    public boolean existsById(String id) {
        // Logic to check if customer exists by ID
        return customerRepository.existsByCustomerId(id);
    }

    public Boolean saveCustomer(Event e) {
        if (e == null) {
            return false;
        } else {
            try {
                // Check if the customer already exists
                if (customerRepository.existsByCustomerId(e.content().customer().id())) {
                    // Customer already exists, handle accordingly (e.g., update or ignore)
                    return false;
                } else {
                    // Customer does not exist, proceed to save
                    CustomerEntity customer = CustomerMapper.toEntity(e.content().customer());
                    customerRepository.save(customer);
                    return true;
                }

            } catch (Exception ex) {
                // Handle exception
                System.out.println("Error occurred while saving customer: " + ex.getMessage());
                return false;
            }

        }
    }

    public Boolean deleteCustomer(Event e) {
        if (e == null) {
            return false;
        } else {
            customerRepository.deleteById(e.content().customer().id());
            return true;
        }
    }

    public Boolean updateCustomer(Event e) {
        if (e == null) {
            return false;
        } else {
            String customerId = e.content().customer().id();
            if (customerRepository.existsByCustomerId(customerId)) {
                // Retrieve the existing customer entity
                CustomerEntity existingCustomer = customerRepository.findByCustomerId(customerId);

                // Map updated fields from the event to the existing customer entity
                CustomerEntity updatedCustomer = CustomerMapper.toEntity(e.content().customer());
                existingCustomer.setFirstName(updatedCustomer.getFirstName());
                existingCustomer.setLastName(updatedCustomer.getLastName());
                existingCustomer.setEmail(updatedCustomer.getEmail());
                existingCustomer.setPhone(updatedCustomer.getPhone());
                existingCustomer.setCompany(updatedCustomer.getCompany());
                existingCustomer.setAutoCollection(updatedCustomer.getAutoCollection());
                existingCustomer.setNetTermDays(updatedCustomer.getNetTermDays());
                existingCustomer.setAllowDirectDebit(updatedCustomer.isAllowDirectDebit());
                existingCustomer.setUpdatedAt(updatedCustomer.getUpdatedAt());
                existingCustomer.setTaxability(updatedCustomer.getTaxability());
                existingCustomer.setPiiCleared(updatedCustomer.getPiiCleared());
                existingCustomer.setChannel(updatedCustomer.getChannel());
                existingCustomer.setResourceVersion(updatedCustomer.getResourceVersion());
                existingCustomer.setDeleted(updatedCustomer.isDeleted());
                existingCustomer.setPromotionalCredits(updatedCustomer.getPromotionalCredits());
                existingCustomer.setRefundableCredits(updatedCustomer.getRefundableCredits());
                existingCustomer.setExcessPayments(updatedCustomer.getExcessPayments());
                existingCustomer.setUnbilledCharges(updatedCustomer.getUnbilledCharges());
                existingCustomer.setPreferredCurrencyCode(updatedCustomer.getPreferredCurrencyCode());
                existingCustomer.setPrimaryPaymentSourceId(updatedCustomer.getPrimaryPaymentSourceId());

                // Save the updated customer entity
                customerRepository.save(existingCustomer);
                return true;
            } else {
                // Customer does not exist, cannot update
                return false;
            }
        }
    }
}
