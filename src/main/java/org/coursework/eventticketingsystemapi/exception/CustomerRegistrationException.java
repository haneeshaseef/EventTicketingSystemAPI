package org.coursework.eventticketingsystemapi.exception;

public class CustomerRegistrationException extends EventTicketingSystemException {
    public CustomerRegistrationException(String message) {
        super(message);
    }

    public CustomerRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
