package com.supermarket.pos.salesapi.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response format for the API.
 */
public record ErrorResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp,
    int status,
    String error,
    String errorCode,
    String message,
    String path,
    List<FieldError> fieldErrors,
    Object details
) {
    public record FieldError(
        String field,
        String message,
        Object rejectedValue
    ) {}
    
    public static ErrorResponse of(int status, String error, String errorCode, 
                                    String message, String path) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            errorCode,
            message,
            path,
            null,
            null
        );
    }
    
    public static ErrorResponse of(int status, String error, String errorCode,
                                    String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            errorCode,
            message,
            path,
            fieldErrors,
            null
        );
    }
    
    public static ErrorResponse withDetails(int status, String error, String errorCode,
                                            String message, String path, Object details) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            errorCode,
            message,
            path,
            null,
            details
        );
    }
}
