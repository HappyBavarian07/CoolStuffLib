package de.happybavarian07.coolstufflib.configstuff.advanced.section.meta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SectionCommentManager {
    private final Map<String, String> comments;
    public SectionCommentManager() {
        this.comments = new ConcurrentHashMap<>();
    }
    public void setComment(String path, String comment) {
        if (comment == null) {
            removeComment(path);
            return;
        }
        comments.put(path, comment);
    }
    public String getComment(String path) {
        return comments.get(path);
    }
    public boolean hasComment(String path) {
        return comments.containsKey(path);
    }
    public void removeComment(String path) {
        comments.remove(path);
    }
    public Map<String, String> getComments() {
        return Map.copyOf(comments);
    }
    public void clearComments() {
        comments.clear();
    }
    public void copyFrom(SectionCommentManager other) {
        if (other == null) return;
        comments.clear();
        comments.putAll(other.comments);
    }
    public SectionCommentManager deepClone() {
        SectionCommentManager clone = new SectionCommentManager();
        clone.comments.putAll(this.comments);
        return clone;
    }
}
