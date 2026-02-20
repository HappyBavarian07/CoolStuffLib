package de.happybavarian07.coolstufflib.service.exception;

public class ServiceDependencyCycleException extends RuntimeException {
    public ServiceDependencyCycleException(String message) {
        super(message);
    }
    public ServiceDependencyCycleException(String message, Throwable cause) {
        super(message, cause);
    }
}