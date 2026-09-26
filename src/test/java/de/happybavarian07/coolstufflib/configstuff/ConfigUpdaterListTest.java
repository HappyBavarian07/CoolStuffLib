package de.happybavarian07.coolstufflib.configstuff;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigUpdaterListTest {
    @Test
    void listEntriesWithQuotesAndBackslashesStayValidYaml() {
        List<String> lines = List.of("plain", "e.g. '\"AdminPanel.open\"'", "C:\\path\\file", "it's");
        Yaml yaml = new Yaml();

        String written = ConfigUpdater.getListAsString(lines, "Help", "", yaml);
        Map<String, Object> read = yaml.load(written);

        assertEquals(lines, read.get("Help"));
    }
}
