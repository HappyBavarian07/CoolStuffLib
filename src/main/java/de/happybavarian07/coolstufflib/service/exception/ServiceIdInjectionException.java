package de.happybavarian07.coolstufflib.service.exception;

public class ServiceIdInjectionException extends RuntimeException {
    public ServiceIdInjectionException(String message) {
        super(message);
    }
    public ServiceIdInjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

