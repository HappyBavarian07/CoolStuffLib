package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

import de.happybavarian07.coolstufflib.languagemanager.expressionengine.exceptions.ExpressionEngineException;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.exceptions.ExpressionVariableException;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.FunctionCall;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The interpreter evaluates parsed expressions and computes their values.
 * <p>
 * This class implements the Visitor pattern to traverse the expression tree produced by
 * the {@link Parser} and computes a result value. It supports mathematical operations,
 * logical operations, variable references, and function calls.
 * </p>
 * <p>
 * The interpreter maintains internal state in the form of variables and registered functions
 * that can be referenced during expression evaluation.
 * </p>
 */
public class Interpreter implements Parser.Expression.Visitor<Object> {
    private final Map<String, VariableWithUses> variables = new HashMap<>();
    private final Map<String, RegisteredFunction> functions = new HashMap<>();
    private final Map<String, Object> context = new HashMap<>();
    private final List<String> functionWhitelist = new ArrayList<>();
    private final List<String> functionBlacklist = new ArrayList<>();
    private final List<String> variableWhitelist = new ArrayList<>();
    private final List<String> variableBlacklist = new ArrayList<>();
    private boolean debug = false;
    private boolean strict = false;
    private int maxRecursionDepth = 100;
    private int evaluationTimeout = 0;
    private final Map<String, Class<?>> variableTypes = new HashMap<>();
    private final Map<String, Class<?>> functionTypes = new HashMap<>();
    private java.util.function.BiConsumer<ExpressionEngine, Exception> errorHandler;
    private java.util.function.BiConsumer<String, Object[]> logger;

    /**
     * Evaluates the given expression and returns its result.
     * <p>
     * This is the main entry point for expression evaluation. It handles any runtime errors
     * that may occur during evaluation and wraps them with additional context.
     * </p>
     *
     * @param expression The expression to evaluate
     * @return The result of evaluating the expression
     * @throws RuntimeException if an error occurs during evaluation
     */
    public Object interpret(Parser.Expression expression) {
        try {
            return evaluate(expression);
        } catch (RuntimeException error) {
            throw new RuntimeException("Runtime error: " + error.getMessage(), error);
        }
    }

    @Override
    public Object visitBinaryExpr(Parser.Expression.Binary expr) {
        if (expr.left == null || expr.right == null) {
            throw new ExpressionEngineException("Binary expression must have both left and right operands.");
        }
        if (expr.operator == null) {
            throw new ExpressionEngineException("Binary expression must have an operator.");
        }
        if (debug) {
            System.out.println("[ExpressionEngine DEBUG] Evaluating binary expression: " + expr);
        }
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type()) {
            case PLUS:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                }
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                throw new ExpressionEngineException("Operands must be two numbers or at least one string.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() - ((Number) right).doubleValue();
            case MULTIPLY:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() * ((Number) right).doubleValue();
            case DIVIDE:
                checkNumberOperands(expr.operator, left, right);
                if (((Number) right).doubleValue() == 0) throw new ExpressionEngineException("Division by zero.");
                return ((Number) left).doubleValue() / ((Number) right).doubleValue();
            case MODULO:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() % ((Number) right).doubleValue();
            case POWER:
                checkNumberOperands(expr.operator, left, right);
                return Math.pow(((Number) left).doubleValue(), ((Number) right).doubleValue());
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() > ((Number) right).doubleValue();
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() >= ((Number) right).doubleValue();
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() < ((Number) right).doubleValue();
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
            case EQUAL:
                return isEqual(left, right);
            case NOT_EQUAL:
                return !isEqual(left, right);
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Parser.Expression.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.type() == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else if (expr.operator.type() == TokenType.AND) {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Parser.Expression.Unary expr) {
        Object right = evaluate(expr.right);
        return switch (expr.operator.type()) {
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                yield -(double) right;
            }
            case NOT -> !isTruthy(right);
            default -> null;
        };
    }

    @Override
    public Object visitLiteralExpr(Parser.Expression.Literal expr) {
        if (expr.value instanceof String str) {
            if ((str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"))) {
                return str.substring(1, str.length() - 1);
            }
            if (context.containsKey(str)) {
                return context.get(str);
            }
            try {
                return Double.valueOf(str);
            } catch (NumberFormatException ignored) {
            }
            if (str.equalsIgnoreCase("true")) return true;
            if (str.equalsIgnoreCase("false")) return false;
            try {
                return Material.valueOf(str.toUpperCase().replace("'", "").replace("\"", ""));
            } catch (IllegalArgumentException e) {
                return str;
            }
        }
        if (expr.value instanceof Integer i) return i.doubleValue();
        if (expr.value instanceof Long l) return l.doubleValue();
        if (expr.value instanceof Float f) return f.doubleValue();
        return expr.value;
    }

    @Override
    public Object visitVariableExpr(Parser.Expression.Variable expr) {
        String name = expr.name.lexeme();
        if (!isVariableAllowed(name))
            throw new ExpressionVariableException("Access to variable '" + name + "' is not allowed");

        if (variables.containsKey(name)) {
            VariableWithUses v = variables.get(name);
            //System.out.println("[Variable tracking] Accessing variable " + name + " (remaining uses: " + v.remainingUses + ")");
            Object value = v.getValue();
            //System.out.println("[Variable tracking] After access variable " + name + " (remaining uses: " + v.remainingUses + ")");
            return value;
        }

        if (context.containsKey(name)) {
            return context.get(name);
        }

        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ExpressionVariableException("Undefined variable or context: " + name);
        }
    }

    @Override
    public Object visitGroupingExpr(Parser.Expression.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitTernaryExpr(Parser.Expression.Ternary expr) {
        Object condition = evaluate(expr.condition);
        if (isTruthy(condition)) {
            return evaluate(expr.trueExpression);
        } else {
            return evaluate(expr.falseExpression);
        }
    }

    @Override
    public Object visitConditionalChainExpr(Parser.Expression.ConditionalChain expr) {
        for (Parser.Expression.ConditionalBranch branch : expr.branches) {
            Object condResult = evaluate(branch.condition);
            if (condResult instanceof Boolean && (Boolean) condResult) {
                return evaluate(branch.output);
            }
        }
        if (expr.elseBranch != null) {
            return evaluate(expr.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitConditionalBranchExpr(Parser.Expression.ConditionalBranch expr) {
        // Not used directly; handled in visitConditionalChainExpr
        return null;
    }

    @Override
    public Object visitAssignmentExpr(Parser.Expression.Assignment expr) {
        Object value = evaluate(expr.value);
        int uses = expr.uses;
        setVariable(expr.name.lexeme(), value, uses);
        return value;
    }

    @Override
    public Object visitSequenceExpr(Parser.Expression.Sequence expr) {
        Object result = null;
        for (Parser.Expression e : expr.exprs) {
            result = evaluate(e);
        }
        return result;
    }

    private Object evaluate(Parser.Expression expression) {
        if (expression == null) {
            throw new ExpressionEngineException("Cannot evaluate null expression");
        }
        try {
            if (debug) {
                System.out.println("[ExpressionEngine DEBUG] Evaluating: " + expression);
            }
            Object result = expression.accept(this);
            if (debug) {
                System.out.println("[ExpressionEngine DEBUG] Result: " + result);
            }
            return result;
        } catch (Exception e) {
            throw new ExpressionEngineException("Error evaluating expression: " + e.getMessage(), e);
        }
    }

    @Override
    public Object visitCallExpr(Parser.Expression.Call expr) {
        String fullFunctionName = expr.name.lexeme();
        String functionName = fullFunctionName;
        String callType = null;
        int lt = fullFunctionName.indexOf('<');
        int gt = fullFunctionName.indexOf('>');
        if (lt != -1 && gt != -1 && gt > lt) {
            functionName = fullFunctionName.substring(0, lt);
            callType = fullFunctionName.substring(lt + 1, gt);
        }
        if (!isFunctionAllowed(functionName))
            throw new ExpressionEngineException("Access to function '" + functionName + "' is not allowed");
        RegisteredFunction reg = functions.get(functionName);
        if (reg == null) {
            throw new ExpressionEngineException("Undefined function: " + fullFunctionName);
        }
        List<Object> arguments = new ArrayList<>();
        for (int i = 0; i < expr.arguments.size(); i++) {
            Object arg = evaluate(expr.arguments.get(i));
            // Type inference & auto-conversion
            if (reg.argTypes != null && i < reg.argTypes.length && reg.argTypes[i] != null) {
                arg = TypeUtil.convert(arg, reg.argTypes[i]);
            }
            arguments.add(arg);
        }
        if (callType == null && reg.defaultType != null) {
            callType = reg.defaultType;
        }
        try {
            Object result = reg.function.call(this, arguments, callType);
            // Auto-convert return type
            if (reg.returnType != null) {
                result = TypeUtil.convert(result, reg.returnType);
            }
            return result;
        } catch (Exception e) {
            throw new ExpressionEngineException("Error calling function " + fullFunctionName + ": " + e.getMessage(), e);
        }
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        if (object instanceof Number) return ((Number) object).doubleValue() != 0;
        if (object instanceof String) return !((String) object).isEmpty();
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Number) return;
        throw new ExpressionEngineException("Invalid operand for operator " + operator.lexeme() + ": " + operand + " (must be a number)");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        checkNumberOperand(operator, left);
        checkNumberOperand(operator, right);
    }

    private String stringify(Object object) {
        if (object == null) return "null";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    public void setVariable(String name, Object value) {
        setVariable(name, value, -1);
    }

    public void setVariable(String name, Object value, int uses) {
        variables.put(name, new VariableWithUses(value, uses));
    }

    public void setVariable(String name, Object value, int uses, boolean overwrite) {
        if (overwrite || !variables.containsKey(name)) {
            variables.put(name, new VariableWithUses(value, uses));
        }
    }

    public boolean hasVariable(String name, boolean checkUses) {
        VariableWithUses var = variables.get(name);
        if (var == null) return false;
        return !checkUses || var.remainingUses > 0;
    }

    public void removeVariable(String name) {
        variables.remove(name);
    }

    /**
     * Retrieves a variable by name and decrements its remaining uses.
     * If the variable has no remaining uses, it is removed from the map.
     *
     * @param name The name of the variable to retrieve.
     * @return The value of the variable, or null if it does not exist or has no remaining uses.
     */
    public Object getVariable(String name) {
        VariableWithUses var = variables.get(name);
        if (var == null) return null;

        return var.getValue();
    }

    /**
     * Peeks at a variable's value without decrementing its uses.
     * This is useful for checking the value without modifying its state.
     *
     * @param name The name of the variable to peek at.
     * @return The value of the variable, or null if it does not exist.
     */
    public Object peekVariable(String name) {
        VariableWithUses var = variables.get(name);
        if (var == null) return null;
        return var.peekValue();
    }

    public void clearVariables() {
        variables.clear();
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public void registerFunction(String name, FunctionCall function) {
        registerFunction(name, function, null, null, null);
    }

    public void registerFunction(String name, FunctionCall function, String defaultType) {
        registerFunction(name, function, defaultType, null, null);
    }

    public void registerFunction(String name, FunctionCall function, String defaultType, Class<?>[] argTypes, Class<?> returnType) {
        if (functions.containsKey(name))
            return;
        functions.put(name, new RegisteredFunction(function, defaultType, argTypes, returnType));
    }

    public void unregisterFunction(String name) {
        functions.remove(name);
    }

    public void clearFunctions() {
        functions.clear();
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean enabled) {
        this.debug = enabled;
    }

    public void putContext(String key, Object value) {
        context.put(key, value);
    }

    public void removeContext(String key) {
        context.remove(key);
    }

    public void clearContext() {
        context.clear();
    }

    public void setFunctionWhitelist(List<String> whitelist) {
        functionWhitelist.clear();
        if (whitelist != null) functionWhitelist.addAll(whitelist);
    }

    public void setFunctionBlacklist(List<String> blacklist) {
        functionBlacklist.clear();
        if (blacklist != null) functionBlacklist.addAll(blacklist);
    }

    public void setVariableWhitelist(List<String> whitelist) {
        variableWhitelist.clear();
        if (whitelist != null) variableWhitelist.addAll(whitelist);
    }

    public void setVariableBlacklist(List<String> blacklist) {
        variableBlacklist.clear();
        if (blacklist != null) variableBlacklist.addAll(blacklist);
    }

    private boolean isFunctionAllowed(String name) {
        if (!functionWhitelist.isEmpty() && !functionWhitelist.contains(name)) return false;
        if (functionBlacklist.contains(name)) return false;
        return true;
    }

    private boolean isVariableAllowed(String name) {
        if (!variableWhitelist.isEmpty() && !variableWhitelist.contains(name)) return false;
        if (variableBlacklist.contains(name)) return false;
        return true;
    }

    public void setStrict(boolean enabled) { strict = enabled; }
    public boolean isStrict() { return strict; }
    public void setMaxRecursionDepth(int maxDepth) { maxRecursionDepth = maxDepth; }
    public int getMaxRecursionDepth() { return maxRecursionDepth; }
    public void setEvaluationTimeout(int timeoutMillis) { evaluationTimeout = timeoutMillis; }
    public int getEvaluationTimeout() { return evaluationTimeout; }
    public void registerVariableType(String name, Class<?> clazz) { if (name != null && clazz != null) variableTypes.put(name, clazz); }
    public void registerFunctionType(String name, Class<?> clazz) { if (name != null && clazz != null) functionTypes.put(name, clazz); }
    public void setErrorHandler(java.util.function.BiConsumer<ExpressionEngine, Exception> handler) { errorHandler = handler; }
    public java.util.function.BiConsumer<ExpressionEngine, Exception> getErrorHandler() { return errorHandler; }
    public void setLogger(java.util.function.BiConsumer<String, Object[]> logger) { this.logger = logger; }
    public java.util.function.BiConsumer<String, Object[]> getLogger() { return logger; }

    private static class VariableWithUses {
        private final Object value;
        private final int maxUses;
        private int remainingUses;

        VariableWithUses(Object value, int uses) {
            this.value = value;
            this.remainingUses = uses;
            this.maxUses = uses;
        }

        /**
         * Retrieves the value of the variable, decrementing its remaining uses.
         * If the variable has unlimited uses (uses < 0), it returns the value without decrementing.
         *
         * @return The value of the variable.
         * @throws ExpressionVariableException if the variable has no remaining uses.
         */
        Object getValue() {
            //System.out.println("[ExpressionEngine DEBUG] Getting value of variable: " + value + " (remaining uses: " + remainingUses + ")");
            if (remainingUses < 0) {
                //System.out.println("[ExpressionEngine DEBUG] Using variable with unlimited uses: " + value);
                return value;
            }
            if (remainingUses == 0) {
                throw new ExpressionVariableException("Variable exceeded its allowed uses (0/" + maxUses + ")");
            }
            //System.out.println("[ExpressionEngine DEBUG] Using variable: " + value + " (remaining uses: " + remainingUses + ")");
            remainingUses--;
            //System.out.println("[ExpressionEngine DEBUG] Using variable: " + value + " (remaining uses: " + remainingUses + ")");
            return value;
        }

        Object peekValue() {
            return value;
        }
    }

    private record RegisteredFunction(FunctionCall function, String defaultType, Class<?>[] argTypes,
                                      Class<?> returnType) {
    }
}
