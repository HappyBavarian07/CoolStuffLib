package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class DefaultGroup implements Group {
    private final String name;
    private final Group parent;
    private final List<Group> subGroups = new ArrayList<>();
    private final List<Key> keys = new ArrayList<>();

    public DefaultGroup(String name, Group parent) {
        this.name = name;
        this.parent = parent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Group getParent() {
        return parent;
    }

    @Override
    public String getFullPath() {
        StringBuilder path = new StringBuilder(name);
        Group current = parent;
        while (current != null) {
            if (current.getName().isEmpty()) {
                if (current.getParent() == null) {
                    break;
                }
                continue;
            }
            path.insert(0, current.getName() + ".");
            current = current.getParent();
        }
        return path.toString();
    }

    @Override
    public List<Group> getSubGroups() {
        return Collections.unmodifiableList(subGroups);
    }

    @Override
    public List<Key> getKeys() {
        return Collections.unmodifiableList(keys);
    }

    @Override
    public Group getGroup(String name) {
        return subGroups.stream().filter(g -> g.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public Key getKey(String name) {
        return keys.stream().filter(k -> k.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public List<Group> findGroups(String filter) {
        List<Group> result = new ArrayList<>();
        for (Group g : subGroups) {

            if (g.getName().contains(filter)) result.add(g);
            result.addAll(g.findGroups(filter));
        }
        return result;
    }

    @Override
    public List<Key> findKeys(String filter) {
        List<Key> result = new ArrayList<>();
        for (Key k : keys) {
            if (k.getName().contains(filter)) result.add(k);
        }
        for (Group g : subGroups) {
            result.addAll(g.findKeys(filter));
        }
        return result;
    }

    @Override
    public List<Key> findKeys(Predicate<Key> predicate) {
        List<Key> result = new ArrayList<>();
        for (Key k : keys) {
            if (predicate.test(k)) result.add(k);
        }
        for (Group g : subGroups) {
            result.addAll(g.findKeys(predicate));
        }
        return result;
    }

    @Override
    public List<Group> findGroups(Predicate<Group> predicate) {
        List<Group> result = new ArrayList<>();
        for (Group g : subGroups) {
            if (predicate.test(g)) result.add(g);
            result.addAll(g.findGroups(predicate));
        }
        return result;
    }

    @Override
    public void addGroup(Group group) {
        subGroups.add(group);
    }

    @Override
    public void addKey(Key key) {
        keys.add(key);
    }
}
