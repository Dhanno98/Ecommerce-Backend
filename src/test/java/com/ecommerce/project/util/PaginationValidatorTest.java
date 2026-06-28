package com.ecommerce.project.util;

import com.ecommerce.project.exceptions.APIException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationValidatorTest {

    private final PaginationValidator paginationValidator = new PaginationValidator();

    /// validate()
    @Test
    void validateShouldSuccessfullyValidateIfAllParametersAreValid() {
        Integer pageNumber = 0;
        Integer pageSize = 10;
        String sortBy = "productId";
        String sortOrder = "asc";
        List<String> allowedSortFields = List.of("productId", "productName", "quantity", "price", "specialPrice");

        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, allowedSortFields);
    }

    @Test
    void validateShouldThrowApiExceptionWhenPageNumberIsLessThanZero() {
        Integer pageNumber = -1;
        Integer pageSize = 10;
        String sortBy = "productId";
        String sortOrder = "asc";
        List<String> allowedSortFields = List.of("productId", "productName", "quantity", "price", "specialPrice");

        APIException exception = assertThrows(
                APIException.class,
                () -> paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, allowedSortFields)
        );

        assertEquals("Page number cannot be negative", exception.getMessage());
    }

    @Test
    void validateShouldThrowApiExceptionWhenPageSizeIsLessThanOne() {
        Integer pageNumber = 0;
        Integer pageSize = 0;
        String sortBy = "productId";
        String sortOrder = "asc";
        List<String> allowedSortFields = List.of("productId", "productName", "quantity", "price", "specialPrice");

        APIException exception = assertThrows(
                APIException.class,
                () -> paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, allowedSortFields)
        );

        assertEquals("Page size must be between 1 and 100", exception.getMessage());
    }

    @Test
    void validateShouldThrowApiExceptionWhenPageSizeIsGreaterThanHundred() {
        Integer pageNumber = 0;
        Integer pageSize = 101;
        String sortBy = "productId";
        String sortOrder = "asc";
        List<String> allowedSortFields = List.of("productId", "productName", "quantity", "price", "specialPrice");

        APIException exception = assertThrows(
                APIException.class,
                () -> paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, allowedSortFields)
        );

        assertEquals("Page size must be between 1 and 100", exception.getMessage());
    }

    @Test
    void validateShouldThrowApiExceptionWhenSortOrderNotInAllowedSortOrder() {
        Integer pageNumber = 0;
        Integer pageSize = 10;
        String sortBy = "productId";
        String sortOrder = "dec";
        List<String> allowedSortFields = List.of("productId", "productName", "quantity", "price", "specialPrice");

        APIException exception = assertThrows(
                APIException.class,
                () -> paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, allowedSortFields)
        );

        assertEquals("Invalid sort order", exception.getMessage());
    }

    @Test
    void validateShouldThrowApiExceptionWhenSortByNotInAllowedSortFields() {
        Integer pageNumber = 0;
        Integer pageSize = 10;
        String sortBy = "userId";
        String sortOrder = "asc";
        List<String> allowedSortFields = List.of("productId", "productName", "quantity", "price", "specialPrice");

        APIException exception = assertThrows(
                APIException.class,
                () -> paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, allowedSortFields)
        );

        assertEquals("Invalid sortBy field", exception.getMessage());
    }
}