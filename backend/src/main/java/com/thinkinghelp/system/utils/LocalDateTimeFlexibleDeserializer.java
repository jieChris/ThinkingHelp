package com.thinkinghelp.system.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;

public class LocalDateTimeFlexibleDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return DateTimeUtils.parseFlexibleDateTime(text);
        } catch (IllegalArgumentException ex) {
            throw ctxt.weirdStringException(text, LocalDateTime.class, ex.getMessage());
        }
    }
}

