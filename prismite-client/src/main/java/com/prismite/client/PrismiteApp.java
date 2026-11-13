package com.prismite.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        PrismiteApp app = new PrismiteApp();
        Scene scene = new Scene(app.getRoot(), 1100, 760);
        stage.setTitle("Prismite");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class PrismiteApp {
    private final BorderPane root = new BorderPane();
    private final ListView<String> instanceList = new ListView<>();
    private final ListView<String> modList = new ListView<>();
    private final ListView<String> searchList = new ListView<>();
    private final TextArea log = new TextArea();
    private final TextField searchField = new TextField();
    private final ComboBox<String> versionBox = new ComboBox<>();
    private final ComboBox<String> loaderBox = new ComboBox<>();
    private final Label status = new Label("Ready");
    private final HttpClient http = HttpClient.newHttpClient();
    private final Path home = Path.of(System.getProperty("user.home"), ".prismite");
    private final Path propsFile = home.resolve("instances.properties");
    private final Map<String, InstanceProfile> instances = new LinkedHashMap<>();
    private String selectedInstanceId;

    public PrismiteApp() {
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #0E1117;");

        Label title = new Label("Prismite");
        title.setTextFill(Color.web("#F5F7FF"));
        title.setFont(Font.font("Segoe UI", 32));

        Label subtitle = new Label("Local Minecraft mod manager");
        subtitle.setTextFill(Color.web("#B8C1D9"));

        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(0, 0, 12, 0));

        versionBox.getItems().addAll("1.21.1", "1.20.1", "1.19.2", "1.18.2");
        versionBox.setValue("1.21.1");
        loaderBox.getItems().addAll("fabric", "forge", "quilt", "vanilla");
        loaderBox.setValue("fabric");

        Button newInstanceBtn = styledButton("New instance", "#52D7FF", "#0E1117");
        Button importInstanceBtn = styledButton("Import folder", "#9A6BFF", "#FFFFFF");
        Button refreshBtn = styledButton("Refresh", "#303846", "#FFFFFF");
        Button addModBtn = styledButton("Add mod", "#FFD166", "#0E1117");
        Button removeModBtn = styledButton("Remove selected", "#C94B55", "#FFFFFF");
        Button openFolderBtn = styledButton("Open mods folder", "#4CAF50", "#FFFFFF");
        Button backupBtn = styledButton("Backup mods", "#52D7FF", "#0E1117");
        Button restoreBtn = styledButton("Restore backup", "#9A6BFF", "#FFFFFF");
        Button searchBtn = styledButton("Search Modrinth", "#9A6BFF", "#FFFFFF");
        Button installBtn = styledButton("Install selected", "#52D7FF", "#0E1117");

        newInstanceBtn.setOnAction(e -> createInstance());
        importInstanceBtn.setOnAction(e -> importFolder());
        refreshBtn.setOnAction(e -> refreshAll());
        addModBtn.setOnAction(e -> addLocalMod());
        removeModBtn.setOnAction(e -> removeSelectedMod());
        openFolderBtn.setOnAction(e -> openSelectedModsFolder());
        backupBtn.setOnAction(e -> backupMods());
        restoreBtn.setOnAction(e -> restoreBackup());
        searchBtn.setOnAction(e -> searchModrinth());
        installBtn.setOnAction(e -> installSelectedMod());

        instanceList.setPrefWidth(340);
        instanceList.setPrefHeight(200);
        instanceList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                String selected = instanceList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectedInstanceId = selected.substring(0, selected.indexOf(" | "));
                    refreshModsForSelectedInstance();
                }
            }
        });

        modList.setPrefHeight(240);
        searchList.setPrefHeight(240);
        log.setEditable(false);
        log.setWrapText(true);
        log.setPrefHeight(200);

        HBox topActions = new HBox(8, newInstanceBtn, importInstanceBtn, refreshBtn);
        HBox modActions = new HBox(8, addModBtn, removeModBtn, openFolderBtn, backupBtn, restoreBtn);
        HBox searchRow = new HBox(8, searchField, versionBox, loaderBox, searchBtn, installBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox left = new VBox(10,
                new Label("Instances"),
                instanceList,
                topActions,
                new Label("Mods"),
                modList,
                modActions
        );
        left.setPrefWidth(420);

        VBox right = new VBox(10,
                new Label("Install mods"),
                searchRow,
                searchList,
                log
        );
        right.setVgrow(log, Priority.ALWAYS);

        HBox main = new HBox(18, left, right);
        main.setAlignment(Pos.TOP_LEFT);
        root.setTop(header);
        root.setCenter(main);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(12, 0, 0, 0));

        initProfiles();
        refreshAll();
    }

    private void initProfiles() {
        try {
            Files.createDirectories(home);
            if (!Files.exists(propsFile)) {
                String defaultDir = defaultMinecraftDirectory().toString();
                saveProfile(new InstanceProfile("default", "Default", "1.21.1", "fabric", defaultDir));
            }
            loadProfiles();
        } catch (IOException e) {
            log.appendText("Failed to initialize profiles: " + e.getMessage() + System.lineSeparator());
        }
    }

    private Path defaultMinecraftDirectory() {
        String homePath = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String appData = System.getenv().getOrDefault("APPDATA", homePath);
            return Path.of(appData, ".minecraft");
        }
        if (os.contains("mac")) {
            return Path.of(homePath, "Library", "Application Support", "minecraft");
        }
        return Path.of(homePath, ".minecraft");
    }

    private void loadProfiles() throws IOException {
        Properties p = new Properties();
        if (Files.exists(propsFile)) {
            try (InputStream in = Files.newInputStream(propsFile)) {
                p.load(in);
            }
        }
        instances.clear();
        for (String key : p.stringPropertyNames()) {
            String value = p.getProperty(key);
            if (value == null || value.isBlank()) continue;
            String[] parts = value.split("\\|", -1);
            if (parts.length >= 4) {
                instances.put(key, new InstanceProfile(key, parts[0], parts[1], parts[2], parts[3]));
            }
        }
        if (instances.isEmpty()) {
            String id = "default";
            InstanceProfile profile = new InstanceProfile(id, "Default", "1.21.1", "fabric", defaultMinecraftDirectory().toString());
            saveProfile(profile);
            instances.put(id, profile);
        }
        selectedInstanceId = instances.keySet().iterator().next();
    }

    private void saveProfile(InstanceProfile profile) throws IOException {
        Files.createDirectories(home);
        Properties p = new Properties();
        if (Files.exists(propsFile)) {
            try (InputStream in = Files.newInputStream(propsFile)) {
                p.load(in);
            }
        }
        p.setProperty(profile.id(), profile.name() + "|" + profile.version() + "|" + profile.loader() + "|" + profile.directory());
        try (OutputStream out = Files.newOutputStream(propsFile)) {
            p.store(out, "Prismite instances");
        }
    }

    private void refreshAll() {
        refreshInstanceList();
        refreshModsForSelectedInstance();
    }

    private void refreshInstanceList() {
        List<String> items = new ArrayList<>();
        for (InstanceProfile p : instances.values()) {
            items.add(p.id() + " | " + p.name() + " | " + p.version() + " | " + p.loader());
        }
        instanceList.getItems().setAll(items);
        if (selectedInstanceId == null && !instances.isEmpty()) {
            selectedInstanceId = instances.keySet().iterator().next();
        }
    }

    private void refreshModsForSelectedInstance() {
        if (selectedInstanceId == null) {
            modList.getItems().clear();
            return;
        }
        InstanceProfile profile = instances.get(selectedInstanceId);
        if (profile == null) {
            modList.getItems().clear();
            return;
        }
        Path modsDir = profile.modsDir();
        try {
            Files.createDirectories(modsDir);
            List<String> names = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir)) {
                for (Path p : stream) {
                    String name = p.getFileName().toString();
                    if (name.endsWith(".jar") || name.endsWith(".zip")) {
                        names.add(name);
                    }
                }
            }
            names.sort(String::compareToIgnoreCase);
            modList.getItems().setAll(names);
            status.setText("Selected: " + profile.name() + " · mods: " + names.size());
        } catch (IOException e) {
            status.setText("Could not read mods folder");
            log.appendText("Error: " + e.getMessage() + System.lineSeparator());
        }
    }

    private void createInstance() {
        TextInputDialog dialog = new TextInputDialog("My pack");
        dialog.setTitle("Create a new instance");
        dialog.setHeaderText("New isolated instance");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            return;
        }

        String name = result.get().trim();
        String version = versionBox.getValue();
        String loader = loaderBox.getValue();

        String id = cleanId(name);
        Path dir = home.resolve("instances").resolve(id).resolve(".minecraft");
        try {
            Files.createDirectories(dir.resolve("mods"));
            Files.createDirectories(dir.resolve("config"));
            InstanceProfile profile = new InstanceProfile(id, name, version, loader, dir.toString());
            saveProfile(profile);
            instances.put(id, profile);
            selectedInstanceId = id;
            refreshInstanceList();
            refreshModsForSelectedInstance();
            log.appendText("Created instance: " + name + System.lineSeparator());
        } catch (IOException e) {
            log.appendText("Could not create instance: " + e.getMessage() + System.lineSeparator());
        }
    }

    private void importFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose an existing Minecraft folder");
        File selected = chooser.showDialog(root.getScene().getWindow());
        if (selected == null) return;

        TextInputDialog nameDialog = new TextInputDialog(selected.getName());
        nameDialog.setTitle("Import Minecraft folder");
        nameDialog.setHeaderText("Give this instance a name");
        nameDialog.setContentText("Instance name:");
        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isEmpty() || nameResult.get().isBlank()) return;

        String name = nameResult.get().trim();
        String id = cleanId(name);
        try {
            InstanceProfile profile = new InstanceProfile(id, name, versionBox.getValue(), loaderBox.getValue(), selected.toPath().toString());
            saveProfile(profile);
            instances.put(id, profile);
            selectedInstanceId = id;
            refreshInstanceList();
            refreshModsForSelectedInstance();
            log.appendText("Imported folder: " + selected.getAbsolutePath() + System.lineSeparator());
        } catch (IOException e) {
            log.appendText("Could not import folder: " + e.getMessage() + System.lineSeparator());
        }
    }

    private void addLocalMod() {
        InstanceProfile profile = getSelectedProfile();
        if (profile == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a mod file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Mod files", "*.jar", "*.zip"));
        File selected = chooser.showOpenDialog(root.getScene().getWindow());
        if (selected == null) return;

        try {
            Path target = profile.modsDir().resolve(selected.getName());
            Files.copy(selected.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            refreshModsForSelectedInstance();
            log.appendText("Added mod: " + selected.getName() + System.lineSeparator());
        } catch (IOException e) {
            log.appendText("Failed to add mod: " + e.getMessage() + System.lineSeparator());
        }
    }

    private void removeSelectedMod() {
        InstanceProfile profile = getSelectedProfile();
        String selected = modList.getSelectionModel().getSelectedItem();
        if (profile == null || selected == null) {
            log.appendText("Select a mod to remove first." + System.lineSeparator());
            return;
        }

        try {
            Files.deleteIfExists(profile.modsDir().resolve(selected));
            refreshModsForSelectedInstance();
            log.appendText("Removed mod: " + selected + System.lineSeparator());
        } catch (IOException e) {
            log.appendText("Could not remove mod: " + e.getMessage() + System.lineSeparator());
        }
    }

    private void openSelectedModsFolder() {
        InstanceProfile profile = getSelectedProfile();
        if (profile == null) return;
        try {
            Path modsDir = profile.modsDir();
            Files.createDirectories(modsDir);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(modsDir.toFile());
            } else {
                log.appendText("Opening folders is not supported on this system." + System.lineSeparator());
            }
        } catch (IOException e) {
            log.appendText("Could not open mods folder: " + e.getMessage() + System.lineSeparator());
        }
    }

    private void backupMods() {
        InstanceProfile profile = getSelectedProfile();
        if (profile == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save backup ZIP");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
        chooser.setInitialFileName(profile.name() + "-mods.zip");
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;

        try {
            Path backupPath = file.toPath();
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(backupPath))) {
                Path modsDir = profile.modsDir();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir)) {
                    for (Path p : stream) {
                        String name = p.getFileName().toString();
                        if (name.endsWith(".jar") || name.endsWith(".zip")) {
                            zip.putNextEntry(new ZipEntry("mods/" + name));
                            Files.copy(p, zip);
                            zip.closeEntry();
                        }
                    }
                }
            }
            log.appendText("Backup saved: " + backupPath + System.lineSeparator());
        } catch (IOException e) {
            log.appendText("Backup failed: " + e.getMessage() + System.lineSeparator());
        }
    }

    private void restoreBackup() {
        InstanceProfile profile = getSelectedProfile();
        if (profile == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a backup ZIP");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) return;

        try {
            int restored = 0;
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file.toPath()))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.startsWith("mods/") && (name.endsWith(".jar") || name.endsWith(".zip"))) {
                        String fileName = name.substring("mods/".length());
                        Path destination = profile.modsDir().resolve(fileName);
                        Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
                        restored++;
                    }
                }
            }
            refreshModsForSelectedInstance();
            log.appendText("Restored " + restored + " file(s) from backup." + System.lineSeparator());
        } catch (IOException e) {
            log.appendText("Restore failed: " + e.getMessage() + System.lineSeparator());
        }
    }

    private void searchModrinth() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        String version = versionBox.getValue();
        String loader = loaderBox.getValue();

        if (query.isBlank()) {
            searchList.getItems().clear();
            searchList.getItems().add("Type a mod name or keyword first");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String url = buildModrinthSearchUrl(query, version, loader);
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "Prismite/1.0")
                        .GET()
                        .build();

                HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    PlatformHelper.run(() -> searchList.getItems().setAll("Request failed: HTTP " + response.statusCode()));
                    return;
                }

                List<String> items = parseSearchResults(response.body());
                PlatformHelper.run(() -> {
                    searchList.getItems().setAll(items.isEmpty() ? List.of("No results found") : items);
                    status.setText("Search complete · Minecraft " + version + " · loader: " + loader);
                });
            } catch (Exception e) {
                PlatformHelper.run(() -> {
                    searchList.getItems().setAll("Search failed: " + e.getMessage());
                    log.appendText("Modrinth search error: " + e.getMessage() + System.lineSeparator());
                });
            }
        });
    }

    private String buildModrinthSearchUrl(String query, String version, String loader) throws UnsupportedEncodingException {
        String base = "https://api.modrinth.com/v2/search?limit=12&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        String facets = "[[\"project_type:mod\"]";
        if (version != null && !version.isBlank()) {
            facets += ", [\"versions:" + version + "\"]";
        }
        if (loader != null && !loader.isBlank() && !"vanilla".equalsIgnoreCase(loader)) {
            facets += ", [\"categories:" + loader.toLowerCase(Locale.ROOT) + "\"]";
        }
        facets += "]";
        return base + "&facets=" + URLEncoder.encode(facets, StandardCharsets.UTF_8);
    }

    private List<String> parseSearchResults(String json) {
        List<String> results = new ArrayList<>();
        int hitIndex = 0;
        while (true) {
            String key = "\"title\":";
            int titlePos = json.indexOf(key, hitIndex);
            if (titlePos == -1) break;
            int start = titlePos + key.length();
            int end = json.indexOf('"', start + 1);
            if (end == -1) break;
            String title = json.substring(start + 1, end - 1);

            int descPos = json.indexOf("\"description\":", start);
            String desc = "";
            if (descPos != -1) {
                int dStart = json.indexOf('"', descPos + 15);
                int dEnd = json.indexOf('"', dStart + 1);
                if (dStart != -1 && dEnd != -1) {
                    desc = json.substring(dStart + 1, dEnd - 1).replace("\\n", " ");
                }
            }
            results.add(title + " — " + desc);
            hitIndex = end + 1;
        }
        return results;
    }

    private void installSelectedMod() {
        String selected = searchList.getSelectionModel().getSelectedItem();
        if (selected == null || selected.startsWith("No results") || selected.startsWith("Search failed") || selected.startsWith("Type a mod name")) {
            log.appendText("Choose a mod from the Modrinth results first." + System.lineSeparator());
            return;
        }

        InstanceProfile profile = getSelectedProfile();
        if (profile == null) {
            log.appendText("Select an instance first." + System.lineSeparator());
            return;
        }

        String title = selected.split(" — ", 2)[0];
        CompletableFuture.runAsync(() -> {
            try {
                String projectParam = URLEncoder.encode(title, StandardCharsets.UTF_8);
                String version = versionBox.getValue();
                String loader = loaderBox.getValue();
                String url = "https://api.modrinth.com/v2/search?limit=1&query=" + projectParam + "&facets="
                        + URLEncoder.encode("[[\"project_type:mod\"],[\"versions:" + version + "\"],[\"categories:" + loader.toLowerCase(Locale.ROOT) + "\"]]", StandardCharsets.UTF_8);
                HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "Prismite/1.0").GET().build();
                HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("Modrinth request failed: HTTP " + response.statusCode());
                }

                String body = response.body();
                int projectIdPos = body.indexOf("\"project_id\":\"");
                if (projectIdPos == -1) {
                    throw new IOException("No project ID returned for " + title);
                }
                int start = projectIdPos + "\"project_id\":\"".length();
                int end = body.indexOf('"', start);
                String projectId = body.substring(start, end);

                String versionsUrl = "https://api.modrinth.com/v2/project/" + projectId + "/version?loaders=[\"" + loader.toLowerCase(Locale.ROOT) + "\"]&game_versions=[\"" + version + "\"]";
                HttpRequest versionsReq = HttpRequest.newBuilder(URI.create(versionsUrl)).header("User-Agent", "Prismite/1.0").GET().build();
                HttpResponse<String> versionsResponse = http.send(versionsReq, HttpResponse.BodyHandlers.ofString());
                if (versionsResponse.statusCode() < 200 || versionsResponse.statusCode() >= 300) {
                    throw new IOException("Version lookup failed: HTTP " + versionsResponse.statusCode());
                }

                String versionsBody = versionsResponse.body();
                int fileUrlPos = versionsBody.indexOf("\"url\":\"");
                int fileNamePos = versionsBody.indexOf("\"filename\":\"");
                if (fileUrlPos == -1 || fileNamePos == -1) {
                    throw new IOException("No downloadable file found for " + title + ". Try another version or loader.");
                }
                int urlStart = fileUrlPos + "\"url\":\"".length();
                int urlEnd = versionsBody.indexOf('"', urlStart);
                String fileUrl = versionsBody.substring(urlStart, urlEnd).replace("\\/", "/");

                int nameStart = fileNamePos + "\"filename\":\"".length();
                int nameEnd = versionsBody.indexOf('"', nameStart);
                String fileName = versionsBody.substring(nameStart, nameEnd);

                Path target = profile.modsDir().resolve(fileName);
                HttpRequest downloadReq = HttpRequest.newBuilder(URI.create(fileUrl)).header("User-Agent", "Prismite/1.0").GET().build();
                HttpResponse<InputStream> fileResponse = http.send(downloadReq, HttpResponse.BodyHandlers.ofInputStream());
                if (fileResponse.statusCode() < 200 || fileResponse.statusCode() >= 300) {
                    throw new IOException("Download failed: HTTP " + fileResponse.statusCode());
                }
                try (InputStream in = fileResponse.body()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                PlatformHelper.run(() -> {
                    log.appendText("Installed: " + fileName + System.lineSeparator());
                    refreshModsForSelectedInstance();
                });
            } catch (Exception e) {
                PlatformHelper.run(() -> log.appendText("Install failed: " + e.getMessage() + System.lineSeparator()));
            }
        });
    }

    private InstanceProfile getSelectedProfile() {
        if (selectedInstanceId == null) return null;
        return instances.get(selectedInstanceId);
    }

    private static String cleanId(String value) {
        String cleaned = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return cleaned.replaceAll("^-|-$", "");
    }

    private static Button styledButton(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-font-weight: bold;");
        return b;
    }

    public BorderPane getRoot() {
        return root;
    }

    private static final class InstanceProfile {
        private final String id;
        private final String name;
        private final String version;
        private final String loader;
        private final String directory;

        InstanceProfile(String id, String name, String version, String loader, String directory) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.loader = loader;
            this.directory = directory;
        }

        String id() { return id; }
        String name() { return name; }
        String version() { return version; }
        String loader() { return loader; }
        String directory() { return directory; }

        Path modsDir() {
            try {
                Path dir = Path.of(directory).resolve("mods");
                Files.createDirectories(dir);
                return dir;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class PlatformHelper {
        static void run(Runnable action) {
            javafx.application.Platform.runLater(action);
        }
    }
}

