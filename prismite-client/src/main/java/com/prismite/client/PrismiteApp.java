package com.prismite.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrismiteApp {
    private final BorderPane root = new BorderPane();
    private final InstanceStore instances = new InstanceStore();
    private final ModrinthClient modrinth = new ModrinthClient();
    private final ModBackupService backups = new ModBackupService();
    private final ModMetadataReader metadata = new ModMetadataReader();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "prismite-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final ComboBox<InstanceStore.InstanceProfile> instanceChoice = new ComboBox<>();
    private final ListView<String> installedMods = new ListView<>();
    private final ListView<ModrinthClient.Project> searchResults = new ListView<>();
    private final TextField searchField = new TextField();
    private final TextArea log = new TextArea();
    private final Label status = new Label("Local-only • no accounts or sign-in");

    public PrismiteApp() {
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0E1117;");

        Label title = new Label("Prismite");
        title.setTextFill(Color.web("#F5F7FF"));
        title.setFont(Font.font("Segoe UI", 30));
        Label subtitle = new Label("Local Minecraft mod manager · Build brighter worlds");
        subtitle.setTextFill(Color.web("#B8C1D9"));
        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(0, 0, 14, 0));

        instanceChoice.setPrefWidth(420);
        instanceChoice.setPromptText("Select an instance");
        instanceChoice.setOnAction(e -> refreshInstalledMods());

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(createInstancesTab(), createModsTab(), createBackupTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        status.setTextFill(Color.web("#B8C1D9"));
        status.setPadding(new Insets(10, 0, 0, 0));
        root.setTop(header);
        root.setCenter(tabs);
        root.setBottom(status);
        reloadInstances();
    }

    public BorderPane getRoot() { return root; }

    private Tab createInstancesTab() {
        VBox panel = panel();
        Label heading = heading("Instances");
        Label explanation = new Label("Create isolated mod folders, or import an existing Minecraft game directory.");
        explanation.setTextFill(Color.web("#B8C1D9"));
        Button create = button("Create isolated instance", "#52D7FF", "#0E1117");
        Button importExisting = button("Import existing folder", "#9A6BFF", "#FFFFFF");
        Button openGameDir = button("Open game folder", "#303846", "#FFFFFF");
        HBox actions = new HBox(10, create, importExisting, openGameDir);
        actions.setAlignment(Pos.CENTER_LEFT);
        create.setOnAction(e -> showInstanceDialog(false));
        importExisting.setOnAction(e -> showInstanceDialog(true));
        openGameDir.setOnAction(e -> withInstance(profile -> openDirectory(Path.of(profile.gameDirectory()))));
        panel.getChildren().addAll(heading, explanation, instanceChoice, actions,
                new Label("Each new instance is stored under ~/.prismite/instances and is separate from your official game folder."));
        styleLabels(panel);
        return tab("Instances", panel);
    }

    private Tab createModsTab() {
        VBox panel = panel();
        Label heading = heading("Installed mods and Modrinth");
        Label compatibility = new Label("Search results are filtered to the selected instance's Minecraft version and loader when installing.");
        compatibility.setTextFill(Color.web("#B8C1D9"));
        Button chooseFolder = button("Choose game folder", "#303846", "#FFFFFF");
        Button addLocal = button("Add local mod file", "#FFD166", "#0E1117");
        Button remove = button("Remove selected", "#C94B55", "#FFFFFF");
        Button refresh = button("Refresh", "#303846", "#FFFFFF");
        HBox localActions = new HBox(8, chooseFolder, addLocal, remove, refresh);
        chooseFolder.setOnAction(e -> importFolderForSelected());
        addLocal.setOnAction(e -> addLocalMod());
        remove.setOnAction(e -> removeSelectedMod());
        refresh.setOnAction(e -> refreshInstalledMods());
        installedMods.setPlaceholder(new Label("Choose or create an instance to see its mods."));
        VBox.setVgrow(installedMods, javafx.scene.layout.Priority.ALWAYS);

        searchField.setPromptText("Search Modrinth mods (public API, no account)");
        Button search = button("Search", "#9A6BFF", "#FFFFFF");
        Button install = button("Install compatible version", "#52D7FF", "#0E1117");
        HBox searchActions = new HBox(8, searchField, search, install);
        HBox.setHgrow(searchField, javafx.scene.layout.Priority.ALWAYS);
        searchResults.setPlaceholder(new Label("Search Modrinth to find mods."));
        VBox.setVgrow(searchResults, javafx.scene.layout.Priority.ALWAYS);
        search.setOnAction(e -> searchModrinth());
        searchField.setOnAction(e -> searchModrinth());
        install.setOnAction(e -> installSelectedMod());
        panel.getChildren().addAll(heading, instanceChoice, compatibility, localActions,
                new Label("Installed files"), installedMods, new Separator(), new Label("Browse Modrinth"), searchActions, searchResults);
        styleLabels(panel);
        return tab("Mods", panel);
    }

    private Tab createBackupTab() {
        VBox panel = panel();
        Label heading = heading("Backup and restore");
        Label explanation = new Label("Backups contain only .jar and .zip files from the selected instance's mods folder.");
        explanation.setTextFill(Color.web("#B8C1D9"));
        Button backup = button("Back up mods…", "#52D7FF", "#0E1117");
        Button restore = button("Restore backup…", "#9A6BFF", "#FFFFFF");
        Button detect = button("Detect loader from folder", "#303846", "#FFFFFF");
        backup.setOnAction(e -> backupMods());
        restore.setOnAction(e -> restoreMods());
        detect.setOnAction(e -> detectLoader());
        panel.getChildren().addAll(heading, explanation, instanceChoice, new HBox(10, backup, restore, detect), log);
        VBox.setVgrow(log, javafx.scene.layout.Priority.ALWAYS);
        log.setEditable(false);
        log.setWrapText(true);
        return tab("Backup / tools", panel);
    }

    private void showInstanceDialog(boolean importing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(importing ? "Import Minecraft folder" : "Create instance");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField name = new TextField(importing ? "Imported Minecraft" : "New instance");
        TextField version = new TextField("1.21.1");
        ComboBox<String> loader = new ComboBox<>();
        loader.getItems().addAll("vanilla", "fabric", "forge", "neoforge", "quilt");
        loader.setValue("fabric");
        VBox fields = new VBox(10, new Label("Instance name"), name, new Label("Minecraft version"), version, new Label("Loader"), loader);
        if (importing) fields.getChildren().add(new Label("You will choose the existing game directory after pressing OK."));
        dialog.getDialogPane().setContent(fields);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
            if (name.getText().isBlank() || version.getText().isBlank()) { message("Name and game version are required."); return; }
            if (importing) {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Choose game directory (.minecraft)");
                java.io.File selected = chooser.showDialog(window());
                if (selected == null) return;
                runTask(() -> instances.importDirectory(name.getText().trim(), version.getText().trim(), loader.getValue(), selected.toPath()),
                        profile -> { log("Imported " + profile.name()); reloadInstances(profile.id()); });
            } else {
                runTask(() -> instances.create(name.getText().trim(), version.getText().trim(), loader.getValue()),
                        profile -> { log("Created isolated instance " + profile.name()); reloadInstances(profile.id()); });
            }
        });
    }

    private void importFolderForSelected() {
        InstanceStore.InstanceProfile current = instanceChoice.getValue();
        if (current == null) { message("Create or import an instance first."); return; }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Minecraft game directory");
        java.io.File selected = chooser.showDialog(window());
        if (selected == null) return;
        runTask(() -> instances.importDirectory(current.name(), current.gameVersion(), current.loader(), selected.toPath()),
                profile -> { log("Imported folder as a new instance."); reloadInstances(profile.id()); });
    }

    private void reloadInstances() { reloadInstances(null); }
    private void reloadInstances(String selectId) {
        runTask(instances::list, profiles -> {
            instanceChoice.getItems().setAll(profiles);
            if (selectId != null) profiles.stream().filter(p -> p.id().equals(selectId)).findFirst().ifPresent(instanceChoice::setValue);
            else if (!profiles.isEmpty() && instanceChoice.getValue() == null) instanceChoice.setValue(profiles.get(0));
            refreshInstalledMods();
        });
    }

    private void refreshInstalledMods() {
        InstanceStore.InstanceProfile profile = instanceChoice.getValue();
        if (profile == null) { installedMods.getItems().clear(); return; }
        runTask(() -> {
            Path dir = instances.modsDirectory(profile);
            try (var files = Files.list(dir)) {
                return files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".jar") || p.toString().toLowerCase().endsWith(".zip"))
                        .sorted().map(metadata::describe).toList();
            }
        }, found -> installedMods.getItems().setAll(found));
    }

    private void addLocalMod() {
        InstanceStore.InstanceProfile profile = selectedInstance();
        if (profile == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select mod file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Mod archives", "*.jar", "*.zip"));
        java.io.File file = chooser.showOpenDialog(window());
        if (file == null) return;
        runTask(() -> {
            Path target = instances.modsDirectory(profile).resolve(file.getName());
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        }, path -> { log("Added " + path.getFileName()); refreshInstalledMods(); });
    }

    private void removeSelectedMod() {
        String selected = installedMods.getSelectionModel().getSelectedItem();
        InstanceStore.InstanceProfile profile = selectedInstance();
        if (selected == null || profile == null) { message("Select an installed mod first."); return; }
        String filename = selected.split("  ·  ", 2)[0];
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove " + filename + " from this instance?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(ButtonType.YES::equals).ifPresent(b -> runTask(() -> {
            Path file = instances.modsDirectory(profile).resolve(filename).normalize();
            if (!file.getParent().equals(instances.modsDirectory(profile).toAbsolutePath().normalize())) throw new IOException("Invalid mod path");
            return Files.deleteIfExists(file);
        }, removed -> { log(removed ? "Removed " + filename : "File was already removed."); refreshInstalledMods(); }));
    }

    private void searchModrinth() {
        status.setText("Searching Modrinth…");
        runTask(() -> modrinth.search(searchField.getText().trim()),
                projects -> { searchResults.getItems().setAll(projects); status.setText("Found " + projects.size() + " Modrinth projects."); });
    }

    private void installSelectedMod() {
        InstanceStore.InstanceProfile profile = selectedInstance();
        ModrinthClient.Project project = searchResults.getSelectionModel().getSelectedItem();
        if (profile == null || project == null) { message("Select an instance and a Modrinth project first."); return; }
        status.setText("Finding a compatible file for " + project.title() + "…");
        runTask(() -> {
            ModrinthClient.Downloadable file = modrinth.findCompatibleFile(project.id(), profile.gameVersion(), profile.loader());
            if (file == null) throw new IOException("No release found for Minecraft " + profile.gameVersion() + " and " + profile.loader() + ". Check the instance version/loader.");
            return modrinth.download(file, instances.modsDirectory(profile));
        }, path -> { status.setText("Installed " + path.getFileName()); log("Installed Modrinth file: " + path.getFileName()); refreshInstalledMods(); });
    }

    private void backupMods() {
        InstanceStore.InstanceProfile profile = selectedInstance();
        if (profile == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save mods backup");
        chooser.setInitialFileName(profile.name().replaceAll("[^A-Za-z0-9._-]", "_") + "-mods-backup.zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP archive", "*.zip"));
        java.io.File destination = chooser.showSaveDialog(window());
        if (destination == null) return;
        runTask(() -> { backups.backup(instances.modsDirectory(profile), destination.toPath()); return destination.toPath(); }, path -> log("Backup saved to " + path));
    }

    private void restoreMods() {
        InstanceStore.InstanceProfile profile = selectedInstance();
        if (profile == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Prismite mods backup");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP archive", "*.zip"));
        java.io.File archive = chooser.showOpenDialog(window());
        if (archive == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Restore mod files into " + profile.name() + "? Files with the same names will be replaced.", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(ButtonType.YES::equals).ifPresent(b -> runTask(() -> backups.restore(archive.toPath(), instances.modsDirectory(profile)), count -> { log("Restored " + count + " mod file(s)."); refreshInstalledMods(); }));
    }

    private void detectLoader() {
        InstanceStore.InstanceProfile profile = selectedInstance();
        if (profile == null) return;
        Path gameDir = Path.of(profile.gameDirectory());
        String detected = "unknown";
        if (Files.isDirectory(gameDir.resolve("mods"))) {
            try (var files = Files.list(gameDir.resolve("mods"))) {
                List<String> names = files.map(p -> p.getFileName().toString().toLowerCase()).toList();
                if (names.stream().anyMatch(n -> n.contains("fabric-api"))) detected = "fabric (inferred from fabric-api mod)";
                else if (names.stream().anyMatch(n -> n.contains("neoforge"))) detected = "neoforge (inferred from filename)";
                else if (names.stream().anyMatch(n -> n.contains("forge"))) detected = "forge (inferred from filename)";
                else if (names.stream().anyMatch(n -> n.contains("quilt"))) detected = "quilt (inferred from filename)";
            } catch (IOException ex) { message("Could not inspect mods: " + ex.getMessage()); return; }
        }
        message("Loader detection is best-effort based on installed mod filenames, not proof of the installed game loader. Result: " + detected + ". Current profile setting: " + profile.loader());
    }

    private void openDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir.toFile());
            else message("Opening folders is not supported on this desktop environment.");
        } catch (IOException ex) { message("Could not open folder: " + ex.getMessage()); }
    }

    private InstanceStore.InstanceProfile selectedInstance() {
        InstanceStore.InstanceProfile profile = instanceChoice.getValue();
        if (profile == null) message("Create or import an instance first.");
        return profile;
    }

    private void withInstance(java.util.function.Consumer<InstanceStore.InstanceProfile> action) {
        InstanceStore.InstanceProfile profile = selectedInstance();
        if (profile != null) action.accept(profile);
    }

    private <T> void runTask(CheckedSupplier<T> action, java.util.function.Consumer<T> success) {
        worker.submit(() -> {
            try {
                T result = action.get();
                Platform.runLater(() -> success.accept(result));
            } catch (Exception ex) {
                Platform.runLater(() -> { status.setText("Action failed"); log("Error: " + ex.getMessage()); message(ex.getMessage()); });
            }
        });
    }

    @FunctionalInterface private interface CheckedSupplier<T> { T get() throws Exception; }
    private Window window() { return root.getScene() == null ? null : root.getScene().getWindow(); }
    private void log(String message) { Platform.runLater(() -> log.appendText(message + System.lineSeparator())); }
    private void message(String text) { new Alert(Alert.AlertType.INFORMATION, text == null ? "Unexpected error" : text, ButtonType.OK).showAndWait(); }
    private VBox panel() { VBox box = new VBox(10); box.setPadding(new Insets(16)); box.setStyle("-fx-background-color: #171C24;"); return box; }
    private Tab tab(String title, VBox content) { Tab tab = new Tab(title); tab.setContent(content); return tab; }
    private Label heading(String text) { Label label = new Label(text); label.setTextFill(Color.web("#F5F7FF")); label.setFont(Font.font("Segoe UI", 21)); return label; }
    private Button button(String text, String background, String foreground) { Button b = new Button(text); b.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + foreground + "; -fx-font-weight: bold;"); return b; }
    private void styleLabels(VBox box) { for (var node : box.getChildren()) if (node instanceof Label label && label.getTextFill().equals(Color.BLACK)) label.setTextFill(Color.web("#F5F7FF")); }
}
