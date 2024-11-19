package org.coursework.eventticketingsystemapi.exception;

public class EventTicketingSystemException extends RuntimeException {
    public EventTicketingSystemException(String message) {
        super(message);
    }

    public EventTicketingSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
