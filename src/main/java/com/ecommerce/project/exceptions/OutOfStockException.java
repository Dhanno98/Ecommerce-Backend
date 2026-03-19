package com.ecommerce.project.exceptions;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class OutOfStockException extends RuntimeException {
    private String message;
    private Map<String, String> map = new HashMap<>();

    public OutOfStockException() {
    }

    public OutOfStockException(String message, Map<String, String> map) {
        super(message);
        this.message = message;
        this.map = map;
    }
}
