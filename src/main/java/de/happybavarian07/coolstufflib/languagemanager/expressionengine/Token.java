package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

import org.jetbrains.annotations.NotNull;

public record Token(TokenType type, String lexeme, Object literal, int position) {
    @Override
    public @NotNull String toString() {
        return type + " " + lexeme + (literal != null ? " " + literal : "") + " at " + position;
    }
}
