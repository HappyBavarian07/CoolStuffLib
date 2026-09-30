package de.happybavarian07.coolstufflib.service.scan;

import de.happybavarian07.coolstufflib.service.api.Config;

public class ScanConfig implements Config {
    public String getString(String key) { return "value-" + key; }
    public int getInt(String key) { return 42; }
    public boolean getBoolean(String key) { return true; }
    public Object get(String key) { return getString(key); }
}
