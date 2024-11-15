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

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(ResourceProcessingException.class)
    public ResponseEntity<Object> handleConfigurationProcessingException(ResourceProcessingException ex) {
        try {
            logger.error("Configuration processing error: {}", ex.getMessage());
            return createErrorResponse(
                    "Failed to process configuration",
                    ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (Exception e) {
            logger.error("Error handling ConfigurationProcessingException", e);
            return handleUnexpectedException(e);
        }
    }

    @ExceptionHandler(InvalidResourceOperationException.class)
    public ResponseEntity<Object> handleInvalidConfigurationException(InvalidResourceOperationException ex) {
        try {
            logger.error("Invalid configuration: {}", ex.getMessage());
            return createErrorResponse(
                    "Invalid configuration",
                    ex.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            logger.error("Error handling InvalidConfigurationException", e);
            return handleUnexpectedException(e);
        }
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        try {
            logger.error("Resource not found: {}", ex.getMessage());
            return createErrorResponse(
                    "Resource not found",
                    ex.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            logger.error("Error handling ResourceNotFoundException", e);
            return handleUnexpectedException(e);
        }
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
        try {
            logger.error("Illegal state: {}", ex.getMessage());
            return createErrorResponse(
                    "Operation cannot be performed in current state",
                    ex.getMessage(),
                    HttpStatus.CONFLICT
            );
        } catch (Exception e) {
            logger.error("Error handling IllegalStateException", e);
            return handleUnexpectedException(e);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        try {
            logger.error("Validation failed: {}", ex.getMessage());
            Map<String, String> errors = new HashMap<>();

            ex.getBindingResult().getFieldErrors().forEach(error -> {
                try {
                    String fieldName = error.getField();
                    String errorMessage = error.getDefaultMessage();
                    errors.put(fieldName, errorMessage);
                    logger.debug("Validation error for field '{}': {}", fieldName, errorMessage);
                } catch (Exception e) {
                    logger.error("Error processing field error", e);
                    errors.put(error.getField(), "Error processing validation message");
                }
            });

            return createErrorResponse(
                    "Validation failed",
                    errors,
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            logger.error("Error handling MethodArgumentNotValidException", e);
            return handleUnexpectedException(e);
        }
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        try {
            logger.error("Data integrity violation: {}", ex.getMessage());
            String details = ex.getMostSpecificCause().getMessage();
            // Clean up the database error message for client consumption
            String clientMessage = "A database constraint was violated";

            return createErrorResponse(
                    clientMessage,
                    details,
                    HttpStatus.CONFLICT
            );
        } catch (Exception e) {
            logger.error("Error handling DataIntegrityViolationException", e);
            return handleUnexpectedException(e);
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        try {
            logger.error("Type mismatch: {}", ex.getMessage());
            String error = String.format(
                    "Parameter '%s' should be of type %s",
                    ex.getName(),
                    ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
            );

            return createErrorResponse(
                    "Invalid parameter type",
                    error,
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            logger.error("Error handling MethodArgumentTypeMismatchException", e);
            return handleUnexpectedException(e);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        try {
            logger.error("Illegal argument: {}", ex.getMessage());
            return createErrorResponse(
                    "Invalid argument provided",
                    ex.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            logger.error("Error handling IllegalArgumentException", e);
            return handleUnexpectedException(e);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception ex) {
        try {
            logger.error("Unexpected error occurred", ex);
            return createErrorResponse(
                    "An unexpected error occurred",
                    "Please contact system administrator if the problem persists",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (Exception e) {
            logger.error("Error handling unexpected exception", e);
            return new ResponseEntity<>(
                    Map.of(
                            "timestamp", LocalDateTime.now(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "message", "Critical error in error handling"
                    ),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}