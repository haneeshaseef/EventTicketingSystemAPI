package org.coursework.eventticketingsystemapi.exception;

public class ResourceNotFoundException extends EventTicketingSystemException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}