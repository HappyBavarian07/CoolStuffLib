package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

public enum TokenType {
    IDENTIFIER, NUMBER, STRING,
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO, POWER,
    EQUAL, ASSIGN, NOT_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
    AND, OR, NOT,
    QUESTION, COLON, COMMA,
    LPAREN, RPAREN,
    IF, ELIF, ELSE,
    LET, AS,
    SEMICOLON,
    NEWLINE,
    EOF
}
