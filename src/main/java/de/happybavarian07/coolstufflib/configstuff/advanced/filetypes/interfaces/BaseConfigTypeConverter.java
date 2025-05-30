package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces;

/*
 * @Author HappyBavarian07
 * @Date Mai 29, 2025 | 21:03
 */
public interface BaseConfigTypeConverter<T, K> {
    K toSerialized(T value);

    T fromSerialized(K serializedValue);

    boolean canConvertFrom(Object value);

    default T tryFromSerialized(K serializedValue) {
        if (!canConvertFrom(serializedValue)) return null;
        try {
            return fromSerialized(serializedValue);
        } catch (Exception e) {
            return null;
        }
    }

    Class<?> getInputType();

    Class<?> getOutputType();
}