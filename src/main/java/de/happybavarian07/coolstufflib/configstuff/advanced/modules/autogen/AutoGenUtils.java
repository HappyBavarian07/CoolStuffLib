package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen;

import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.DefaultGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.DefaultKey;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Key;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates.AutoGenTemplate;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates.SimpleObjectTemplate;

public class AutoGenUtils {
    public static AutoGenModule createDefaultAutoGenModule() {
        // Add default template(s) if needed
        return new AutoGenModule();
    }
    public static Group createGroup(String name, Group parent) {
        return new DefaultGroup(name, parent);
    }
    public static Key createKey(String name, Class<?> type, Object value) {
        return new DefaultKey(name, type, value);
    }

    public static Group getOrCreateGroupByPath(Group root, String path) {
        String[] parts = path.split("\\.");
        Group current = root;
        for (String part : parts) {
            Group next = current.getGroup(part);
            if (next == null) {
                next = createGroup(part, current);
                current.addGroup(next);
            }
            current = next;
        }
        return current;
    }

    public static AutoGenTemplate createTemplateFromObject(String basePath, Object obj) {
        return new SimpleObjectTemplate(basePath, obj);
    }

    public static AutoGenTemplate createTemplateFromMap(String basePath, java.util.Map<String, Object> map) {
        return new SimpleObjectTemplate(basePath, map);
    }
}
