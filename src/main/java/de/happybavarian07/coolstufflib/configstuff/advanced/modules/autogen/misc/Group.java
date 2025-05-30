package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc;

import java.util.List;
import java.util.function.Predicate;

public interface Group {
    String getName();
    Group getParent();
    String getFullPath();
    List<Group> getSubGroups();
    List<Key> getKeys();
    Group getGroup(String name);
    Key getKey(String name);
    List<Group> findGroups(String filter);
    List<Key> findKeys(String filter);
    List<Group> findGroups(Predicate<Group> predicate);
    List<Key> findKeys(Predicate<Key> predicate);
    void addGroup(Group group);
    void addKey(Key key);
}
