package de.happybavarian07.coolstufflib.service.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <p>Represents an external API provider for the TranslationService.</p>
 */
public interface TranslationAPI {

    /**
     * <p>Gets the unique identifier for this API (e.g., "google", "deepl").</p>
     *
     * @return the API identifier
     */
    String getApiId();

    /**
     * <p>Translates text to the target language.</p>
     *
     * @param sourceText the text to translate
     * @param targetLang the target language code
     * @return a future resolving to the translated string
     */
    CompletableFuture<String> translate(String sourceText, String targetLang);

    /**
     * <p>Translates text from a specific language to a target language.</p>
     *
     * @param sourceText the text to translate
     * @param sourceLang the source language code
     * @param targetLang the target language code
     * @return a future resolving to the translated string
     */
    CompletableFuture<String> translate(String sourceText, String sourceLang, String targetLang);

    /**
     * <p>Retrieves a list of supported language codes.</p>
     *
     * @return a future resolving to the list of supported languages
     */
    CompletableFuture<List<String>> getSupportedLanguages();
}
