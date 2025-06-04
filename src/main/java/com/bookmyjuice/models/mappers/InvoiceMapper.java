package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.InvoiceEntity;
import com.chargebee.models.Invoice;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class InvoiceMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static InvoiceEntity toEntity(Invoice invoice) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(invoice.id());
        entity.setCustomerId(invoice.customerId());
        entity.setAmount(invoice.amountDue() != null ? BigDecimal.valueOf(invoice.amountDue()).movePointLeft(2) : null);
        entity.setStatus(invoice.status() != null ? invoice.status().toString() : null);
        entity.setCreatedAt(invoice.date() != null ? new Date(invoice.date().getTime()) : null);
        entity.setNotes(convertNotesToJson(invoice.notes())); // Convert notes to JSON
        entity.setDueDate(invoice.dueDate() != null ? new Date(invoice.dueDate().getTime()) : null); // Map due date
        return entity;
    }

    public static void toEntity(Invoice invoice, InvoiceEntity entity) {
        entity.setCustomerId(invoice.customerId());
        entity.setAmount(invoice.amountDue() != null ? BigDecimal.valueOf(invoice.amountDue()).movePointLeft(2) : null);
        entity.setStatus(invoice.status() != null ? invoice.status().toString() : null);
        entity.setCreatedAt(invoice.date() != null ? new Date(invoice.date().getTime()) : null);
        entity.setNotes(convertNotesToJson(invoice.notes())); // Convert notes to JSON
        entity.setDueDate(invoice.dueDate() != null ? new Date(invoice.dueDate().getTime()) : null); // Map due date
    }

    private static String convertNotesToJson(List<Invoice.Note> notes) {
        if (notes == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(notes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert notes to JSON", e);
        }
    }
}