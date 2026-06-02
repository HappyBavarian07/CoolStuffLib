package de.happybavarian07.coolstufflib.service.impl;

import de.happybavarian07.coolstufflib.service.api.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * <p>Next-Gen feature: Handles automatic generation and translation of language files via external APIs.</p>
 */
public class TranslationService implements Service {
    private final UUID serviceId = UUID.randomUUID();
    private final Logger logger = Logger.getLogger("TranslationService");
    
    private final Map<String, TranslationAPI> registeredApis = new ConcurrentHashMap<>();
    private TranslationAPI activeApi = null;

    @Override
    public UUID id() {
        return serviceId;
    }

    @Override
    public String serviceName() {
        return "translation-service";
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            logger.info("TranslationService initialized. Automatic language generation ready.");
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            registeredApis.clear();
            activeApi = null;
            logger.info("TranslationService shutting down.");
        });
    }
    
    /**
     * <p>Registers a new translation API provider.</p>
     *
     * @param api the TranslationAPI implementation
     */
    public void registerApi(TranslationAPI api) {
        if (api == null || api.getApiId() == null) return;
        registeredApis.put(api.getApiId().toLowerCase(), api);
        if (activeApi == null) {
            activeApi = api;
        }
    }

    /**
     * <p>Sets the currently active translation API by ID.</p>
     *
     * @param apiId the ID of the registered API
     * @throws IllegalArgumentException if the API is not registered
     */
    public void setActiveApi(String apiId) {
        if (apiId == null) return;
        TranslationAPI api = registeredApis.get(apiId.toLowerCase());
        if (api == null) {
            throw new IllegalArgumentException("Translation API '" + apiId + "' is not registered.");
        }
        this.activeApi = api;
    }

    /**
     * <p>Translates a source string into the target language using the active API.</p>
     *
     * @param sourceText the text to translate
     * @param targetLang the target language code (e.g., 'es', 'de')
     * @return a future resolving to the translated string, or the original if no API is active
     */
    public CompletableFuture<String> translateAsync(String sourceText, String targetLang) {
        if (activeApi == null) return CompletableFuture.completedFuture(sourceText);
        return activeApi.translate(sourceText, targetLang);
    }
    
    /**
     * <p>Translates text from a specific language to a target language.</p>
     *
     * @param sourceText the text to translate
     * @param sourceLang the source language code
     * @param targetLang the target language code
     * @return a future resolving to the translated string
     */
    public CompletableFuture<String> translateAsync(String sourceText, String sourceLang, String targetLang) {
        if (activeApi == null) return CompletableFuture.completedFuture(sourceText);
        return activeApi.translate(sourceText, sourceLang, targetLang);
    }

    /**
     * <p>Retrieves a list of supported language codes from the active API.</p>
     *
     * @return a future resolving to the list of supported languages
     */
    public CompletableFuture<List<String>> getSupportedLanguages() {
        if (activeApi == null) return CompletableFuture.completedFuture(Collections.emptyList());
        return activeApi.getSupportedLanguages();
    }
    
    /**
     * <p>Gets all registered translation APIs.</p>
     *
     * @return a set of registered API IDs
     */
    public Set<String> getRegisteredApiIds() {
        return Collections.unmodifiableSet(registeredApis.keySet());
    }
}
