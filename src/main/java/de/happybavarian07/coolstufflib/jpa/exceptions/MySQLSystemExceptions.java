package de.happybavarian07.coolstufflib.jpa.exceptions;

public class MySQLSystemExceptions {

    public static class DatabaseConnectionException extends Exception {
        public DatabaseConnectionException(String message) {
            super(message);
        }

        public DatabaseConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class OutputConversionException extends Exception {
        public OutputConversionException(String message) {
            super(message);
        }

        public OutputConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class RepositoryException extends RuntimeException {
        public RepositoryException(String message) {
            super(message);
        }

        public RepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class EntityValidationException extends RepositoryException {
        public EntityValidationException(String message) {
            super(message);
        }

        public EntityValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
