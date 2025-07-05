package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.AutoGenUtils;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public final class SimpleMapTemplate implements AutoGenTemplate {
    private final String basePath;
    private final Map<String, Object> values;
    private final Gson gson;

    public SimpleMapTemplate(String basePath, Map<String, Object> values) {
        this.basePath = basePath != null ? basePath : "";
        this.values = new LinkedHashMap<>(values);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
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

    @Override
    public void writeToFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IOException("Failed to create directories for file: " + file.getAbsolutePath());
        }

        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

            writer.write(gson.toJson(values));
        }
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
