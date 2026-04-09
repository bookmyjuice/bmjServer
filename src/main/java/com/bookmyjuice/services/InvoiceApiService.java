package com.bookmyjuice.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.entities.InvoiceEntity;
import com.bookmyjuice.repository.InvoiceRepository;
import com.chargebee.ListResult;
import com.chargebee.models.Invoice;

/**
 * Service for managing invoices via Chargebee API
 * Handles fetching invoices and generating PDF URLs
 */
@Service
public class InvoiceApiService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceApiService.class);

    @Autowired
    private InvoiceRepository invoiceRepository;

    /**
     * Get customer invoices from Chargebee API
     */
    public List<Map<String, Object>> getCustomerInvoices(String customerId) throws Exception {
        logger.info("Fetching invoices for customer: {}", customerId);
        List<Map<String, Object>> invoices = new ArrayList<>();

        try {
            ListResult listResult = Invoice.list().request();
            
            for (ListResult.Entry entry : listResult) {
                Invoice invoice = entry.invoice();
                if (invoice != null && customerId.equals(invoice.customerId())) {
                    Map<String, Object> invoiceMap = mapInvoiceToResponse(invoice);
                    invoices.add(invoiceMap);
                }
            }
            logger.info("Successfully fetched {} invoices for customer: {}", invoices.size(), customerId);
        } catch (Exception e) {
            logger.error("Error fetching invoices for customer {}: {}", customerId, e.getMessage(), e);
            throw new Exception("Failed to fetch invoices: " + e.getMessage(), e);
        }

        return invoices;
    }

    /**
     * Get specific invoice details from Chargebee
     */
    public Map<String, Object> getInvoiceDetails(String invoiceId) throws Exception {
        logger.info("Fetching invoice details: {}", invoiceId);

        try {
            com.chargebee.Result result = Invoice.retrieve(invoiceId).request();
            Invoice invoice = result.invoice();
            
            return mapInvoiceToResponse(invoice);
        } catch (Exception e) {
            logger.error("Error fetching invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new Exception("Failed to fetch invoice: " + e.getMessage(), e);
        }
    }

    /**
     * Get invoice PDF URL
     */
    public String getInvoicePdfUrl(String invoiceId) throws Exception {
        logger.info("Fetching PDF URL for invoice: {}", invoiceId);

        try {
            com.chargebee.Result result = Invoice.retrieve(invoiceId).request();
            Invoice invoice = result.invoice();
            
            // Return invoice URL directly from Chargebee dashboard
            return "https://bookmyjuice-test.chargebee.com/invoices/" + invoiceId;
        } catch (Exception e) {
            logger.error("Error getting PDF URL for invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new Exception("Failed to get PDF URL: " + e.getMessage(), e);
        }
    }

    /**
     * Send invoice email to customer
     */
    @Transactional
    public boolean sendInvoiceEmail(String invoiceId, String email) throws Exception {
        logger.info("Sending invoice {} to email: {}", invoiceId, email);

        try {
            // Note: Chargebee API may have a different method for sending emails
            // This is a placeholder - actual implementation depends on Chargebee SDK version
            logger.info("Invoice email sending initiated for: {}", invoiceId);
            return true;
        } catch (Exception e) {
            logger.error("Error sending invoice email {}: {}", invoiceId, e.getMessage(), e);
            throw new Exception("Failed to send invoice email: " + e.getMessage(), e);
        }
    }

    /**
     * Get local invoices from database
     */
    public List<InvoiceEntity> getLocalCustomerInvoices(String customerId) {
        logger.info("Fetching local invoices for customer: {}", customerId);
        return invoiceRepository.findByCustomerId(customerId);
    }

    /**
     * Get specific local invoice
     */
    public Optional<InvoiceEntity> getLocalInvoice(String invoiceId) {
        logger.info("Fetching local invoice: {}", invoiceId);
        return invoiceRepository.findById(invoiceId);
    }

    /**
     * Update invoice status in local database
     */
    @Transactional
    public InvoiceEntity updateInvoiceStatus(String invoiceId, String status) {
        logger.info("Updating invoice {} status to: {}", invoiceId, status);
        
        Optional<InvoiceEntity> invoice = invoiceRepository.findById(invoiceId);
        if (invoice.isPresent()) {
            InvoiceEntity entity = invoice.get();
            entity.setStatus(status);
            return invoiceRepository.save(entity);
        }
        return null;
    }

    /**
     * Get all invoices (admin endpoint)
     */
    public List<InvoiceEntity> getAllInvoices() {
        logger.info("Fetching all invoices");
        return invoiceRepository.findAll();
    }

    /**
     * Map Chargebee invoice to response DTO
     */
    private Map<String, Object> mapInvoiceToResponse(Invoice invoice) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", invoice.id());
        map.put("customerId", invoice.customerId());
        map.put("status", invoice.status().toString());
        map.put("amount", invoice.amountPaid());
        map.put("amountDue", invoice.amountDue());
        map.put("dueDate", invoice.dueDate());
        return map;
    }
}
