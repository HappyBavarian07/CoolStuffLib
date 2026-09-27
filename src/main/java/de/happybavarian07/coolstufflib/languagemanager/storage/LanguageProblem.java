package de.happybavarian07.coolstufflib.languagemanager.storage;

public record LanguageProblem(String file, int line, String message) {
    @Override
    public String toString() {
        return (line > 0 ? file + ":" + line : file) + " - " + message;
    }
}
