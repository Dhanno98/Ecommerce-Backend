package com.ecommerce.project.exceptions;

import com.ecommerce.project.payload.APIResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class MyGlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> myMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> response = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String fieldName = ((FieldError)err).getField();
            String message = err.getDefaultMessage();
            response.put(fieldName, message);
        });
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> myResourceNotFoundException(ResourceNotFoundException e) {
        String message = e.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse> myAPIException(APIException e) {
        String message = e.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<Object> myOutOfStockException(OutOfStockException e) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", "Requested quantity for these items is greater than the available stock! Please remove excess items and then place the order.");
        map.put("excess items", e.getMap());
        map.put("status", false);
        return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse> myMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'";
        APIResponse apiResponse = new APIResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        APIResponse response = new APIResponse("Duplicate entry found", false);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<APIResponse> handleMaxSizeException(MaxUploadSizeExceededException e) {
        APIResponse response = new APIResponse("Image size exceeds 5MB", false);
        return new ResponseEntity<>(response, HttpStatus.CONTENT_TOO_LARGE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse> handleInvalidEnum(HttpMessageNotReadableException ex) {
        APIResponse response = new APIResponse(
                        "Invalid enum value supplied in request body",
                        false);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
