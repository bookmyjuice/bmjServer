package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bookmyjuice.repository.InvoiceRepository;
import com.bookmyjuice.models.entities.InvoiceEntity;
import com.bookmyjuice.models.mappers.InvoiceMapper;
import com.chargebee.models.Event;

@Service
public class InvoiceService {
    @Autowired
    private InvoiceRepository invoiceRepository;

    public boolean saveOrUpdateInvoice(Event event) {
        var invoice = event.content().invoice();
        InvoiceEntity entity = invoiceRepository.findById(invoice.id())
            .orElseGet(() -> InvoiceMapper.toEntity(invoice));
        InvoiceMapper.toEntity(invoice, entity);
        invoiceRepository.save(entity);
        return true;
    }

    public boolean deleteInvoice(Event event) {
        var invoice = event.content().invoice();
        if (invoiceRepository.existsById(invoice.id())) {
            invoiceRepository.deleteById(invoice.id());
        }
        return true;
    }
}
