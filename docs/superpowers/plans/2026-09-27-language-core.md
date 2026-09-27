# Language Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give CoolStuffLib's language manager a storage layer with backends (split YAML folders by default, the old single file for migration, the jar as defaults), safe text-merge updates that never rewrite owner lines, a per-call message API, a fallback chain, and owner tools (`/lang`, key debug, file watcher), without breaking the existing `LanguageManager` API.

**Architecture:** A new package `languagemanager.storage` reads and writes `LanguageEntry` records through `LanguageBackend` implementations. `LanguageConfig` keeps its public API but builds its in-memory `YamlConfiguration` from the merged layers (owner files over jar defaults) and swaps it atomically on reload, so every existing getter keeps working on top of it. New builders (`message()`, `item()`, `title()`) render with per-call placeholders through the same pipeline.

**Tech Stack:** Java 17, Spigot API 1.21.10 (SnakeYAML 2.x from the server), JUnit 5, Mockito 5.

**Spec:** `docs/superpowers/specs/2026-09-27-language-core-design.md` (read it first). Two decisions made while planning, applied in Task 12 to the spec:
- No separate template compiler. The merged layers already live in memory, and `ExpressionEngine` already caches parsed expressions (`getOrParseExpression`), so compiling templates would add code without a measurable gain. The "snapshot" is the immutable merged entry map plus the `YamlConfiguration` built from it, swapped in one assignment.
- L3 was wrong: `addPlaceholder` already overwrites (`placeholders.replace(...)`). Nothing to fix there; the placeholder map only becomes thread-safe (Task 7).

## Global Constraints

- CoolStuffLib repo: `F:\InteliJ Programs\CoolStuffLib`, branch `newfeature`, release 3.0.0 (unreleased). Nothing is pushed without the user's explicit yes.
- The public API listed under "Compatibility contract" in the spec keeps compiling and returns the same results.
- Server owners' files are never rewritten: only missing keys are inserted and changed keys replaced, line by line (`YamlDocument`). Every write is validated with SnakeYAML before it replaces the file.
- Commits: small, one logical change each, **no `Co-Authored-By` or other AI attribution**. Minimal comments. Don't reformat untouched code.
- Tests: `mvn -B test -Dgpg.skip -Dtest=<Class>` in the repo root; the full suite is `mvn -B test -Dgpg.skip`. The suite rewrites files under `TestOutputs/`; restore them with `git checkout -- TestOutputs` before committing.
- Tooling: the shell hook in this environment mangles backslashes in Bash commands. Write Java, YAML and scripts with the file tools (Write/Edit), never through heredocs.
- Docs (wiki, tutorials) are written in first person ("I"), never "we".

## File Structure

New, `src/main/java/de/happybavarian07/coolstufflib/languagemanager/storage/`:

| File | Responsibility |
|---|---|
| `YamlDocument.java` | Line-based YAML view: key index, comments, replace/insert without touching other lines |
| `YamlEntries.java` | Read a YAML text into `LanguageEntry` leaves; merge entries into a file (validate, atomic write) |
| `LanguageEntry.java`, `LanguageProblem.java`, `ReadResult.java` | Neutral data records |
| `LanguageBackend.java` | Backend interface |
| `LanguageParseException.java` | Carries a `LanguageProblem` out of `YamlEntries.read` |
| `SplitRule.java` | Key ↔ file mapping of the split layout |
| `SplitYamlBackend.java` | `languages/<lang>/…` (default) |
| `LegacyYamlBackend.java` | `languages/<lang>.yml` |
| `ClasspathLanguageSource.java` | Read-only defaults from the plugin jar (split or legacy) |
| `LanguageMigration.java`, `MigrationReport.java` | Copy between backends, one-time legacy → split move with backup |
| `LanguageStorage.java`, `PrepareResult.java`, `LoadResult.java` | Per-language prepare (migrate + add missing keys) and load (merge layers) |

New, `src/main/java/de/happybavarian07/coolstufflib/languagemanager/`: `MessageBuilder.java`, `ItemBuilder.java`, `TitleBuilder.java`, `PlayerLanguageStore.java`, `YamlPlayerLanguageStore.java`, `LanguageCommandManager.java`, `LanguageFileWatcher.java`.

Modified: `LanguageConfig`, `LanguageFile`, `LanguageManager`, `LanguageCache`, `PerPlayerLanguageHandler`, `LanguageFileMigrator` (all in `languagemanager/`), `CoolStuffLibBuilder`, `CoolStuffLib`.

---

### Task 0: Golden test of today's output

Captures what the current implementation renders for every message, title and custom value of AdminPanel's language files, so every later task proves it changes nothing.

**Files:**
- Create: `src/test/resources/golden/adminpanel/en.yml`, `de.yml` (copies)
- Create: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/GoldenLanguageOutputTest.java`
- Create (generated): `src/test/resources/golden/adminpanel/en.expected.txt`, `de.expected.txt`

- [ ] **Step 1: Copy the fixtures**

```bash
mkdir -p src/test/resources/golden/adminpanel
cp /c/Users/quiri/IdeaProjects/AdminPanel/src/main/resources/languages/en.yml src/test/resources/golden/adminpanel/en.yml
cp /c/Users/quiri/IdeaProjects/AdminPanel/src/main/resources/languages/de.yml src/test/resources/golden/adminpanel/de.yml
```

- [ ] **Step 2: Write the test**

```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/** Run with -Dgolden.record=true once to write the expected files from the current implementation. */
class GoldenLanguageOutputTest {
    private static final Path FIXTURES = Path.of("src/test/resources/golden/adminpanel");

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(strings = {"en", "de"})
    void everyMessageTitleAndValueRendersAsBefore(String language) throws IOException {
        Path source = FIXTURES.resolve(language + ".yml");
        Files.copy(source, tempDir.resolve(language + ".yml"));
        LanguageManager lgm = new LanguageManager(mock(JavaPlugin.class), tempDir.toFile(), "golden-none", "[P]");
        LanguageFile file = new LanguageFile(tempDir.toFile(), "golden-none", language);
        lgm.addLang(file, language);
        lgm.setCurrentLang(file, false);

        YamlConfiguration raw = YamlConfiguration.loadConfiguration(source.toFile());
        List<String> lines = new ArrayList<>();
        for (String key : raw.getKeys(true)) {
            if (raw.isConfigurationSection(key) || key.startsWith("Items.")) continue;
            String rendered;
            if (key.startsWith("Messages.") && raw.isString(key)) {
                rendered = lgm.getMessage(key.substring("Messages.".length()), null, false);
            } else if (key.startsWith("MenuTitles.") && raw.isString(key)) {
                rendered = lgm.getMenuTitle(key.substring("MenuTitles.".length()), null);
            } else {
                rendered = String.valueOf(lgm.getCustomObject(key, null, "<none>", false));
            }
            lines.add(key + " = " + rendered.replace("\r", "\\r").replace("\n", "\\n"));
        }

        Path expected = FIXTURES.resolve(language + ".expected.txt");
        if (Boolean.getBoolean("golden.record")) {
            Files.write(expected, lines, StandardCharsets.UTF_8);
            return;
        }
        assertEquals(Files.readAllLines(expected, StandardCharsets.UTF_8), lines);
    }
}
```

- [ ] **Step 3: Record with the current implementation**

Run: `mvn -B test -Dgpg.skip -Dtest=GoldenLanguageOutputTest -Dgolden.record=true`
Expected: PASS, and `en.expected.txt` / `de.expected.txt` exist with one line per key (check a few lines by eye: colors are `§` codes, `%prefix%` is `[P]`).

- [ ] **Step 4: Run without recording**

Run: `mvn -B test -Dgpg.skip -Dtest=GoldenLanguageOutputTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git checkout -- TestOutputs
git add src/test/resources/golden src/test/java/de/happybavarian07/coolstufflib/languagemanager/GoldenLanguageOutputTest.java
git commit -m "test(language): golden output of every AdminPanel message, title and value"
```

---

### Task 1: YamlDocument

**Files:**
- Create: `src/main/java/de/happybavarian07/coolstufflib/languagemanager/storage/YamlDocument.java`
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage/YamlDocumentTest.java`

**Interfaces:**
- Produces: `YamlDocument.parse(String)`, `text()`, `node(String) : Optional<Node>`, `keys()`, `isSection(String)`, `commentOf(String) : @Nullable String`, `set(String key, Object value, @Nullable String comment, Function<String,String> sectionComment)` (throws `IllegalStateException` on value/section conflicts), `static scalar(Object)`, `record Node(String key, String name, int line, int end, int indent, boolean inlineValue)` (`line` is 0-based, `end` exclusive).

- [ ] **Step 1: Write the failing tests**

```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlDocumentTest {
    private static final Yaml YAML = new Yaml(new SafeConstructor(new LoaderOptions()));

    @Test
    void untouchedTextStaysByteForByte() {
        String text = "# header\r\nMessages:\n  # greeting\r\n  Hello: 'Hi'\n  List:\n  - 'a'\n\n";
        assertEquals(text, YamlDocument.parse(text).text());
    }

    @Test
    void replacingAValueChangesOnlyItsLines() {
        YamlDocument doc = YamlDocument.parse("A:\n  # keep me\n  B: 'old'   \n  C: 'c'\n");
        doc.set("A.B", "new", null, k -> null);
        assertEquals("A:\n  # keep me\n  B: 'new'\n  C: 'c'\n", doc.text());
    }

    @Test
    void missingKeyGoesAfterTheLastChildOfItsSection() {
        YamlDocument doc = YamlDocument.parse("A:\n  B: 'b'\n  C:\n    D: 'd'\nE: 'e'\n");
        doc.set("A.F", "f", "new key", k -> null);
        assertEquals("A:\n  B: 'b'\n  C:\n    D: 'd'\n  # new key\n  F: 'f'\nE: 'e'\n", doc.text());
    }

    @Test
    void missingSectionsAreCreatedWithTheirComments() {
        YamlDocument doc = YamlDocument.parse("A:\n  B: 'b'\n");
        doc.set("A.X.Y.Z", 5, null, k -> k.equals("A.X") ? "section x" : null);
        assertEquals("A:\n  B: 'b'\n  # section x\n  X:\n    Y:\n      Z: 5\n", doc.text());
    }

    @Test
    void writesIntoAnEmptyDocument() {
        YamlDocument doc = YamlDocument.parse("");
        doc.set("A.B", List.of("x", "y"), null, k -> null);
        assertEquals("A:\n  B:\n    - 'x'\n    - 'y'\n", doc.text());
    }

    @Test
    void listItemsAtTheKeyIndentBelongToTheKey() {
        YamlDocument doc = YamlDocument.parse("A:\n- 'a'\n- 'b'\nB: 'b'\n");
        doc.set("A", List.of("c"), null, k -> null);
        assertEquals("A:\n  - 'c'\nB: 'b'\n", doc.text());
    }

    @Test
    void usesTheIndentOfExistingSiblings() {
        YamlDocument doc = YamlDocument.parse("A:\n    B: 'b'\n");
        doc.set("A.C", "c", null, k -> null);
        assertEquals("A:\n    B: 'b'\n    C: 'c'\n", doc.text());
    }

    @ParameterizedTest
    @ValueSource(strings = {"it's", "say \"hi\"", "C:\\path\\x", "line1\nline2", "&aÄö€ ✓", "tab\there", "#no comment", "key: value", ""})
    void scalarsSurviveARoundTrip(String value) {
        YamlDocument doc = YamlDocument.parse("");
        doc.set("K", value, null, k -> null);
        Map<String, Object> read = YAML.load(doc.text());
        assertEquals(value, read.get("K"));
    }

    @Test
    void reservedWordsAreQuotedAsKeys() {
        YamlDocument doc = YamlDocument.parse("");
        doc.set("Items.true.Name", "x", null, k -> null);
        Map<String, Object> read = YAML.load(doc.text());
        Map<?, ?> items = (Map<?, ?>) read.get("Items");
        assertEquals("x", ((Map<?, ?>) items.get("true")).get("Name"));
    }

    @Test
    void aValueCannotBecomeASection() {
        YamlDocument doc = YamlDocument.parse("A: 'a'\n");
        assertThrows(IllegalStateException.class, () -> doc.set("A.B", "b", null, k -> null));
    }

    @Test
    void aSectionCannotBecomeAValue() {
        YamlDocument doc = YamlDocument.parse("A:\n  B: 'b'\n");
        assertThrows(IllegalStateException.class, () -> doc.set("A", "a", null, k -> null));
    }

    @Test
    void readsCommentsAndLines() {
        YamlDocument doc = YamlDocument.parse("# one\n# two\nA:\n  # three\n  B: 'b'\n");
        assertEquals("one\ntwo", doc.commentOf("A"));
        assertEquals("three", doc.commentOf("A.B"));
        assertEquals(4, doc.node("A.B").orElseThrow().line());
        assertTrue(doc.isSection("A"));
        assertFalse(doc.isSection("A.B"));
    }
}
```

- [ ] **Step 2: Run the tests and see them fail**

Run: `mvn -B test -Dgpg.skip -Dtest=YamlDocumentTest`
Expected: compilation error, `YamlDocument` does not exist.

- [ ] **Step 3: Implement**

```java
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
    private static final Set<String> RESERVED = Set.of("true", "false", "yes", "no", "on", "off", "null", "y", "n", "~");

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
```

- [ ] **Step 4: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=YamlDocumentTest`
Expected: PASS (13 tests incl. the parameterized ones). If `replacingAValueChangesOnlyItsLines` fails on the trailing spaces, check that `KEY_LINE` ends with `[ \t]*$` and the value group is lazy.

- [ ] **Step 5: Commit**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager/storage/YamlDocument.java src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage/YamlDocumentTest.java
git commit -m "feat(language): YamlDocument edits YAML files line by line without touching other lines"
```

---

### Task 2: Entries, split rule and SplitYamlBackend

**Files:**
- Create in `storage/`: `LanguageEntry.java`, `LanguageProblem.java`, `ReadResult.java`, `LanguageBackend.java`, `LanguageParseException.java`, `YamlEntries.java`, `SplitRule.java`, `SplitYamlBackend.java`
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage/SplitRuleTest.java`, `SplitYamlBackendTest.java`

**Interfaces:**
- Consumes: `YamlDocument` (Task 1).
- Produces:
  - `record LanguageEntry(String key, Object value, @Nullable String comment, @Nullable Location origin)` with `record Location(String file, int line)`.
  - `record LanguageProblem(String file, int line, String message)`.
  - `record ReadResult(Map<String, LanguageEntry> entries, Map<String, String> sectionComments, List<LanguageProblem> problems)` with `static empty()` and `Set<String> failedFiles()`.
  - `interface LanguageBackend { String id(); Set<String> languages(); ReadResult read(String language); List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments); default List<LanguageProblem> write(String language, Collection<LanguageEntry> entries); boolean isWritable(); default String fileOf(String language, String key); }` — `fileOf` returns the same label `read` uses in problems.
  - `SplitRule.of(String key) : Target(String file, String relativeKey)`, `SplitRule.fullKey(String file, String relativeKey)`, `SplitRule.HEADER_KEYS`.
  - `new SplitYamlBackend(File languagesFolder)`, `SplitYamlBackend.ID = "yaml-split"`.
  - Package-private `YamlEntries.read(String text, String file, UnaryOperator<String> toFullKey) : Parsed(List<LanguageEntry> entries, Map<String,String> sectionComments)` and `YamlEntries.merge(Path path, String file, List<Map.Entry<String, LanguageEntry>> entries, Function<String,String> sectionComment) : List<LanguageProblem>`.

- [ ] **Step 1: Write the records and the interface**

`LanguageEntry.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

/** One value of a language file: full dotted key, value (string, number, boolean or list), comment and origin. */
public record LanguageEntry(String key, Object value, @Nullable String comment, @Nullable Location origin) {
    public record Location(String file, int line) {
        @Override
        public String toString() {
            return line > 0 ? file + ":" + line : file;
        }
    }
}
```

`LanguageProblem.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

public record LanguageProblem(String file, int line, String message) {
    @Override
    public String toString() {
        return (line > 0 ? file + ":" + line : file) + " - " + message;
    }
}
```

`ReadResult.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.util.*;

public record ReadResult(Map<String, LanguageEntry> entries, Map<String, String> sectionComments, List<LanguageProblem> problems) {
    public static ReadResult empty() {
        return new ReadResult(Map.of(), Map.of(), List.of());
    }

    public Set<String> failedFiles() {
        Set<String> files = new HashSet<>();
        for (LanguageProblem problem : problems) files.add(problem.file());
        return files;
    }
}
```

`LanguageBackend.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Where the texts of a plugin's languages are stored. Implementations never delete keys. */
public interface LanguageBackend {
    String id();

    Set<String> languages();

    /** Reads everything it can; broken files are reported in the result instead of throwing. */
    ReadResult read(String language);

    /** Adds or updates the entries. {@code sectionComments} maps full section keys to comments for new sections. */
    List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments);

    default List<LanguageProblem> write(String language, Collection<LanguageEntry> entries) {
        return write(language, entries, Map.of());
    }

    boolean isWritable();

    /** The file label a key of this language belongs to, as used in {@link LanguageProblem#file()}. */
    default String fileOf(String language, String key) {
        return language;
    }
}
```

`LanguageParseException.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

final class LanguageParseException extends Exception {
    private final LanguageProblem problem;

    LanguageParseException(LanguageProblem problem) {
        super(problem.toString());
        this.problem = problem;
    }

    LanguageProblem problem() {
        return problem;
    }
}
```

- [ ] **Step 2: Write the failing tests**

`SplitRuleTest.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplitRuleTest {
    @ParameterizedTest
    @CsvSource({
            "LanguageFullName, language.yml, LanguageFullName",
            "Messages.Player.General.NoPermissions, messages/Player.yml, General.NoPermissions",
            "Messages.ToManyArguments, messages/_root.yml, ToManyArguments",
            "Items.StartMenu.HintItem.displayName, items/StartMenu.yml, HintItem.displayName",
            "MenuTitles.PlayerManager.Selector, titles.yml, PlayerManager.Selector",
            "CustomVariables.x, other.yml, CustomVariables.x",
            "Messages.Bad Name.X, other.yml, Messages.Bad Name.X"
    })
    void mapsKeysToFilesAndBack(String key, String file, String relative) {
        SplitRule.Target target = SplitRule.of(key);
        assertEquals(file, target.file());
        assertEquals(relative, target.relativeKey());
        assertEquals(key, SplitRule.fullKey(target.file(), target.relativeKey()));
    }
}
```

`SplitYamlBackendTest.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitYamlBackendTest {
    @TempDir
    Path folder;

    @Test
    void writesEntriesIntoTheirFilesAndReadsThemBack() {
        SplitYamlBackend backend = new SplitYamlBackend(folder.toFile());
        List<LanguageProblem> problems = backend.write("en", List.of(
                new LanguageEntry("LanguageFullName", "English", "header", null),
                new LanguageEntry("Messages.Player.Greeting", "Hi 'you'", "greets", null),
                new LanguageEntry("Messages.Plain", "plain", null, null),
                new LanguageEntry("Items.Heal.lore", List.of("a", "b"), null, null)),
                Map.of("Items.Heal", "the heal item"));

        assertTrue(problems.isEmpty(), problems.toString());
        assertTrue(Files.exists(folder.resolve("en/language.yml")));
        assertTrue(Files.exists(folder.resolve("en/messages/Player.yml")));
        ReadResult read = backend.read("en");
        assertEquals("Hi 'you'", read.entries().get("Messages.Player.Greeting").value());
        assertEquals("greets", read.entries().get("Messages.Player.Greeting").comment());
        assertEquals("en/messages/Player.yml:2", read.entries().get("Messages.Player.Greeting").origin().toString());
        assertEquals(List.of("a", "b"), read.entries().get("Items.Heal.lore").value());
        assertEquals("the heal item", read.sectionComments().get("Items.Heal"));
        assertEquals(java.util.Set.of("en"), backend.languages());
    }

    @Test
    void keepsTheOwnersLinesWhenAddingKeys() throws IOException {
        Path file = folder.resolve("en/messages/Player.yml");
        Files.createDirectories(file.getParent());
        String owner = "# my notes\r\nGreeting: \"Servus\"   # changed by me\r\n";
        Files.writeString(file, owner);

        new SplitYamlBackend(folder.toFile()).write("en", List.of(new LanguageEntry("Messages.Player.Bye", "Bye", null, null)));

        assertEquals(owner + "Bye: 'Bye'\r\n", Files.readString(file));
    }

    @Test
    void aBrokenFileOnlyLosesItsOwnKeys() throws IOException {
        Files.createDirectories(folder.resolve("en/messages"));
        Files.writeString(folder.resolve("en/messages/Good.yml"), "A: 'a'\n");
        Files.writeString(folder.resolve("en/messages/Bad.yml"), "B: 'unclosed\n");

        ReadResult read = new SplitYamlBackend(folder.toFile()).read("en");

        assertEquals("a", read.entries().get("Messages.Good.A").value());
        assertEquals(1, read.problems().size());
        assertEquals("en/messages/Bad.yml", read.problems().get(0).file());
        assertEquals(java.util.Set.of("en/messages/Bad.yml"), read.failedFiles());
    }

    @Test
    void neverWritesAnInvalidFile() throws IOException {
        Path file = folder.resolve("en/messages/Player.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "Greeting: 'hi'\n");

        List<LanguageProblem> problems = new SplitYamlBackend(folder.toFile())
                .write("en", List.of(new LanguageEntry("Messages.Player.Greeting.Sub", "x", null, null)));

        assertEquals(1, problems.size());
        assertEquals("Greeting: 'hi'\n", Files.readString(file));
    }
}
```

- [ ] **Step 3: Run the tests and see them fail**

Run: `mvn -B test -Dgpg.skip -Dtest=SplitRuleTest+SplitYamlBackendTest`
Expected: compilation errors for `SplitRule`, `SplitYamlBackend`.

- [ ] **Step 4: Implement `SplitRule`, `YamlEntries`, `SplitYamlBackend`**

`SplitRule.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Which file of the split layout a key lives in: header keys in {@code language.yml}, {@code Messages.<Section>.*}
 * in {@code messages/<Section>.yml}, {@code Items.<Section>.*} in {@code items/<Section>.yml}, two-part keys in
 * {@code messages/_root.yml} / {@code items/_root.yml}, {@code MenuTitles.*} in {@code titles.yml}, anything else in
 * {@code other.yml}.</p>
 */
public final class SplitRule {
    public static final Set<String> HEADER_KEYS = Set.of("LanguageFullName", "LanguageVersion", "LanguageUUID",
            "LanguageDescription", "LanguageParent", "LanguageLocales");
    private static final Pattern FILE_SAFE = Pattern.compile("[A-Za-z0-9_\\-]+");

    public record Target(String file, String relativeKey) {
    }

    private SplitRule() {
    }

    public static Target of(String key) {
        String[] parts = key.split("\\.", 3);
        String root = parts[0];
        if (HEADER_KEYS.contains(root)) return new Target("language.yml", key);
        String folder = root.equals("Messages") ? "messages" : root.equals("Items") ? "items" : null;
        if (folder != null && parts.length == 2) return new Target(folder + "/_root.yml", parts[1]);
        if (folder != null && parts.length == 3 && FILE_SAFE.matcher(parts[1]).matches() && !parts[1].startsWith("_")) {
            return new Target(folder + "/" + parts[1] + ".yml", parts[2]);
        }
        if (root.equals("MenuTitles") && parts.length >= 2) {
            return new Target("titles.yml", key.substring("MenuTitles.".length()));
        }
        return new Target("other.yml", key);
    }

    public static String fullKey(String file, String relativeKey) {
        if (file.equals("titles.yml")) return "MenuTitles." + relativeKey;
        String[] path = file.split("/");
        if (path.length == 2 && path[1].endsWith(".yml")) {
            String root = path[0].equals("messages") ? "Messages" : path[0].equals("items") ? "Items" : null;
            if (root != null) {
                String section = path[1].substring(0, path[1].length() - 4);
                return section.equals("_root") ? root + "." + relativeKey : root + "." + section + "." + relativeKey;
            }
        }
        return relativeKey;
    }
}
```

`YamlEntries.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

final class YamlEntries {
    record Parsed(List<LanguageEntry> entries, Map<String, String> sectionComments) {
    }

    private YamlEntries() {
    }

    static Yaml yaml() {
        return new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    static Parsed read(String text, String file, UnaryOperator<String> toFullKey) throws LanguageParseException {
        Object root;
        try {
            root = yaml().load(text);
        } catch (YAMLException e) {
            throw new LanguageParseException(problem(file, e));
        }
        if (root == null) return new Parsed(List.of(), Map.of());
        if (!(root instanceof Map<?, ?> map)) {
            throw new LanguageParseException(new LanguageProblem(file, 1, "The file must contain keys, not a single value"));
        }
        YamlDocument doc = YamlDocument.parse(text);
        List<LanguageEntry> entries = new ArrayList<>();
        Map<String, String> sectionComments = new LinkedHashMap<>();
        flatten(map, "", doc, file, toFullKey, entries, sectionComments);
        return new Parsed(entries, sectionComments);
    }

    private static void flatten(Map<?, ?> map, String prefix, YamlDocument doc, String file, UnaryOperator<String> toFullKey,
                                List<LanguageEntry> entries, Map<String, String> sectionComments) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String path = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> child) {
                String comment = doc.commentOf(path);
                if (comment != null) sectionComments.put(toFullKey.apply(path), comment);
                flatten(child, path, doc, file, toFullKey, entries, sectionComments);
            } else if (value != null) {
                int line = doc.node(path).map(node -> node.line() + 1).orElse(0);
                entries.add(new LanguageEntry(toFullKey.apply(path), value, doc.commentOf(path), new LanguageEntry.Location(file, line)));
            }
        }
    }

    static LanguageProblem problem(String file, YAMLException e) {
        int line = e instanceof MarkedYAMLException marked && marked.getProblemMark() != null
                ? marked.getProblemMark().getLine() + 1 : 0;
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().lines().findFirst().orElse("");
        return new LanguageProblem(file, line, message);
    }

    static List<LanguageProblem> merge(Path path, String file, List<Map.Entry<String, LanguageEntry>> entries,
                                       Function<String, String> sectionComment) {
        List<LanguageProblem> problems = new ArrayList<>();
        String before;
        try {
            before = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            problems.add(new LanguageProblem(file, 0, "Could not read: " + e.getMessage()));
            return problems;
        }
        YamlDocument doc = YamlDocument.parse(before);
        for (Map.Entry<String, LanguageEntry> entry : entries) {
            try {
                doc.set(entry.getKey(), entry.getValue().value(), entry.getValue().comment(), sectionComment);
            } catch (IllegalStateException e) {
                problems.add(new LanguageProblem(file, 0, e.getMessage()));
            }
        }
        String after = doc.text();
        if (after.equals(before)) return problems;
        try {
            yaml().load(after);
        } catch (YAMLException e) {
            problems.add(new LanguageProblem(file, 0, "Not written, the result would not be valid YAML: " + problem(file, e).message()));
            return problems;
        }
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, after, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            problems.add(new LanguageProblem(file, 0, "Could not write: " + e.getMessage()));
        }
        return problems;
    }
}
```

`SplitYamlBackend.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/** The default layout: one folder per language, split into small files by {@link SplitRule}. */
public final class SplitYamlBackend implements LanguageBackend {
    public static final String ID = "yaml-split";
    private final File folder;

    public SplitYamlBackend(File folder) {
        this.folder = folder;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public Set<String> languages() {
        Set<String> names = new TreeSet<>();
        File[] dirs = folder.listFiles(file -> file.isDirectory() && !file.getName().startsWith("_") && !file.getName().startsWith("."));
        if (dirs != null) for (File dir : dirs) names.add(dir.getName());
        return names;
    }

    @Override
    public ReadResult read(String language) {
        Path root = folder.toPath().resolve(language);
        if (!Files.isDirectory(root)) return ReadResult.empty();
        Map<String, LanguageEntry> entries = new LinkedHashMap<>();
        Map<String, String> sectionComments = new LinkedHashMap<>();
        List<LanguageProblem> problems = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(path -> path.toString().endsWith(".yml")).sorted().toList();
        } catch (IOException e) {
            return new ReadResult(Map.of(), Map.of(), List.of(new LanguageProblem(language, 0, "Could not list files: " + e.getMessage())));
        }
        for (Path file : files) {
            String relative = root.relativize(file).toString().replace('\\', '/');
            String label = language + "/" + relative;
            try {
                YamlEntries.Parsed parsed = YamlEntries.read(Files.readString(file, StandardCharsets.UTF_8), label,
                        key -> SplitRule.fullKey(relative, key));
                for (LanguageEntry entry : parsed.entries()) entries.put(entry.key(), entry);
                sectionComments.putAll(parsed.sectionComments());
            } catch (LanguageParseException e) {
                problems.add(e.problem());
            } catch (IOException e) {
                problems.add(new LanguageProblem(label, 0, "Could not read: " + e.getMessage()));
            }
        }
        return new ReadResult(entries, sectionComments, problems);
    }

    @Override
    public List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments) {
        Map<String, List<Map.Entry<String, LanguageEntry>>> byFile = new LinkedHashMap<>();
        for (LanguageEntry entry : entries) {
            SplitRule.Target target = SplitRule.of(entry.key());
            byFile.computeIfAbsent(target.file(), file -> new ArrayList<>()).add(Map.entry(target.relativeKey(), entry));
        }
        List<LanguageProblem> problems = new ArrayList<>();
        for (Map.Entry<String, List<Map.Entry<String, LanguageEntry>>> file : byFile.entrySet()) {
            String name = file.getKey();
            problems.addAll(YamlEntries.merge(folder.toPath().resolve(language).resolve(name), language + "/" + name,
                    file.getValue(), relative -> sectionComments.get(SplitRule.fullKey(name, relative))));
        }
        return problems;
    }

    @Override
    public String fileOf(String language, String key) {
        return language + "/" + SplitRule.of(key).file();
    }
}
```

- [ ] **Step 5: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=SplitRuleTest+SplitYamlBackendTest+YamlDocumentTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager/storage src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage
git commit -m "feat(language): language backends and the split YAML layout"
```

---

### Task 3: LegacyYamlBackend and ClasspathLanguageSource

**Files:**
- Create in `storage/`: `LegacyYamlBackend.java`, `ClasspathLanguageSource.java`
- Create fixtures: `src/test/resources/lang-fixtures/legacy/en.yml`, `src/test/resources/lang-fixtures/split/en/language.yml`, `src/test/resources/lang-fixtures/split/en/messages/Player.yml`
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage/LegacyAndClasspathTest.java`

**Interfaces:**
- Consumes: Task 2 types, `YamlEntries`.
- Produces: `new LegacyYamlBackend(File folder)` (`ID = "yaml-legacy"`, `fileOf` → `"<lang>.yml"`), `new ClasspathLanguageSource(ClassLoader loader, String directory)` (`ID = "jar"`, read-only; `read` uses split files under `<directory>/<lang>/` when present, otherwise `<directory>/<lang>.yml`).

- [ ] **Step 1: Create the fixtures**

`src/test/resources/lang-fixtures/legacy/en.yml`:
```yaml
# Header comment
LanguageFullName: 'English'
Messages:
  Player:
    # greeting
    Greeting: '%prefix% Hello'
  Plain: 'root message'
Items:
  # the heal item
  Heal:
    material: GOLDEN_APPLE
    displayName: '&aHeal'
    lore:
      - '&7Heals you'
MenuTitles:
  Showcase: 'Showcase'
```

`src/test/resources/lang-fixtures/split/en/language.yml`:
```yaml
LanguageFullName: 'English'
```

`src/test/resources/lang-fixtures/split/en/messages/Player.yml`:
```yaml
Greeting: 'Hello from the split layout'
```

- [ ] **Step 2: Write the failing test**

```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LegacyAndClasspathTest {
    @TempDir
    Path folder;

    @Test
    void legacyBackendReadsAndWritesTheSingleFile() throws IOException {
        Files.copy(Path.of("src/test/resources/lang-fixtures/legacy/en.yml"), folder.resolve("en.yml"));
        LegacyYamlBackend backend = new LegacyYamlBackend(folder.toFile());

        ReadResult read = backend.read("en");
        assertEquals("%prefix% Hello", read.entries().get("Messages.Player.Greeting").value());
        assertEquals("greeting", read.entries().get("Messages.Player.Greeting").comment());
        assertEquals("Header comment", read.entries().get("LanguageFullName").comment());
        assertEquals("the heal item", read.sectionComments().get("Items.Heal"));
        assertEquals(Set.of("en"), backend.languages());
        assertEquals("en.yml", backend.fileOf("en", "Messages.Player.Greeting"));

        assertTrue(backend.write("en", List.of(new LanguageEntry("Messages.Player.Bye", "Bye", null, null))).isEmpty());
        assertEquals("Bye", backend.read("en").entries().get("Messages.Player.Bye").value());
    }

    @Test
    void classpathSourcePrefersTheSplitLayout() {
        ClasspathLanguageSource split = new ClasspathLanguageSource(getClass().getClassLoader(), "lang-fixtures/split");
        assertEquals("Hello from the split layout", split.read("en").entries().get("Messages.Player.Greeting").value());
        assertEquals(Set.of("en"), split.languages());
        assertFalse(split.isWritable());
    }

    @Test
    void classpathSourceReadsASingleFile() {
        ClasspathLanguageSource legacy = new ClasspathLanguageSource(getClass().getClassLoader(), "lang-fixtures/legacy");
        ReadResult read = legacy.read("en");
        assertEquals("%prefix% Hello", read.entries().get("Messages.Player.Greeting").value());
        assertEquals("jar:lang-fixtures/legacy/en.yml:6", read.entries().get("Messages.Player.Greeting").origin().toString());
        assertTrue(legacy.read("fr").entries().isEmpty());
    }
}
```

- [ ] **Step 3: Run it and see it fail**

Run: `mvn -B test -Dgpg.skip -Dtest=LegacyAndClasspathTest`
Expected: compilation errors.

- [ ] **Step 4: Implement**

`LegacyYamlBackend.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** The layout before 3.0.0: one {@code languages/<lang>.yml} file per language. */
public final class LegacyYamlBackend implements LanguageBackend {
    public static final String ID = "yaml-legacy";
    private final File folder;

    public LegacyYamlBackend(File folder) {
        this.folder = folder;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public Set<String> languages() {
        Set<String> names = new TreeSet<>();
        File[] files = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".yml") && !file.getName().startsWith("_"));
        if (files != null) for (File file : files) names.add(file.getName().substring(0, file.getName().length() - 4));
        return names;
    }

    @Override
    public ReadResult read(String language) {
        File file = new File(folder, language + ".yml");
        if (!file.isFile()) return ReadResult.empty();
        String label = language + ".yml";
        try {
            YamlEntries.Parsed parsed = YamlEntries.read(Files.readString(file.toPath(), StandardCharsets.UTF_8), label, key -> key);
            Map<String, LanguageEntry> entries = new LinkedHashMap<>();
            for (LanguageEntry entry : parsed.entries()) entries.put(entry.key(), entry);
            return new ReadResult(entries, parsed.sectionComments(), List.of());
        } catch (LanguageParseException e) {
            return new ReadResult(Map.of(), Map.of(), List.of(e.problem()));
        } catch (IOException e) {
            return new ReadResult(Map.of(), Map.of(), List.of(new LanguageProblem(label, 0, "Could not read: " + e.getMessage())));
        }
    }

    @Override
    public List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments) {
        List<Map.Entry<String, LanguageEntry>> list = new ArrayList<>();
        for (LanguageEntry entry : entries) list.add(Map.entry(entry.key(), entry));
        return YamlEntries.merge(new File(folder, language + ".yml").toPath(), language + ".yml", list, sectionComments::get);
    }

    @Override
    public String fileOf(String language, String key) {
        return language + ".yml";
    }
}
```

`ClasspathLanguageSource.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/** Read-only defaults shipped in the plugin jar, in the split layout or as single files. */
public final class ClasspathLanguageSource implements LanguageBackend {
    public static final String ID = "jar";
    private final ClassLoader loader;
    private final String directory;

    public ClasspathLanguageSource(ClassLoader loader, String directory) {
        this.loader = loader;
        this.directory = directory.endsWith("/") ? directory.substring(0, directory.length() - 1) : directory;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public Set<String> languages() {
        Set<String> names = new TreeSet<>();
        for (String path : list(directory)) {
            int slash = path.indexOf('/');
            if (slash > 0) names.add(path.substring(0, slash));
            else if (path.endsWith(".yml")) names.add(path.substring(0, path.length() - 4));
        }
        return names;
    }

    @Override
    public ReadResult read(String language) {
        List<String> splitFiles = list(directory + "/" + language).stream().filter(path -> path.endsWith(".yml")).toList();
        Map<String, LanguageEntry> entries = new LinkedHashMap<>();
        Map<String, String> sectionComments = new LinkedHashMap<>();
        List<LanguageProblem> problems = new ArrayList<>();
        if (!splitFiles.isEmpty()) {
            for (String relative : splitFiles) {
                String resource = directory + "/" + language + "/" + relative;
                readInto(resource, key -> SplitRule.fullKey(relative, key), entries, sectionComments, problems);
            }
        } else {
            readInto(directory + "/" + language + ".yml", key -> key, entries, sectionComments, problems);
        }
        return new ReadResult(entries, sectionComments, problems);
    }

    private void readInto(String resource, java.util.function.UnaryOperator<String> toFullKey, Map<String, LanguageEntry> entries,
                          Map<String, String> sectionComments, List<LanguageProblem> problems) {
        String text = text(resource);
        if (text == null) return;
        try {
            YamlEntries.Parsed parsed = YamlEntries.read(text, "jar:" + resource, toFullKey);
            for (LanguageEntry entry : parsed.entries()) entries.put(entry.key(), entry);
            sectionComments.putAll(parsed.sectionComments());
        } catch (LanguageParseException e) {
            problems.add(e.problem());
        }
    }

    @Override
    public List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments) {
        return List.of(new LanguageProblem("jar:" + directory, 0, "The jar is read-only"));
    }

    private String text(String resource) {
        URL url = loader.getResource(resource);
        if (url == null) return null;
        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            try (InputStream in = connection.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /** Files below {@code dir} (recursive), as paths relative to it; works for folders and jars. */
    private List<String> list(String dir) {
        URL url = loader.getResource(dir);
        if (url == null) return List.of();
        try {
            if ("file".equals(url.getProtocol())) {
                Path root = Path.of(url.toURI());
                if (!Files.isDirectory(root)) return List.of();
                try (Stream<Path> walk = Files.walk(root)) {
                    return walk.filter(Files::isRegularFile).map(path -> root.relativize(path).toString().replace('\\', '/')).sorted().toList();
                }
            }
            if ("jar".equals(url.getProtocol())) {
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                connection.setUseCaches(false);
                String prefix = dir + "/";
                try (JarFile jar = connection.getJarFile()) {
                    return jar.stream().map(JarEntry::getName)
                            .filter(name -> name.startsWith(prefix) && !name.endsWith("/"))
                            .map(name -> name.substring(prefix.length())).sorted().toList();
                }
            }
        } catch (IOException | URISyntaxException e) {
            return List.of();
        }
        return List.of();
    }
}
```

- [ ] **Step 5: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=LegacyAndClasspathTest`
Expected: PASS. If the origin line assertion is off by one, check that the fixture has the header comment on line 1 and `Greeting` on line 6.

- [ ] **Step 6: Commit**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager/storage src/test/resources/lang-fixtures src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage/LegacyAndClasspathTest.java
git commit -m "feat(language): single-file backend and read-only defaults from the plugin jar"
```

---

### Task 4: Migration between backends

**Files:**
- Create in `storage/`: `MigrationReport.java`, `LanguageMigration.java`
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage/LanguageMigrationTest.java`

**Interfaces:**
- Produces: `record MigrationReport(String language, int entries, List<String> differences, List<LanguageProblem> problems, @Nullable File backup)` with `ok()` and `summary()`; `LanguageMigration.copy(LanguageBackend from, LanguageBackend to, String language) : MigrationReport`; `LanguageMigration.migrateLegacyFile(File folder, String language) : @Nullable MigrationReport` (null when there is nothing to migrate).

- [ ] **Step 1: Write the failing test**

```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LanguageMigrationTest {
    @TempDir
    Path folder;

    @Test
    void movesALegacyFileIntoTheSplitLayoutWithABackup() throws IOException {
        Files.copy(Path.of("src/test/resources/golden/adminpanel/en.yml"), folder.resolve("en.yml"));
        ReadResult before = new LegacyYamlBackend(folder.toFile()).read("en");

        MigrationReport report = LanguageMigration.migrateLegacyFile(folder.toFile(), "en");

        assertNotNull(report);
        assertTrue(report.ok(), report.summary());
        assertFalse(Files.exists(folder.resolve("en.yml")));
        assertTrue(Files.exists(report.backup().toPath()));
        ReadResult after = new SplitYamlBackend(folder.toFile()).read("en");
        assertEquals(before.entries().keySet(), after.entries().keySet());
        for (String key : before.entries().keySet()) {
            assertEquals(before.entries().get(key).value(), after.entries().get(key).value(), key);
        }
        assertNull(LanguageMigration.migrateLegacyFile(folder.toFile(), "en"));
    }

    @Test
    void aBrokenLegacyFileStaysWhereItIs() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  A: 'unclosed\n");

        MigrationReport report = LanguageMigration.migrateLegacyFile(folder.toFile(), "en");

        assertNotNull(report);
        assertFalse(report.ok());
        assertTrue(Files.exists(folder.resolve("en.yml")));
        assertFalse(Files.exists(folder.resolve("en")));
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageMigrationTest`
Expected: compilation errors.

- [ ] **Step 3: Implement**

`MigrationReport.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public record MigrationReport(String language, int entries, List<String> differences, List<LanguageProblem> problems,
                              @Nullable File backup) {
    public boolean ok() {
        return differences.isEmpty() && problems.isEmpty();
    }

    public String summary() {
        return language + ": " + entries + " keys, " + differences.size() + " differences"
                + (differences.isEmpty() ? "" : " " + differences.subList(0, Math.min(5, differences.size())))
                + (problems.isEmpty() ? "" : ", problems: " + problems);
    }
}
```

`LanguageMigration.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public final class LanguageMigration {
    private static final DateTimeFormatter BACKUP_NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private LanguageMigration() {
    }

    /** Copies every entry of {@code language} from one backend to another and compares both sides afterwards. */
    public static MigrationReport copy(LanguageBackend from, LanguageBackend to, String language) {
        ReadResult source = from.read(language);
        if (!source.problems().isEmpty()) {
            return new MigrationReport(language, 0, List.of(), source.problems(), null);
        }
        List<LanguageProblem> problems = new ArrayList<>(to.write(language, source.entries().values(), source.sectionComments()));
        ReadResult target = to.read(language);
        problems.addAll(target.problems());
        List<String> differences = new ArrayList<>();
        for (LanguageEntry entry : source.entries().values()) {
            LanguageEntry copied = target.entries().get(entry.key());
            if (copied == null || !Objects.equals(entry.value(), copied.value())) differences.add(entry.key());
        }
        return new MigrationReport(language, source.entries().size(), differences, problems, null);
    }

    /**
     * <p>Moves {@code languages/<lang>.yml} into the split layout when there is no {@code languages/<lang>/} yet.
     * The old file goes to {@code languages/_backup/<date>/}. On any difference the split folder is removed again
     * and the old file stays.</p>
     */
    public static @Nullable MigrationReport migrateLegacyFile(File folder, String language) {
        File legacy = new File(folder, language + ".yml");
        File split = new File(folder, language);
        if (!legacy.isFile() || split.exists()) return null;
        MigrationReport report = copy(new LegacyYamlBackend(folder), new SplitYamlBackend(folder), language);
        if (!report.ok()) {
            deleteTree(split.toPath());
            return report;
        }
        try {
            Path backupDir = folder.toPath().resolve("_backup").resolve(LocalDateTime.now().format(BACKUP_NAME));
            Files.createDirectories(backupDir);
            Path backup = backupDir.resolve(legacy.getName());
            Files.move(legacy.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
            return new MigrationReport(language, report.entries(), List.of(), List.of(), backup.toFile());
        } catch (IOException e) {
            deleteTree(split.toPath());
            return new MigrationReport(language, report.entries(), List.of(),
                    List.of(new LanguageProblem(legacy.getName(), 0, "Could not move the old file: " + e.getMessage())), null);
        }
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
        }
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageMigrationTest`
Expected: PASS. A difference for a key means `YamlDocument.scalar` or the split rule lost something; the report names the key.

- [ ] **Step 5: Commit**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager/storage src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage/LanguageMigrationTest.java
git commit -m "feat(language): migrate between backends and move old single files into the split layout"
```

---

### Task 5: LanguageStorage and the rewired LanguageConfig

The language manager starts using the storage layer. The golden test from Task 0 must still pass.

**Files:**
- Create in `storage/`: `LanguageStorage.java`, `PrepareResult.java`, `LoadResult.java`
- Modify: `languagemanager/LanguageConfig.java` (whole class), `languagemanager/LanguageFile.java`, `languagemanager/LanguageCache.java`, `languagemanager/LanguageManager.java` (`addLanguagesToList`, `updateLangFiles`, `reloadLanguages`, `addLang`, `getObjectFromLanguageCacheOrConfig`, the four `getLanguageCache(...).containsKey` conditions, new `logReports`)
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage/LanguageStorageTest.java`; existing `GoldenLanguageOutputTest`, `CustomObjectTest`, `LanguageManagerTest`

**Interfaces:**
- Consumes: Tasks 1–4.
- Produces:
  - `record PrepareResult(String language, @Nullable MigrationReport migration, List<String> addedKeys, List<LanguageProblem> problems)`
  - `record LoadResult(String language, String backend, Map<String, LanguageEntry> merged, Map<String, LanguageEntry> own, Map<String, LanguageEntry> defaults, List<LanguageProblem> problems)`
  - `new LanguageStorage(File folder, String resourceDirectory)`, `prepare(String language, String updateLanguage)`, `load(String language, String updateLanguage, @Nullable LoadResult previous)`, `write(String language, Collection<LanguageEntry>)`, `diskFor(String language)`, `locationOf(String language)`; static `registerBackend(String id, Function<File, LanguageBackend>)`, `backendIds()`, `setDefaultBackend(String id)`, `createBackend(String id, File folder)`, `backendIdFor(File folder)`, `useBackend(File folder, String id)` (persists the choice in `<folder>/_backend.txt`).
  - `LanguageConfig`: existing API plus `update(String updateLanguage)`, `write(Collection<LanguageEntry>) : List<LanguageProblem>`, `getLoaded() : LoadResult`, `getStorage()`, `originOf(String key) : String`, `drainReport() : List<String>`.
  - `LanguageManager`: `logReports() : List<String>` (logs and returns the lines), `getLastReport() : List<String>`.

- [ ] **Step 1: Write the failing storage test**

```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LanguageStorageTest {
    @TempDir
    Path folder;

    @Test
    void freshInstallWritesTheJarDefaultsWithComments() throws IOException {
        LanguageStorage storage = new LanguageStorage(folder.toFile(), "lang-fixtures/legacy");

        PrepareResult prepare = storage.prepare("en", "en");

        assertTrue(prepare.problems().isEmpty(), prepare.problems().toString());
        assertTrue(prepare.addedKeys().contains("Messages.Player.Greeting"));
        String player = Files.readString(folder.resolve("en/messages/Player.yml"));
        assertTrue(player.contains("# greeting"), player);
        assertTrue(Files.readString(folder.resolve("en/items/Heal.yml")).contains("material: 'GOLDEN_APPLE'"));
    }

    @Test
    void ownerValuesWinAndMissingKeysComeFromTheJar() throws IOException {
        Path file = folder.resolve("en/messages/Player.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "Greeting: 'Servus'\n");
        LanguageStorage storage = new LanguageStorage(folder.toFile(), "lang-fixtures/legacy");

        LoadResult loaded = storage.load("en", "en", null);

        assertEquals("Servus", loaded.merged().get("Messages.Player.Greeting").value());
        assertEquals("root message", loaded.merged().get("Messages.Plain").value());
        assertEquals("en/messages/Player.yml:1", loaded.merged().get("Messages.Player.Greeting").origin().toString());
    }

    @Test
    void aBrokenOwnerFileKeepsItsLastWorkingValues() throws IOException {
        Path file = folder.resolve("en/messages/Player.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "Greeting: 'Servus'\n");
        LanguageStorage storage = new LanguageStorage(folder.toFile(), "lang-fixtures/legacy");
        LoadResult first = storage.load("en", "en", null);

        Files.writeString(file, "Greeting: 'broken\n");
        LoadResult second = storage.load("en", "en", first);

        assertEquals("Servus", second.merged().get("Messages.Player.Greeting").value());
        assertEquals(1, second.problems().size());
    }

    @Test
    void anOwnerSectionReplacesAJarValue() throws IOException {
        Path file = folder.resolve("en/messages/_root.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "Plain:\n  text: 'rich'\n");
        LoadResult loaded = new LanguageStorage(folder.toFile(), "lang-fixtures/legacy").load("en", "en", null);

        assertNull(loaded.merged().get("Messages.Plain"));
        assertEquals("rich", loaded.merged().get("Messages.Plain.text").value());
    }

    @Test
    void theBackendChoiceIsRemembered() {
        LanguageStorage.useBackend(folder.toFile(), LegacyYamlBackend.ID);
        assertEquals(LegacyYamlBackend.ID, LanguageStorage.backendIdFor(folder.toFile()));
        assertEquals(LegacyYamlBackend.ID, new LanguageStorage(folder.toFile(), "x").diskFor("en").id());
        assertTrue(List.copyOf(LanguageStorage.backendIds()).containsAll(List.of(SplitYamlBackend.ID, LegacyYamlBackend.ID)));
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageStorageTest`
Expected: compilation errors.

- [ ] **Step 3: Implement the records and `LanguageStorage`**

`PrepareResult.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record PrepareResult(String language, @Nullable MigrationReport migration, List<String> addedKeys,
                            List<LanguageProblem> problems) {
}
```

`LoadResult.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.util.List;
import java.util.Map;

/** One language after merging: {@code own} (owner files) over {@code defaults} (jar) gives {@code merged}. */
public record LoadResult(String language, String backend, Map<String, LanguageEntry> merged, Map<String, LanguageEntry> own,
                         Map<String, LanguageEntry> defaults, List<LanguageProblem> problems) {
}
```

`LanguageStorage.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager.storage;

import de.happybavarian07.coolstufflib.utils.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Everything one plugin's language folder needs: the owner's backend, the jar defaults, migration and merging. */
public final class LanguageStorage {
    private static final String BACKEND_FILE = "_backend.txt";
    private static final Map<String, Function<File, LanguageBackend>> BACKENDS = new ConcurrentHashMap<>();
    private static volatile String defaultBackend = SplitYamlBackend.ID;

    static {
        BACKENDS.put(SplitYamlBackend.ID, SplitYamlBackend::new);
        BACKENDS.put(LegacyYamlBackend.ID, LegacyYamlBackend::new);
    }

    private final File folder;
    private final ClasspathLanguageSource jar;

    public LanguageStorage(File folder, String resourceDirectory) {
        this.folder = folder;
        this.jar = new ClasspathLanguageSource(FileUtils.class.getClassLoader(), resourceDirectory == null ? "languages" : resourceDirectory);
    }

    public static void registerBackend(String id, Function<File, LanguageBackend> factory) {
        BACKENDS.put(id, factory);
    }

    public static Set<String> backendIds() {
        return new TreeSet<>(BACKENDS.keySet());
    }

    public static void setDefaultBackend(String id) {
        if (!BACKENDS.containsKey(id)) throw new IllegalArgumentException("Unknown language backend: " + id);
        defaultBackend = id;
    }

    public static LanguageBackend createBackend(String id, File folder) {
        Function<File, LanguageBackend> factory = BACKENDS.get(id);
        if (factory == null) throw new IllegalArgumentException("Unknown language backend: " + id);
        return factory.apply(folder);
    }

    /** The backend this folder uses: the one saved by {@link #useBackend}, otherwise the default. */
    public static String backendIdFor(File folder) {
        File file = new File(folder, BACKEND_FILE);
        if (file.isFile()) {
            try {
                String id = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
                if (BACKENDS.containsKey(id)) return id;
            } catch (IOException ignored) {
            }
        }
        return defaultBackend;
    }

    public static void useBackend(File folder, String id) {
        if (!BACKENDS.containsKey(id)) throw new IllegalArgumentException("Unknown language backend: " + id);
        try {
            Files.createDirectories(folder.toPath());
            Files.writeString(new File(folder, BACKEND_FILE).toPath(), id + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the language backend choice", e);
        }
    }

    /** The owner's backend for this language; an unmigrated single file is still read in place. */
    public LanguageBackend diskFor(String language) {
        String id = backendIdFor(folder);
        if (SplitYamlBackend.ID.equals(id)) {
            boolean split = new File(folder, language).isDirectory();
            boolean legacy = new File(folder, language + ".yml").isFile();
            return !split && legacy ? new LegacyYamlBackend(folder) : new SplitYamlBackend(folder);
        }
        return createBackend(id, folder);
    }

    /** Jar defaults for the language, or for {@code updateLanguage} (then {@code en}) when the jar has no such language. */
    public ReadResult defaults(String language, @Nullable String updateLanguage) {
        ReadResult own = jar.read(language);
        if (!own.entries().isEmpty() || updateLanguage == null || updateLanguage.equals(language)) return own;
        ReadResult update = jar.read(updateLanguage);
        return update.entries().isEmpty() && !"en".equals(updateLanguage) ? jar.read("en") : update;
    }

    /** Moves an old single file into the split layout and adds keys that exist in the jar but not in the owner's files. */
    public PrepareResult prepare(String language, @Nullable String updateLanguage) {
        MigrationReport migration = SplitYamlBackend.ID.equals(backendIdFor(folder))
                ? LanguageMigration.migrateLegacyFile(folder, language) : null;
        LanguageBackend disk = diskFor(language);
        ReadResult current = disk.read(language);
        ReadResult defaults = defaults(language, updateLanguage);
        Set<String> failed = current.failedFiles();
        Set<String> ownKeys = current.entries().keySet();
        Set<String> ownPrefixes = prefixes(ownKeys);
        List<LanguageEntry> missing = new ArrayList<>();
        for (LanguageEntry entry : defaults.entries().values()) {
            if (ownKeys.contains(entry.key()) || conflicts(entry.key(), ownKeys, ownPrefixes)) continue;
            if (failed.contains(disk.fileOf(language, entry.key()))) continue;
            missing.add(entry);
        }
        List<LanguageProblem> problems = new ArrayList<>(current.problems());
        if (!missing.isEmpty() && disk.isWritable()) problems.addAll(disk.write(language, missing, defaults.sectionComments()));
        return new PrepareResult(language, migration, missing.stream().map(LanguageEntry::key).toList(), problems);
    }

    /** Merges the owner's files over the jar defaults. Keys of files that fail to load keep their values from {@code previous}. */
    public LoadResult load(String language, @Nullable String updateLanguage, @Nullable LoadResult previous) {
        LanguageBackend disk = diskFor(language);
        ReadResult current = disk.read(language);
        ReadResult defaults = defaults(language, updateLanguage);
        Map<String, LanguageEntry> own = new LinkedHashMap<>(current.entries());
        if (previous != null && !current.problems().isEmpty()) {
            Set<String> failed = current.failedFiles();
            for (LanguageEntry old : previous.own().values()) {
                if (!own.containsKey(old.key()) && failed.contains(disk.fileOf(language, old.key()))) own.put(old.key(), old);
            }
        }
        Set<String> ownPrefixes = prefixes(own.keySet());
        Map<String, LanguageEntry> merged = new LinkedHashMap<>();
        for (LanguageEntry entry : defaults.entries().values()) {
            if (!conflicts(entry.key(), own.keySet(), ownPrefixes)) merged.put(entry.key(), entry);
        }
        merged.putAll(own);
        List<LanguageProblem> problems = new ArrayList<>(current.problems());
        problems.addAll(defaults.problems());
        return new LoadResult(language, disk.id(), Collections.unmodifiableMap(merged), Collections.unmodifiableMap(own),
                defaults.entries(), List.copyOf(problems));
    }

    public List<LanguageProblem> write(String language, Collection<LanguageEntry> entries) {
        return diskFor(language).write(language, entries);
    }

    public File locationOf(String language) {
        File dir = new File(folder, language);
        return dir.isDirectory() ? dir : new File(folder, language + ".yml");
    }

    public File getFolder() {
        return folder;
    }

    /** A jar key conflicts when the owner has a value above it or a section at it. */
    private static boolean conflicts(String key, Set<String> ownKeys, Set<String> ownPrefixes) {
        if (ownPrefixes.contains(key)) return true;
        int dot = key.indexOf('.');
        while (dot > 0) {
            if (ownKeys.contains(key.substring(0, dot))) return true;
            dot = key.indexOf('.', dot + 1);
        }
        return false;
    }

    private static Set<String> prefixes(Set<String> keys) {
        Set<String> prefixes = new HashSet<>();
        for (String key : keys) {
            int dot = key.indexOf('.');
            while (dot > 0) {
                prefixes.add(key.substring(0, dot));
                dot = key.indexOf('.', dot + 1);
            }
        }
        return prefixes;
    }
}
```

- [ ] **Step 4: Run the storage test**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageStorageTest`
Expected: PASS. Note: `FileUtils.class.getClassLoader()` is the test class loader here, so `lang-fixtures/...` resolves.

- [ ] **Step 5: Replace `LanguageConfig`**

Replace the whole content of `languagemanager/LanguageConfig.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.storage.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * <p>One language's values. {@link #getConfig()} is built in memory from the owner's files over the jar defaults
 * and replaced as a whole on every reload, so readers never see half a reload.</p>
 */
public class LanguageConfig {
    private final String langName;
    private final LanguageStorage storage;
    private volatile String updateLanguage = "en";
    private volatile LoadResult loaded;
    private volatile FileConfiguration config;
    private volatile PrepareResult pendingPrepare;
    private List<LanguageProblem> reportedLoadProblems = List.of();

    public LanguageConfig(File langFile, File langFolder, String resourceDirectory, String langName) {
        this.langName = langName;
        this.storage = new LanguageStorage(langFolder, resourceDirectory);
        saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        LoadResult result = storage.load(langName, updateLanguage, loaded);
        YamlConfiguration next = new YamlConfiguration();
        for (LanguageEntry entry : result.merged().values()) next.set(entry.key(), entry.value());
        loaded = result;
        config = next;
    }

    public FileConfiguration getConfig() {
        if (config == null) reloadConfig();
        return config;
    }

    /** Writes values that were changed through {@link #getConfig()} to the owner's files. */
    public void saveConfig() {
        FileConfiguration current = config;
        LoadResult base = loaded;
        if (current == null || base == null) return;
        List<LanguageEntry> changed = new ArrayList<>();
        for (String key : current.getKeys(true)) {
            if (current.isConfigurationSection(key)) continue;
            Object value = current.get(key);
            LanguageEntry known = base.merged().get(key);
            if (known == null || !Objects.equals(known.value(), value)) {
                changed.add(new LanguageEntry(key, value, known == null ? null : known.comment(), null));
            }
        }
        if (!changed.isEmpty()) write(changed);
    }

    /** Migrates an old single file and adds keys that are in the jar but not in the owner's files. */
    public void saveDefaultConfig() {
        pendingPrepare = storage.prepare(langName, updateLanguage);
    }

    public void update(String updateLanguage) {
        this.updateLanguage = updateLanguage == null || updateLanguage.isBlank() ? "en" : updateLanguage;
        saveDefaultConfig();
        reloadConfig();
    }

    public List<LanguageProblem> write(Collection<LanguageEntry> entries) {
        List<LanguageProblem> problems = storage.write(langName, entries);
        reloadConfig();
        return problems;
    }

    public String getLangName() {
        return langName;
    }

    /** The language's folder in the split layout, or its single file. */
    public File getFile() {
        return storage.locationOf(langName);
    }

    public LoadResult getLoaded() {
        if (loaded == null) reloadConfig();
        return loaded;
    }

    public LanguageStorage getStorage() {
        return storage;
    }

    public String originOf(String key) {
        LoadResult result = loaded;
        LanguageEntry entry = result == null ? null : result.merged().get(key);
        return entry == null || entry.origin() == null ? "?" : entry.origin().toString();
    }

    /** Report lines since the last call: migration, added keys and problems (each problem once). */
    public synchronized List<String> drainReport() {
        Set<String> lines = new LinkedHashSet<>();
        PrepareResult prepare = pendingPrepare;
        pendingPrepare = null;
        if (prepare != null) {
            MigrationReport migration = prepare.migration();
            if (migration != null && migration.ok()) {
                lines.add("Moved " + langName + ".yml into the folder " + langName + "/ (" + migration.entries()
                        + " keys, backup: " + migration.backup() + ")");
            } else if (migration != null) {
                lines.add("Could not move " + langName + ".yml into the new layout, still using the old file: " + migration.summary());
            }
            if (!prepare.addedKeys().isEmpty()) {
                List<String> added = prepare.addedKeys();
                lines.add("Added " + added.size() + " new keys to " + langName + ": "
                        + String.join(", ", added.subList(0, Math.min(10, added.size()))) + (added.size() > 10 ? ", ..." : ""));
            }
            prepare.problems().forEach(problem -> lines.add("Problem in " + problem));
        }
        LoadResult result = loaded;
        if (result != null && !result.problems().equals(reportedLoadProblems)) {
            result.problems().forEach(problem -> lines.add("Problem in " + problem));
            reportedLoadProblems = result.problems();
        }
        return new ArrayList<>(lines);
    }
}
```

- [ ] **Step 6: Adjust `LanguageFile`**

Replace the constructor and `getLangFile()` and remove the `langFile` field:
```java
    private final String langName;
    private final LanguageConfig langConfig;

    public LanguageFile(File langFolder, String resourceDirectory, String langName) {
        this.langName = langName;
        this.langConfig = new LanguageConfig(new File(langFolder, langName + ".yml"), langFolder, resourceDirectory, langName);
    }

    /** The language's folder in the split layout, or its single file before migration. */
    public File getLangFile() {
        return langConfig.getFile();
    }
```

- [ ] **Step 7: Simplify `LanguageCache` (L2)**

Replace the body of `LanguageCache` so it no longer schedules anything and is thread-safe; keep the public methods:
```java
@Deprecated(since = "3.0.0")
public class LanguageCache {
    private final String languageName;
    private final Map<String, Object> languageCache = new java.util.concurrent.ConcurrentHashMap<>();

    public LanguageCache(String languageName) {
        this.languageName = languageName;
    }

    public void setup() {
    }

    public void addData(String key, Object value, boolean replace) {
        if (value == null) return;
        if (replace) languageCache.put(key, value);
        else languageCache.putIfAbsent(key, value);
    }

    public Object getData(String key) {
        return languageCache.get(key);
    }

    public boolean containsKey(String key) {
        return languageCache.containsKey(key);
    }

    public void removeData(String key) {
        languageCache.remove(key);
    }

    public void clearCache() {
        languageCache.clear();
    }

    public String getLanguageName() {
        return languageName;
    }

    public Map<String, Object> getLanguageCache() {
        return languageCache;
    }
}
```
Remove now unused imports (`Executors`, `ScheduledExecutorService`, `TimeUnit`). Keep any other public method the file has today with the same signature.

- [ ] **Step 8: Rewire `LanguageManager`**

1. Remove `import de.happybavarian07.coolstufflib.configstuff.ConfigUpdater;`.
2. Replace `addLanguagesToList`:
```java
    public void addLanguagesToList(boolean log) {
        Set<String> names = new TreeSet<>();
        File[] files = langFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith("_") || name.startsWith(".")) continue;
                if (file.isDirectory()) names.add(name);
                else if (name.endsWith(".yml")) names.add(name.substring(0, name.length() - 4));
            }
        }
        for (String name : names) {
            if (name.equals("default") || registeredLanguages.containsKey(name)) continue;
            LanguageFile languageFile = new LanguageFile(langFolder, resourceDirectory, name);
            if (log) getLogger().log(Level.INFO, "Language: " + languageFile.getLangFile() + " successfully registered!");
            registeredLanguages.put(name, languageFile);
            languageCaches.put(name, new LanguageCache(name));
            addEngineForLanguage(name, true, true);
        }
        logReports();
    }
```
3. Replace `updateLangFiles` and add `updateLanguage()`, `logReports()`, `getLastReport()` and the field `private volatile List<String> lastReport = List.of();`:
```java
    public void updateLangFiles() {
        String updateLanguage = updateLanguage();
        for (LanguageFile languageFile : getRegisteredLanguages().values()) {
            languageFile.getLangConfig().update(updateLanguage);
        }
        logReports();
    }

    private String updateLanguage() {
        try {
            String language = plugin.getConfig().getString("Plugin.languageForUpdates");
            return language == null || language.isBlank() ? "en" : language;
        } catch (RuntimeException e) {
            return "en";
        }
    }

    /** Logs what changed since the last report (migration, added keys, problems) and returns the lines. */
    public List<String> logReports() {
        List<String> lines = new ArrayList<>();
        for (LanguageFile languageFile : registeredLanguages.values()) {
            for (String line : languageFile.getLangConfig().drainReport()) {
                lines.add(line);
                getLogger().log(line.startsWith("Problem") || line.startsWith("Could not") ? Level.WARNING : Level.INFO, line);
            }
        }
        lastReport = List.copyOf(lines);
        return lines;
    }

    public List<String> getLastReport() {
        return lastReport;
    }
```
4. In `reloadLanguages`, remove the line `getLang(langFiles, true).getLangConfig().reloadConfig();` (the update already reloads).
5. In `addLang`, after `addEngineForLanguage(langName, true, true);` add `logReports();`.
6. Replace `getObjectFromLanguageCacheOrConfig`:
```java
    public <T> T getObjectFromLanguageCacheOrConfig(String path, String langName, Class<T> clazz) {
        LanguageFile langFile = getLang(langName, true);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null) return getDefaultInstance(clazz);
        Object configObject = langConfig.getConfig().get(path);
        if (configObject == null) return getDefaultInstance(clazz);
        try {
            return clazz.cast(configObject);
        } catch (ClassCastException e) {
            return null;
        }
    }
```
7. Remove the cache part of the four existence checks:
   - `getMessage`: `... || !langConfig.getConfig().contains("Messages." + path)) && !getLanguageCache(langName).containsKey("Messages." + path)` → `... || !langConfig.getConfig().contains("Messages." + path))`
   - `getItem`: same for `"Items." + path`
   - `getMenuTitle`: same for `"MenuTitles." + path`
   - `getCustomObject`: `!langConfig.getConfig().contains(path) && !getLanguageCache(langName).containsKey(path)` → `!langConfig.getConfig().contains(path)`

- [ ] **Step 9: Run the language tests**

Run: `mvn -B test -Dgpg.skip -Dtest=GoldenLanguageOutputTest+CustomObjectTest+LanguageManagerTest+LanguageStorageTest`
Expected: PASS. The golden test now migrates the copied `en.yml` into `en/` inside the temp folder and must render every key exactly as recorded in Task 0. A failing line names the key; compare `LegacyYamlBackend.read` and `SplitYamlBackend.read` values for it.

- [ ] **Step 10: Run the whole suite**

Run: `mvn -B test -Dgpg.skip`
Expected: PASS (all tests).

- [ ] **Step 11: Commit (two commits)**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager/storage src/test/java/de/happybavarian07/coolstufflib/languagemanager/storage/LanguageStorageTest.java
git commit -m "feat(language): LanguageStorage prepares and merges owner files over the jar defaults"
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager
git commit -m "feat(language): language files live in the split layout, are updated by text merge and reload atomically"
```

---

### Task 6: Fallback chain, `Messages.` optional, rich and plural values in the old getters

**Files:**
- Modify: `languagemanager/LanguageManager.java` (`getMessage`, `getItem`, `getMenuTitle`, `getCustomObject`; new `languageWith`, `stripRoot`, `textOf`, `renderMessage`, `renderText`, `applyLocal`)
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/LanguageFallbackTest.java`

**Interfaces:**
- Produces (package-private in `LanguageManager`, used by Task 7):
  - `LanguageFile languageWith(String fullPath, LanguageFile start)`
  - `static String stripRoot(String path, String root)`
  - `static @Nullable String textOf(ConfigurationSection config, String fullPath, @Nullable Long count)`
  - `String renderMessage(String path, @Nullable Player player, LanguageFile start, Map<String, ?> local, @Nullable Long count, boolean resetAfter)`
  - `String renderText(String raw, String fullPath, @Nullable Player player, String langName, Map<String, ?> local, @Nullable Long count, boolean resetAfter)`
  - `String applyLocal(String text, Map<String, ?> local, @Nullable Long count, PlaceholderType type)`

- [ ] **Step 1: Write the failing test**

```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LanguageFallbackTest {
    @TempDir
    Path folder;
    private LanguageManager lgm;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(folder.resolve("en.yml"), """
                LanguageFullName: English
                Messages:
                  OnlyEnglish: 'english only'
                  Shared: 'shared en'
                  Apples:
                    one: '%count% apple'
                    other: '%count% apples'
                  Rich:
                    text: 'rich text'
                    actionbar: 'bar'
                """);
        Files.writeString(folder.resolve("de.yml"), """
                LanguageFullName: Deutsch
                Messages:
                  OnlyGerman: 'nur deutsch'
                  Shared: 'geteilt de'
                """);
        Files.writeString(folder.resolve("at.yml"), """
                LanguageFullName: Oesterreichisch
                LanguageParent: de
                Messages:
                  Shared: 'geteilt at'
                """);
        lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "none", "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
    }

    private Player playerWithLanguage(String language) {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration data = new YamlConfiguration();
        data.set("playerdata." + uuid + ".language", language);
        lgm.setPLHandler(new PerPlayerLanguageHandler(lgm, new File(folder.toFile(), "data.yml"), data));
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }

    @Test
    void missingKeysComeFromTheParentThenTheServerLanguage() {
        Player austrian = playerWithLanguage("at");
        assertEquals("geteilt at", lgm.getMessage("Shared", austrian, false));
        assertEquals("nur deutsch", lgm.getMessage("OnlyGerman", austrian, false));
        assertEquals("english only", lgm.getMessage("OnlyEnglish", austrian, false));
        assertTrue(lgm.getMessage("Nowhere", austrian, false).startsWith("null path: Messages.Nowhere"));
    }

    @Test
    void theMessagesRootIsOptionalEverywhere() {
        assertEquals("shared en", lgm.getMessage("Messages.Shared", null, false));
        assertEquals("shared en", lgm.getCustomObject("Shared", null, "x", false));
        assertEquals("shared en", lgm.getCustomObject("Messages.Shared", null, "x", false));
    }

    @Test
    void richAndPluralValuesGiveTextToTheOldApi() {
        assertEquals("rich text", lgm.getMessage("Rich", null, false));
        assertEquals("%count% apples", lgm.getMessage("Apples", null, false));
    }

    @Test
    void pluralVariantAndLocalPlaceholders() {
        LanguageFile en = lgm.getLang("en", true);
        assertEquals("1 apple", lgm.renderMessage("Apples", null, en, Map.of(), 1L, false));
        assertEquals("3 apples", lgm.renderMessage("Apples", null, en, Map.of(), 3L, false));
        assertEquals(List.of(), List.copyOf(lgm.getPlaceholders().keySet()));
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageFallbackTest`
Expected: compilation error (`renderMessage` missing) — then assertion failures once it compiles.

- [ ] **Step 3: Add the helpers to `LanguageManager`**

Add imports `org.bukkit.configuration.file.FileConfiguration` (if missing) and add:
```java
    /** The first language along language → LanguageParent → server language that has the key; {@code start} if none. */
    LanguageFile languageWith(String fullPath, LanguageFile start) {
        Set<String> seen = new HashSet<>();
        LanguageFile language = start;
        while (language != null && seen.add(language.getLangName())) {
            FileConfiguration config = language.getLangConfig().getConfig();
            if (config.contains(fullPath)) return language;
            String parent = config.getString("LanguageParent");
            language = parent == null ? null : getLang(parent, false);
        }
        LanguageFile server = getCurrentLang();
        if (server != null && seen.add(server.getLangName()) && server.getLangConfig().getConfig().contains(fullPath)) return server;
        return start;
    }

    static String stripRoot(String path, String root) {
        return path.startsWith(root + ".") ? path.substring(root.length() + 1) : path;
    }

    /** A message's text: a plain value, the plural variant for {@code count}, or the {@code text} of a rich entry. */
    static @Nullable String textOf(ConfigurationSection config, String fullPath, @Nullable Long count) {
        Object value = config.get(fullPath);
        if (value == null || value instanceof List<?>) return null;
        if (!(value instanceof ConfigurationSection section)) return value.toString();
        if (count != null) {
            String variant = count == 0 && section.isString("zero") ? "zero" : count == 1 && section.isString("one") ? "one" : "other";
            if (section.isString(variant)) return section.getString(variant);
        }
        if (section.isString("text")) return section.getString("text");
        return section.isString("other") ? section.getString("other") : null;
    }

    String applyLocal(String text, Map<String, ?> local, @Nullable Long count, PlaceholderType type) {
        if (count != null && !local.containsKey("%count%")) text = new Placeholder("%count%", String.valueOf(count), type).replace(text);
        for (Map.Entry<String, ?> entry : local.entrySet()) {
            text = new Placeholder(entry.getKey(), String.valueOf(entry.getValue()), type).replace(text);
        }
        return text;
    }

    String renderMessage(String path, @Nullable Player player, LanguageFile start, Map<String, ?> local, @Nullable Long count, boolean resetAfter) {
        path = stripRoot(path, "Messages");
        applyPathExpressionVariables(player, path);
        String fullPath = "Messages." + path;
        LanguageFile langFile = languageWith(fullPath, start);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null) return "null config";
        String raw = textOf(langConfig.getConfig(), fullPath, count);
        if (raw == null) return "null path: " + fullPath;
        return renderText(raw, fullPath, player, langFile.getLangName(), local, count, resetAfter);
    }

    String renderText(String raw, String fullPath, @Nullable Player player, String langName, Map<String, ?> local,
                      @Nullable Long count, boolean resetAfter) {
        String message = Utils.format(player, raw, prefix);
        message = applyLocal(message, local, count, PlaceholderType.MESSAGE);
        if (!placeholders.isEmpty()) {
            List<String> includedKeys = new ArrayList<>(getPlaceholderKeysInMessage(message, PlaceholderType.MESSAGE));
            message = replacePlaceholders(PlaceholderType.MESSAGE, message);
            if (resetAfter) resetSpecificPlaceholders(PlaceholderType.MESSAGE, includedKeys);
        }
        return parseEmbeddedExpressions(message, player, langName);
    }
```

- [ ] **Step 4: Route the old getters through them**

`getMessage(String path, Player player, String langName, boolean resetAfter)` becomes:
```java
    public String getMessage(String path, Player player, String langName, boolean resetAfter) {
        return renderMessage(path, player, getLangOrPlayerLang(true, langName, player), Map.of(), null, resetAfter);
    }
```
In `getItem(String path, Player player, String langName, boolean resetAfter, MaterialCondition condition)`, replace the first two lines with:
```java
        path = stripRoot(path, "Items");
        LanguageFile langFile = languageWith("Items." + path, getLangOrPlayerLang(false, langName, player));
```
In `getMenuTitle(String path, Player player, String langName)`, replace `applyPathExpressionVariables(player, path);` and the next line with:
```java
        path = stripRoot(path, "MenuTitles");
        applyPathExpressionVariables(player, path);
        LanguageFile langFile = languageWith("MenuTitles." + path, getLangOrPlayerLang(false, langName, player));
```
In `getCustomObject(String path, Player player, String langName, T defaultValue, boolean resetAfter)`, replace the first line with:
```java
        LanguageFile start = getLangOrPlayerLang(false, langName, player);
        LanguageFile langFile = languageWith(path, start);
        if (!langFile.getLangConfig().getConfig().contains(path) && !path.startsWith("Messages.")) {
            LanguageFile withRoot = languageWith("Messages." + path, start);
            if (withRoot.getLangConfig().getConfig().contains("Messages." + path)) {
                path = "Messages." + path;
                langFile = withRoot;
            }
        }
```

- [ ] **Step 5: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageFallbackTest+GoldenLanguageOutputTest+CustomObjectTest+LanguageManagerTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager/LanguageManager.java src/test/java/de/happybavarian07/coolstufflib/languagemanager/LanguageFallbackTest.java
git commit -m "feat(language): LanguageParent fallback, optional Messages root, rich and plural values"
```

---

### Task 7: Thread-safe placeholders and the new builders

**Files:**
- Create: `languagemanager/MessageBuilder.java`, `ItemBuilder.java`, `TitleBuilder.java`
- Modify: `languagemanager/LanguageManager.java` (placeholder map, `getItem`/`getMenuTitle` split into `renderItem`/`renderTitle`, `parseEmbeddedExpressions`, `message()`/`item()`/`title()`, `send`)
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/MessageBuilderTest.java`

**Interfaces:**
- Consumes: Task 6 helpers.
- Produces: `LanguageManager.message(String) : MessageBuilder`, `item(String) : ItemBuilder`, `title(String) : TitleBuilder`; `MessageBuilder.with(String, Object)`, `with(Map<String, ?>)`, `count(long)`, `lang(String)`, `text(@Nullable Player)`, `lines(@Nullable Player)`, `send(CommandSender)`; `ItemBuilder.with/lang/build(@Nullable Player) : ItemStack`; `TitleBuilder.with/lang/text(@Nullable Player)`; package-private `LanguageManager.startFor(@Nullable String language, @Nullable Player viewer, boolean messages) : LanguageFile`, `renderLines(...)`, `send(...)`, `renderItem(...)`, `renderTitle(...)`.

- [ ] **Step 1: Write the failing test**

```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MessageBuilderTest {
    @TempDir
    Path folder;
    private LanguageManager lgm;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(folder.resolve("en.yml"), """
                Messages:
                  Target: '%prefix% hello %target%'
                  Help:
                    - 'line one %target%'
                    - 'line two'
                  Apples:
                    one: '%count% apple'
                    other: '%count% apples'
                MenuTitles:
                  Menu: 'Menu of %target%'
                """);
        Files.writeString(folder.resolve("de.yml"), """
                Messages:
                  Target: 'hallo %target%'
                """);
        lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "none", "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
    }

    @Test
    void rendersWithItsOwnPlaceholders() {
        assertEquals("[P] hello Alex", lgm.message("Target").with("%target%", "Alex").text(null));
        assertEquals("hallo Alex", lgm.message("Target").lang("de").with("%target%", "Alex").text(null));
        assertEquals(List.of("line one Alex", "line two"), lgm.message("Help").with("%target%", "Alex").lines(null));
        assertEquals("1 apple", lgm.message("Apples").count(1).text(null));
        assertEquals("Menu of Alex", lgm.title("Menu").with("%target%", "Alex").text(null));
        assertTrue(lgm.getPlaceholders().isEmpty());
    }

    @Test
    void globalPlaceholdersStillApplyAndStay() {
        lgm.addPlaceholder(PlaceholderType.MESSAGE, "%target%", "Global", false);
        assertEquals("[P] hello Global", lgm.message("Target").text(null));
        assertEquals("[P] hello Local", lgm.message("Target").with("%target%", "Local").text(null));
        assertTrue(lgm.getPlaceholders().containsKey("%target%"));
    }

    @Test
    void rendersSafelyFromManyThreads() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> results = new java.util.ArrayList<>();
            for (int t = 0; t < 4; t++) {
                int id = t;
                results.add(pool.submit(() -> {
                    for (int i = 0; i < 500; i++) {
                        String name = "P" + id + "-" + i;
                        if (!lgm.message("Target").with("%target%", name).text(null).endsWith(name)) return false;
                    }
                    return true;
                }));
            }
            for (int i = 0; i < 500; i++) {
                lgm.addPlaceholder(PlaceholderType.ITEM, "%other" + i + "%", i, false);
                lgm.removePlaceholder(PlaceholderType.ITEM, "%other" + i + "%");
            }
            for (Future<Boolean> result : results) assertTrue(result.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `mvn -B test -Dgpg.skip -Dtest=MessageBuilderTest`
Expected: compilation errors.

- [ ] **Step 3: Make the placeholder map thread-safe**

In the constructor replace `this.placeholders = new LinkedHashMap<>();` with `this.placeholders = Collections.synchronizedMap(new LinkedHashMap<>());`. Wrap the body of each of these methods in `synchronized (placeholders) { ... }`: `addPlaceholder`, `addPlaceholders`, `removePlaceholder`, `removePlaceholders`, `resetPlaceholders`, `resetSpecificPlaceholders`, `getPlaceholderKeysInMessage`, `replacePlaceholders(PlaceholderType, String)`. In `renderText` wrap the `if (!placeholders.isEmpty()) { ... }` block in `synchronized (placeholders) { ... }`.

In `parseEmbeddedExpressions`, the engine is not thread-safe; evaluate under its lock:
```java
            try {
                synchronized (engine) {
                    result = engine.parse(expr, Object.class);
                }
                matcher.appendReplacement(sb, result == null ? "null" : Matcher.quoteReplacement(result.toString()));
            } catch (Exception e) {
```

- [ ] **Step 4: Split `getItem` and `getMenuTitle` so they accept local placeholders**

Rename the body of `getItem(String path, Player player, String langName, boolean resetAfter, MaterialCondition condition)` into
```java
    ItemStack renderItem(String path, @Nullable Player player, LanguageFile start, Map<String, ?> local, boolean resetAfter,
                         @Nullable MaterialCondition condition)
```
with these changes: the start is given (`LanguageFile langFile = languageWith("Items." + path, start);` after `path = stripRoot(path, "Items");`), `langName` is taken from `langFile.getLangName()`, and in the lore and display name loops apply the local values before the global ones:
```java
        for (String s : lore == null ? List.<String>of() : lore) {
            String withLocal = applyLocal(s, local, null, PlaceholderType.ITEM);
            includedKeys.addAll(getPlaceholderKeysInMessage(withLocal, PlaceholderType.ITEM));
            String temp = replacePlaceholders(PlaceholderType.ITEM, withLocal);
            loreWithPlaceholders.add(Utils.format(player, temp, prefix));
        }
        ...
        String formattedName = applyLocal(Utils.format(player, displayName, prefix), local, null, PlaceholderType.ITEM);
        includedKeys.addAll(getPlaceholderKeysInMessage(formattedName, PlaceholderType.ITEM));
        meta.setDisplayName(replacePlaceholders(PlaceholderType.ITEM, formattedName));
```
The recursive call for disabled items becomes `return renderItem("General.DisabledItem", player, start, local, false, null);`. The public method becomes:
```java
    public ItemStack getItem(String path, Player player, String langName, boolean resetAfter, MaterialCondition condition) {
        return renderItem(path, player, getLangOrPlayerLang(false, langName, player), Map.of(), resetAfter, condition);
    }
```
Do the same for titles:
```java
    public String getMenuTitle(String path, Player player, String langName) {
        return renderTitle(path, player, getLangOrPlayerLang(false, langName, player), Map.of());
    }

    String renderTitle(String path, @Nullable Player player, LanguageFile start, Map<String, ?> local) {
        path = stripRoot(path, "MenuTitles");
        applyPathExpressionVariables(player, path);
        LanguageFile langFile = languageWith("MenuTitles." + path, start);
        String langName = langFile.getLangName();
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null) return "null config";
        if (langConfig.getConfig().getString("MenuTitles." + path) == null || !langConfig.getConfig().contains("MenuTitles." + path))
            return "null path: MenuTitles." + path;
        String title = applyLocal(getObjectFromLanguageCacheOrConfig("MenuTitles." + path, langName, String.class), local, null, PlaceholderType.MENUTITLE);
        List<String> includedKeys = new ArrayList<>(getPlaceholderKeysInMessage(title, PlaceholderType.MENUTITLE));
        title = replacePlaceholders(PlaceholderType.MENUTITLE, title);
        resetSpecificPlaceholders(PlaceholderType.MENUTITLE, includedKeys);
        return parseEmbeddedExpressions(Utils.format(player, title, prefix), player, langName);
    }
```

- [ ] **Step 5: Add lines, send and the entry points to `LanguageManager`**

```java
    public MessageBuilder message(String path) {
        return new MessageBuilder(this, path);
    }

    public ItemBuilder item(String path) {
        return new ItemBuilder(this, path);
    }

    public TitleBuilder title(String path) {
        return new TitleBuilder(this, path);
    }

    /** An explicit language wins over the viewer's language for the new API. */
    LanguageFile startFor(@Nullable String language, @Nullable Player viewer, boolean messages) {
        if (language != null) return getLang(language, true);
        return getLangOrPlayerLang(messages, getCurrentLangName(), viewer);
    }

    List<String> renderLines(String path, @Nullable Player player, LanguageFile start, Map<String, ?> local, @Nullable Long count) {
        String fullPath = "Messages." + stripRoot(path, "Messages");
        LanguageFile langFile = languageWith(fullPath, start);
        Object value = langFile.getLangConfig().getConfig().get(fullPath);
        if (!(value instanceof List<?> list)) return List.of(renderMessage(path, player, start, local, count, false));
        List<String> lines = new ArrayList<>();
        for (Object line : list) lines.add(renderText(String.valueOf(line), fullPath, player, langFile.getLangName(), local, count, false));
        return lines;
    }

    void send(String path, CommandSender sender, LanguageFile start, Map<String, ?> local, @Nullable Long count) {
        Player player = sender instanceof Player p ? p : null;
        String fullPath = "Messages." + stripRoot(path, "Messages");
        LanguageFile langFile = languageWith(fullPath, start);
        ConfigurationSection rich = langFile.getLangConfig().getConfig().getConfigurationSection(fullPath);
        boolean isRich = rich != null && (rich.contains("text") || rich.contains("actionbar") || rich.contains("title")
                || rich.contains("subtitle") || rich.contains("sound"));
        if (!isRich || rich.isString("text")) sender.sendMessage(renderMessage(path, player, start, local, count, false));
        if (!isRich || player == null) return;
        String lang = langFile.getLangName();
        if (rich.isString("actionbar")) {
            String bar = renderText(rich.getString("actionbar"), fullPath + ".actionbar", player, lang, local, count, false);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(bar));
        }
        if (rich.isString("title") || rich.isString("subtitle")) {
            String title = rich.isString("title") ? renderText(rich.getString("title"), fullPath + ".title", player, lang, local, count, false) : "";
            String subtitle = rich.isString("subtitle") ? renderText(rich.getString("subtitle"), fullPath + ".subtitle", player, lang, local, count, false) : "";
            player.sendTitle(title, subtitle, 10, 70, 20);
        }
        if (rich.isString("sound")) playSound(player, rich.getString("sound"));
    }

    /** {@code sound} is a Minecraft sound key with optional volume and pitch: {@code "entity.villager.no 0.5 1"}. */
    static void playSound(Player player, String sound) {
        String[] parts = sound.trim().split("\\s+");
        try {
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1f;
            player.playSound(player.getLocation(), parts[0], volume, pitch);
        } catch (NumberFormatException e) {
            getLogger().warning("Invalid sound in a language file: " + sound);
        }
    }
```

- [ ] **Step 6: Create the builders**

`MessageBuilder.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>One message with its own placeholders. Nothing is stored in the language manager, so it can be used from
 * any thread.</p>
 * <pre><code>lgm.message("Player.General.NoPermissions").with("%target%", name).send(sender);</code></pre>
 */
public final class MessageBuilder {
    private final LanguageManager lgm;
    private final String path;
    private final Map<String, Object> placeholders = new LinkedHashMap<>();
    private Long count;
    private String language;

    MessageBuilder(LanguageManager lgm, String path) {
        this.lgm = lgm;
        this.path = path;
    }

    public MessageBuilder with(String key, Object value) {
        placeholders.put(key, value);
        return this;
    }

    public MessageBuilder with(Map<String, ?> values) {
        placeholders.putAll(values);
        return this;
    }

    /** Picks the {@code zero}/{@code one}/{@code other} variant and sets {@code %count%}. */
    public MessageBuilder count(long count) {
        this.count = count;
        return this;
    }

    /** Renders in this language instead of the viewer's. */
    public MessageBuilder lang(String language) {
        this.language = language;
        return this;
    }

    public String text(@Nullable Player viewer) {
        return lgm.renderMessage(path, viewer, lgm.startFor(language, viewer, true), placeholders, count, false);
    }

    public List<String> lines(@Nullable Player viewer) {
        return lgm.renderLines(path, viewer, lgm.startFor(language, viewer, true), placeholders, count);
    }

    /** Sends the chat text and, for rich entries, the action bar, title and sound. */
    public void send(CommandSender sender) {
        Player viewer = sender instanceof Player player ? player : null;
        lgm.send(path, sender, lgm.startFor(language, viewer, true), placeholders, count);
    }
}
```

`ItemBuilder.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** <pre><code>lgm.item("StartMenu.HintItem").with("%x%", x).build(player);</code></pre> */
public final class ItemBuilder {
    private final LanguageManager lgm;
    private final String path;
    private final Map<String, Object> placeholders = new LinkedHashMap<>();
    private String language;

    ItemBuilder(LanguageManager lgm, String path) {
        this.lgm = lgm;
        this.path = path;
    }

    public ItemBuilder with(String key, Object value) {
        placeholders.put(key, value);
        return this;
    }

    public ItemBuilder with(Map<String, ?> values) {
        placeholders.putAll(values);
        return this;
    }

    public ItemBuilder lang(String language) {
        this.language = language;
        return this;
    }

    public ItemStack build(@Nullable Player viewer) {
        return lgm.renderItem(path, viewer, lgm.startFor(language, viewer, false), placeholders, false, null);
    }
}
```

`TitleBuilder.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** <pre><code>lgm.title("StartMenu").text(player);</code></pre> */
public final class TitleBuilder {
    private final LanguageManager lgm;
    private final String path;
    private final Map<String, Object> placeholders = new LinkedHashMap<>();
    private String language;

    TitleBuilder(LanguageManager lgm, String path) {
        this.lgm = lgm;
        this.path = path;
    }

    public TitleBuilder with(String key, Object value) {
        placeholders.put(key, value);
        return this;
    }

    public TitleBuilder lang(String language) {
        this.language = language;
        return this;
    }

    public String text(@Nullable Player viewer) {
        return lgm.renderTitle(path, viewer, lgm.startFor(language, viewer, false), placeholders);
    }
}
```

- [ ] **Step 7: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=MessageBuilderTest+LanguageFallbackTest+GoldenLanguageOutputTest+CustomObjectTest+LanguageManagerTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager src/test/java/de/happybavarian07/coolstufflib/languagemanager/MessageBuilderTest.java
git commit -m "feat(language): message, item and title builders with their own placeholders; thread-safe placeholder map"
```

---

### Task 8: PlayerLanguageStore (L1)

**Files:**
- Create: `languagemanager/PlayerLanguageStore.java`, `languagemanager/YamlPlayerLanguageStore.java`
- Modify: `languagemanager/PerPlayerLanguageHandler.java`
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/PlayerLanguageStoreTest.java`

**Interfaces:**
- Produces: `interface PlayerLanguageStore { @Nullable String get(UUID); void set(UUID, String); void remove(UUID); Map<UUID, String> all(); }`; `new YamlPlayerLanguageStore(File dataFile, FileConfiguration data)` with `save()`; `new PerPlayerLanguageHandler(LanguageManager, PlayerLanguageStore)`; `PerPlayerLanguageHandler.getStore()`.

- [ ] **Step 1: Write the failing test**

```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PlayerLanguageStoreTest {
    @TempDir
    Path folder;

    @Test
    void emptyDataFileHasNoPlayerLanguages() {
        YamlPlayerLanguageStore store = new YamlPlayerLanguageStore(folder.resolve("data.yml").toFile(), new YamlConfiguration());
        assertTrue(store.all().isEmpty());
    }

    @Test
    void keepsTheDataYmlLayout() throws IOException {
        File dataFile = folder.resolve("data.yml").toFile();
        YamlPlayerLanguageStore store = new YamlPlayerLanguageStore(dataFile, new YamlConfiguration());
        UUID uuid = UUID.randomUUID();
        store.set(uuid, "de");
        assertTrue(Files.readString(dataFile.toPath()).contains("playerdata:"));
        assertEquals("de", YamlConfiguration.loadConfiguration(dataFile).getString("playerdata." + uuid + ".language"));
        assertEquals("de", store.all().get(uuid));
        store.remove(uuid);
        assertNull(store.get(uuid));
    }

    @Test
    void handlerWithAnEmptyStoreDoesNotThrow() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  A: 'a'\n");
        LanguageManager lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "none", "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
        PerPlayerLanguageHandler handler = new PerPlayerLanguageHandler(lgm, new YamlPlayerLanguageStore(folder.resolve("data.yml").toFile(), new YamlConfiguration()));
        assertTrue(handler.getPlayerLanguages().isEmpty());
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `mvn -B test -Dgpg.skip -Dtest=PlayerLanguageStoreTest`
Expected: compilation errors.

- [ ] **Step 3: Implement**

`PlayerLanguageStore.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/** Where each player's chosen language is kept. */
public interface PlayerLanguageStore {
    @Nullable String get(UUID player);

    void set(UUID player, String language);

    void remove(UUID player);

    Map<UUID, String> all();
}
```

`YamlPlayerLanguageStore.java`:
```java
package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Stores {@code playerdata.<uuid>.language} in the plugin's data file, the layout used since 1.x. */
public final class YamlPlayerLanguageStore implements PlayerLanguageStore {
    private final File dataFile;
    private final FileConfiguration data;

    public YamlPlayerLanguageStore(File dataFile, FileConfiguration data) {
        this.dataFile = dataFile;
        this.data = data;
    }

    @Override
    public @Nullable String get(UUID player) {
        return data.getString("playerdata." + player + ".language");
    }

    @Override
    public void set(UUID player, String language) {
        data.set("playerdata." + player + ".language", language);
        save();
    }

    @Override
    public void remove(UUID player) {
        data.set("playerdata." + player, null);
        save();
    }

    @Override
    public Map<UUID, String> all() {
        Map<UUID, String> languages = new HashMap<>();
        ConfigurationSection section = data.getConfigurationSection("playerdata");
        if (section == null) return languages;
        for (String key : section.getKeys(false)) {
            String language = section.getString(key + ".language");
            if (language == null) continue;
            try {
                languages.put(UUID.fromString(key), language);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return languages;
    }

    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            try {
                CoolStuffLib.logError("Failed to save per-player language data", e);
            } catch (RuntimeException notSetUp) {
                LanguageManager.getLogger().log(Level.SEVERE, "Failed to save per-player language data", e);
            }
        }
    }
}
```

Replace the fields, constructors and methods of `PerPlayerLanguageHandler` (keep the class javadoc):
```java
    private final LanguageManager lgm;
    private final PlayerLanguageStore store;

    public PerPlayerLanguageHandler(LanguageManager lgm, File dataFile, FileConfiguration dataConfig) {
        this(lgm, new YamlPlayerLanguageStore(dataFile, dataConfig));
    }

    public PerPlayerLanguageHandler(LanguageManager lgm, PlayerLanguageStore store) {
        this.lgm = lgm;
        this.store = store;
    }

    public PlayerLanguageStore getStore() {
        return store;
    }

    public String getPlayerLanguageName(UUID uuid) {
        String language = store.get(uuid);
        return language == null ? lgm.getCurrentLangName() : language;
    }

    /** The player's language, or {@code null} when it is not set or no longer registered. */
    public LanguageFile getPlayerLanguage(UUID uuid) {
        String language = store.get(uuid);
        return language == null ? null : lgm.getLang(language, false);
    }

    public Map<UUID, LanguageFile> getPlayerLanguages() {
        Map<UUID, LanguageFile> languages = new HashMap<>();
        store.all().forEach((uuid, name) -> {
            LanguageFile language = lgm.getLang(name, false);
            if (language != null) languages.put(uuid, language);
        });
        return languages;
    }

    public void setPlayerLanguage(UUID uuid, String language) {
        if (lgm.getLang(language, false) == null) language = lgm.getCurrentLangName();
        store.set(uuid, language);
    }

    public void removePlayerLanguage(UUID uuid) {
        store.remove(uuid);
    }

    public void saveConfig() {
        if (store instanceof YamlPlayerLanguageStore yaml) yaml.save();
    }
```

- [ ] **Step 4: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=PlayerLanguageStoreTest+LanguageFallbackTest+LanguageManagerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager src/test/java/de/happybavarian07/coolstufflib/languagemanager/PlayerLanguageStoreTest.java
git commit -m "feat(language): player languages behind PlayerLanguageStore; no error without playerdata"
```

---

### Task 9: LanguageFileMigrator on top of the storage

**Files:**
- Modify: `languagemanager/LanguageFileMigrator.java`, `LanguageManager.createMigratorForLanguage`
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/LanguageFileMigratorTest.java`

**Interfaces:**
- Produces: `new LanguageFileMigrator(Map<String, Object> userValues, Map<String, Object> resourceValues, Consumer<Map<String, Object>> writer)`; the old `(File, InputStream)` constructor keeps working.

- [ ] **Step 1: Write the failing test**

```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LanguageFileMigratorTest {
    @Test
    void comparesValuesAndWritesTheSelectedOnes() {
        Map<String, Object> written = new HashMap<>();
        LanguageFileMigrator migrator = new LanguageFileMigrator(
                Map.of("A", "mine", "B", "same", "Old", "gone"),
                Map.of("A", "jar", "B", "same", "New", "fresh"),
                written::putAll);

        List<LanguageFileMigrator.MigrationEntry> entries = migrator.getMigrationEntries();
        assertEquals(LanguageFileMigrator.MigrationStatus.DIFFERENT_VALUE, status(entries, "A"));
        assertEquals(LanguageFileMigrator.MigrationStatus.UNCHANGED, status(entries, "B"));
        assertEquals(LanguageFileMigrator.MigrationStatus.MISSING_IN_USER, status(entries, "New"));
        assertEquals(LanguageFileMigrator.MigrationStatus.MISSING_IN_RESOURCE, status(entries, "Old"));
        assertTrue(migrator.filesDifferByHash());

        entries.stream().filter(e -> e.getKey().equals("New")).findFirst().orElseThrow().setSelectedForMigration(true);
        migrator.migrateSelected();
        assertEquals(Map.of("New", "fresh"), written);
    }

    private static LanguageFileMigrator.MigrationStatus status(List<LanguageFileMigrator.MigrationEntry> entries, String key) {
        return entries.stream().filter(e -> e.getKey().equals(key)).findFirst().orElseThrow().getStatus();
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageFileMigratorTest`
Expected: compilation error (no such constructor).

- [ ] **Step 3: Implement**

In `LanguageFileMigrator` replace the fields and constructor, `filesDifferByHash`, `scanForMigrations` and `migrateSelected`; keep `MigrationEntry`, `MigrationStatus`, `getMigrationEntries`, `editEntry` and the hash helpers:
```java
    private final File userConfigFile;
    private final InputStream resourceStream;
    private final Map<String, Object> userValues;
    private final Map<String, Object> resourceValues;
    private final Consumer<Map<String, Object>> writer;
    private final List<MigrationEntry> migrationEntries = new ArrayList<>();

    public LanguageFileMigrator(File userConfigFile, InputStream resourceStream) {
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userConfigFile);
        this.userConfigFile = userConfigFile;
        this.resourceStream = resourceStream;
        this.userValues = flattenConfig(userConfig);
        this.resourceValues = flattenConfig(YamlConfiguration.loadConfiguration(new InputStreamReader(resourceStream)));
        this.writer = changes -> {
            changes.forEach(userConfig::set);
            try {
                userConfig.save(userConfigFile);
            } catch (Exception ignored) {
            }
        };
        scanForMigrations();
    }

    /** Compares the owner's values with the jar's; {@code writer} receives the selected keys and values. */
    public LanguageFileMigrator(Map<String, Object> userValues, Map<String, Object> resourceValues, Consumer<Map<String, Object>> writer) {
        this.userConfigFile = null;
        this.resourceStream = null;
        this.userValues = new LinkedHashMap<>(userValues);
        this.resourceValues = new LinkedHashMap<>(resourceValues);
        this.writer = writer;
        scanForMigrations();
    }

    public boolean filesDifferByHash() {
        if (userConfigFile == null || resourceStream == null) return !userValues.equals(resourceValues);
        return !getFileHash(userConfigFile).equals(getStreamHash(resourceStream));
    }

    private void scanForMigrations() {
        migrationEntries.clear();
        for (Map.Entry<String, Object> resource : resourceValues.entrySet()) {
            String key = resource.getKey();
            Object userValue = userValues.get(key);
            if (!userValues.containsKey(key)) {
                migrationEntries.add(new MigrationEntry(key, null, resource.getValue(), MigrationStatus.MISSING_IN_USER));
            } else if (userValue != null && !userValue.equals(resource.getValue())) {
                migrationEntries.add(new MigrationEntry(key, userValue, resource.getValue(), MigrationStatus.DIFFERENT_VALUE));
            }
        }
        for (Map.Entry<String, Object> user : userValues.entrySet()) {
            String key = user.getKey();
            if (!resourceValues.containsKey(key)) {
                migrationEntries.add(new MigrationEntry(key, user.getValue(), null, MigrationStatus.MISSING_IN_RESOURCE));
            } else if (resourceValues.get(key).equals(user.getValue())) {
                migrationEntries.add(new MigrationEntry(key, user.getValue(), resourceValues.get(key), MigrationStatus.UNCHANGED));
            }
        }
    }

    private static Map<String, Object> flattenConfig(FileConfiguration config) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : config.getKeys(true)) {
            Object value = config.get(key);
            if (value != null && !(value instanceof ConfigurationSection)) map.put(key, value);
        }
        return map;
    }

    public void migrateSelected() {
        Map<String, Object> changes = new LinkedHashMap<>();
        for (MigrationEntry entry : migrationEntries) {
            if (entry.isSelectedForMigration() && entry.getResourceValue() != null) {
                changes.put(entry.getKey(), entry.getUserValue() != null ? entry.getUserValue() : entry.getResourceValue());
            }
        }
        if (!changes.isEmpty()) writer.accept(changes);
    }
```
Add `import java.util.function.Consumer;`. Replace `createMigratorForLanguage` in `LanguageManager`:
```java
    public LanguageFileMigrator createMigratorForLanguage(String langName) {
        LanguageConfig config = getLang(langName, true).getLangConfig();
        LoadResult loaded = config.getLoaded();
        return new LanguageFileMigrator(values(loaded.own()), values(loaded.defaults()), changes -> config.write(
                changes.entrySet().stream().map(e -> new LanguageEntry(e.getKey(), e.getValue(), null, null)).toList()));
    }

    private static Map<String, Object> values(Map<String, LanguageEntry> entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        entries.forEach((key, entry) -> values.put(key, entry.value()));
        return values;
    }
```
with imports `de.happybavarian07.coolstufflib.languagemanager.storage.LanguageEntry` and `...storage.LoadResult`. `createMigratorForLanguage` no longer needs `plugin.getResource`.

- [ ] **Step 4: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageFileMigratorTest+GoldenLanguageOutputTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib/languagemanager src/test/java/de/happybavarian07/coolstufflib/languagemanager/LanguageFileMigratorTest.java
git commit -m "feat(language): LanguageFileMigrator compares the owner's values with the jar through the storage"
```

---

### Task 10: Owner tools — key debug, find, missing, migrate and the `/lang` command

**Files:**
- Create: `languagemanager/LanguageCommandManager.java`
- Modify: `LanguageManager.java` (debug, `findKeys`, `missingKeys`, `migrateAll`, `enableCommands`/`getCommandName`, debug marker in `renderText`, `renderItem`, `renderTitle`), `CoolStuffLibBuilder.java` (`LanguageManagerBuilder`), `CoolStuffLib.java` (`setup`)
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/LanguageToolsTest.java`

**Interfaces:**
- Produces: `LanguageManager.toggleDebug(CommandSender) : boolean`, `isDebugging(@Nullable Player)`, `findKeys(String language, String text, int limit) : List<String>`, `missingKeys(String language) : MissingKeys` (`record MissingKeys(List<String> missingInFiles, List<String> unknownToPlugin)`), `migrateAll(String backendId) : List<MigrationReport>`, `enableCommands(String name)`, `getCommandName()`; builder `LanguageManagerBuilder.enableCommands(String)`, `watchFiles(boolean)`, `setBackend(String)`; `new LanguageCommandManager(LanguageManager, JavaPlugin, String name)`.

- [ ] **Step 1: Write the failing test**

```java
package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.storage.LegacyYamlBackend;
import de.happybavarian07.coolstufflib.languagemanager.storage.MigrationReport;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LanguageToolsTest {
    @TempDir
    Path folder;
    private LanguageManager lgm;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  Player:\n    Greeting: '&aHello there'\n    Bye: 'Bye'\n");
        lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "lang-fixtures/legacy", "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
    }

    @Test
    void debugShowsTheKeyAndItsOrigin() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        assertTrue(lgm.toggleDebug(console));
        assertTrue(lgm.getMessage("Player.Greeting", null, false).endsWith("[Messages.Player.Greeting @ en/messages/Player.yml:1]"));
        assertFalse(lgm.toggleDebug(console));
        assertFalse(lgm.getMessage("Player.Greeting", null, false).contains("["));
    }

    @Test
    void findsKeysByVisibleText() {
        assertEquals(List.of("Messages.Player.Greeting @ en/messages/Player.yml:1"), lgm.findKeys("en", "hello THERE", 10));
    }

    @Test
    void reportsKeysTheFilesDoNotKnow() {
        LanguageManager.MissingKeys missing = lgm.missingKeys("en");
        assertTrue(missing.unknownToPlugin().contains("Messages.Player.Bye"));
        assertTrue(missing.missingInFiles().isEmpty());
    }

    @Test
    void migratesAllLanguagesToAnotherBackend() {
        List<MigrationReport> reports = lgm.migrateAll(LegacyYamlBackend.ID);
        assertTrue(reports.stream().allMatch(MigrationReport::ok), reports.toString());
        assertTrue(Files.exists(folder.resolve("en.yml")));
        assertEquals("§aHello there", lgm.getMessage("Player.Greeting", null, false));
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageToolsTest`
Expected: compilation errors.

- [ ] **Step 3: Implement the tools in `LanguageManager`**

Fields and methods:
```java
    private final Set<UUID> debugPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile boolean consoleDebug;
    private volatile String commandName;

    public record MissingKeys(List<String> missingInFiles, List<String> unknownToPlugin) {
    }

    /** Turns the key display on or off for the sender; returns the new state. */
    public boolean toggleDebug(CommandSender sender) {
        if (sender instanceof Player player) {
            if (debugPlayers.remove(player.getUniqueId())) return false;
            debugPlayers.add(player.getUniqueId());
            return true;
        }
        consoleDebug = !consoleDebug;
        return consoleDebug;
    }

    public boolean isDebugging(@Nullable Player player) {
        return player == null ? consoleDebug : debugPlayers.contains(player.getUniqueId());
    }

    String debugMarked(String text, String fullPath, @Nullable Player player, String langName) {
        if (!isDebugging(player)) return text;
        LanguageFile language = getLang(langName, false);
        return text + " §8[" + fullPath + " @ " + (language == null ? "?" : language.getLangConfig().originOf(fullPath)) + "]";
    }

    /** Keys whose text (without colors) contains {@code text}, as {@code key @ file:line}. */
    public List<String> findKeys(String language, String text, int limit) {
        LanguageConfig config = getLang(language, true).getLangConfig();
        String needle = org.bukkit.ChatColor.stripColor(Utils.chat(text)).toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        for (Map.Entry<String, LanguageEntry> entry : config.getLoaded().merged().entrySet()) {
            if (!(entry.getValue().value() instanceof String value)) continue;
            if (org.bukkit.ChatColor.stripColor(Utils.chat(value)).toLowerCase(Locale.ROOT).contains(needle)) {
                hits.add(entry.getKey() + " @ " + config.originOf(entry.getKey()));
                if (hits.size() >= limit) break;
            }
        }
        return hits;
    }

    public MissingKeys missingKeys(String language) {
        LoadResult loaded = getLang(language, true).getLangConfig().getLoaded();
        List<String> missing = loaded.defaults().keySet().stream().filter(key -> !loaded.own().containsKey(key)).sorted().toList();
        List<String> unknown = loaded.own().keySet().stream()
                .filter(key -> !loaded.defaults().containsKey(key) && !SplitRule.HEADER_KEYS.contains(key)).sorted().toList();
        return new MissingKeys(missing, unknown);
    }

    /** Copies every language to {@code backendId} and uses it from now on (saved in the language folder). */
    public List<MigrationReport> migrateAll(String backendId) {
        LanguageBackend target = LanguageStorage.createBackend(backendId, langFolder);
        List<MigrationReport> reports = new ArrayList<>();
        for (LanguageFile language : registeredLanguages.values()) {
            LanguageBackend source = language.getLangConfig().getStorage().diskFor(language.getLangName());
            if (source.id().equals(target.id())) continue;
            reports.add(LanguageMigration.copy(source, target, language.getLangName()));
        }
        if (reports.stream().allMatch(MigrationReport::ok)) {
            LanguageStorage.useBackend(langFolder, backendId);
            registeredLanguages.values().forEach(language -> language.getLangConfig().reloadConfig());
        }
        return reports;
    }

    public void enableCommands(String name) {
        this.commandName = name;
    }

    public @Nullable String getCommandName() {
        return commandName;
    }
```
Imports: `de.happybavarian07.coolstufflib.languagemanager.storage.*`.

Hook the marker in:
- `renderText`: `return debugMarked(parseEmbeddedExpressions(message, player, langName), fullPath, player, langName);`
- `renderTitle`: `return debugMarked(parseEmbeddedExpressions(Utils.format(player, title, prefix), player, langName), "MenuTitles." + path, player, langName);`
- `renderItem`, before `item.setItemMeta(meta);`:
```java
        if (isDebugging(player)) {
            List<String> debugLore = new ArrayList<>(meta.getLore() == null ? List.of() : meta.getLore());
            debugLore.add("§8Items." + path + " @ " + langFile.getLangConfig().originOf("Items." + path + ".displayName"));
            meta.setLore(debugLore);
        }
```

- [ ] **Step 4: Create `LanguageCommandManager`**

```java
package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.commandmanagement.Argument;
import de.happybavarian07.coolstufflib.commandmanagement.CommandData;
import de.happybavarian07.coolstufflib.commandmanagement.CommandManager;
import de.happybavarian07.coolstufflib.commandmanagement.HelpCommand;
import de.happybavarian07.coolstufflib.languagemanager.storage.LanguageStorage;
import de.happybavarian07.coolstufflib.languagemanager.storage.MigrationReport;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

/** {@code /<name> reload|debug|find|missing|migrate} for server owners; enabled with {@code enableCommands(name)}. */
@CommandData
public class LanguageCommandManager extends CommandManager {
    private final LanguageManager languages;
    private final JavaPlugin plugin;
    private final String name;

    public LanguageCommandManager(LanguageManager languages, JavaPlugin plugin, String name) {
        this.languages = languages;
        this.plugin = plugin;
        this.name = name;
    }

    @Override
    public String getCommandName() {
        return name;
    }

    @Override
    public String getCommandUsage() {
        return "/" + name + " <reload|debug|find|missing|migrate>";
    }

    @Override
    public String getCommandInfo() {
        return "Tools for the language files";
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return plugin;
    }

    @Override
    public List<String> getCommandAliases() {
        return List.of();
    }

    @Override
    public String getCommandPermissionAsString() {
        return plugin.getName().toLowerCase(Locale.ROOT) + ".lang";
    }

    @Override
    public boolean autoRegisterPermission() {
        return true;
    }

    @Override
    public void setup() {
        String permission = getCommandPermissionAsString() + ".";
        registerSubCommand(new HelpCommand(getCommandName()));
        registerSubCommand(subCommand("reload").permission(permission + "reload").info("Reloads the language files")
                .executes((sender, args) -> {
                    languages.reloadLanguages(null, false);
                    List<String> report = languages.getLastReport();
                    send(sender, report.isEmpty() ? "&aReloaded, nothing to report." : "&aReloaded:");
                    report.forEach(line -> send(sender, "&7- " + line));
                    return true;
                }).build());
        registerSubCommand(subCommand("debug").permission(permission + "debug").info("Shows the key next to every text")
                .executes((sender, args) -> {
                    send(sender, languages.toggleDebug(sender) ? "&aKeys are shown now." : "&cKeys are hidden again.");
                    return true;
                }).build());
        registerSubCommand(subCommand("find").permission(permission + "find").info("Finds the key of a text you see in game")
                .arguments(Argument.text("text"))
                .executes((sender, args) -> {
                    String language = sender instanceof Player player
                            ? languages.getLangOrPlayerLang(true, languages.getCurrentLangName(), player).getLangName()
                            : languages.getCurrentLangName();
                    List<String> hits = languages.findKeys(language, args.joined(0), 10);
                    send(sender, hits.isEmpty() ? "&cNo text contains that." : "&aFound in " + language + ":");
                    hits.forEach(hit -> send(sender, "&7- " + hit));
                    return true;
                }).build());
        registerSubCommand(subCommand("missing").permission(permission + "missing").info("Lists keys missing in or unknown to the files")
                .arguments(Argument.custom("language", sender -> languages.getRegisteredLanguages().keySet()).optional())
                .executes((sender, args) -> {
                    String language = args.string(0, languages.getCurrentLangName());
                    LanguageManager.MissingKeys missing = languages.missingKeys(language);
                    send(sender, "&aMissing in the files of " + language + ": &f" + missing.missingInFiles().size());
                    missing.missingInFiles().stream().limit(20).forEach(key -> send(sender, "&7- " + key));
                    send(sender, "&aNot used by the plugin: &f" + missing.unknownToPlugin().size());
                    missing.unknownToPlugin().stream().limit(20).forEach(key -> send(sender, "&7- " + key));
                    return true;
                }).build());
        registerSubCommand(subCommand("migrate").permission(permission + "migrate").info("Moves all languages to another storage")
                .arguments(Argument.custom("backend", sender -> LanguageStorage.backendIds()))
                .confirm()
                .executes((sender, args) -> {
                    List<MigrationReport> reports = languages.migrateAll(args.string(0));
                    reports.forEach(report -> send(sender, (report.ok() ? "&a" : "&c") + report.summary()));
                    send(sender, reports.stream().allMatch(MigrationReport::ok) ? "&aNow using " + args.string(0) + "." : "&cNothing changed.");
                    return true;
                }).build());
    }

    private void send(CommandSender sender, String text) {
        sender.sendMessage(Utils.chat(text));
    }
}
```
`subCommand(String)` is the protected helper of `CommandManager` that returns a `SubCommandBuilder`; `confirm()` makes the sender run `migrate` twice.

- [ ] **Step 5: Builder and registration**

In `CoolStuffLibBuilder.LanguageManagerBuilder` add fields and methods, and apply them in `build()`:
```java
        private String commandName;
        private boolean watchFiles;
        private String backend;

        /** Registers {@code /<name>} with reload, debug, find, missing and migrate (needs the command manager). */
        public LanguageManagerBuilder enableCommands(String name) {
            this.commandName = name;
            return this;
        }

        /** Reloads a language a second after one of its files changed. */
        public LanguageManagerBuilder watchFiles(boolean watchFiles) {
            this.watchFiles = watchFiles;
            return this;
        }

        /** Storage for languages without a saved choice: "yaml-split" (default) or "yaml-legacy". */
        public LanguageManagerBuilder setBackend(String backend) {
            this.backend = backend;
            return this;
        }

        public CoolStuffLibBuilder build() {
            if (backend != null) LanguageStorage.setDefaultBackend(backend);
            File folder = languageFolder != null ? languageFolder : new File(parent.javaPluginUsingLib.getDataFolder(), "languages");
            parent.languageManager = new LanguageManager(parent.javaPluginUsingLib, folder, resourceDirectory, prefix);
            if (commandName != null) parent.languageManager.enableCommands(commandName);
            parent.languageManager.watchFiles(watchFiles);
            parent.usePlayerLangHandler = this.usePlayerLangHandler;
            return parent;
        }
```
(`watchFiles(boolean)` on `LanguageManager` is added in Task 11; until then add an empty `public void watchFiles(boolean watch) { }` and `public boolean isWatchingFiles() { return false; }` to `LanguageManager` so this compiles.) Import `de.happybavarian07.coolstufflib.languagemanager.storage.LanguageStorage`.

In `CoolStuffLib.setup()`, directly after the `if (commandManagerRegistry != null) { ... }` block:
```java
        if (languageManager != null && commandManagerRegistry != null && languageManager.getCommandName() != null) {
            commandManagerRegistry.register(new LanguageCommandManager(languageManager, javaPluginUsingLib, languageManager.getCommandName()));
        }
```

- [ ] **Step 6: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageToolsTest+GoldenLanguageOutputTest+MessageBuilderTest`
Expected: PASS. After the migration, `en/messages/Player.yml` starts with `Greeting: '&aHello there'` (line 1) and `Bye: 'Bye'` (line 2); the split file has no `Player:` line, which is why both tests expect `Player.yml:1`.

- [ ] **Step 7: Run the whole suite and commit**

Run: `mvn -B test -Dgpg.skip`
Expected: PASS.

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib src/test/java/de/happybavarian07/coolstufflib/languagemanager/LanguageToolsTest.java
git commit -m "feat(language): /lang command, key debug display, find, missing keys and backend migration"
```

---

### Task 11: File watcher

**Files:**
- Create: `languagemanager/LanguageFileWatcher.java`
- Modify: `LanguageManager.java` (`watchFiles`, `isWatchingFiles`, `startWatching`, `stopWatching`, `reloadChanged`, `shutdown`), `CoolStuffLib.java` (`setup`)
- Test: `src/test/java/de/happybavarian07/coolstufflib/languagemanager/LanguageFileWatcherTest.java`

**Interfaces:**
- Produces: `new LanguageFileWatcher(Path root, long debounceMillis, Consumer<Set<String>> onChange)` with `start()` and `close()`; `LanguageManager.startWatching()`, `stopWatching()`.

- [ ] **Step 1: Write the failing test**

```java
package de.happybavarian07.coolstufflib.languagemanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageFileWatcherTest {
    @TempDir
    Path folder;

    @Test
    void reportsTheChangedLanguageOnceAfterTheDebounce() throws Exception {
        Files.createDirectories(folder.resolve("en/messages"));
        CompletableFuture<Set<String>> changed = new CompletableFuture<>();
        try (LanguageFileWatcher watcher = new LanguageFileWatcher(folder, 200, changed::complete)) {
            watcher.start();
            Thread.sleep(200);
            Files.writeString(folder.resolve("en/messages/Player.yml"), "A: 'a'\n");
            Files.writeString(folder.resolve("en/messages/Player.yml"), "A: 'b'\n");
            assertEquals(Set.of("en"), changed.get(10, TimeUnit.SECONDS));
        }
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageFileWatcherTest`
Expected: compilation error.

- [ ] **Step 3: Implement the watcher**

```java
package de.happybavarian07.coolstufflib.languagemanager;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

/** Calls {@code onChange} with the changed language names once no file changed for {@code debounceMillis}. */
public final class LanguageFileWatcher implements AutoCloseable {
    private final Path root;
    private final long debounceMillis;
    private final Consumer<Set<String>> onChange;
    private final WatchService service;
    private final Thread thread;
    private volatile boolean running = true;

    public LanguageFileWatcher(Path root, long debounceMillis, Consumer<Set<String>> onChange) throws IOException {
        this.root = root;
        this.debounceMillis = debounceMillis;
        this.onChange = onChange;
        this.service = root.getFileSystem().newWatchService();
        this.thread = new Thread(this::loop, "CoolStuffLib-LanguageWatcher");
        this.thread.setDaemon(true);
    }

    public void start() throws IOException {
        try (Stream<Path> dirs = Files.walk(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) register(dir);
        }
        thread.start();
    }

    private void register(Path dir) throws IOException {
        Path relative = root.relativize(dir);
        if (relative.getNameCount() > 0 && relative.getName(0).toString().startsWith("_")) return;
        dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    }

    private void loop() {
        Set<String> pending = new HashSet<>();
        long last = 0;
        while (running) {
            WatchKey key;
            try {
                key = service.poll(Math.max(50, debounceMillis / 4), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ClosedWatchServiceException e) {
                return;
            }
            if (key != null) {
                Path dir = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (!(event.context() instanceof Path name)) continue;
                    Path changed = dir.resolve(name);
                    if (name.toString().endsWith(".tmp")) continue;
                    if (event.kind() == ENTRY_CREATE && Files.isDirectory(changed)) {
                        try {
                            register(changed);
                        } catch (IOException ignored) {
                        }
                    }
                    String language = languageOf(changed);
                    if (language != null) {
                        pending.add(language);
                        last = System.currentTimeMillis();
                    }
                }
                key.reset();
            } else if (!pending.isEmpty() && System.currentTimeMillis() - last >= debounceMillis) {
                Set<String> languages = Set.copyOf(pending);
                pending.clear();
                onChange.accept(languages);
            }
        }
    }

    private String languageOf(Path changed) {
        Path relative = root.relativize(changed);
        if (relative.getNameCount() == 0) return null;
        String first = relative.getName(0).toString();
        if (first.startsWith("_") || first.startsWith(".")) return null;
        if (relative.getNameCount() == 1) return first.endsWith(".yml") ? first.substring(0, first.length() - 4) : null;
        return first;
    }

    @Override
    public void close() {
        running = false;
        try {
            service.close();
        } catch (IOException ignored) {
        }
        thread.interrupt();
    }
}
```

- [ ] **Step 4: Wire it into `LanguageManager` and `CoolStuffLib`**

Replace the placeholder `watchFiles`/`isWatchingFiles` from Task 10:
```java
    private volatile boolean watchFiles;
    private LanguageFileWatcher watcher;

    public void watchFiles(boolean watch) {
        this.watchFiles = watch;
    }

    public boolean isWatchingFiles() {
        return watchFiles;
    }

    public synchronized void startWatching() {
        if (watcher != null) return;
        try {
            watcher = new LanguageFileWatcher(langFolder.toPath(), 1000, this::reloadChanged);
            watcher.start();
        } catch (IOException e) {
            watcher = null;
            getLogger().log(Level.WARNING, "Could not watch the language folder", e);
        }
    }

    public synchronized void stopWatching() {
        if (watcher != null) watcher.close();
        watcher = null;
    }

    /** Runs on the watcher thread: re-reads the changed languages, then updates variables on the main thread. */
    void reloadChanged(Set<String> languages) {
        for (String name : languages) {
            LanguageFile language = getLang(name, false);
            if (language != null) language.getLangConfig().reloadConfig();
        }
        Runnable afterReload = () -> {
            if (currentLang != null) handleVariablesSection(currentLang, true);
            logReports();
        };
        try {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, afterReload);
        } catch (RuntimeException noServer) {
            afterReload.run();
        }
    }
```
In `shutdown()`, add `stopWatching();` as the first statement inside the lambda.

In `CoolStuffLib.setup()`, after the `/lang` registration from Task 10:
```java
        if (languageManager != null && languageManager.isWatchingFiles()) {
            languageManager.startWatching();
        }
```

- [ ] **Step 5: Run the tests**

Run: `mvn -B test -Dgpg.skip -Dtest=LanguageFileWatcherTest+LanguageToolsTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git checkout -- TestOutputs
git add src/main/java/de/happybavarian07/coolstufflib src/test/java/de/happybavarian07/coolstufflib/languagemanager/LanguageFileWatcherTest.java
git commit -m "feat(language): reload a language a second after one of its files changed"
```

---

### Task 12: Server tests, AdminPanel, docs

**Files:**
- Create: `F:\InteliJ Programs\CoolStuffLibTest\src\main\java\de\happybavarian07\coolstufflibtest\suites\LanguageCoreSuite.java`
- Modify: CoolStuffLibTest `CoolStuffLibTest.java` (suite list), `LibraryBootstrap.java` (builder), `src/main/resources/languages/en.yml` (test keys)
- Modify: this repo's spec (`docs/superpowers/specs/2026-09-27-language-core-design.md`), `.claude/issues/AUDIT.md`
- Modify: wiki `F:\InteliJ Programs\GitBookWiki\coolstufflib\coolstufflib\guides\languages.md`, `...\migrating-to-3.md`

- [ ] **Step 1: Full suite and install**

```bash
mvn -B test -Dgpg.skip
git checkout -- TestOutputs
mvn -B -q install -DskipTests -Dgpg.skip
git checkout -- .flattened-pom.xml
```
Expected: all tests pass.

- [ ] **Step 2: Test keys for the server suite**

Add under `Messages:` → `Test:` in CoolStuffLibTest's `src/main/resources/languages/en.yml` (same indentation as the existing `Greeting` key):
```yaml
    Apples:
      one: '%count% apple'
      other: '%count% apples'
    Rich:
      text: 'Rich chat text'
      actionbar: '&eRich action bar'
```

- [ ] **Step 3: Enable the tools in the test plugin**

In `LibraryBootstrap`, extend the language builder chain:
```java
                .withLanguageManager()
                    .setResourceDirectory("languages")
                    .setPrefix("&8[&6CSLTest&8]&r")
                    .enablePlayerLanguageHandler()
                    .enableCommands("csllang")
                    .watchFiles(true)
                    .build()
```

- [ ] **Step 4: Write the suite**

```java
package de.happybavarian07.coolstufflibtest.suites;

import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflibtest.harness.FakePlayer;
import de.happybavarian07.coolstufflibtest.harness.TestContext;
import de.happybavarian07.coolstufflibtest.harness.TestSuite;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public final class LanguageCoreSuite implements TestSuite {
    @Override
    public String name() {
        return "language-core";
    }

    @Override
    public CompletableFuture<Void> run(TestContext ctx) throws Exception {
        LanguageManager lgm = ctx.lgm();
        File folder = lgm.getLangFolder();
        FakePlayer player = ctx.fakePlayer("LangCore");

        ctx.check("languages use the split layout", new File(folder, "en/language.yml").isFile(), folder.list());
        ctx.check("no old single file left", !new File(folder, "en.yml").exists(), folder.list());

        String local = lgm.message("Test.Target").with("%target%", "Alex").text(player.player());
        ctx.check("builder placeholder", local.contains("Alex"), local);
        ctx.check("builder leaves the global map alone", !lgm.getPlaceholders().containsKey("%target%"), lgm.getPlaceholders().keySet());
        ctx.checkEquals("plural one", "1 apple", lgm.message("Test.Apples").count(1).text(player.player()));
        ctx.checkEquals("plural other", "4 apples", lgm.message("Test.Apples").count(4).text(player.player()));
        ctx.checkEquals("rich entry text through the old API", "Rich chat text", lgm.getMessage("Test.Rich", player.player(), false));

        ctx.run("key debug", () -> {
            lgm.toggleDebug(Bukkit.getConsoleSender());
            String debug = lgm.getMessage("Test.Greeting", null, false);
            lgm.toggleDebug(Bukkit.getConsoleSender());
            ctx.check("shows key and file", debug.contains("[Messages.Test.Greeting @ en/messages/Test.yml:"), debug);
        });
        ctx.check("find by visible text", lgm.findKeys("en", "Hello", 5).stream().anyMatch(hit -> hit.startsWith("Messages.Test.Greeting")),
                lgm.findKeys("en", "Hello", 5));

        ctx.run("a broken file does not take the language down", () -> {
            File broken = new File(folder, "en/messages/Broken.yml");
            Files.writeString(broken.toPath(), "Broken: 'unclosed\n");
            try {
                lgm.getLang("en", true).getLangConfig().reloadConfig();
                ctx.check("other messages still work", lgm.getMessage("Test.Greeting", null, false).contains("Hello"),
                        lgm.getMessage("Test.Greeting", null, false));
                ctx.check("the problem is reported", lgm.getLang("en", true).getLangConfig().getLoaded().problems().stream()
                        .anyMatch(problem -> problem.file().endsWith("Broken.yml")), lgm.getLang("en", true).getLangConfig().getLoaded().problems());
            } finally {
                Files.deleteIfExists(broken.toPath());
                lgm.getLang("en", true).getLangConfig().reloadConfig();
            }
        });

        ctx.check("/csllang is registered", ctx.dispatch(Bukkit.getConsoleSender(), "csllang missing en"), "dispatch failed");
        return ctx.done();
    }
}
```
Add `.add(new LanguageCoreSuite())` after `.add(new LanguageSuite())` in `CoolStuffLibTest.java`.

- [ ] **Step 5: Run the server suite**

Run the existing script: `bash tools/run-server-tests.sh` in `F:\InteliJ Programs\CoolStuffLibTest`.
Expected: every check passes in both rounds, including the old `language` suite (per-player language, items, titles). On the first start the log shows "Moved en.yml into the folder en/" for `en` and `de`.

- [ ] **Step 6: Commit the test plugin**

```bash
git -C "F:/InteliJ Programs/CoolStuffLibTest" add -A src
git -C "F:/InteliJ Programs/CoolStuffLibTest" commit -m "test: language core suite (split layout, builders, plural, rich, debug, broken files, /csllang)"
```

- [ ] **Step 7: AdminPanel on the test server**

Build AdminPanel against the installed CoolStuffLib and run `tools/run-adminpanel-smoke.sh` in `C:\Users\quiri\IdeaProjects\AdminPanel`. Expected: exit 0; `plugins/Admin-Panel/languages/en/` and `de/` exist, `languages/_backup/<date>/en.yml` holds the old file, no `null path` in the log. Tell the user to check in game: start menu texts, `/perplayerlang de`, and the language migration menu (it now compares through the storage).

- [ ] **Step 8: Docs**

1. Spec: add a section "Changes during planning" with the two decisions from this plan's header, and remove L3 from the list of intended behaviour changes.
2. `.claude/issues/AUDIT.md`: L1 fixed (Task 8), L2 fixed (Task 5), L3 removed (was wrong: `addPlaceholder` replaces existing values), L4 fixed (Task 6).
3. Wiki `guides/languages.md`: a section "Where the files live" (split layout, `_backup`, `_backend.txt`), "Adding and changing texts" (owner files are never rewritten, new keys are added to the matching file), "Rich and plural entries", "`lgm.message(...)`", "`/lang` tools" (builder `enableCommands`, `watchFiles`, `setBackend`). First person, plain.
4. Wiki `migrating-to-3.md`: under "Things That Behave Differently" add "Language files move into folders" (automatic, backup, `getLangFile()` returns the folder, `LanguageCache` is unused); under "New Ways to Do Old Things" add `lgm.message(...)`.

- [ ] **Step 9: Commit docs**

```bash
git add docs/superpowers/specs/2026-09-27-language-core-design.md
git commit -m "docs: language core spec, decisions made while planning"
git -C "F:/InteliJ Programs/GitBookWiki" add coolstufflib
git -C "F:/InteliJ Programs/GitBookWiki" commit -m "docs(coolstufflib): language folders, rich entries, message builder and /lang tools"
```

---

## Self-Review Notes

- **Spec coverage:** storage and backends (Tasks 1–3), migration and `/lang migrate` (Tasks 4, 10), updates by text merge (Tasks 1, 2, 5), layers and atomic reload with per-file isolation (Task 5), entry kinds text/list/item/rich/plural (Tasks 6, 7), lookup order and `Messages.` optional (Task 6), new API (Task 7), old API wrappers (Tasks 5–7, golden test), per-player store (Task 8), `LanguageConfig#getConfig()` view and `saveConfig()` (Task 5), `LanguageFileMigrator` (Task 9), owner tools (Tasks 10, 11), start report (Task 5 `logReports`), tests and rollout (Task 12).
- **Deviations from the spec, on purpose:** no template compiler (see header); `LanguageBackend.write` returns problems instead of `void`, and takes section comments so migrated and added sections keep their comments.
- **Known limit:** `YamlDocument` understands block mappings, block lists and single-line scalars (the format of all current language files). Block scalars (`|`, `>`) and multi-line plain scalars are read by SnakeYAML correctly but cannot be edited in place; `set` on such a key replaces its lines as a whole.
