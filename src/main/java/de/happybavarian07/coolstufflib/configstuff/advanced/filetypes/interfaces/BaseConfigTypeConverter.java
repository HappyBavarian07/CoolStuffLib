package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces;

/**
 * <p>Interface for converting between runtime object types and their serialized representations
 * for configuration file storage and retrieval operations.</p>
 *
 * <p>Type converters enable:</p>
 * <ul>
 *   <li>Custom object serialization for complex types</li>
 *   <li>Type-safe deserialization with validation</li>
 *   <li>Bidirectional conversion between formats</li>
 *   <li>Error handling for invalid data</li>
 * </ul>
 *
 * <pre><code>
 * BaseConfigTypeConverter<UUID, String> uuidConverter =
 *     new BaseConfigTypeConverter<UUID, String>() {
 *         public String toSerialized(UUID uuid) {
 *             return uuid.toString();
 *         }
 *
 *         public UUID fromSerialized(String str) {
 *             return UUID.fromString(str);
 *         }
 *     };
 * </code></pre>
 *
 * @param <T> the runtime type to convert from/to
 * @param <K> the serialized type for storage
 */
public interface BaseConfigTypeConverter<T, K> {

    /**
     * <p>Converts a runtime object to its serialized representation for storage.</p>
     *
     * <pre><code>
     * UUID uuid = UUID.randomUUID();
     * String serialized = converter.toSerialized(uuid);
     * </code></pre>
     *
     * @param value the runtime object to serialize
     * @return the serialized representation
     */
    K toSerialized(T value);

    /**
     * <p>Converts a serialized value back to the runtime object type.</p>
     *
     * <pre><code>
     * String serialized = "550e8400-e29b-41d4-a716-446655440000";
     * UUID uuid = converter.fromSerialized(serialized);
     * </code></pre>
     *
     * @param serializedValue the serialized value to convert
     * @return the runtime object
     */
    T fromSerialized(K serializedValue);

    /**
     * <p>Checks if this converter can handle the given value type.</p>
     *
     * <pre><code>
     * if (converter.canConvertFrom(someValue)) {
     *     UUID result = converter.fromSerialized((String) someValue);
     * }
     * </code></pre>
     *
     * @param value the value to check for conversion compatibility
     * @return true if the value can be converted, false otherwise
     */
    boolean canConvertFrom(Object value);

    /**
     * <p>Safely attempts to convert a serialized value, returning null on failure.</p>
     *
     * <pre><code>
     * UUID uuid = converter.tryFromSerialized("invalid-uuid");
     * if (uuid == null) {
     *     // Handle conversion failure
     * }
     * </code></pre>
     *
     * @param serializedValue the value to attempt conversion on
     * @return the converted object, or null if conversion fails
     */
    default T tryFromSerialized(K serializedValue) {
        if (!canConvertFrom(serializedValue)) return null;
        try {
            return fromSerialized(serializedValue);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>Gets the runtime type class this converter handles.</p>
     *
     * <pre><code>
     * Class<?> inputType = converter.getInputType();
     * // Returns UUID.class for UUID converter
     * </code></pre>
     *
     * @return the input type class
     */
    Class<?> getInputType();

    /**
     * <p>Gets the serialized type class this converter produces.</p>
     *
     * <pre><code>
     * Class<?> outputType = converter.getOutputType();
     * // Returns String.class for UUID converter
     * </code></pre>
     *
     * @return the output type class
     */
    Class<?> getOutputType();
}