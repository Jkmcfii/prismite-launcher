package com.prismite.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class ModrinthClient {
    private static final String API = "https://api.modrinth.com/v2";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Project> search(String query) throws IOException, InterruptedException {
        String facets = URLEncoder.encode("[[\"project_type:mod\"]]", StandardCharsets.UTF_8);
        String url = API + "/search?limit=30&facets=" + facets + "&query=" + enc(query == null ? "" : query);
        JsonNode hits = getJson(url).path("hits");
        List<Project> result = new ArrayList<>();
        for (JsonNode hit : hits) {
            result.add(new Project(text(hit, "project_id"), text(hit, "title"), text(hit, "description"),
                    hit.path("downloads").asLong(0), text(hit, "author")));
        }
        return result;
    }

    public Downloadable findCompatibleFile(String projectId, String gameVersion, String loader) throws IOException, InterruptedException {
        String url = API + "/project/" + encPath(projectId) + "/version?game_versions="
                + enc("[\"" + gameVersion + "\"]") + "&loaders=" + enc("[\"" + loader.toLowerCase() + "\"]");
        JsonNode versions = getJson(url);
        if (!versions.isArray() || versions.isEmpty()) return null;
        JsonNode files = versions.get(0).path("files");
        JsonNode chosen = null;
        for (JsonNode file : files) {
            if (file.path("primary").asBoolean(false)) { chosen = file; break; }
        }
        if (chosen == null && !files.isEmpty()) chosen = files.get(0);
        if (chosen == null) return null;
        return new Downloadable(text(chosen, "filename"), text(chosen, "url"));
    }

    public Path download(Downloadable file, Path modsDirectory) throws IOException, InterruptedException {
        if (!file.url().startsWith("https://")) throw new IOException("Mod download URL must use HTTPS");
        String lower = file.filename().toLowerCase();
        if (!(lower.endsWith(".jar") || lower.endsWith(".zip"))) throw new IOException("Modrinth returned an unsupported file type");
        Files.createDirectories(modsDirectory);
        Path destination = modsDirectory.resolve(Path.of(file.filename()).getFileName()).normalize();
        if (!destination.getParent().equals(modsDirectory.toAbsolutePath().normalize())) {
            throw new IOException("Invalid download filename");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(file.url())).timeout(Duration.ofMinutes(2))
                .header("User-Agent", "Prismite/0.1.0 (local mod manager)").GET().build();
        HttpResponse<Path> response = http.send(request, HttpResponse.BodyHandlers.ofFile(destination));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(destination);
            throw new IOException("Download failed with HTTP " + response.statusCode());
        }
        return destination;
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Prismite/0.1.0 (local mod manager)").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Modrinth API returned HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    private static String text(JsonNode node, String key) { return node.path(key).asText(""); }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String encPath(String value) { return enc(value); }

    public record Project(String id, String title, String description, long downloads, String author) {
        @Override public String toString() { return title + " · " + downloads + " downloads · by " + author; }
    }
    public record Downloadable(String filename, String url) {}
}
