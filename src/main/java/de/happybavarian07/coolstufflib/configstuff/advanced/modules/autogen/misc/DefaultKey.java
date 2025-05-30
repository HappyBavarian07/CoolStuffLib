package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc;

public class DefaultKey implements Key {
    private final String name;
    private final Class<?> type;
    private Object value;

    public DefaultKey(String name, Class<?> type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    public String getName() { return name; }

    @Override
    public Object getValue() { return value; }

    @Override
    public void setValue(Object value) { this.value = value; }

    @Override
    public Class<?> getType() { return type; }
}
