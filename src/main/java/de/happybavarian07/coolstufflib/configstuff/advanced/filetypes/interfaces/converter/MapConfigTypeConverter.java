package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.converter;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.BaseConfigTypeConverter;

import java.util.Map;

/*
 * @Author HappyBavarian07
 * @Date Mai 29, 2025 | 21:07
 */
public abstract class MapConfigTypeConverter<T> implements BaseConfigTypeConverter<T, Map<String, Object>> {
    private final Class<T> inputType;

    public MapConfigTypeConverter(Class<T> inputType) {
        this.inputType = inputType;
    }

    @Override
    public Class<?> getInputType() {
        return inputType;
    }

    @Override
    public Class<?> getOutputType() {
        return Map.class;
    }
}
