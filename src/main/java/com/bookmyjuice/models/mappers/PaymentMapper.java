package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.PaymentEntity;
import com.chargebee.models.Transaction;
import java.math.BigDecimal;
import java.util.Date;

public class PaymentMapper {
    public static PaymentEntity toEntity(Transaction transaction) {
        PaymentEntity entity = new PaymentEntity();
        entity.setTransactionId(transaction.id());
        entity.setCustomerId(transaction.customerId());
        entity.setAmount(transaction.amount() != null ? BigDecimal.valueOf(transaction.amount()).movePointLeft(2) : null);
        entity.setStatus(transaction.status());
        entity.setDate(transaction.date() != null ? new Date(transaction.date().getTime()) : null);
        // Map more fields as needed
        return entity;
    }

    public static void toEntity(Transaction transaction, PaymentEntity entity) {
        entity.setTransactionId(transaction.id());
        entity.setCustomerId(transaction.customerId());
        entity.setAmount(transaction.amount() != null ? BigDecimal.valueOf(transaction.amount()).movePointLeft(2) : null);
        entity.setStatus(transaction.status());
        entity.setDate(transaction.date() != null ? new Date(transaction.date().getTime()) : null);
        // Map more fields as needed
    }
}
