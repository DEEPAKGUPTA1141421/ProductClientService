package com.ProductClientService.ProductClientService.Utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle @Valid validation errors (DTO field validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, message, null, 400));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, ex.getMessage(), null, 400));
    }


    // Handle validation errors thrown directly (like @Validated at method level)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().iterator().next().getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, message, null, 400));
    }

    // Handle missing request params
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = "Missing required parameter: " + ex.getParameterName();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, message, null, 400));
    }

    // Handle bad JSON — unwraps Jackson cause to give a field-level message
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleBadJson(HttpMessageNotReadableException ex) {
        String message = resolveJsonError(ex.getCause());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, message, null, 400));
    }

    private String resolveJsonError(Throwable cause) {
        if (cause instanceof InvalidFormatException ife) {
            String field = ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(0).getFieldName();
            Class<?> targetType = ife.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                String allowed = Arrays.stream(targetType.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                return "Field '" + field + "' has invalid value '" + ife.getValue()
                        + "'. Allowed values: " + allowed;
            }
            String typeName = targetType != null ? targetType.getSimpleName() : "unknown";
            return "Field '" + field + "' has invalid value '" + ife.getValue()
                    + "'. Expected type: " + typeName;
        }
        if (cause instanceof MismatchedInputException mie) {
            String field = mie.getPath().isEmpty() ? "unknown" : mie.getPath().get(0).getFieldName();
            String typeName = mie.getTargetType() != null ? mie.getTargetType().getSimpleName() : "unknown";
            return "Field '" + field + "' is missing or has wrong type (expected: " + typeName + ")";
        }
        if (cause instanceof JsonParseException jpe) {
            return "Malformed JSON: " + jpe.getOriginalMessage();
        }
        return "Invalid request body";
    }

    // Catch-all for other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Something went wrong: " + ex.getMessage(), null, 500));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String rawMessage = ex.getMostSpecificCause().getMessage();

        // Extract clean DB error message
        String cleanMessage = extractDbErrorMessage(rawMessage);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, cleanMessage, null, 409));
    }

    public static String extractDbErrorMessage(String fullError) {
        if (fullError == null || fullError.isEmpty()) {
            return "Unknown database error";
        }

        int errorIndex = fullError.indexOf("ERROR:");
        if (errorIndex != -1) {
            return fullError.substring(errorIndex).trim();
        }

        return fullError; // fallback
    }

    public static boolean isDatabaseError(String fullError) {
        if (fullError == null)
            return false;

        return fullError.contains("ERROR:")
                || fullError.contains("violates foreign key constraint")
                || fullError.contains("duplicate key")
                || fullError.contains("SQL [");
    }
}
