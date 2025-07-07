package de.happybavarian07.coolstufflib.cache.expression;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class ExpressionCacheKey {
    private final String expression;
    private final int variableStateHash;
    private final String[] relevantVariables;

    public ExpressionCacheKey(String expression, int variableStateHash, String[] relevantVariables) {
        this.expression = expression;
        this.variableStateHash = variableStateHash;
        this.relevantVariables = relevantVariables != null ? relevantVariables : new String[0];
    }

    public ExpressionCacheKey(String expression, Map<String, Object> variables) {
        this.expression = expression;
        if (variables == null || variables.isEmpty()) {
            this.variableStateHash = 0;
            this.relevantVariables = new String[0];
        } else {
            this.relevantVariables = variables.keySet().toArray(new String[0]);
            Arrays.sort(this.relevantVariables);

            int hash = 1;
            for (String var : this.relevantVariables) {
                Object value = variables.get(var);
                hash = 31 * hash + (value != null ? value.hashCode() : 0);
            }
            this.variableStateHash = hash;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpressionCacheKey that = (ExpressionCacheKey) o;
        return variableStateHash == that.variableStateHash &&
                Objects.equals(expression, that.expression) &&
                Arrays.equals(relevantVariables, that.relevantVariables);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(expression, variableStateHash);
        result = 31 * result + Arrays.hashCode(relevantVariables);
        return result;
    }

    public String getExpression() {
        return expression;
    }

    public int getVariableStateHash() {
        return variableStateHash;
    }

    public String[] getRelevantVariables() {
        return relevantVariables;
    }

    @Override
    public String toString() {
        return "ExpressionCacheKey{" + "expression='" + expression + '\'' +
                ", variableStateHash=" + variableStateHash +
                ", relevantVariables=" + Arrays.toString(relevantVariables) +
                '}';
    }
}
