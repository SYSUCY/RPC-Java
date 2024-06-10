package com.sysu.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonSerializer implements Serializer {
    private final ObjectMapper objectMapper;

    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public <T> byte[] serialize(T object) throws IOException {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to serialize object", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
        try {
            return objectMapper.readValue(bytes, type);
        } catch (IOException e) {
            throw new IOException("Failed to deserialize object", e);
        }
    }
}
