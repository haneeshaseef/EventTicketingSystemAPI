package org.coursework.eventticketingsystemapi.exception;

public class ResourceProcessingException extends EventTicketingSystemException {
    public ResourceProcessingException(String message) {
        super(message);
    }

    public ResourceProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}