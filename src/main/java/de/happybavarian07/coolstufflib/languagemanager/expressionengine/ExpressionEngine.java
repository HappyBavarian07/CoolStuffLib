package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.conditions.HeadMaterialCondition;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.exceptions.ExpressionEngineException;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.FunctionCall;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.MaterialCondition;
import de.happybavarian07.coolstufflib.utils.Head;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A modern implementation of the conditional expression parser using a lexer/parser/interpreter pattern.
 * This provides better handling of nested conditions and more efficient parsing.
 * <p>
 * The ExpressionParser combines the Lexer, Parser, and Interpreter components to provide a complete
 * pipeline for evaluating expressions within language files. It supports:
 * <ul>
 *   <li>Mathematical expressions (e.g., "2 + 3 * 4")</li>
 *   <li>Logical expressions (e.g., "value > 10 && isAdmin")</li>
 *   <li>Conditional chains (e.g., "if condition: valueA elif otherCondition: valueB else: valueC")</li>
 *   <li>Function calls (e.g., "Out<Material>(STONE)")</li>
 *   <li>Variable references</li>
 * </ul>
 * </p>
 * <p>
 * This parser is particularly useful for dynamic content in language files, allowing
 * for conditional rendering and computation based on runtime context.
 * </p>
 *
 * @author HappyBavarian07
 * @since 2025-05-24
 */
public class ExpressionEngine {
    private final Interpreter interpreter = new Interpreter();
    private final Lexer lexer = new Lexer("");
    private final Parser parser = new Parser(null);
    private final LanguageFunctionManager functionManager;

    public ExpressionEngine() {
        this.functionManager = new LanguageFunctionManager(this);
    }

    /**
     * Registers a function with the expression parser.
     * <p>
     * Registered functions can be called from within expressions using the syntax:
     * functionName(arg1, arg2, ...)
     * </p>
     *
     * @param name         The name of the function to register
     * @param functionCall The implementation of the function
     */
    public void registerFunction(String name, FunctionCall functionCall) {
        this.interpreter.registerFunction(name, functionCall, null);
    }

    /**
     * Registers a function with the expression parser with a default type parameter.
     * <p>
     * Registered functions can be called from within expressions using the syntax:
     * functionName(arg1, arg2, ...) or functionName&lt;Type&gt;(arg1, arg2, ...)
     * </p>
     * <p>
     * If the function is called without an explicit type parameter, the default type will be used.
     * </p>
     *
     * @param name         The name of the function to register
     * @param functionCall The implementation of the function
     * @param defaultType  The default type parameter to use when not specified in the call
     */
    public void registerFunction(String name, FunctionCall functionCall, String defaultType) {
        this.interpreter.registerFunction(name, functionCall, defaultType);
    }

    /**
     * Registers a function with argument and return type information.
     *
     * @param name The function name
     * @param functionCall The implementation
     * @param defaultType The default type parameter
     * @param argTypes Argument types (null for any)
     * @param returnType Return type (null for any)
     */
    public void registerFunction(String name, FunctionCall functionCall, String defaultType, Class<?>[] argTypes, Class<?> returnType) {
        this.interpreter.registerFunction(name, functionCall, defaultType, argTypes, returnType);
    }

    public void unregisterFunction(String name) {
        this.interpreter.unregisterFunction(name);
    }

    /**
     * Returns the function manager for this expression parser.
     * <p>
     * The function manager provides additional utilities for registering and managing
     * functions that can be called from expressions.
     * </p>
     *
     * @return The associated LanguageFunctionManager instance
     */
    public LanguageFunctionManager getFunctionManager() {
        return functionManager;
    }

    /**
     * Returns the interpreter used by this expression parser.
     *
     * @return The Interpreter instance
     */
    public Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     * Returns the parser used by this expression parser.
     *
     * @return The Parser instance
     */
    public Parser getParser() {
        return parser;
    }

    /**
     * Returns the lexer used by this expression parser.
     *
     * @return The Lexer instance
     */
    public Lexer getLexer() {
        return lexer;
    }

    /**
     * Parses an expression that returns a material with potential head texture support.
     * <p>
     * This method extends the standard material parsing to include support for
     * custom player heads with texture values. The syntax for a head material is:
     * <code>HEAD('player_name')</code> or <code>HEAD_TEXTURE('base64_value')</code>
     * </p>
     *
     * @param expression      The expression to parse
     * @param defaultMaterial The default material to return if parsing fails
     * @return A MaterialCondition or HeadMaterial representing the result
     */
    public MaterialCondition parse(String expression, Material defaultMaterial) {
        try {
            lexer.setSource(expression);
            List<Token> tokens = lexer.scanTokens();
            parser.setTokens(tokens);

            // Try to parse as a conditional chain first
            Parser.Expression expr;
            if (tokens.stream().anyMatch(t -> t.type() == TokenType.IF || t.type() == TokenType.ELIF || t.type() == TokenType.ELSE)) {
                expr = parser.parseConditionalChain();
            } else {
                expr = parser.parse();
            }

            if (expr == null) {
                throw new IllegalArgumentException("Failed to parse expression: " + expression);
            }

            // Evaluate the conditional chain
            if (expr instanceof Parser.Expression.ConditionalChain chain) {
                for (Parser.Expression.ConditionalBranch branch : chain.branches) {
                    Object condResult = interpreter.interpret(branch.condition);
                    if (condResult instanceof Boolean && (Boolean) condResult) {
                        Object out = interpreter.interpret(branch.output);
                        return parseMaterialOutput(out, defaultMaterial);
                    }
                }
                if (chain.elseBranch != null) {
                    Object out = interpreter.interpret(chain.elseBranch);
                    return parseMaterialOutput(out, defaultMaterial);
                }
                return new DirectMaterialCondition(defaultMaterial);
            }

            throw new IllegalArgumentException("Failed to parse expression: " + expression);
        } catch (RuntimeException e) {
            if (e instanceof ExpressionEngineException) throw e;
            throw new ExpressionEngineException("Error parsing expression: " + expression + " - " + e.getMessage(), e);
        }
    }

    private MaterialCondition parseMaterialOutput(Object out, Material defaultMaterial) {
        if (out instanceof Material) {
            return new DirectMaterialCondition((Material) out);
        } else if (out instanceof String s) {

            // Handle HEAD and HEAD_TEXTURE
            if (s.startsWith("HEAD(") && s.endsWith(")")) {
                String playerName = s.substring(5, s.length() - 1).trim();
                // Remove quotes if present
                if ((playerName.startsWith("'") && playerName.endsWith("'")) ||
                        (playerName.startsWith("\"") && playerName.endsWith("\""))) {
                    playerName = playerName.substring(1, playerName.length() - 1);
                }
                return new HeadMaterialCondition(playerName, false);
            } else if (s.startsWith("HEAD_TEXTURE(") && s.endsWith(")")) {
                String textureValue = s.substring(13, s.length() - 1).trim();
                // Remove quotes if present
                if ((textureValue.startsWith("'") && textureValue.endsWith("'")) ||
                        (textureValue.startsWith("\"") && textureValue.endsWith("\""))) {
                    textureValue = textureValue.substring(1, textureValue.length() - 1);
                }
                return new HeadMaterialCondition(textureValue, true);
            } else if (s.startsWith("HEAD_OBJECT(") && s.endsWith(")")) {
                String headName = s.substring(12, s.length() - 1).trim();
                // Remove quotes if present
                if ((headName.startsWith("'") && headName.endsWith("'")) ||
                        (headName.startsWith("\"") && headName.endsWith("\""))) {
                    headName = headName.substring(1, headName.length() - 1);
                }
                return new HeadMaterialCondition(Head.valueOf(headName));
            }

            if (s.startsWith("Out<") && s.contains(":")) {
                throw new IllegalArgumentException("Old-style Out<Type>:Value syntax is no longer supported. Use Out<Type>(Value) instead.");
            }
        } else if (out instanceof Parser.Expression.Call) {
            // Evaluate function call output (e.g., Out<Material>(STONE))
            Object result = interpreter.interpret((Parser.Expression) out);
            if (result instanceof Material) {
                return new DirectMaterialCondition((Material) result);
            }
            // Check for HEAD function calls
            if (result instanceof String s) {
                if (s.startsWith("HEAD:")) {
                    return new HeadMaterialCondition(s.substring(5), false);
                } else if (s.startsWith("HEAD_TEXTURE:")) {
                    return new HeadMaterialCondition(s.substring(13), true);
                } else if (s.startsWith("Out<") && s.contains(":")) {
                    throw new IllegalArgumentException("Old-style Out<Type>:Value syntax is no longer supported. Use Out<Type>(Value) instead.");
                } else if (s.startsWith("HEAD_OBJECT:")) {
                    String headName = s.substring(12);
                    return new HeadMaterialCondition(Head.valueOf(headName));
                }
            }
            // Try to cast primitives if needed
            if (result instanceof Number) {
                try {
                    return new DirectMaterialCondition(Material.valueOf(result.toString().toUpperCase()));
                } catch (Exception e) {
                    return new DirectMaterialCondition(defaultMaterial);
                }
            }
        }
        return new DirectMaterialCondition(defaultMaterial);
    }

    /**
     * Parses an expression and returns the result of its evaluation with type conversion.
     * <p>
     * This method evaluates an expression and attempts to convert the result to the specified type.
     * It supports parsing direct values (like material names) as well as complex expressions
     * including conditional chains and function calls.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * // Parse a direct material
     * Material material = parser.parse("DIAMOND_BLOCK", Material.class);
     *
     * // Parse a conditional expression returning a string
     * String message = parser.parse(
     *     "if player.level > 10: 'High level player' else: 'Low level player'",
     *     String.class
     * );
     *
     * // Parse a numeric expression
     * Double value = parser.parse("2 * (3 + 4)", Double.class);
     * </pre>
     * </p>
     *
     * @param <T>        the expected return type
     * @param expression the expression to parse
     * @param type       the class object representing the expected return type
     * @return the parsed expression result converted to type T
     * @throws IllegalArgumentException if the expression is invalid or cannot be parsed,
     *                                  or if the result cannot be converted to the specified type
     */
    public <T> T parse(String expression, Class<T> type) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }

        expression = expression.trim();
        if (expression.isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }

        try {
            // First check if it's a direct material name
            if (isMaterialName(expression)) {
                Material material = Material.valueOf(expression.toUpperCase().replace("\"", "").replace("'", ""));
                return type.cast(material);
            }

            Parser.Expression expr = tokenizeAndParseExpression(expression);
            if (expr == null) {
                return null;
            }

            if (expr instanceof Parser.Expression.ConditionalChain chain) {
                for (int i = 0; i < chain.branches.size(); i++) {
                    Parser.Expression.ConditionalBranch branch = chain.branches.get(i);
                    Object condResult = interpreter.interpret(branch.condition);
                    if (isTruthy(condResult)) {
                        Object output = interpreter.interpret(branch.output);
                        return type.cast(output);
                    }
                }

                if (chain.elseBranch != null) {
                    Object output = interpreter.interpret(chain.elseBranch);
                    return type.cast(output);
                }
                return null;
            }

            // Handle other types
            if (type.isInstance(expr) && !(expr instanceof Parser.Expression.Call)) {
                return type.cast(expr);
            }

            // if expr is a function call, we need to evaluate it

            Object result = interpreter.interpret(expr);

            if (type.isPrimitive() && result.getClass().isPrimitive()) {
                return type.cast(result);
            }

            if (!type.isInstance(result)) {
                throw new IllegalArgumentException("Unexpected result type: " +
                        (result != null ? result.getClass().getSimpleName() : "null"));
            }

            return type.cast(result);
        } catch (RuntimeException e) {
            if (e instanceof ExpressionEngineException) throw e;
            throw new ExpressionEngineException("Error parsing expression: " + expression + " - " + e.getMessage(), e);
        }
    }

    private Parser.@Nullable Expression tokenizeAndParseExpression(String expression) {
        lexer.setSource(expression);
        List<Token> tokens = lexer.scanTokens();

        if (tokens.isEmpty() || tokens.size() == 1 && tokens.get(0).type() == TokenType.EOF) {
            return null;
        }

        parser.setTokens(tokens);
        return parser.parse();
    }

    public Object parsePrimitive(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }

        expression = expression.trim();
        if (expression.isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }

        try {
            if (isMaterialName(expression)) {
                return Material.valueOf(expression.toUpperCase().replace("\"", "").replace("'", ""));
            }

            Parser.Expression expr = tokenizeAndParseExpression(expression);
            if (expr == null) {
                return null;
            }

            if (expr instanceof Parser.Expression.ConditionalChain chain) {
                for (int i = 0; i < chain.branches.size(); i++) {
                    Parser.Expression.ConditionalBranch branch = chain.branches.get(i);
                    Object condResult = interpreter.interpret(branch.condition);
                    if (isTruthy(condResult)) {
                        return interpreter.interpret(branch.output);
                    }
                }

                if (chain.elseBranch != null) {
                    return interpreter.interpret(chain.elseBranch);
                }
                return null;
            }

            return interpreter.interpret(expr);
        } catch (RuntimeException e) {
            if (e instanceof ExpressionEngineException) throw e;
            throw new ExpressionEngineException("Error parsing expression: " + expression + " - " + e.getMessage(), e);
        }
    }

    private boolean isTruthy(Object value) {
        return switch (value) {
            case null -> false;
            case Boolean b -> b;
            case Number number -> number.doubleValue() != 0;
            default -> true;
        };
    }

    private boolean isMaterialName(String str) {
        if (str == null || str.isEmpty()) return false;
        String name = str.trim().toUpperCase();
        name = name.replace("\"", "").replace("'", "");
        try {
            Material.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void setVariable(String name, Object value) {
        interpreter.setVariable(name, value);
    }

    public void setVariable(String name, Object value, int uses) {
        interpreter.setVariable(name, value, uses);
    }

    public Object getVariable(String name) {
        return interpreter.getVariable(name);
    }

    public void clearVariables() {
        interpreter.clearVariables();
    }

    public void clearFunctions() {
        interpreter.clearFunctions();
    }

    public Object peekVariable(String key) {
        return interpreter.peekVariable(key);
    }

    public void removeVariable(String key) {
        interpreter.removeVariable(key);
    }

    /**
     * Pretty-prints or minifies an expression string.
     *
     * @param expr The expression
     * @param pretty True for pretty-print, false for minified
     * @return The formatted expression
     */
    public String formatExpression(String expr, boolean pretty) {
        Parser.Expression parsed = tokenizeAndParseExpression(expr);
        if (parsed == null) return expr;
        return pretty ? formatPretty(parsed, 0) : formatCompact(parsed);
    }

    private String formatPretty(Parser.Expression expr, int indent) {
        String pad = "  ".repeat(indent);
        if (expr instanceof Parser.Expression.Binary b) {
            return pad + formatPretty(b.left, indent+1) + " " + b.operator.lexeme() + "\n" + pad + formatPretty(b.right, indent+1);
        } else if (expr instanceof Parser.Expression.Call c) {
            StringBuilder sb = new StringBuilder(pad + c.name.lexeme() + "(");
            for (int i = 0; i < c.arguments.size(); i++) {
                sb.append(formatPretty(c.arguments.get(i), indent+1));
                if (i < c.arguments.size()-1) sb.append(", ");
            }
            return sb.append(")").toString();
        } else if (expr instanceof Parser.Expression.Literal l) {
            return pad + l.value;
        } else if (expr instanceof Parser.Expression.Variable v) {
            return pad + v.name.lexeme();
        }
        return pad + expr.toString();
    }
    private String formatCompact(Parser.Expression expr) {
        if (expr instanceof Parser.Expression.Binary b) {
            return formatCompact(b.left) + b.operator.lexeme() + formatCompact(b.right);
        } else if (expr instanceof Parser.Expression.Call c) {
            StringBuilder sb = new StringBuilder(c.name.lexeme() + "(");
            for (int i = 0; i < c.arguments.size(); i++) {
                sb.append(formatCompact(c.arguments.get(i)));
                if (i < c.arguments.size()-1) sb.append(",");
            }
            return sb.append(")").toString();
        } else if (expr instanceof Parser.Expression.Literal l) {
            return l.value.toString();
        } else if (expr instanceof Parser.Expression.Variable v) {
            return v.name.lexeme();
        }
        return expr.toString();
    }

    /**
     * Registers the built-in lang(key) function for localization support.
     *
     * @param langManager The LanguageManager instance
     */
    public void registerLangFunction(LanguageManager langManager) {
        registerFunction("lang", (interpreter, args, type) -> {
            if (args == null || args.size() != 1) throw new RuntimeException("lang(key) expects 1 argument");
            Object key = args.get(0);
            if (key == null) return "";
            return langManager.getCustomObject(key.toString(), null, "", false);
        }, "string", new Class<?>[]{String.class}, String.class);
    }

    public void setFunctionWhitelist(java.util.List<String> whitelist) {
        interpreter.setFunctionWhitelist(whitelist);
    }
    public void setFunctionBlacklist(java.util.List<String> blacklist) {
        interpreter.setFunctionBlacklist(blacklist);
    }
    public void setVariableWhitelist(java.util.List<String> whitelist) {
        interpreter.setVariableWhitelist(whitelist);
    }
    public void setVariableBlacklist(java.util.List<String> blacklist) {
        interpreter.setVariableBlacklist(blacklist);
    }

    /**
     * Validates the syntax of an expression without evaluating it.
     * Returns null if valid, or an error message if invalid.
     *
     * @param expression The expression to validate.
     * @return Null if valid, otherwise an error message.
     */
    public String validate(String expression) {
        try {
            Parser.Expression expr = tokenizeAndParseExpression(expression);
            if (expr == null) return "Empty or invalid expression.";
            return null;
        } catch (Exception e) {
            if (e instanceof ExpressionEngineException) throw e;
            throw new ExpressionEngineException("Error parsing expression: " + expression + " - " + e.getMessage(), e);
        }
    }

    /**
     * Enables or disables debug/trace mode for this engine.
     * When enabled, each evaluation step is logged to the console.
     *
     * @param enabled True to enable debug mode, false to disable.
     */
    public void setDebugMode(boolean enabled) {
        interpreter.setDebug(enabled);
    }

    /**
     * Returns whether debug/trace mode is enabled.
     *
     * @return True if debug mode is enabled, false otherwise.
     */
    public boolean isDebugMode() {
        return interpreter.isDebug();
    }

    /**
     * Injects a custom context object into the engine for use in expressions.
     * Functions can access injected context via a special variable map.
     *
     * @param key   The context key.
     * @param value The context object.
     */
    public void putContext(String key, Object value) {
        interpreter.putContext(key, value);
    }

    /**
     * Removes a context object by key.
     *
     * @param key The context key to remove.
     */
    public void removeContext(String key) {
        interpreter.removeContext(key);
    }

    /**
     * Clears all injected context objects.
     */
    public void clearContext() {
        interpreter.clearContext();
    }

    private record DirectMaterialCondition(Material getMaterial) implements MaterialCondition {

        @Override
        public boolean isTrue() {
            return true;
        }

        @Override
        public String getName() {
            return "DirectMaterialCondition";
        }

        @Override
        public MaterialCondition setTrueMaterial(Material material) {
            throw new UnsupportedOperationException("DirectMaterialCondition does not support setting true material.");
        }

        @Override
        public MaterialCondition setFalseMaterial(Material material) {
            throw new UnsupportedOperationException("DirectMaterialCondition does not support setting false material.");
        }

        @Override
        public MaterialCondition setDefaultMaterial(Material material) {
            throw new UnsupportedOperationException("DirectMaterialCondition does not support setting default material.");
        }
    }

    private static class BooleanCondition implements MaterialCondition {
        private final boolean value;
        private Material trueMaterial = Material.GREEN_WOOL;
        private Material falseMaterial = Material.RED_WOOL;

        public BooleanCondition(boolean value, Material trueMaterial, Material falseMaterial) {
            this.value = value;
            this.trueMaterial = trueMaterial;
            this.falseMaterial = falseMaterial;
        }

        @Override
        public Material getMaterial() {
            return value ? trueMaterial : falseMaterial;
        }

        @Override
        public boolean isTrue() {
            return value;
        }

        @Override
        public String getName() {
            return "BooleanCondition";
        }

        @Override
        public MaterialCondition setTrueMaterial(Material material) {
            this.trueMaterial = material;
            return this;
        }

        @Override
        public MaterialCondition setFalseMaterial(Material material) {
            this.falseMaterial = material;
            return this;
        }

        @Override
        public MaterialCondition setDefaultMaterial(Material material) {
            throw new UnsupportedOperationException("BooleanCondition does not support setting default material.");
        }
    }

    private static class ExpressionCondition implements MaterialCondition {
        private final Interpreter interpreter;
        private final Parser.Expression expression;
        private Material trueMaterial;
        private Material falseMaterial;
        private Material defaultMaterial;

        public ExpressionCondition(Interpreter interpreter, Parser.Expression expression, Material defaultMaterial, Material trueMaterial, Material falseMaterial) {
            this.interpreter = interpreter;
            this.expression = expression;
            this.defaultMaterial = defaultMaterial;
            this.trueMaterial = trueMaterial;
            this.falseMaterial = falseMaterial;
        }

        @Override
        public Material getMaterial() {
            Object result = interpreter.interpret(expression);
            if (result instanceof Material) {
                return (Material) result;
            } else if (result instanceof Boolean) {
                return (boolean) result ? trueMaterial : falseMaterial;
            } else {
                return defaultMaterial;
            }
        }

        @Override
        public boolean isTrue() {
            Object result = interpreter.interpret(expression);
            if (result instanceof Boolean) {
                return (boolean) result;
            }
            return false;
        }

        @Override
        public String getName() {
            return "Expression(" + expression + ")";
        }

        @Override
        public MaterialCondition setTrueMaterial(Material material) {
            this.trueMaterial = material;
            return this;
        }

        @Override
        public MaterialCondition setFalseMaterial(Material material) {
            this.falseMaterial = material;
            return this;
        }

        @Override
        public MaterialCondition setDefaultMaterial(Material material) {
            this.defaultMaterial = material;
            return this;
        }
    }

    /**
     * Evaluates an expression string and returns the result.
     *
     * @param expression the expression to evaluate
     * @return the result of evaluation
     * @throws IllegalArgumentException if the expression is invalid or cannot be parsed
     */
    public Object evaluate(String expression) {
        return parse(expression, Object.class);
    }
}
