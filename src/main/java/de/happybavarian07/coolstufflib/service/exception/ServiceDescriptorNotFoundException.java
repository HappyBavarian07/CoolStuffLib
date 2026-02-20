package de.happybavarian07.coolstufflib.service.exception;

public class ServiceDescriptorNotFoundException extends RuntimeException {
    public ServiceDescriptorNotFoundException(String message) {
        super(message);
    }
}

