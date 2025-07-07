package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

import de.happybavarian07.coolstufflib.cache.expression.ExpressionCache;
import de.happybavarian07.coolstufflib.cache.expression.ExpressionCacheKey;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.conditions.HeadMaterialCondition;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.exceptions.ExpressionEngineException;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.FunctionCall;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.MaterialCondition;
import de.happybavarian07.coolstufflib.utils.Head;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A modern implementation of the conditional expression parser using a lexer/parser/interpreter pattern.
 * This provides better handling of nested conditions and more efficient parsing.
 * <p>
 * The ExpressionParser combines the Lexer, Parser, and Interpreter components to provide a complete
 * pipeline for evaluating expressions within language files. It supports:
 * </p>
 * <ul>
 *   <li>Mathematical expressions (e.g., "2 + 3 * 4")</li>
 *   <li>Logical expressions (e.g., "value &gt; 10 &amp;&amp; isAdmin")</li>
 *   <li>Conditional chains (e.g., "if condition: valueA elif otherCondition: valueB else: valueC")</li>
 *   <li>Function calls (e.g., "Out(STONE)")</li>
 *   <li>Variable references</li>
 * </ul>
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
    private final ExpressionCache<Parser.Expression> parseCache = new ExpressionCache<>();
    private final ExpressionCache<Object> evalCache = new ExpressionCache<>();

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
            if (isMaterialName(expression)) {
                Material material = Material.valueOf(expression.toUpperCase().replace("\"", "").replace("'", ""));
                return type.cast(material);
            }
            Map<String, Object> variableState = new HashMap<>();
            Parser.Expression expr = getOrParseExpression(expression, variableState);
            Object result = getOrEvalExpression(expr, expression, variableState);
            if (result == null) {
                return null;
            }
            if (type.isPrimitive() && result instanceof Number) {
                return type.cast(((Number) result).doubleValue());
            }
            if (!type.isInstance(result)) {
                throw new IllegalArgumentException("Unexpected result type: " + (result != null ? result.getClass().getSimpleName() : "null"));
            }
            return type.cast(result);
        } catch (RuntimeException e) {
            if (e instanceof ExpressionEngineException) throw e;
            throw new ExpressionEngineException("Error parsing expression: " + expression + " - " + e.getMessage(), e);
        }
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
            Map<String, Object> variableState = new HashMap<>();
            Parser.Expression expr = getOrParseExpression(expression, variableState);
            return getOrEvalExpression(expr, expression, variableState);
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

    public Object evaluate(String expression, Map<String, Object> variables) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }
        expression = expression.trim();
        if (expression.isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }

        try {
            if (interpreter.getLogger() != null) {
                interpreter.getLogger().accept("Evaluating expression: {}", new Object[]{expression});
            }

            if (isMaterialName(expression)) {
                Material material = Material.valueOf(expression.toUpperCase().replace("\"", "").replace("'", ""));
                return material;
            }

            Parser.Expression expr = getOrParseExpression(expression, variables);
            interpreter.clearVariables();
            for (Map.Entry<String, Object> var : variables.entrySet()) {
                interpreter.setVariable(var.getKey(), var.getValue());
            }

            Object result = getOrEvalExpression(expr, expression, variables);

            if (interpreter.getLogger() != null) {
                interpreter.getLogger().accept("Expression evaluation completed. Result: {}", new Object[]{result});
            }

            return result;
        } catch (RuntimeException e) {
            if (interpreter.getErrorHandler() != null) {
                interpreter.getErrorHandler().accept(this, e);
            }
            if (e instanceof ExpressionEngineException) throw e;
            throw new ExpressionEngineException("Error evaluating expression: " + expression + " - " + e.getMessage(), e);
        }
    }

    private Parser.Expression getOrParseExpression(String expression, Map<String, Object> variableState) {
        Set<String> relevantVars = extractRelevantVariables(expression);
        Map<String, Object> relevantState = new HashMap<>();
        for (String var : relevantVars) {
            if (variableState != null && variableState.containsKey(var)) {
                relevantState.put(var, variableState.get(var));
            }
        }

        ExpressionCacheKey key = new ExpressionCacheKey(expression, relevantState);
        Parser.Expression expr = parseCache.get(key);
        if (expr == null) {
            if (interpreter.getLogger() != null) {
                interpreter.getLogger().accept("Cache miss for parse: {}", new Object[]{expression});
            }
            lexer.setSource(expression);
            List<Token> tokens = lexer.scanTokens();
            parser.setTokens(tokens);
            expr = parser.parse();
            parseCache.put(key, expr);
        } else if (interpreter.getLogger() != null) {
            interpreter.getLogger().accept("Cache hit for parse: {}", new Object[]{expression});
        }
        return expr;
    }

    private Object getOrEvalExpression(Parser.Expression expr, String expression, Map<String, Object> variableState) {
        Set<String> relevantVars = extractRelevantVariables(expression);
        Map<String, Object> relevantState = new HashMap<>();
        for (String var : relevantVars) {
            if (variableState != null && variableState.containsKey(var)) {
                relevantState.put(var, variableState.get(var));
            }
        }

        ExpressionCacheKey key = new ExpressionCacheKey(expression, relevantState);
        Object result = evalCache.get(key);
        if (result == null) {
            if (interpreter.getLogger() != null) {
                interpreter.getLogger().accept("Cache miss for eval: {}", new Object[]{expression});
            }
            result = interpreter.interpret(expr);
            evalCache.put(key, result);
        } else if (interpreter.getLogger() != null) {
            interpreter.getLogger().accept("Cache hit for eval: {}", new Object[]{expression});
        }
        return result;
    }

    private Set<String> extractRelevantVariables(String expression) {
        Set<String> vars = new HashSet<>();
        lexer.setSource(expression);
        List<Token> tokens = lexer.scanTokens();
        for (Token token : tokens) {
            if (token.type() == TokenType.IDENTIFIER) {
                vars.add(token.lexeme());
            }
        }
        return vars;
    }

    private int computeVariableStateHash(Map<String, Object> variableState, Set<String> relevantVars) {
        int hash = 1;
        for (String var : relevantVars) {
            Object value = variableState.get(var);
            hash = 31 * hash + (value != null ? value.hashCode() : 0);
        }
        return hash;
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

    public void setVariable(String key, Object value, int uses) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Variable key cannot be null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("Variable value cannot be null");
        }
        interpreter.setVariable(key, value, uses);
    }

    public void setVariable(String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Variable key cannot be null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("Variable value cannot be null");
        }
        interpreter.setVariable(key, value);
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

    /**
     * Enables or disables strict mode for the parser.
     * <p>
     * In strict mode, the parser enforces stricter rules on the syntax and semantics of expressions.
     * This may include restrictions on variable usage, function calls, and type conversions.
     * </p>
     *
     * @param enabled True to enable strict mode, false to disable.
     */
    public void setStrictMode(boolean enabled) {
        interpreter.setStrict(enabled);
    }

    /**
     * Returns whether strict mode is enabled.
     *
     * @return True if strict mode is enabled, false otherwise.
     */
    public boolean isStrictMode() {
        return interpreter.isStrict();
    }

    /**
     * Sets the maximum depth for recursive expressions.
     * <p>
     * This prevents stack overflow errors in cases of deeply nested or recursive expressions.
     * </p>
     *
     * @param maxDepth The maximum depth (default is 100)
     */
    public void setMaxRecursionDepth(int maxDepth) {
        interpreter.setMaxRecursionDepth(maxDepth);
    }

    /**
     * Gets the current maximum depth for recursive expressions.
     *
     * @return The maximum depth
     */
    public int getMaxRecursionDepth() {
        return interpreter.getMaxRecursionDepth();
    }

    /**
     * Sets the timeout for expression evaluation in milliseconds.
     * <p>
     * If an expression exceeds this time limit, it will be terminated and an exception will be thrown.
     * </p>
     *
     * @param timeoutMillis The timeout in milliseconds
     */
    public void setEvaluationTimeout(int timeoutMillis) {
        interpreter.setEvaluationTimeout(timeoutMillis);
    }

    /**
     * Gets the current timeout for expression evaluation.
     *
     * @return The timeout in milliseconds
     */
    public int getEvaluationTimeout() {
        return interpreter.getEvaluationTimeout();
    }

    /**
     * Registers a custom variable type with the engine.
     * <p>
     * This allows expressions to use custom objects and types beyond the built-in ones.
     * </p>
     *
     * @param name The name of the variable type
     * @param clazz The class object representing the variable type
     */
    public void registerVariableType(String name, Class<?> clazz) {
        interpreter.registerVariableType(name, clazz);
    }

    /**
     * Registers a custom function type with the engine.
     * <p>
     * This allows expressions to use custom functions with specific signatures and behaviors.
     * </p>
     *
     * @param name The name of the function type
     * @param clazz The class object representing the function type
     */
    public void registerFunctionType(String name, Class<?> clazz) {
        interpreter.registerFunctionType(name, clazz);
    }

    /**
     * Sets a custom error handler for the expression engine.
     * <p>
     * The error handler is called when an error occurs during parsing or evaluation of expressions.
     * </p>
     *
     * @param handler The error handler function
     */
    public void setErrorHandler(BiConsumer<ExpressionEngine, Exception> handler) {
        interpreter.setErrorHandler(handler);
    }

    /**
     * Gets the current error handler for the expression engine.
     *
     * @return The error handler function
     */
    public BiConsumer<ExpressionEngine, Exception> getErrorHandler() {
        return interpreter.getErrorHandler();
    }

    /**
     * Sets a custom logger for the expression engine.
     * <p>
     * The logger is used to log debug and trace information during expression parsing and evaluation.
     * </p>
     *
     * @param logger The logger function
     */
    public void setLogger(BiConsumer<String, Object[]> logger) {
        interpreter.setLogger(logger);
    }

    /**
     * Gets the current logger for the expression engine.
     *
     * @return The logger function
     */
    public BiConsumer<String, Object[]> getLogger() {
        return interpreter.getLogger();
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
}
