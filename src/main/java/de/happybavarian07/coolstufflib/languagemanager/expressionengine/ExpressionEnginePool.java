package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.FunctionCall;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExpressionEnginePool manages the lifecycle and retrieval of ExpressionEngine instances for
 * different language contexts and player-language associations. It avoids per-player engine
 * bloat by using UUID-based mapping and provides efficient retrieval for both default and
 * per-language engines. Player-to-engine mapping is used only as a cache for fast lookups.
 * <p>
 * Key design points:
 * <ul>
 *   <li>Each language is mapped to a unique engine UUID and ExpressionEngine instance.</li>
 *   <li>The default language engine is non-replaceable and always available.</li>
 *   <li>Player-to-engine mapping is a cache, not a permanent per-player storage, to keep memory usage low.</li>
 *   <li>Thread-safe via ConcurrentHashMap for all mappings.</li>
 *   <li>Provides methods to add, retrieve, update, and clear engines by language or player context.</li>
 * </ul>
 * <p>
 * Usage:
 * <ul>
 *   <li>Use {@link #getEngineForLanguage(String)} for per-language engine retrieval.</li>
 *   <li>Use {@link #getEngineForPlayer(Player, String)} for player-language context retrieval, with fallback to default.</li>
 *   <li>Use {@link #addEngineForLanguage(String, ExpressionEngine)} to register a new engine for a language.</li>
 *   <li>Use {@link #updatePlayerEngine(Player, String)} to update a player's engine mapping when their language changes.</li>
 *   <li>Use {@link #clearPlayerEngine(Player)} and {@link #clearLanguageEngine(String)} for cleanup.</li>
 *   <li>Use {@link #registerGlobalFunction(String, FunctionCall, String)} to register a function globally across all engines.</li>
 *   <li>Use {@link #registerFunctionForLanguage(String, String, FunctionCall, String)} to register a function for a specific language engine.</li>
 *   <li>Use {@link #unregisterGlobalFunction(String)} and {@link #unregisterFunctionForLanguage(String, String)} to remove functions.</li>
 *   <li>Use {@link #getEngineIterator()} to iterate over all registered ExpressionEngines.</li>
 *   <li>Use {@link #getDefaultLanguageEngine()} to retrieve the default ExpressionEngine instance.</li>
 * </ul>
 *
 * @author HappyBavarian07
 * @since 2025-05-25
 */
public class ExpressionEnginePool {
    private final Map<UUID, ExpressionEngine> engines = new ConcurrentHashMap<>(); // engineUUID -> engine
    private final Map<String, UUID> languageToEngineUUID = new ConcurrentHashMap<>(); // languageName -> engineUUID
    private final Map<UUID, UUID> playerToEngineUUID = new ConcurrentHashMap<>(); // playerUUID -> engineUUID (cache)
    private final Map<String, Map.Entry<FunctionCall, String>> globalFunctions = new ConcurrentHashMap<>(); // functionName -> FunctionCall
    private final UUID defaultEngineUUID;
    private final String defaultLanguageName;

    /**
     * Constructs a new ExpressionEnginePool with a default language and engine.
     *
     * @param defaultLanguageName The name of the default language.
     * @param defaultEngine The ExpressionEngine instance for the default language.
     */
    /**
     * <p>Creates a new pool for managing language-specific expression engines, setting up the default language and engine.</p>
     * <pre><code>
     * ExpressionEnginePool pool = new ExpressionEnginePool("en", new ExpressionEngine());
     * </code></pre>
     *
     * @param defaultLanguageName the name of the default language
     * @param defaultEngine       the engine instance for the default language
     */
    public ExpressionEnginePool(String defaultLanguageName, ExpressionEngine defaultEngine) {
        this.defaultLanguageName = defaultLanguageName;
        this.defaultEngineUUID = UUID.randomUUID();
        engines.put(defaultEngineUUID, defaultEngine);
        languageToEngineUUID.put(defaultLanguageName, defaultEngineUUID);
    }

    /**
     * Gets the ExpressionEngine for the default language.
     *
     * @return The default ExpressionEngine instance.
     */
    public ExpressionEngine getDefaultLanguageEngine() {
        return engines.get(defaultEngineUUID);
    }

    /**
     * Gets the ExpressionEngine for a specific language.
     *
     * @param languageName The language name.
     * @return The ExpressionEngine for the language, or the default engine if not found.
     */
    public ExpressionEngine getEngineForLanguage(String languageName) {
        UUID engineUUID = languageToEngineUUID.get(languageName);
        if (engineUUID == null) return getDefaultLanguageEngine();
        return engines.getOrDefault(engineUUID, getDefaultLanguageEngine());
    }

    /**
     * Gets the ExpressionEngine for a player and language context.
     * Uses player-to-engine mapping as a cache for efficient lookups.
     *
     * @param player       The player instance.
     * @param languageName The language name.
     * @return The ExpressionEngine for the player-language context, or the default engine if not found.
     */
    public ExpressionEngine getEngineForPlayer(Player player, String languageName) {
        if (player == null || languageName == null || languageName.equals(defaultLanguageName)) {
            return getDefaultLanguageEngine();
        }
        UUID playerUUID = player.getUniqueId();
        UUID engineUUID = playerToEngineUUID.get(playerUUID);
        if (engineUUID != null && engines.containsKey(engineUUID)) {
            return engines.get(engineUUID);
        }
        // Fallback: assign engine for language
        engineUUID = languageToEngineUUID.get(languageName);
        if (engineUUID == null) engineUUID = defaultEngineUUID;
        playerToEngineUUID.put(playerUUID, engineUUID);
        return engines.getOrDefault(engineUUID, getDefaultLanguageEngine());
    }

    /**
     * Registers a new ExpressionEngine for a language and returns its UUID.
     * If the language already has an engine, returns the existing UUID.
     *
     * @param languageName The language name.
     * @param engine       The ExpressionEngine to register.
     * @return The UUID of the registered engine.
     */
    public UUID addEngineForLanguage(String languageName, ExpressionEngine engine) {
        if (languageToEngineUUID.containsKey(languageName)) return languageToEngineUUID.get(languageName);
        UUID engineUUID = UUID.randomUUID();
        engines.put(engineUUID, engine);
        for (String functionName : globalFunctions.keySet()) {
            engine.registerFunction(functionName, globalFunctions.get(functionName).getKey(), globalFunctions.get(functionName).getValue());
        }
        languageToEngineUUID.put(languageName, engineUUID);
        return engineUUID;
    }

    /**
     * Updates a player's engine mapping when their language changes.
     *
     * @param player       The player instance.
     * @param languageName The new language name for the player.
     */
    public void updatePlayerEngine(Player player, String languageName) {
        if (player == null || languageName == null) return;
        UUID engineUUID = languageToEngineUUID.get(languageName);
        if (engineUUID != null) playerToEngineUUID.put(player.getUniqueId(), engineUUID);
    }

    /**
     * Clears the cached engine mapping for a player.
     *
     * @param player The player whose cache should be cleared.
     */
    public void clearPlayerEngine(Player player) {
        if (player != null) playerToEngineUUID.remove(player.getUniqueId());
    }

    /**
     * Removes the engine mapping for a language (except the default language).
     *
     * @param languageName The language name to remove.
     */
    public void clearLanguageEngine(String languageName) {
        UUID engineUUID = languageToEngineUUID.remove(languageName);
        if (engineUUID != null && !engineUUID.equals(defaultEngineUUID)) {
            engines.remove(engineUUID);
        }
    }

    /**
     * Registers a global function for all ExpressionEngines.
     *
     * @param functionName The name of the function.
     * @param function     The FunctionCall implementation.
     * @param defaultType  The default type for the function.
     */
    public void registerGlobalFunction(String functionName, FunctionCall function, String defaultType) {
        for (ExpressionEngine engine : engines.values()) {
            if (engine != null) {
                engine.registerFunction(functionName, function, defaultType);
            }
        }
        getDefaultLanguageEngine().registerFunction(functionName, function, defaultType);
        globalFunctions.put(functionName, Map.entry(function, defaultType));
    }

    /**
     * Registers a function for a specific language's ExpressionEngine.
     *
     * @param languageName The language name.
     * @param functionName The name of the function.
     * @param function     The FunctionCall implementation.
     * @param defaultType  The default type for the function.
     */
    public void registerFunctionForLanguage(String languageName, String functionName, FunctionCall function, String defaultType) {
        ExpressionEngine engine = getEngineForLanguage(languageName);
        if (engine != null) {
            engine.registerFunction(functionName, function, defaultType);
        }
    }

    /**
     * Unregisters a global function from all ExpressionEngines.
     *
     * @param functionName The name of the function to unregister.
     */
    public void unregisterGlobalFunction(String functionName) {
        for (ExpressionEngine engine : engines.values()) {
            if (engine != null) {
                engine.unregisterFunction(functionName);
            }
        }
        getDefaultLanguageEngine().unregisterFunction(functionName);
    }

    /**
     * Unregisters a function from a specific language's ExpressionEngine.
     *
     * @param languageName The language name.
     * @param functionName The name of the function to unregister.
     */
    public void unregisterFunctionForLanguage(String languageName, String functionName) {
        ExpressionEngine engine = getEngineForLanguage(languageName);
        if (engine != null) {
            engine.unregisterFunction(functionName);
        }
    }

    /**
     * Gets an iterator over all ExpressionEngines in the pool.
     *
     * @return An iterator for the ExpressionEngine instances.
     */
    public Iterator<ExpressionEngine> getEngineIterator() {
        return engines.values().iterator();
    }

    /**
     * Peeks a variable from the first ExpressionEngine that contains it.
     *
     * @param key The variable key to peek.
     * @return The value of the variable, or null if not found in any engine.
     */
    public Object peekVariable(String key) {
        for (ExpressionEngine engine : engines.values()) {
            if (engine != null && engine.getInterpreter().hasVariable(key)) {
                return engine.peekVariable(key);
            }
        }
        return null;
    }

    /**
     * Gets a variable from the first ExpressionEngine that contains it.
     *
     * @param key The variable key to retrieve.
     * @return The value of the variable, or null if not found in any engine.
     */
    public Object getVariable(String key) {
        for (ExpressionEngine engine : engines.values()) {
            if (engine != null && engine.getInterpreter().hasVariable(key)) {
                return engine.getVariable(key);
            }
        }
        return null;
    }
}
