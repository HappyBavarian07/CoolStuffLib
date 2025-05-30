package de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces;

import de.happybavarian07.coolstufflib.languagemanager.expressionengine.Interpreter;

import java.util.List;

@FunctionalInterface
public interface FunctionCall {
    Object call(Interpreter interpreter, List<Object> arguments, String callType);
}
