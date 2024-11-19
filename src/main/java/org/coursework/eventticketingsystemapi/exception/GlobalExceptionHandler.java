package org.coursework.eventticketingsystemapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Create standardized error response method
    private ResponseEntity<Object> createErrorResponse(String message, Object errors, HttpStatus httpStatus) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", LocalDateTime.now());
            body.put("status", httpStatus.value());
            body.put("error", httpStatus.getReasonPhrase());
            body.put("message", message);
            if (errors != null) {
                body.put("details", errors);
            }
            return new ResponseEntity<>(body, httpStatus);
        } catch (Exception e) {
            logger.error("Error creating error response", e);
            return new ResponseEntity<>(
                    Map.of("message", "Internal server error while processing error response"),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Base EventTicketingSystem Exception Handler
    @ExceptionHandler(EventTicketingSystemException.class)
    public ResponseEntity<Object> handleEventTicketingSystemException(EventTicketingSystemException ex) {
        logger.error("Event Ticketing System Exception: {}", ex.getMessage());
        return createErrorResponse(
                "Event Ticketing System Error",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // Resource Processing Exception
    @ExceptionHandler(ResourceProcessingException.class)
    public ResponseEntity<Object> handleResourceProcessingException(ResourceProcessingException ex) {
        logger.error("Resource Processing Error: {}", ex.getMessage());
        return createErrorResponse(
                "Resource Processing Failed",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // Resource Not Found Exception
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.error("Resource Not Found: {}", ex.getMessage());
        return createErrorResponse(
                "Resource Not Found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    // Invalid Resource Operation Exception
    @ExceptionHandler(InvalidResourceOperationException.class)
    public ResponseEntity<Object> handleInvalidResourceOperationException(InvalidResourceOperationException ex) {
        logger.error("Invalid Resource Operation: {}", ex.getMessage());
        return createErrorResponse(
                "Invalid Operation",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    // Customer Registration Exception
    @ExceptionHandler(CustomerRegistrationException.class)
    public ResponseEntity<Object> handleCustomerRegistrationException(CustomerRegistrationException ex) {
        logger.error("Customer Registration Error: {}", ex.getMessage());
        return createErrorResponse(
                "Customer Registration Failed",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    // Vendor Registration Exception
    @ExceptionHandler(VendorRegistrationException.class)
    public ResponseEntity<Object> handleVendorRegistrationException(VendorRegistrationException ex) {
        logger.error("Vendor Registration Error: {}", ex.getMessage());
        return createErrorResponse(
                "Vendor Registration Failed",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    // Validation Exception Handler
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        org.springframework.validation.FieldError::getField,
                        org.springframework.validation.FieldError::getDefaultMessage
                ));

        logger.error("Validation Error: {}", errors);
        return createErrorResponse(
                "Validation Failed",
                errors,
                HttpStatus.BAD_REQUEST
        );
    }

    // Data Integrity Violation Handler
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        logger.error("Data Integrity Violation: {}", ex.getMessage());
        return createErrorResponse(
                "Data Constraint Violation",
                "Unique constraint or data integrity rule violated",
                HttpStatus.CONFLICT
        );
    }

    // Type Mismatch Exception
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String errorMessage = String.format(
                "Invalid type for parameter '%s'. Expected type: %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "Unknown"
        );
        logger.error("Type Mismatch Error: {}", errorMessage);
        return createErrorResponse(
                "Invalid Parameter Type",
                errorMessage,
                HttpStatus.BAD_REQUEST
        );
    }

    // Catch-all for Unexpected Exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        return createErrorResponse(
                "Unexpected Error",
                "An unexpected error occurred. Please contact support.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}