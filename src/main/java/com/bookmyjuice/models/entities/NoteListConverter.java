package com.bookmyjuice.models.entities;

import com.chargebee.models.Invoice.Note;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class NoteListConverter implements AttributeConverter<List<Note>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Note> notes) {
        try {
            return objectMapper.writeValueAsString(notes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert notes to JSON", e);
        }
    }

    @Override
    public List<Note> convertToEntityAttribute(String notesJson) {
        try {
            return objectMapper.readValue(notesJson, new TypeReference<List<Note>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to notes", e);
        }
    }
}