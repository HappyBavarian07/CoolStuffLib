package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Line-based view of a block-style YAML file. It finds keys, replaces values and inserts keys without touching
 * any other line, so comments, quoting and line endings of hand-edited files survive.</p>
 */
public final class YamlDocument {
    private static final Pattern KEY_LINE = Pattern.compile(
            "^( *)('(?:[^']|'')*'|\"(?:[^\"\\\\]|\\\\.)*\"|[^\\s#'\"\\-][^:#]*?|-[^\\s:#][^:#]*?)\\s*:(?:[ \\t]+(.*?))?[ \\t]*$");
    private static final Pattern PLAIN_KEY = Pattern.compile("[A-Za-z0-9_\\-]+");
    private static final Set<String> RESERVED = Set.of("true", "false", "yes", "no", "on", "off", "null", "~");

    public record Node(String key, String name, int line, int end, int indent, boolean inlineValue) {
    }

    private record KeyLine(int line, int indent, String name, boolean inlineValue) {
    }

    private final List<String> lines;
    private final List<String> endings;
    private Map<String, Node> index = Map.of();

    private YamlDocument(List<String> lines, List<String> endings) {
        this.lines = lines;
        this.endings = endings;
        reindex();
    }

    public static YamlDocument parse(String text) {
        List<String> lines = new ArrayList<>();
        List<String> endings = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int newline = text.indexOf('\n', start);
            if (newline < 0) {
                lines.add(text.substring(start));
                endings.add("");
                break;
            }
            boolean crlf = newline > start && text.charAt(newline - 1) == '\r';
            lines.add(text.substring(start, crlf ? newline - 1 : newline));
            endings.add(crlf ? "\r\n" : "\n");
            start = newline + 1;
        }
        return new YamlDocument(lines, endings);
    }

    public String text() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) out.append(lines.get(i)).append(endings.get(i));
        return out.toString();
    }

    public Optional<Node> node(String key) {
        return Optional.ofNullable(index.get(key));
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(index.keySet());
    }

    public boolean isSection(String key) {
        Node node = index.get(key);
        if (node == null || node.inlineValue()) return false;
        String prefix = key + ".";
        for (String other : index.keySet()) {
            if (other.startsWith(prefix)) return true;
        }
        return false;
    }

    /** Comment lines directly above the key, without '#' and one following space; null when there are none. */
    public @Nullable String commentOf(String key) {
        Node node = index.get(key);
        if (node == null) return null;
        Deque<String> comment = new ArrayDeque<>();
        for (int i = node.line() - 1; i >= 0; i--) {
            String trimmed = lines.get(i).trim();
            if (!trimmed.startsWith("#")) break;
            String text = trimmed.substring(1);
            comment.addFirst(text.startsWith(" ") ? text.substring(1) : text);
        }
        return comment.isEmpty() ? null : String.join("\n", comment);
    }

    /**
     * <p>Replaces the value of {@code key} or inserts it after the last child of its nearest existing section.
     * {@code sectionComment} gives the comment for sections that have to be created (full key → comment).</p>
     */
    public void set(String key, Object value, @Nullable String comment, Function<String, String> sectionComment) {
        Node existing = index.get(key);
        if (existing != null) {
            if (isSection(key)) throw new IllegalStateException(key + " is a section, not a value");
            replace(existing.line(), existing.end(), render(existing.name(), value, existing.indent()));
            reindex();
            return;
        }
        String[] parts = key.split("\\.");
        int known = parts.length - 1;
        Node parent = null;
        while (known > 0) {
            parent = index.get(String.join(".", Arrays.copyOf(parts, known)));
            if (parent != null) break;
            known--;
        }
        if (parent != null && parent.inlineValue()) {
            throw new IllegalStateException(parent.key() + " is a value, not a section");
        }
        int indent = parent == null ? 0 : childIndent(parent);
        List<String> block = new ArrayList<>();
        for (int i = known; i < parts.length - 1; i++) {
            addComment(block, sectionComment.apply(String.join(".", Arrays.copyOf(parts, i + 1))), indent);
            block.add(" ".repeat(indent) + quoteKey(parts[i]) + ":");
            indent += 2;
        }
        addComment(block, comment, indent);
        block.addAll(render(parts[parts.length - 1], value, indent));
        insert(parent == null ? lines.size() : parent.end(), block);
        reindex();
    }

    public static String scalar(Object value) {
        if (value == null) return "''";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        String text = value.toString();
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0 || text.indexOf('\t') >= 0) {
            return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
        }
        return "'" + text.replace("'", "''") + "'";
    }

    static String quoteKey(String name) {
        boolean plain = PLAIN_KEY.matcher(name).matches() && !RESERVED.contains(name.toLowerCase(Locale.ROOT));
        return plain ? name : "'" + name.replace("'", "''") + "'";
    }

    private static List<String> render(String name, Object value, int indent) {
        String pad = " ".repeat(indent);
        if (value instanceof List<?> list) {
            if (list.isEmpty()) return List.of(pad + quoteKey(name) + ": []");
            List<String> out = new ArrayList<>();
            out.add(pad + quoteKey(name) + ":");
            for (Object item : list) out.add(pad + "  - " + scalar(item));
            return out;
        }
        if (value instanceof Map<?, ?> map) {
            List<String> out = new ArrayList<>();
            out.add(pad + quoteKey(name) + ":");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                out.addAll(render(String.valueOf(entry.getKey()), entry.getValue(), indent + 2));
            }
            return out;
        }
        return List.of(pad + quoteKey(name) + ": " + scalar(value));
    }

    private static void addComment(List<String> block, @Nullable String comment, int indent) {
        if (comment == null || comment.isBlank()) return;
        for (String line : comment.split("\n", -1)) {
            block.add(" ".repeat(indent) + "#" + (line.isEmpty() ? "" : " " + line));
        }
    }

    private int childIndent(Node parent) {
        String prefix = parent.key() + ".";
        for (Node node : index.values()) {
            if (node.key().startsWith(prefix) && node.key().indexOf('.', prefix.length()) < 0) return node.indent();
        }
        return parent.indent() + 2;
    }

    private void replace(int from, int to, List<String> block) {
        String lastEnding = endings.get(to - 1);
        for (int i = to - 1; i >= from; i--) {
            lines.remove(i);
            endings.remove(i);
        }
        insertLines(from, block, lastEnding.isEmpty() && from < lines.size() ? newline() : lastEnding);
    }

    private void insert(int at, List<String> block) {
        if (at > 0 && endings.get(at - 1).isEmpty()) endings.set(at - 1, newline());
        insertLines(at, block, newline());
    }

    private void insertLines(int at, List<String> block, String lastEnding) {
        String newline = newline();
        for (int i = 0; i < block.size(); i++) {
            lines.add(at + i, block.get(i));
            endings.add(at + i, i == block.size() - 1 ? lastEnding : newline);
        }
    }

    private String newline() {
        int crlf = 0;
        int lf = 0;
        for (String ending : endings) {
            if (ending.equals("\r\n")) crlf++;
            else if (ending.equals("\n")) lf++;
        }
        return crlf > lf ? "\r\n" : "\n";
    }

    private void reindex() {
        List<KeyLine> keyLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("- ") || trimmed.equals("-")) continue;
            Matcher matcher = KEY_LINE.matcher(lines.get(i));
            if (!matcher.matches()) continue;
            String value = matcher.group(3);
            boolean inline = value != null && !value.isEmpty() && !value.startsWith("#");
            keyLines.add(new KeyLine(i, matcher.group(1).length(), unquote(matcher.group(2).trim()), inline));
        }
        Map<String, Node> result = new LinkedHashMap<>();
        Deque<KeyLine> openLines = new ArrayDeque<>();
        Deque<String> openKeys = new ArrayDeque<>();
        for (int k = 0; k < keyLines.size(); k++) {
            KeyLine key = keyLines.get(k);
            while (!openLines.isEmpty() && openLines.peek().indent() >= key.indent()) {
                openLines.pop();
                openKeys.pop();
            }
            String full = openKeys.isEmpty() ? key.name() : openKeys.peek() + "." + key.name();
            int end = lines.size();
            for (int n = k + 1; n < keyLines.size(); n++) {
                if (keyLines.get(n).indent() <= key.indent()) {
                    end = keyLines.get(n).line();
                    break;
                }
            }
            while (end - 1 > key.line() && isBlankOrComment(lines.get(end - 1))) end--;
            result.put(full, new Node(full, key.name(), key.line(), end, key.indent(), key.inlineValue()));
            openLines.push(key);
            openKeys.push(full);
        }
        index = result;
    }

    private static boolean isBlankOrComment(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }

    private static String unquote(String name) {
        if (name.length() >= 2 && name.startsWith("'") && name.endsWith("'")) {
            return name.substring(1, name.length() - 1).replace("''", "'");
        }
        if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
            return name.substring(1, name.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return name;
    }
}
