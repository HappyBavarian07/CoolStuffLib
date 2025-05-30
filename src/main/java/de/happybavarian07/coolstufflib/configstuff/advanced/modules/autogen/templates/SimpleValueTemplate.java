package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates;

import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.AutoGenUtils;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;

import java.util.Collections;
import java.util.Map;

public class SimpleValueTemplate implements AutoGenTemplate {
    private final String basePath;
    private final Object value;

    public SimpleValueTemplate(String basePath, Object value) {
        this.basePath = basePath != null ? basePath : "";
        this.value = value;
    }

    @Override
    public String getBasePath() {
        return basePath;
    }

    @Override
    public Map<String, Object> toMap() {
        return Collections.singletonMap(basePath, value);
    }

    @Override
    public void applyTo(Group root) {
        Group group = AutoGenUtils.getOrCreateGroupByPath(root, basePath);
        String key = basePath.contains(".") ? basePath.substring(basePath.lastIndexOf('.') + 1) : basePath;
        if (!key.isEmpty()) {
            group.addKey(AutoGenUtils.createKey(key, value != null ? value.getClass() : Object.class, value));
        }
    }
}
