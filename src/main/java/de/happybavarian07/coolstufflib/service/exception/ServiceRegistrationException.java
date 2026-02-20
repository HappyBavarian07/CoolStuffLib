package de.happybavarian07.coolstufflib.service.exception;

public class ServiceRegistrationException extends RuntimeException {
    public ServiceRegistrationException(String message) {
        super(message);
    }
    public ServiceRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

