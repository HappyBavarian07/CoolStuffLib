package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates;

import java.util.Map;

public final class AutoGenTemplates {
    private AutoGenTemplates() {}

    public static AutoGenTemplate fromMap(String basePath, Map<String, Object> values) {
        return new SimpleMapTemplate(basePath, values);
    }

    public static AutoGenTemplate fromObject(String basePath, Object source) {
        return new SimpleObjectTemplate(basePath, source);
    }

    public static AutoGenTemplate fromValue(String basePath, Object value) {
        return new SimpleValueTemplate(basePath, value);
    }
}
