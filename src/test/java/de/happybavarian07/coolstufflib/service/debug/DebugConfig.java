package de.happybavarian07.coolstufflib.service.debug;
import de.happybavarian07.coolstufflib.service.api.Config;

public class DebugConfig implements Config {
    public String getString(String key) { return "value-" + key; }
    public int getInt(String key) { return 42; }
    public boolean getBoolean(String key) { return true; }
    public Object get(String key) { return getString(key); }
}