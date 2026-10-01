package de.happybavarian07.coolstufflib.utils;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemUtilsTest {
    private static final String HASH = "1289d5b178626ea23d0b0c3d2df5c085e8375056bf685b5ed5bb477fe8472d94";
    private static final String URL = "http://textures.minecraft.net/texture/" + HASH;

    @Test
    void aHashBecomesATextureUrl() throws Exception {
        assertEquals("https://textures.minecraft.net/texture/" + HASH, ItemUtils.textureUrl(HASH).toString());
    }

    @Test
    void aFullUrlIsUsedAsItIs() throws Exception {
        assertEquals(URL, ItemUtils.textureUrl(URL).toString());
        assertEquals("https://example.com/skin.png", ItemUtils.textureUrl(" https://example.com/skin.png ").toString());
    }

    @Test
    void aBase64ValueGivesTheUrlInsideIt() throws Exception {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + URL + "\"}}}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        assertEquals(URL, ItemUtils.textureUrl(base64).toString());
    }

    @Test
    void aBase64ValueWithoutAnUrlIsRejected() {
        String base64 = Base64.getEncoder().encodeToString("{\"textures\":{}}".getBytes(StandardCharsets.UTF_8));
        assertThrows(MalformedURLException.class, () -> ItemUtils.textureUrl(base64));
        assertThrows(MalformedURLException.class, () -> ItemUtils.textureUrl("eyJ!!!"));
    }
}
