package com.ecommerce.project.exceptions;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class OutOfStockException extends RuntimeException {
    private Map<String, String> errors = new HashMap<>();

    public OutOfStockException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
}
