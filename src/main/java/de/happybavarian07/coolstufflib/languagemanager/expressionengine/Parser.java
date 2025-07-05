package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>A recursive descent parser that processes tokens into an abstract syntax tree (Expression objects)
 * for the language manager's expression engine. This parser implements a complete expression grammar
 * with proper operator precedence and supports advanced language features.</p>
 *
 * <p>The parser follows a traditional grammar hierarchy to handle operator precedence correctly:</p>
 * <ul>
 * <li>expression → ternary</li>
 * <li>ternary → or (? expression : expression)?</li>
 * <li>or → and ("or" and)*</li>
 * <li>and → equality ("and" equality)*</li>
 * <li>equality → comparison (("==" | "!=") comparison)*</li>
 * <li>comparison → term (("&gt;", "&gt;=", "&lt;", "&lt;=") term)*</li>
 * <li>term → factor (("+-") factor)*</li>
 * <li>factor → unary (("*" | "/" | "%" | "^") unary)*</li>
 * <li>unary → ("!" | "-") unary | primary</li>
 * <li>primary → NUMBER | STRING | IDENTIFIER | "(" expression ")" | IDENTIFIER(arguments)</li>
 * </ul>
 *
 * <p>The parser also supports advanced features including:</p>
 * <ul>
 * <li>Conditional chains with if/elif/else statements</li>
 * <li>Variable assignments with scoped lifetimes</li>
 * <li>Function calls with arguments</li>
 * <li>Complex nested expressions</li>
 * <li>Material-based conditional expressions</li>
 * </ul>
 *
 * <pre><code>
 * // Basic expression parsing
 * Lexer lexer = new Lexer("player.level &gt; 10 &amp;&amp; player.hasPermission('admin')");
 * List&lt;Token&gt; tokens = lexer.scanTokens();
 * Parser parser = new Parser(tokens);
 * Expression result = parser.parse();
 *
 * // Conditional chain parsing
 * String conditionalExpr = "if player.level > 20: DIAMOND_BLOCK elif player.level > 10: GOLD_BLOCK else: STONE";
 * parser.setTokens(new Lexer(conditionalExpr).scanTokens());
 * Expression conditional = parser.parse();
 * </code></pre>
 */
public class Parser {
    private static class ExpressionSyntaxException extends RuntimeException {
        public ExpressionSyntaxException(String message) {
            super(message);
        }
    }

    private List<Token> tokens;
    private int current = 0;

    /**
     * <p>Constructs a new Parser with the specified list of tokens to parse.</p>
     *
     * <pre><code>
     * Lexer lexer = new Lexer("player.health + 50");
     * List&lt;Token&gt; tokens = lexer.scanTokens();
     * Parser parser = new Parser(tokens);
     * </code></pre>
     *
     * @param tokens the list of tokens to parse, null will be converted to an empty list
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens != null ? tokens : new ArrayList<>();
    }

    /**
     * <p>Sets a new token list to be parsed and resets the current parsing position to the beginning.
     * This allows reusing the same parser instance for multiple expressions.</p>
     *
     * <pre><code>
     * Parser parser = new Parser(Collections.emptyList());
     * parser.setTokens(lexer1.scanTokens());
     * Expression expr1 = parser.parse();
     *
     * parser.setTokens(lexer2.scanTokens());
     * Expression expr2 = parser.parse();
     * </code></pre>
     *
     * @param tokens the new list of tokens to parse, null will be converted to an empty list
     */
    public void setTokens(List<Token> tokens) {
        this.tokens = tokens != null ? tokens : new ArrayList<>();
        this.current = 0;
    }

    /**
     * <p>Parses the current token list and returns the resulting expression tree. This is the main
     * entry point for parsing that automatically detects the expression type and delegates to
     * the appropriate parsing method.</p>
     *
     * <p>The method supports parsing:</p>
     * <ul>
     * <li>Conditional chains (starting with 'if')</li>
     * <li>Variable assignments (starting with 'let')</li>
     * <li>Standard expressions (mathematical, logical, function calls)</li>
     * </ul>
     *
     * <pre><code>
     * // Parse a simple expression
     * parser.setTokens(new Lexer("5 + 3 * 2").scanTokens());
     * Expression mathExpr = parser.parse();
     *
     * // Parse a conditional chain
     * parser.setTokens(new Lexer("if x > 10: 'high' else: 'low'").scanTokens());
     * Expression conditionalExpr = parser.parse();
     * </code></pre>
     *
     * @return the parsed expression tree, or null if parsing fails due to syntax errors
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
     * <p>Parses a conditional chain expression with support for multiple if/elif branches
     * and an optional else branch. This method handles complex conditional logic with
     * proper syntax validation.</p>
     *
     * <p>Supported syntax format:</p>
     * <pre><code>
     * if condition: result
     * elif condition: result
     * elif condition: result
     * else: result
     * </code></pre>
     *
     * <p>Each branch consists of a condition expression followed by a colon and then
     * the result expression to evaluate if the condition is true. The elif and else
     * branches are optional.</p>
     *
     * <pre><code>
     * // Complex conditional chain example
     * String expr = """
     *     if player.hasPermission("admin"): DIAMOND_BLOCK
     *     elif player.hasPermission("moderator"): GOLD_BLOCK
     *     elif player.level > 10: IRON_BLOCK
     *     else: STONE
     *     """;
     * parser.setTokens(new Lexer(expr).scanTokens());
     * Expression result = parser.parseConditionalChain();
     * </code></pre>
     *
     * @return a ConditionalChain expression containing all the parsed branches and optional else clause
     * @throws ExpressionSyntaxException if the conditional syntax is invalid or malformed
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
        //System.out.println("Assigning variable: " + name.lexeme() + " with uses: " + uses);
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

    /**
     * <p>Abstract base class for all expression nodes in the abstract syntax tree. Each expression
     * represents a syntactic construct that can be evaluated to produce a value during execution.</p>
     *
     * <p>This class implements the Visitor pattern to allow different operations to be performed
     * on expression trees without modifying the expression classes themselves. Each concrete
     * expression type implements the accept method to dispatch to the appropriate visitor method.</p>
     *
     * <pre><code>
     * Expression expr = new Binary(
     *     new Literal(10),
     *     new Token(TokenType.PLUS, "+", null, 1),
     *     new Literal(5)
     * );
     * Integer result = expr.accept(new EvaluationVisitor());
     * </code></pre>
     */
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

        /**
         * <p>Represents a binary operation expression with a left operand, operator, and right operand.
         * Handles mathematical operations (+, -, *, /, %, ^) and comparison operations (==, !=, &lt;, &gt;, &lt;=, &gt;=).</p>
         *
         * <pre><code>
         * // Creates: 10 + 5
         * Expression expr = new Binary(
         *     new Literal(10),
         *     new Token(TokenType.PLUS, "+", null, 1),
         *     new Literal(5)
         * );
         * </code></pre>
         */
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

        /**
         * <p>Represents a logical expression that combines two expressions with a logical operator.</p>
         *
         * <p>Handles logical AND and OR operations where the right operand may not be evaluated
         * if the left operand determines the result.</p>
         *
         * <pre><code>
         * // Creates: player.isOnline() &amp;&amp; player.hasPermission("admin")
         * Expression expr = new Logical(
         *     new Call(new Token(TokenType.IDENTIFIER, "player.isOnline", null, 1), Collections.emptyList()),
         *     new Token(TokenType.AND, "&amp;&amp;", null, 1),
         *     new Call(new Token(TokenType.IDENTIFIER, "player.hasPermission", null, 1),
         *         Arrays.asList(new Literal("admin")))
         * );
         * </code></pre>
         */
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

        /**
         * <p>Represents a unary operation expression that applies an operator to a single operand.
         * Handles negation (-) and logical NOT (!) operations.</p>
         *
         * <pre><code>
         * // Creates: !player.isBanned()
         * Expression expr = new Unary(
         *     new Token(TokenType.BANG, "!", null, 1),
         *     new Call(new Token(TokenType.IDENTIFIER, "player.isBanned", null, 1), Collections.emptyList())
         * );
         * </code></pre>
         */
        public static class Unary extends Expression {
            public final Token operator;
            public final Expression right;

            public Unary(Token operator, Expression right) {
                this.operator = operator;
                this.right = right;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitUnaryExpr(this);
            }
        }

        /**
         * <p>Represents a literal value expression that contains a constant value such as numbers,
         * strings, booleans, or null. This is a terminal node in the expression tree.</p>
         *
         * <pre><code>
         * Expression numberLiteral = new Literal(42);
         * Expression stringLiteral = new Literal("Hello World");
         * Expression booleanLiteral = new Literal(true);
         * Expression nullLiteral = new Literal(null);
         * </code></pre>
         */
        public static class Literal extends Expression {
            public final Object value;

            public Literal(Object value) {
                this.value = value;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitLiteralExpr(this);
            }
        }

        /**
         * <p>Represents a variable reference expression that looks up a value by name in the
         * current execution context. Variables can reference player properties, configuration
         * values, or previously assigned values.</p>
         *
         * <pre><code>
         * // References a variable named "playerLevel"
         * Expression expr = new Variable(
         *     new Token(TokenType.IDENTIFIER, "playerLevel", null, 1)
         * );
         * </code></pre>
         */
        public static class Variable extends Expression {
            public final Token name;

            public Variable(Token name) {
                this.name = name;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitVariableExpr(this);
            }
        }

        /**
         * <p>Represents a grouped expression enclosed in parentheses that controls evaluation order.
         * The grouping itself doesn't change the value but affects operator precedence.</p>
         *
         * <pre><code>
         * // Creates: (10 + 5) * 2
         * Expression expr = new Binary(
         *     new Grouping(new Binary(new Literal(10), plusToken, new Literal(5))),
         *     multiplyToken,
         *     new Literal(2)
         * );
         * </code></pre>
         */
        public static class Grouping extends Expression {
            public final Expression expression;

            public Grouping(Expression expression) {
                this.expression = expression;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitGroupingExpr(this);
            }
        }

        /**
         * <p>Represents a function call expression with a function name and zero or more arguments.
         * Function calls can access player data, perform calculations, or invoke custom functions
         * registered with the expression engine.</p>
         *
         * <pre><code>
         * // Creates: max(player.level, 10)
         * Expression expr = new Call(
         *     new Token(TokenType.IDENTIFIER, "max", null, 1),
         *     Arrays.asList(
         *         new Variable(new Token(TokenType.IDENTIFIER, "player.level", null, 1)),
         *         new Literal(10)
         *     )
         * );
         * </code></pre>
         */
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

        /**
         * <p>Represents a ternary conditional expression (condition ? trueValue : falseValue) that
         * evaluates the condition and returns one of two values based on the result.</p>
         *
         * <pre><code>
         * // Creates: player.level > 10 ? "Advanced" : "Beginner"
         * Expression expr = new Ternary(
         *     new Binary(playerLevel, greaterToken, new Literal(10)),
         *     new Literal("Advanced"),
         *     new Literal("Beginner")
         * );
         * </code></pre>
         */
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
                return visitor.visitTernaryExpr(this);
            }
        }

        /**
         * <p>Represents a complete conditional chain with multiple if/elif branches and an optional
         * else clause. This provides more readable multi-condition logic than nested ternary expressions.</p>
         *
         * <pre><code>
         * // Creates: if player.level > 20: DIAMOND elif player.level > 10: GOLD else: STONE
         * Expression expr = new ConditionalChain(
         *     Arrays.asList(
         *         new ConditionalBranch(levelGt20, new Literal("DIAMOND")),
         *         new ConditionalBranch(levelGt10, new Literal("GOLD"))
         *     ),
         *     new Literal("STONE")
         * );
         * </code></pre>
         */
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

        /**
         * <p>Represents a single branch within a conditional chain, containing a condition
         * and the expression to evaluate if that condition is true. Used as part of
         * ConditionalChain expressions.</p>
         *
         * <pre><code>
         * // Creates a branch: player.level > 10: "Advanced Player"
         * Expression branch = new ConditionalBranch(
         *     new Binary(playerLevel, greaterToken, new Literal(10)),
         *     new Literal("Advanced Player")
         * );
         * </code></pre>
         */
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

        /**
         * <p>Represents a variable assignment expression that creates a temporary variable with
         * a specified lifetime. The variable exists for a limited number of uses and is automatically
         * cleaned up when the use count is exceeded.</p>
         *
         * <pre><code>
         * // Creates: let tempValue = player.level * 2 (usable 5 times)
         * Expression expr = new Assignment(
         *     new Token(TokenType.IDENTIFIER, "tempValue", null, 1),
         *     new Binary(playerLevel, multiplyToken, new Literal(2)),
         *     5
         * );
         * </code></pre>
         */
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
                return visitor.visitAssignmentExpr(this);
            }
        }

        /**
         * <p>Represents a sequence of expressions that are evaluated in order, with the final
         * expression's value being the result of the sequence. This allows complex multi-step
         * calculations and variable assignments within a single expression context.</p>
         *
         * <pre><code>
         * // Creates a sequence: assign tempVar, then use it in calculation
         * Expression expr = new Sequence(Arrays.asList(
         *     new Assignment(tempVarToken, playerLevelExpr, 3),
         *     new Binary(new Variable(tempVarToken), plusToken, new Literal(10))
         * ));
         * </code></pre>
         */
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
