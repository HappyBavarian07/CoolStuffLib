package de.happybavarian07.coolstufflib.service.exception;

public class DuplicateServiceNameException extends RuntimeException {
    public DuplicateServiceNameException(String message) {
        super(message);
    }
}

