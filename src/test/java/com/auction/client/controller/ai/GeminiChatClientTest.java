package com.auction.client.controller.ai;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiChatClientTest {
    @Test
    void constructorApiKeyAndMissingKeyPathWork() {
        GeminiChatClient missing = new GeminiChatClient("", null);
        GeminiChatClient configured = new GeminiChatClient("key", "model-x");

        assertFalse(missing.hasApiKey());
        assertTrue(configured.hasApiKey());
        assertThrows(IllegalStateException.class, () -> missing.sendMessage("hello", chunk -> {}));
    }

    @Test
    void privateJsonHelpersBuildEscapeAndExtractText() throws Exception {
        GeminiChatClient client = new GeminiChatClient("key", "model-x");

        String request = (String) invoke(client, "buildRequestBody",
                new Class<?>[] {String.class, String.class},
                "hello \"world\"\nline", "ctx");
        assertTrue(request.contains("ctx\\n\\nCau hoi cua nguoi dung"));
        assertTrue(request.contains("\\\"world\\\""));

        String escaped = (String) invoke(client, "escapeJson",
                new Class<?>[] {String.class},
                "\"slash\\\n\t\u0001");
        assertEquals("\\\"slash\\\\\\n\\t\\u0001", escaped);

        String extracted = (String) invoke(client, "extractText",
                new Class<?>[] {String.class},
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Xin chao\\n\"},{\"text\":\"A\\u0042\"}]}}]}");
        assertEquals("Xin chao\nAB", extracted);

        String noText = (String) invoke(client, "extractText", new Class<?>[] {String.class}, "{\"x\":1}");
        assertEquals("", noText);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = GeminiChatClient.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
