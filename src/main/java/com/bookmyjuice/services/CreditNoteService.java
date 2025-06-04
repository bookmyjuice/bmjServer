package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bookmyjuice.repository.CreditNoteRepository;
import com.bookmyjuice.models.entities.CreditNoteEntity;
import com.bookmyjuice.models.mappers.CreditNoteMapper;
import com.chargebee.models.Event;

@Service
public class CreditNoteService {
    @Autowired
    private CreditNoteRepository creditNoteRepository;

    public boolean saveOrUpdateCreditNote(Event event) {
        var creditNote = event.content().creditNote();
        CreditNoteEntity entity = creditNoteRepository.findById(creditNote.id())
            .orElseGet(() -> CreditNoteMapper.toEntity(creditNote));
        CreditNoteMapper.toEntity(creditNote, entity);
        creditNoteRepository.save(entity);
        return true;
    }

    public boolean deleteCreditNote(Event event) {
        var creditNote = event.content().creditNote();
        if (creditNoteRepository.existsById(creditNote.id())) {
            creditNoteRepository.deleteById(creditNote.id());
        }
        return true;
    }
}
