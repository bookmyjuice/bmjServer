package com.bookmyjuice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.entities.BillingAddressEntity;
import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.models.mappers.CustomerMapper;
import com.bookmyjuice.repository.CustomerRepository;
import com.chargebee.models.Event;

@Service
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    public CustomerEntity getCustomerById(String id) {
        logger.info("Fetching customer with ID: {}", id);
        return customerRepository.findById(id).orElseThrow(
            () -> {
                logger.error("Customer not found with ID: {}", id);
                return new IllegalArgumentException("Customer not found with id: " + id);
            }
        );
    }

    public boolean existsById(String id) {
        logger.info("Checking if customer exists with ID: {}", id);
        return customerRepository.existsById(id);
    }

    public Boolean saveCustomer(Event e) {
        if (e == null || e.content() == null || e.content().customer() == null) {
            logger.error("Invalid event data received for saving customer: {}", e);
            return false;
        }
        try {
            String customerId = e.content().customer().id();
            logger.info("Processing save customer event for ID: {}", customerId);
            if (customerRepository.existsById(customerId)) {
                logger.info("Customer already exists with ID: {}", customerId);
                return false;
            } else {
                CustomerEntity customer = CustomerMapper.toEntity(e.content().customer());
                // Ensure addresses are linked
                // if (customer.getBillingAddress() != null) {
                //     customer.getBillingAddress().setId(customerId + "-billing");
                // }
                // if (customer.getShippingAddress() != null) {
                //     customer.getShippingAddress().setId(customerId + "-shipping");
                // }
                customerRepository.save(customer);
                logger.info("Customer saved successfully with ID: {}", customerId);
                return true;
            }
        } catch (Exception ex) {
            logger.error("Error occurred while saving customer: {}", ex.getMessage(), ex);
            return false;
        }
    }

    public Boolean deleteCustomer(Event e) {
        if (e == null || e.content() == null || e.content().customer() == null) {
            logger.error("Invalid event data received for deleting customer: {}", e);
            return false;
        }

        try {
            String customerId = e.content().customer().id();
            logger.info("Processing delete customer event for ID: {}", customerId);

            if (customerRepository.existsById(customerId)) {
                customerRepository.deleteById(customerId);
                logger.info("Customer deleted successfully with ID: {}", customerId);
                return true;
            } else {
                logger.warn("Customer not found with ID: {}", customerId);
                return false;
            }
        } catch (Exception ex) {
            logger.error("Error occurred while deleting customer: {}", ex.getMessage(), ex);
            return false;
        }
    }

    public Boolean updateCustomer(Event e) {
        if (e == null || e.content() == null || e.content().customer() == null) {
            logger.error("Invalid event data received for updating customer: {}", e);
            return false;
        }
        try {
            String customerId = e.content().customer().id();
            logger.info("Processing update customer event for ID: {}", customerId);
            if (customerRepository.existsById(customerId)) {
                CustomerEntity existingCustomer = customerRepository.findById(customerId)
                        .orElseThrow(() -> {
                            logger.error("Customer not found with ID: {}", customerId);
                            return new RuntimeException("Customer not found with ID: " + customerId);
                        });
                CustomerEntity updatedCustomer = CustomerMapper.toEntity(e.content().customer());
                logger.debug("Mapped updated customer entity: {}", updatedCustomer);
                // Update fields
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
                // Update addresses
                if (updatedCustomer.getBillingAddress() != null) {
                    // updatedCustomer.getBillingAddress().setId(customerId + "-billing");
                    existingCustomer.setBillingAddress(updatedCustomer.getBillingAddress());
                }
                if (updatedCustomer.getShippingAddress() != null) {
                    // updatedCustomer.getShippingAddress().setId(customerId + "-shipping");
                    existingCustomer.setShippingAddress(updatedCustomer.getShippingAddress());
                }
                customerRepository.save(existingCustomer);
                logger.info("Customer updated successfully with ID: {}", customerId);
                return true;
            } else {
                logger.warn("Customer not found with ID: {}", customerId);
                return false;
            }
        } catch (Exception ex) {
            logger.error("Error occurred while updating customer: {}", ex.getMessage(), ex);
            return false;
        }
    }

    public boolean migrateCustomer(Event e) {
        if (e == null || e.content() == null || e.content().customer() == null) {
            logger.error("Invalid event data received for migrating customer: {}", e);
            return false;
        }

        try {
            String customerId = e.content().customer().id();
            logger.info("Processing customer migration event for ID: {}", customerId);

            CustomerEntity customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

            customer.setMigrated(true);
            customerRepository.save(customer);
            logger.info("Customer migrated successfully: {}", customerId);

            return true;
        } catch (Exception ex) {
            logger.error("Error migrating customer: {}", ex.getMessage());
            return false;
        }
    }

    private BillingAddressEntity mapBillingAddress(com.chargebee.models.Customer.BillingAddress billingAddress) {
        if (billingAddress == null) {
            return null;
        }

        try {
            String jsonString = billingAddress.toJson();
            com.chargebee.org.json.JSONObject json = new com.chargebee.org.json.JSONObject(jsonString);
            BillingAddressEntity entity = new BillingAddressEntity();
            entity.setLine1(json.optString("line1"));
            entity.setLine2(json.optString("line2"));
            entity.setCity(json.optString("city"));
            entity.setState(json.optString("state"));
            entity.setCountry(json.optString("country"));
            entity.setZip(json.optString("zip"));
            entity.setPhone(json.optString("phone"));
            return entity;
        } catch (Exception ex) {
            logger.error("Error mapping billing address: {}", ex.getMessage(), ex);
            return null;
        }
    }

    public boolean updateBillingAddress(Event e) {
        if (e == null || e.content() == null || e.content().customer() == null) {
            logger.error("Invalid event data received for updating billing address: {}", e);
            return false;
        }

        try {
            String customerId = e.content().customer().id();
            logger.info("Processing billing address update event for ID: {}", customerId);

            CustomerEntity customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

            BillingAddressEntity billingAddressEntity = mapBillingAddress(e.content().customer().billingAddress());
            customer.setBillingAddress(billingAddressEntity);
            customerRepository.save(customer);
            logger.info("Customer billing address updated successfully: {}", customerId);

            return true;
        } catch (Exception ex) {
            logger.error("Error updating billing address: {}", ex.getMessage());
            return false;
        }
    }
}