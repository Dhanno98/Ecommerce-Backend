package com.ecommerce.project.util;

import com.ecommerce.project.exceptions.APIException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaginationValidator {

    private static final List<String> ALLOWED_SORT_ORDERS = List.of("asc", "desc");

    public void validate(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, List<String> allowedSortFields) {
        if (pageNumber < 0) {
            throw new APIException("Page number cannot be negative");
        }

        if (pageSize < 1 || pageSize > 100) {
            throw new APIException("Page size must be between 1 and 100");
        }

        if (!ALLOWED_SORT_ORDERS.contains(sortOrder.toLowerCase().trim())) {
            throw new APIException("Invalid sort order");
        }

        if (!allowedSortFields.contains(sortBy.trim())) {
            throw new APIException("Invalid sortBy field");
        }
    }
}
