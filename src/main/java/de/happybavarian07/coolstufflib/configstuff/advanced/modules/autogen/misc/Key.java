package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc;

public interface Key {
    /**
     * Returns the name of the key.
     * This is used to identify the key in the configuration.
     * @return The name of the key.
     */
    String getName();
    /**
     * Returns the value of the key.
     * This method should be used to retrieve the value from the configuration.
     * @return The current value of the key.
     */
    Object getValue();
    /**
     * Sets the value of the key.
     * This method should be used to update the value in the configuration.
     * @param value The new value to set.
     */
    void setValue(Object value);
    /**
     * Returns the type of the value.
     * This is used to determine how to handle the value in serialization and deserialization.
     * @return The class type of the value.
     */
    Class<?> getType();
}
