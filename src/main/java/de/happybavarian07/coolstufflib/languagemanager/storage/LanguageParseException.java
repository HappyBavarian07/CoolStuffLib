package de.happybavarian07.coolstufflib.languagemanager.storage;

final class LanguageParseException extends Exception {
    private final LanguageProblem problem;

    LanguageParseException(LanguageProblem problem) {
        super(problem.toString());
        this.problem = problem;
    }

    LanguageProblem problem() {
        return problem;
    }
}
