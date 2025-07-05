package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.AutoGenUtils;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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
        Map<String, Object> result = new HashMap<>();
        if (basePath.contains(".")) {
            // Handle nested path
            String[] parts = basePath.split("\\.");
            Map<String, Object> current = result;

            for (int i = 0; i < parts.length - 1; i++) {
                Map<String, Object> next = new HashMap<>();
                current.put(parts[i], next);
                current = next;
            }

            current.put(parts[parts.length - 1], value);
        } else {
            // Simple key-value
            result.put(basePath, value);
        }

        return result;
    }

    @Override
    public void applyTo(Group root) {
        Group group = AutoGenUtils.getOrCreateGroupByPath(root, basePath);
        String key = basePath.contains(".") ? basePath.substring(basePath.lastIndexOf('.') + 1) : basePath;
        if (!key.isEmpty()) {
            group.addKey(AutoGenUtils.createKey(key, value != null ? value.getClass() : Object.class, value));
        }
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

            writer.write(gson.toJson(toMap()));
        }
    }
}
