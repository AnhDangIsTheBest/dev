package com.auction.client.controller.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.function.Consumer;

public class GeminiChatClient {

    private static final String DEFAULT_MODEL = "gemini-flash-latest";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:streamGenerateContent?alt=sse";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public GeminiChatClient() {
        this(readApiKey(), readModel());
    }

    public GeminiChatClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void sendMessage(String userText, Consumer<String> onChunk) throws IOException, InterruptedException {
        sendMessage(userText, null, onChunk);
    }

    public void sendMessage(String userText, String requestContext, Consumer<String> onChunk) throws IOException, InterruptedException {
        if (!hasApiKey()) {
            throw new IllegalStateException("Missing GEMINI_API_KEY or -Dgemini.api.key");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(API_URL, model)))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(userText, requestContext), StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String error = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("Gemini API error " + response.statusCode() + ": " + error);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }

                String json = line.substring(5).trim();
                if (json.isBlank() || "[DONE]".equals(json)) {
                    continue;
                }

                String text = extractText(json);
                if (!text.isBlank()) {
                    onChunk.accept(text);
                }
            }
        }
    }

    private String buildRequestBody(String userText, String requestContext) {
        String text = userText;
        if (requestContext != null && !requestContext.isBlank()) {
            text = requestContext + "\n\nCau hoi cua nguoi dung: " + userText;
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":\"")
                .append(escapeJson(text))
                .append("\"}]}],\"generationConfig\":{\"temperature\":0.7}}");
        return json.toString();
    }

    private String extractText(String json) {
        StringBuilder result = new StringBuilder();
        int index = 0;
        while ((index = json.indexOf("\"text\"", index)) >= 0) {
            int colon = json.indexOf(':', index + 6);
            if (colon < 0) {
                break;
            }

            int start = json.indexOf('"', colon + 1);
            if (start < 0) {
                break;
            }

            ParseResult parsed = readJsonString(json, start + 1);
            result.append(parsed.value);
            index = parsed.nextIndex;
        }
        return result.toString();
    }

    private ParseResult readJsonString(String json, int start) {
        StringBuilder value = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '"') {
                break;
            }
            if (c == '\\' && i < json.length()) {
                char escaped = json.charAt(i++);
                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> {
                        if (i + 4 <= json.length()) {
                            String hex = json.substring(i, i + 4);
                            value.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                    }
                    default -> value.append(escaped);
                }
            } else {
                value.append(c);
            }
        }
        return new ParseResult(value.toString(), i);
    }

    private String escapeJson(String text) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String readApiKey() {
        String key = System.getProperty("gemini.api.key");
        if (key == null || key.isBlank()) {
            key = System.getenv("GEMINI_API_KEY");
        }
        if (key == null || key.isBlank()) {
            key = readApiKeyFromLocalFile();
        }
        return key;
    }

    private static String readApiKeyFromLocalFile() {
        return readLocalProperty("gemini.api.key");
    }

    private static String readModel() {
        String model = System.getProperty("gemini.model");
        if (model == null || model.isBlank()) {
            model = readLocalProperty("gemini.model");
        }
        return model == null || model.isBlank() ? DEFAULT_MODEL : model;
    }

    private static String readLocalProperty(String propertyName) {
        Path path = Path.of("gemini.properties");
        if (!Files.isRegularFile(path)) {
            return null;
        }

        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return properties.getProperty(propertyName);
        } catch (IOException e) {
            return null;
        }
    }

    private record ParseResult(String value, int nextIndex) {}
}
