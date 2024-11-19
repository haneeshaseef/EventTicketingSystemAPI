package org.coursework.eventticketingsystemapi.exception;

public class InvalidResourceOperationException extends EventTicketingSystemException {
    public InvalidResourceOperationException(String message) {
        super(message);
    }

    public InvalidResourceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
