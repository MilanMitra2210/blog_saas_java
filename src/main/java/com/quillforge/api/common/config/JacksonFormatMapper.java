package com.quillforge.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

public class JacksonFormatMapper implements FormatMapper {

    private final ObjectMapper objectMapper;

    public JacksonFormatMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        if (charSequence == null) {
            return null;
        }
        try {
            return objectMapper.readValue(charSequence.toString(), objectMapper.constructType(javaType.getJavaType()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON to " + javaType.getTypeName(), e);
        }
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writerFor(objectMapper.constructType(javaType.getJavaType())).writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object of type " + value.getClass().getName() + " with type " + javaType.getTypeName(), e);
        }
    }
}
