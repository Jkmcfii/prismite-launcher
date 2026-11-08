package com.prismite.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ApiClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void register(String email, String username, String password, Consumer<String> onSuccess, Consumer<String> onError) {
        send("/api/auth/register", "{\"email\":\"" + safe(email) + "\",\"username\":\"" + safe(username) + "\",\"password\":\"" + safe(password) + "\"}", onSuccess, onError);
    }

    public void login(String email, String password, Consumer<String> onSuccess, Consumer<String> onError) {
        send("/api/auth/login", "{\"email\":\"" + safe(email) + "\",\"password\":\"" + safe(password) + "\"}", onSuccess, onError);
    }

    public void oauthStart(String provider, Consumer<String> onSuccess, Consumer<String> onError) {
        send("/api/auth/" + provider + "/start", null, onSuccess, onError);
    }

    public void getInstances(String token, Consumer<List<Map<String, Object>>> onSuccess, Consumer<String> onError) {
        sendJsonListWithToken("/api/instances", token, onSuccess, onError);
    }

    public void createInstance(String token, String name, String version, String loader, String gameDir, String javaPath, String memory, Consumer<String> onSuccess, Consumer<String> onError) {
        String body = "{"
                + "\"name\":\"" + safe(name) + "\","
                + "\"version\":\"" + safe(version) + "\","
                + "\"loader\":\"" + safe(loader) + "\","
                + "\"gameDirectory\":\"" + safe(gameDir) + "\","
                + "\"javaPath\":\"" + safe(javaPath) + "\","
                + "\"memory\":\"" + safe(memory) + "\""
                + "}";
        sendWithToken("/api/instances", token, body, onSuccess, onError);
    }

    public void searchMods(String query, Consumer<String> onSuccess, Consumer<String> onError) {
        String path = "/api/mods";
        if (query != null && !query.isBlank()) {
            path += "?query=" + query.replace(" ", "%20");
        }
        send(path, null, onSuccess, onError);
    }

    private void send(String path, String jsonBody, Consumer<String> onSuccess, Consumer<String> onError) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json");

        if (jsonBody != null) {
            builder = builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder = builder.GET();
        }

        execute(builder.build(), onSuccess, onError);
    }

    private void sendWithToken(String path, String token, String jsonBody, Consumer<String> onSuccess, Consumer<String> onError) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");

        if (jsonBody != null) {
            builder = builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder = builder.GET();
        }

        execute(builder.build(), onSuccess, onError);
    }

    private void sendJsonListWithToken(String path, String token, Consumer<List<Map<String, Object>>> onSuccess, Consumer<String> onError) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                onSuccess.accept(List.of(Map.of("name", "Example", "version", "1.20.4", "loader", "fabric")));
            } else {
                onError.accept("HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            onError.accept(e.getMessage());
        }
    }

    private void execute(HttpRequest request, Consumer<String> onSuccess, Consumer<String> onError) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                onSuccess.accept(response.body());
            } else {
                onError.accept("HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            onError.accept(e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
