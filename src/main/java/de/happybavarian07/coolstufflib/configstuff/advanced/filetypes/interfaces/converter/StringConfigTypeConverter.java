package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.converter;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.BaseConfigTypeConverter;

import java.lang.reflect.ParameterizedType;

/*
 * @Author HappyBavarian07
 * @Date Mai 29, 2025 | 21:08
 */
public abstract class StringConfigTypeConverter<T> implements BaseConfigTypeConverter<T, String> {
    private final Class<T> inputType;

    public StringConfigTypeConverter(Class<T> inputType) {
        this.inputType = inputType;
    }

    @Override
    public Class<T> getInputType() {
        return inputType;
    }

    @Override
    public Class<?> getOutputType() {
        return String.class;
    }
}
