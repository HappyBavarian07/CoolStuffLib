package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates;

import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.AutoGenUtils;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;

import java.util.*;

public final class SimpleMapTemplate implements AutoGenTemplate {
    private final String basePath;
    private final Map<String, Object> values;

    public SimpleMapTemplate(String basePath, Map<String, Object> values) {
        this.basePath = basePath != null ? basePath : "";
        this.values = new LinkedHashMap<>(values);
    }

    @Override
    public String getBasePath() {
        return basePath;
    }

    @Override
    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(values);
    }

    @Override
    public void applyTo(Group root) {
        applyToGroup(root, values, basePath);
    }

    private void applyToGroup(Group group, Map<String, Object> map, String currentPath) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fullPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                Group subGroup = AutoGenUtils.getOrCreateGroupByPath(group, fullPath);
                applyToGroup(subGroup, (Map<String, Object>) entry.getValue(), fullPath);
            } else {
                group.addKey(AutoGenUtils.createKey(
                        entry.getKey(),
                        entry.getValue() != null ? entry.getValue().getClass() : Object.class,
                        entry.getValue()
                ));
            }
        }
    }
}
