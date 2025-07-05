package de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces;

import de.happybavarian07.coolstufflib.languagemanager.expressionengine.Interpreter;

import java.util.List;

/**
 * <p>Functional interface for defining custom functions that can be called within
 * the expression engine, providing extensible computation capabilities.</p>
 *
 * <p>Function calls enable:</p>
 * <ul>
 *   <li>Custom mathematical operations</li>
 *   <li>String manipulation and formatting</li>
 *   <li>External system integration</li>
 *   <li>Dynamic value computation</li>
 * </ul>
 *
 * <pre><code>
 * FunctionCall maxFunction = (interpreter, args, callType) -> {
 *     return args.stream()
 *         .filter(Number.class::isInstance)
 *         .map(Number.class::cast)
 *         .mapToDouble(Number::doubleValue)
 *         .max().orElse(0.0);
 * };
 *
 * interpreter.registerFunction("max", maxFunction);
 * </code></pre>
 */
@FunctionalInterface
public interface FunctionCall {

    /**
     * <p>Executes the function with the provided arguments and context.</p>
     *
     * <pre><code>
     * Object result = functionCall.call(interpreter,
     *     List.of(10, 20, 5), "number");
     * </code></pre>
     *
     * @param interpreter the expression interpreter providing execution context
     * @param arguments the list of arguments passed to the function
     * @param callType the type hint for the expected result type
     * @return the computed result of the function execution
     */
    Object call(Interpreter interpreter, List<Object> arguments, String callType);
}
