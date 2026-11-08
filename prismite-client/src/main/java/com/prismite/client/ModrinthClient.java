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
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Public, unauthenticated Modrinth API client. */
public final class ModrinthClient {
    private static final String API = "https://api.modrinth.com/v2";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Project> search(String query, String gameVersion, String loader) throws IOException, InterruptedException {
        List<String> groups = new ArrayList<>();
        groups.add("[\"project_type:mod\"]");
        if (gameVersion != null && !gameVersion.isBlank()) groups.add("[\"versions:" + gameVersion + "\"]");
        if (loader != null && !loader.equalsIgnoreCase("vanilla")) groups.add("[\"categories:" + loader.toLowerCase() + "\"]");
        String facets = URLEncoder.encode("[" + String.join(",", groups) + "]", StandardCharsets.UTF_8);
        String url = API + "/search?limit=40&facets=" + facets + "&query=" + enc(query == null ? "" : query);
        JsonNode hits = getJson(url).path("hits");
        List<Project> result = new ArrayList<>();
        for (JsonNode hit : hits) {
            result.add(new Project(text(hit, "project_id"), text(hit, "slug"), text(hit, "title"),
                    text(hit, "description"), hit.path("downloads").asLong(0), text(hit, "author"),
                    text(hit, "icon_url"), stringList(hit.path("categories"))));
        }
        return result;
    }

    public ProjectDetails details(String projectId) throws IOException, InterruptedException {
        JsonNode node = getJson(API + "/project/" + encPath(projectId));
        return new ProjectDetails(text(node, "title"), text(node, "body"), text(node, "license"),
                text(node, "source_url"), text(node, "issues_url"), stringList(node.path("categories")));
    }

    /** Installs the best compatible release and its required Modrinth dependencies. */
    public List<Path> installProject(String projectId, String gameVersion, String loader, Path modsDirectory)
            throws IOException, InterruptedException {
        Files.createDirectories(modsDirectory);
        List<Path> installed = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        installRecursive(projectId, gameVersion, loader, modsDirectory, visited, installed, 0);
        return installed;
    }

    private void installRecursive(String projectId, String gameVersion, String loader, Path modsDirectory,
                                  Set<String> visited, List<Path> installed, int depth)
            throws IOException, InterruptedException {
        if (depth > 12 || !visited.add(projectId)) return;
        JsonNode versions = getJson(API + "/project/" + encPath(projectId) + "/version?game_versions="
                + enc("[\"" + gameVersion + "\"]") + "&loaders=" + enc("[\"" + loader.toLowerCase() + "\"]"));
        if (!versions.isArray() || versions.isEmpty()) {
            if (depth == 0) throw new IOException("No compatible release found for Minecraft " + gameVersion + " and " + loader + ".");
            return;
        }
        JsonNode chosenVersion = versions.get(0);
        JsonNode files = chosenVersion.path("files");
        JsonNode chosenFile = null;
        for (JsonNode file : files) if (file.path("primary").asBoolean(false)) { chosenFile = file; break; }
        if (chosenFile == null && !files.isEmpty()) chosenFile = files.get(0);
        if (chosenFile == null) throw new IOException("Compatible release has no downloadable file.");

        String filename = text(chosenFile, "filename");
        String url = text(chosenFile, "url");
        installed.add(download(new Downloadable(filename, url), modsDirectory));

        JsonNode dependencies = chosenVersion.path("dependencies");
        for (JsonNode dependency : dependencies) {
            if ("required".equalsIgnoreCase(text(dependency, "dependency_type"))) {
                String depProject = text(dependency, "project_id");
                if (!depProject.isBlank()) installRecursive(depProject, gameVersion, loader, modsDirectory, visited, installed, depth + 1);
            }
        }
    }

    private Path download(Downloadable file, Path modsDirectory) throws IOException, InterruptedException {
        if (!file.url().startsWith("https://")) throw new IOException("Mod download URL must use HTTPS");
        String lower = file.filename().toLowerCase();
        if (!(lower.endsWith(".jar") || lower.endsWith(".zip"))) throw new IOException("Unsupported mod file type");
        Path safeDirectory = modsDirectory.toAbsolutePath().normalize();
        Path destination = safeDirectory.resolve(Path.of(file.filename()).getFileName()).normalize();
        if (!destination.getParent().equals(safeDirectory)) throw new IOException("Invalid download filename");
        HttpRequest request = HttpRequest.newBuilder(URI.create(file.url())).timeout(Duration.ofMinutes(2))
                .header("User-Agent", "Prismite/0.2.0 (local mod manager)").GET().build();
        HttpResponse<Path> response = http.send(request, HttpResponse.BodyHandlers.ofFile(destination));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(destination);
            throw new IOException("Download failed with HTTP " + response.statusCode());
        }
        return destination;
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Prismite/0.2.0 (local mod manager)").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("Modrinth API returned HTTP " + response.statusCode());
        return mapper.readTree(response.body());
    }

    private static List<String> stringList(JsonNode array) {
        List<String> result = new ArrayList<>();
        if (array.isArray()) for (JsonNode item : array) result.add(item.asText());
        return result;
    }
    private static String text(JsonNode node, String key) { return node.path(key).asText(""); }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String encPath(String value) { return enc(value); }

    public record Project(String id, String slug, String title, String description, long downloads,
                          String author, String iconUrl, List<String> categories) {
        @Override public String toString() { return title + " · " + downloads + " downloads · by " + author; }
    }
    public record ProjectDetails(String title, String body, String license, String sourceUrl, String issuesUrl, List<String> categories) {}
    public record Downloadable(String filename, String url) {}
}
