package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bookmyjuice.repository.PaymentRepository;
import com.bookmyjuice.models.entities.PaymentEntity;
import com.chargebee.models.Event;
import java.math.BigDecimal;
import java.util.Date;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    public boolean saveOrUpdatePayment(Event event) {
        var invoice = event.content().invoice();
        PaymentEntity entity = paymentRepository.findById(invoice.id())
            .orElseGet(() -> {
                PaymentEntity newEntity = new PaymentEntity();
                newEntity.setInvoiceId(invoice.id());
                newEntity.setAmount(BigDecimal.valueOf(invoice.amountDue()).movePointLeft(2));
                newEntity.setDate(new Date(invoice.date().getTime()));
                paymentRepository.save(newEntity);
                return newEntity;
            });
        entity.setCustomerId(invoice.customerId());
        entity.setInvoiceStatus(invoice.status().toString());
        paymentRepository.save(entity);
        return true;
    }

    public boolean deletePayment(Event event) {
        var invoice = event.content().invoice();
        if (paymentRepository.existsById(invoice.id())) {
            paymentRepository.deleteById(invoice.id());
        }
        return true;
    }

    public void processTransaction(com.chargebee.models.Transaction transaction) {
        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setTransactionId(transaction.id());
        paymentEntity.setAmount(BigDecimal.valueOf(transaction.amount()).movePointLeft(2));
        paymentEntity.setCurrencyCode(transaction.currencyCode());
        paymentEntity.setStatus(transaction.status());
        paymentEntity.setDate(transaction.date());

        paymentRepository.save(paymentEntity);
    }
}
