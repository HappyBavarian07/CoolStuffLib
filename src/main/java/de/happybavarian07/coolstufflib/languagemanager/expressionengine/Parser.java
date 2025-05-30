package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

import java.util.ArrayList;
import java.util.List;

/**
 * A recursive descent parser that processes tokens into an abstract syntax tree (Expression objects).
 * <p>
 * The parser follows a traditional grammar hierarchy to handle operator precedence:
 * expression → ternary
 * ternary → or (? expression : expression)?
 * or → and ("or" and)*
 * and → equality ("and" equality)*
 * equality → comparison (("==" | "!=") comparison)*
 * comparison → term ((">", ">=", "<", "<=") term)*
 * term → factor (("+-") factor)*
 * factor → unary (("*" | "/" | "%" | "^") unary)*
 * unary → ("!" | "-") unary | primary
 * primary → NUMBER | STRING | IDENTIFIER | "(" expression ")" | IDENTIFIER(arguments)
 * </p>
 * <p>
 * It also handles conditional chains in the form: if condition: result elif condition: result else: result
 * </p>
 */
public class Parser {
    private static class ExpressionSyntaxException extends RuntimeException {
        public ExpressionSyntaxException(String message) {
            super(message);
        }
    }

    private List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens != null ? tokens : new ArrayList<>();
    }

    /**
     * Sets the token list to be parsed and resets the current position to the beginning.
     *
     * @param tokens The list of tokens to parse
     */
    public void setTokens(List<Token> tokens) {
        this.tokens = tokens != null ? tokens : new ArrayList<>();
        this.current = 0;
    }

    /**
     * Parses the current token list and returns the resulting expression.
     * <p>
     * This method determines whether to parse as a standard expression or as a conditional chain
     * based on the presence of an IF token at the beginning.
     * </p>
     *
     * @return The parsed expression, or null if parsing fails
     */
    public Expression parse() {
        try {
            if (check(TokenType.IF)) {
                return parseConditionalChain();
            }
            if (check(TokenType.LET)) {
                return assignment();
            }
            return sequence();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parses a conditional chain expression in the form:
     * <pre>
     * if condition: result
     * elif condition: result
     * else: result
     * </pre>
     * <p>
     * The method handles multiple if/elif branches and an optional else branch. Each branch consists
     * of a condition expression followed by a colon and then the result expression to evaluate
     * if the condition is true.
     * </p>
     * <p>
     * Example:
     * <pre>
     * if player.hasPermission("admin"): DIAMOND_BLOCK
     * elif player.hasPermission("moderator"): GOLD_BLOCK
     * else: STONE
     * </pre>
     * </p>
     *
     * @return A ConditionalChain expression containing all the parsed branches
     * @throws RuntimeException if the syntax is invalid
     */
    public Expression parseConditionalChain() {
        List<Expression.ConditionalBranch> branches = new ArrayList<>();
        Expression elseBranch = null;
        boolean found = false;
        matchAll(-1, TokenType.NEWLINE);

        while (check(TokenType.IF) || check(TokenType.ELIF)) {
            matchAll(-1, TokenType.NEWLINE);
            advance();

            Expression condition = expression();

            matchAll(-1, TokenType.NEWLINE);
            if (!check(TokenType.COLON) && check(TokenType.ELSE)) {
                break;
            }
            consume(TokenType.COLON, "Expect ':' after condition.");
            matchAll(-1, TokenType.NEWLINE);

            Expression output = expression();
            branches.add(new Expression.ConditionalBranch(condition, output));
            found = true;

            matchAll(-1, TokenType.NEWLINE);

            if (check(TokenType.ELSE) || check(TokenType.EOF)) {
                break;
            }

            if (!(check(TokenType.IF) || check(TokenType.ELIF) || check(TokenType.ELSE) || check(TokenType.EOF))) {
                throw new ExpressionSyntaxException("Expected 'if', 'elif', 'else', or end of input after branch.");
            }
        }

        matchAll(-1, TokenType.NEWLINE);

        if (check(TokenType.ELSE)) {
            advance();

            matchAll(-1, TokenType.NEWLINE);
            consume(TokenType.COLON, "Expect ':' after 'else' keyword.");
            matchAll(-1, TokenType.NEWLINE);

            elseBranch = expression();
            found = true;
        }

        if (!found) {
            throw new ExpressionSyntaxException("Expected 'if', 'elif', or 'else'.");
        }

        return new Expression.ConditionalChain(branches, elseBranch);
    }

    private Expression assignment() {
        consume(TokenType.LET, "Expect 'let' at start of assignment.");
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name after 'let'.");
        consume(TokenType.ASSIGN, "Expect '=' after variable name.");
        Expression value = expression();
        int uses = -1;
        if (match(TokenType.AS)) {
            Token usesToken = consume(TokenType.NUMBER, "Expect number after 'as' for variable uses.");
            uses = ((Number) usesToken.literal()).intValue();
        }
        System.out.println("Assigning variable: " + name.lexeme() + " with uses: " + uses);
        return new Expression.Assignment(name, value, uses);
    }

    private Expression expression() {
        return ternary();
    }

    private Expression ternary() {
        Expression expression = or();
        if (match(TokenType.QUESTION)) {
            Expression trueExpression = expression();
            consume(TokenType.COLON, "Expect ':' after true branch of ternary.");
            Expression falseExpression = expression();
            return new Expression.Ternary(expression, trueExpression, falseExpression);
        }
        return expression;
    }

    private Expression or() {
        Expression expression = and();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expression right = and();
            expression = new Expression.Logical(expression, operator, right);
        }
        return expression;
    }

    private Expression and() {
        Expression expression = equality();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expression right = equality();
            expression = new Expression.Logical(expression, operator, right);
        }
        return expression;
    }

    private Expression equality() {
        Expression expression = comparison();
        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            Token operator = previous();
            Expression right = comparison();
            expression = new Expression.Binary(expression, operator, right);
        }
        return expression;
    }

    private Expression comparison() {
        Expression expression = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expression right = term();
            expression = new Expression.Binary(expression, operator, right);
        }
        return expression;
    }

    private Expression term() {
        Expression expression = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expression right = factor();
            expression = new Expression.Binary(expression, operator, right);
        }
        return expression;
    }

    private Expression factor() {
        Expression expression = unary();
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO, TokenType.POWER)) {
            Token operator = previous();
            Expression right = unary();
            expression = new Expression.Binary(expression, operator, right);
        }
        return expression;
    }

    private Expression unary() {
        if (match(TokenType.NOT, TokenType.MINUS)) {
            Token operator = previous();
            Expression right = unary();
            return new Expression.Unary(operator, right);
        }
        return primary();
    }

    private Expression primary() {
        if (match(TokenType.NUMBER)) {
            return new Expression.Literal(previous().literal());
        }
        if (match(TokenType.STRING)) {
            // Always wrap string literal with quotes for Interpreter to recognize
            String raw = previous().literal().toString();
            String quoted = '"' + raw + '"';
            return new Expression.Literal(quoted);
        }
        if (match(TokenType.IDENTIFIER)) {
            Token name = previous();
            // Support generic function calls like Out<Material>(...)
            // Only do this if we're not in a comparison context
            // We need to check if this is part of a comparison or a generic type
            if (match(TokenType.LESS)) {
                // Store the current position in case we need to backtrack
                int checkpointCurrent = current - 1; // -1 to go back before the < token
                boolean isGenericCall = isIsGenericCall();

                if (isGenericCall) {
                    // This is a generic call like Out<Material>
                    StringBuilder typeBuilder = new StringBuilder();
                    while (!check(TokenType.GREATER) && !isAtEnd()) {
                        typeBuilder.append(peek().lexeme());
                        advance();
                    }
                    consume(TokenType.GREATER, "Expect '>' after generic type.");
                    name = new Token(name.type(), name.lexeme() + "<" + typeBuilder + ">", name.literal(), name.position());
                } else {
                    // This is a comparison operator, revert to the state before the < token
                    current = checkpointCurrent;
                    return new Expression.Variable(name);
                }
            }
            // Check if this is a function call
            if (match(TokenType.LPAREN)) {
                List<Expression> arguments = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        arguments.add(expression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "Expect ')' after function arguments.");
                return new Expression.Call(name, arguments);
            }
            return new Expression.Variable(name);
        }
        if (match(TokenType.LPAREN)) {
            Expression expression = expression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
            return new Expression.Grouping(expression);
        }
        throw new ExpressionSyntaxException("Expect expression.");
    }

    private boolean isIsGenericCall() {
        boolean hasGreater = false;
        int nesting = 1;

        // Look ahead to see if this is likely a generic declaration
        int lookAhead = current;
        while (lookAhead < tokens.size() && nesting > 0) {
            Token lookToken = tokens.get(lookAhead);
            if (lookToken.type() == TokenType.LESS) nesting++;
            if (lookToken.type() == TokenType.GREATER) {
                nesting--;
                if (nesting == 0) hasGreater = true;
            }
            lookAhead++;
        }

        // If the next token after GREATER is LPAREN, it's likely a generic function call
        // Otherwise, it's probably a comparison operator
        return hasGreater && lookAhead < tokens.size() &&
                tokens.get(lookAhead).type() == TokenType.LPAREN;
    }

    private Expression sequence() {
        List<Expression> exprs = new ArrayList<>();
        exprs.add(expression());
        while (match(TokenType.SEMICOLON)) {
            if (isAtEnd()) break;
            exprs.add(expression());
        }
        if (exprs.size() == 1) return exprs.get(0);
        return new Expression.Sequence(exprs);
    }

    private boolean match(TokenType... types) {
        if (tokens == null || tokens.isEmpty()) return false;
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean matchAll(int x, TokenType... types) {
        if (tokens == null || tokens.isEmpty()) return false;
        int count = 0;
        boolean matchedAtAll = false;
        while (!isAtEnd() && count < x) {
            boolean matched = false;
            for (TokenType type : types) {
                if (check(type)) {
                    advance();
                    matchedAtAll = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) break;
            count++;
        }
        return matchedAtAll;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new ExpressionSyntaxException(message);
    }

    // Consume without advancing the current token
    private Token consumeWithoutAdvance(TokenType type, String message) {
        if (check(type)) return peek();
        throw new ExpressionSyntaxException(message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd() || tokens == null || tokens.isEmpty()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    public static abstract class Expression {
        public abstract <R> R accept(Visitor<R> visitor);

        public static interface Visitor<R> {
            R visitBinaryExpr(Binary expr);

            R visitCallExpr(Call expr);

            R visitLogicalExpr(Logical expr);

            R visitUnaryExpr(Unary expr);

            R visitLiteralExpr(Literal expr);

            R visitVariableExpr(Variable expr);

            R visitGroupingExpr(Grouping expr);

            R visitTernaryExpr(Ternary expr);

            R visitConditionalChainExpr(ConditionalChain expr);

            R visitConditionalBranchExpr(ConditionalBranch expr);

            R visitAssignmentExpr(Assignment expr);

            R visitSequenceExpr(Sequence expr);
        }

        public static class Binary extends Expression {
            public final Expression left;
            public final Token operator;
            public final Expression right;

            public Binary(Expression left, Token operator, Expression right) {
                this.left = left;
                this.operator = operator;
                this.right = right;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitBinaryExpr(this);
            }
        }

        public static class Logical extends Expression {
            public final Expression left;
            public final Token operator;
            public final Expression right;

            public Logical(Expression left, Token operator, Expression right) {
                this.left = left;
                this.operator = operator;
                this.right = right;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitLogicalExpr(this);
            }
        }

        public static class Unary extends Expression {
            public final Token operator;
            public final Expression right;

            public Unary(Token operator, Expression right) {
                this.operator = operator;
                this.right = right;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return (R) visitor.visitUnaryExpr(this);
            }
        }

        public static class Literal extends Expression {
            public final Object value;

            public Literal(Object value) {
                this.value = value;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return (R) visitor.visitLiteralExpr(this);
            }
        }

        public static class Variable extends Expression {
            public final Token name;

            public Variable(Token name) {
                this.name = name;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return (R) visitor.visitVariableExpr(this);
            }
        }

        public static class Grouping extends Expression {
            public final Expression expression;

            public Grouping(Expression expression) {
                this.expression = expression;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return (R) visitor.visitGroupingExpr(this);
            }
        }

        public static class Call extends Expression {
            public final Token name;
            public final List<Expression> arguments;

            public Call(Token name, List<Expression> arguments) {
                this.name = name;
                this.arguments = arguments;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitCallExpr(this);
            }
        }

        public static class Ternary extends Expression {
            public final Expression condition;
            public final Expression trueExpression;
            public final Expression falseExpression;

            public Ternary(Expression condition, Expression trueExpression, Expression falseExpression) {
                this.condition = condition;
                this.trueExpression = trueExpression;
                this.falseExpression = falseExpression;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return (R) visitor.visitTernaryExpr(this);
            }
        }

        public static class ConditionalChain extends Expression {
            public final List<ConditionalBranch> branches;
            public final Expression elseBranch;

            public ConditionalChain(List<ConditionalBranch> branches, Expression elseBranch) {
                this.branches = branches;
                this.elseBranch = elseBranch;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitConditionalChainExpr(this);
            }
        }

        public static class ConditionalBranch extends Expression {
            public final Expression condition;
            public final Expression output;

            public ConditionalBranch(Expression condition, Expression output) {
                this.condition = condition;
                this.output = output;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitConditionalBranchExpr(this);
            }
        }

        public static class Assignment extends Expression {
            public final Token name;
            public final Expression value;
            public final int uses;

            public Assignment(Token name, Expression value, int uses) {
                this.name = name;
                this.value = value;
                this.uses = uses;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return (R) visitor.visitAssignmentExpr(this);
            }
        }

        public static class Sequence extends Expression {
            public final List<Expression> exprs;

            public Sequence(List<Expression> exprs) {
                this.exprs = exprs;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitSequenceExpr(this);
            }
        }
    }
}
