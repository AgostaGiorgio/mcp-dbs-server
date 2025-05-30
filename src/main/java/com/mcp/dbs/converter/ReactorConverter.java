package com.mcp.dbs.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ReactorConverter implements ToolCallResultConverter {

    private final ObjectMapper objectMapper;

    public ReactorConverter() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @SuppressWarnings("null")
    public String convert(Object result, Type returnType) {

        if (isResultMono(returnType)) {
            result = ((Mono<?>) result).block();
        } else {
            result = ((Flux<?>) result).collectList().block();
        }

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert result to JSON", e);
        }
    }

    private boolean isResultMono(Type returnType) {
        if (returnType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) returnType).getRawType();
            if (rawType instanceof Class<?>) {
                Class<?> rawClass = (Class<?>) rawType;
                return Mono.class.isAssignableFrom(rawClass);
            }
        } else if (returnType instanceof Class<?>) {
            Class<?> rawClass = (Class<?>) returnType;
            return Mono.class.isAssignableFrom(rawClass);
        }
        return false;
    }

}
