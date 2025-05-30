package de.happybavarian07.coolstufflib.languagemanager.expressionengine.exceptions;

public class ExpressionEngineException extends RuntimeException {
    public ExpressionEngineException(String message) { super(message); }
    public ExpressionEngineException(String message, Throwable cause) { super(message, cause); }
}
