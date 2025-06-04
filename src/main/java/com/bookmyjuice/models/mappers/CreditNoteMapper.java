package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.CreditNoteEntity;
import com.chargebee.models.CreditNote;
import java.math.BigDecimal;
import java.util.Date;

public class CreditNoteMapper {
    public static CreditNoteEntity toEntity(CreditNote creditNote) {
        CreditNoteEntity entity = new CreditNoteEntity();
        entity.setId(creditNote.id());
        entity.setCustomerId(creditNote.customerId());
        entity.setAmount(creditNote.total() != null ? BigDecimal.valueOf(creditNote.total()).movePointLeft(2) : null);
        entity.setStatus(creditNote.status() != null ? creditNote.status().toString() : null);
        entity.setCreatedAt(creditNote.date() != null ? new Date(creditNote.date().getTime()) : null);
        // Map more fields as needed
        return entity;
    }
    public static void toEntity(CreditNote creditNote, CreditNoteEntity entity) {
        entity.setCustomerId(creditNote.customerId());
        entity.setAmount(creditNote.total() != null ? BigDecimal.valueOf(creditNote.total()).movePointLeft(2) : null);
        entity.setStatus(creditNote.status() != null ? creditNote.status().toString() : null);
        entity.setCreatedAt(creditNote.date() != null ? new Date(creditNote.date().getTime()) : null);
        // Map more fields as needed
    }
}
