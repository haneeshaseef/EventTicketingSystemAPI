package org.coursework.eventticketingsystemapi.exception;

public class VendorRegistrationException extends EventTicketingSystemException {
    public VendorRegistrationException(String message) {
        super(message);
    }

    public VendorRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}