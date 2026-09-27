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
