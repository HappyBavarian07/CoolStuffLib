package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.BaseConfigTypeConverter;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.converter.MapConfigTypeConverter;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.converter.StringConfigTypeConverter;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

public class ConfigTypeConverterRegistry {
    private final Map<Class<?>, BaseConfigTypeConverter<?, ?>> otherConverters = new HashMap<>();
    private final List<MapConfigTypeConverter<?>> mapConverters = new ArrayList<>();
    private final List<StringConfigTypeConverter<?>> stringConverters = new ArrayList<>();

    public <T, K> void register(Class<T> type, BaseConfigTypeConverter<T, K> converter) {
        if (converter instanceof MapConfigTypeConverter<?>) {
            mapConverters.add((MapConfigTypeConverter<?>) converter);
        } else if (converter instanceof StringConfigTypeConverter<?>) {
            stringConverters.add((StringConfigTypeConverter<?>) converter);
        } else {
            otherConverters.put(type, converter);
        }
    }

    public <T, K> void unregister(Class<T> type, BaseConfigTypeConverter<T, K> converter) {
        if (converter instanceof MapConfigTypeConverter<?>) {
            mapConverters.remove((MapConfigTypeConverter<?>) converter);
        } else if (converter instanceof StringConfigTypeConverter<?>) {
            stringConverters.remove((StringConfigTypeConverter<?>) converter);
        } else {
            otherConverters.remove(type);
        }
    }

    public Collection<BaseConfigTypeConverter<?, ?>> getAll() {
        List<BaseConfigTypeConverter<?, ?>> allConverters = new ArrayList<>(otherConverters.values());
        allConverters.addAll(mapConverters);
        allConverters.addAll(stringConverters);
        return allConverters;
    }

    @SuppressWarnings("unchecked")
    public <T, K> BaseConfigTypeConverter<T, K> get(Class<T> type) {
        return (BaseConfigTypeConverter<T, K>) otherConverters.get(type);
    }

    public Object tryFromSerialized(Object value) {
        if (value == null) return null;
        for (StringConfigTypeConverter<?> c : stringConverters) {
            if (c.canConvertFrom(value.toString()) && value instanceof String) {
                @SuppressWarnings("unchecked")
                StringConfigTypeConverter<Object> objConverter = (StringConfigTypeConverter<Object>) c;
                return objConverter.tryFromSerialized(value.toString());
            }
        }
        for (MapConfigTypeConverter<?> c : mapConverters) {
            if (c.canConvertFrom(value)) {
                @SuppressWarnings("unchecked")
                MapConfigTypeConverter<Object> objConverter = (MapConfigTypeConverter<Object>) c;
                return objConverter.tryFromSerialized((Map<String, Object>) value);
            }
        }
        for (BaseConfigTypeConverter<?, ?> converter : otherConverters.values()) {
            if (converter.canConvertFrom(value)) {
                @SuppressWarnings("unchecked")
                BaseConfigTypeConverter<Object, Object> objConverter = (BaseConfigTypeConverter<Object, Object>) converter;
                return objConverter.tryFromSerialized(value);
            }
        }
        return value;
    }

    public Object tryToSerialized(Object value) {
        if (value == null) return null;
        for (StringConfigTypeConverter<?> c : stringConverters) {
            if (c.getInputType().isInstance(value)) {
                @SuppressWarnings("unchecked")
                StringConfigTypeConverter<Object> objConverter = (StringConfigTypeConverter<Object>) c;
                return objConverter.toSerialized(value);
            }
        }
        for (MapConfigTypeConverter<?> c : mapConverters) {
            if (c.getInputType().isInstance(value)) {
                @SuppressWarnings("unchecked")
                MapConfigTypeConverter<Object> objConverter = (MapConfigTypeConverter<Object>) c;
                return objConverter.toSerialized(value);
            }
        }
        for (BaseConfigTypeConverter<?, ?> converter : otherConverters.values()) {
            if (converter.getInputType().isInstance(value)) {
                @SuppressWarnings("unchecked")
                BaseConfigTypeConverter<Object, Object> objConverter = (BaseConfigTypeConverter<Object, Object>) converter;
                return objConverter.toSerialized(value);
            }
        }
        return value;
    }


    public static ConfigTypeConverterRegistry defaultRegistry() {
        ConfigTypeConverterRegistry registry = new ConfigTypeConverterRegistry();
        registry.register(UUID.class, new StringConfigTypeConverter<>(UUID.class) {
            @Override
            public String toSerialized(UUID value) {
                return value.toString();
            }

            @Override
            public UUID fromSerialized(String v) {
                return UUID.fromString(v);
            }

            @Override
            public boolean canConvertFrom(Object v) {
                return v instanceof String s && isUuid(s);
            }

            private boolean isUuid(String s) {
                try {
                    UUID.fromString(s);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
        registry.register(File.class, new StringConfigTypeConverter<>(File.class) {
            private static final String FILE_PREFIX = "__file__:";

            @Override
            public String toSerialized(File value) {
                return FILE_PREFIX + value.getAbsolutePath();
            }

            @Override
            public File fromSerialized(String v) {
                return v.startsWith(FILE_PREFIX) ? new File(v.substring(FILE_PREFIX.length())) : null;
            }

            @Override
            public boolean canConvertFrom(Object v) {
                return v instanceof String s && s.startsWith(FILE_PREFIX);
            }
        });
        registry.register(LocalDate.class, new StringConfigTypeConverter<>(LocalDate.class) {
            @Override
            public String toSerialized(LocalDate value) {
                return value.toString();
            }

            @Override
            public LocalDate fromSerialized(String v) {
                return LocalDate.parse(v);
            }

            @Override
            public boolean canConvertFrom(Object v) {
                return v instanceof String s && isIsoDate(s);
            }

            private boolean isIsoDate(String s) {
                try {
                    LocalDate.parse(s);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
        return registry;
    }
}
