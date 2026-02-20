package de.happybavarian07.coolstufflib.service.exception;

public class ServiceLifecycleException extends RuntimeException {
    public ServiceLifecycleException(String message) {
        super(message);
    }
    public ServiceLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}


