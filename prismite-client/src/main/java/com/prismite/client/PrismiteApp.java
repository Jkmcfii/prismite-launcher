package com.prismite.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrismiteApp {
    private final BorderPane root = new BorderPane();
    private final InstanceStore instances = new InstanceStore();
    private final ModrinthClient modrinth = new ModrinthClient();
    private final ModBackupService backups = new ModBackupService();
    private final ModMetadataReader metadata = new ModMetadataReader();
    private final ExecutorService worker = Executors.newFixedThreadPool(3, r -> { Thread t = new Thread(r, "prismite-worker"); t.setDaemon(true); return t; });
    private final ComboBox<InstanceStore.InstanceProfile> instanceChoice = new ComboBox<>();
    private final ListView<String> installedMods = new ListView<>();
    private final ListView<ModrinthClient.Project> searchResults = new ListView<>();
    private final TextField searchField = new TextField();
    private final TextArea activity = new TextArea();
    private final Label status = new Label("Starting Prismite…");
    private final ComboBox<String> loaderFilter = new ComboBox<>();
    private final ProgressIndicator progress = new ProgressIndicator();

    public PrismiteApp() {
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #0E1117;");
        Label title = new Label("Prismite");
        title.setTextFill(Color.web("#F5F7FF"));
        title.setFont(Font.font("Segoe UI", 30));
        Label subtitle = new Label("Discover, install, and manage Minecraft mods");
        subtitle.setTextFill(Color.web("#B8C1D9"));
        VBox header = new VBox(3, title, subtitle);
        header.setPadding(new Insets(0, 0, 12, 0));

        instanceChoice.setPrefWidth(420);
        instanceChoice.setPromptText("Select instance");
        instanceChoice.setOnAction(e -> refreshInstalledMods());
        status.setTextFill(Color.web("#B8C1D9"));
        progress.setMaxSize(22, 22);
        progress.setVisible(false);
        HBox footer = new HBox(10, progress, status);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(createBrowseTab(), createLibraryTab(), createBackupTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setTop(header);
        root.setCenter(tabs);
        root.setBottom(footer);
        loadOrCreateStarterInstance();
    }

    public BorderPane getRoot() { return root; }

    private Tab createBrowseTab() {
        VBox panel = panel();
        Label heading = heading("Discover mods");
        Label info = muted("Browse Modrinth without an account. Select an instance to filter by Minecraft version and loader.");
        HBox instanceBar = new HBox(10, new Label("Install into:"), instanceChoice,
                new Label("Loader:"), loaderFilter);
        instanceBar.setAlignment(Pos.CENTER_LEFT);
        loaderFilter.getItems().addAll("Use instance loader", "fabric", "forge", "neoforge", "quilt", "fabric (server)");
        loaderFilter.setValue("Use instance loader");
        searchField.setPromptText("Search mods, e.g. Sodium, minimap, performance…");
        Button search = button("Search", "#9A6BFF", "#FFFFFF");
        Button install = button("Install selected mod", "#52D7FF", "#0E1117");
        HBox searchRow = new HBox(8, searchField, search, install);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchResults.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(ModrinthClient.Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) { setText(null); setGraphic(null); return; }
                Label name = new Label(project.title());
                name.setTextFill(Color.web("#F5F7FF"));
                name.setFont(Font.font("Segoe UI", 15));
                Label description = new Label(project.description());
                description.setTextFill(Color.web("#B8C1D9"));
                description.setWrapText(true);
                Label meta = new Label(project.author() + " · " + project.downloads() + " downloads · " + String.join(", ", project.categories()));
                meta.setTextFill(Color.web("#52D7FF"));
                VBox card = new VBox(4, name, description, meta);
                card.setPadding(new Insets(8));
                setGraphic(card);
            }
        });
        searchResults.setPlaceholder(muted("Search to browse compatible Modrinth projects."));
        searchResults.setPrefHeight(430);
        search.setOnAction(e -> searchModrinth());
        searchField.setOnAction(e -> searchModrinth());
        install.setOnAction(e -> installSelectedMod());
        Button details = button("Project details", "#303846", "#FFFFFF");
        details.setOnAction(e -> showProjectDetails());
        HBox bottom = new HBox(8, details);
        panel.getChildren().addAll(heading, info, instanceBar, searchRow, searchResults, bottom);
        VBox.setVgrow(searchResults, Priority.ALWAYS);
        return tab("Discover", panel);
    }

    private Tab createLibraryTab() {
        VBox panel = panel();
        Label heading = heading("My mods");
        Label note = muted("Changes affect only the selected instance. The official Minecraft launcher remains responsible for sign-in and playing.");
        Button create = button("New isolated instance", "#52D7FF", "#0E1117");
        Button importFolder = button("Import game folder…", "#9A6BFF", "#FFFFFF");
        Button openFolder = button("Open instance folder", "#303846", "#FFFFFF");
        Button addLocal = button("Add local mod…", "#FFD166", "#0E1117");
        Button remove = button("Remove selected", "#C94B55", "#FFFFFF");
        Button refresh = button("Refresh", "#303846", "#FFFFFF");
        create.setOnAction(e -> createInstanceDialog());
        importFolder.setOnAction(e -> importInstanceDialog());
        openFolder.setOnAction(e -> withInstance(p -> openDirectory(Path.of(p.gameDirectory()))));
        addLocal.setOnAction(e -> addLocalMod());
        remove.setOnAction(e -> removeSelectedMod());
        refresh.setOnAction(e -> refreshInstalledMods());
        HBox row = new HBox(8, create, importFolder, openFolder);
        HBox modActions = new HBox(8, addLocal, remove, refresh);
        installedMods.setPlaceholder(muted("No mod files in this instance yet."));
        installedMods.setPrefHeight(420);
        installedMods.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (!empty) setTextFill(Color.web("#F5F7FF"));
            }
        });
        panel.getChildren().addAll(heading, note, instanceChoice, row, modActions, installedMods);
        VBox.setVgrow(installedMods, Priority.ALWAYS);
        return tab("My mods", panel);
    }

    private Tab createBackupTab() {
        VBox panel = panel();
        Label heading = heading("Backups and activity");
        Label info = muted("Back up or restore mod files for the selected instance. Prismite never handles account credentials.");
        Button backup = button("Back up mods…", "#52D7FF", "#0E1117");
        Button restore = button("Restore backup…", "#9A6BFF", "#FFFFFF");
        backup.setOnAction(e -> backupMods());
        restore.setOnAction(e -> restoreMods());
        activity.setEditable(false);
        activity.setWrapText(true);
        panel.getChildren().addAll(heading, info, instanceChoice, new HBox(8, backup, restore), activity);
        VBox.setVgrow(activity, Priority.ALWAYS);
        return tab("Backups", panel);
    }

    private void loadOrCreateStarterInstance() {
        runTask(() -> {
            List<InstanceStore.InstanceProfile> list = instances.list();
            if (list.isEmpty()) list.add(instances.create("My mods", "1.21.1", "fabric"));
            return list;
        }, profiles -> {
            instanceChoice.getItems().setAll(profiles);
            instanceChoice.setValue(profiles.get(0));
            status.setText("Ready · local-only · no sign-in required");
            refreshInstalledMods();
            searchModrinth();
        });
    }

    private void createInstanceDialog() {
        TextInputDialog dialog = new TextInputDialog("My modpack");
        dialog.setTitle("New instance");
        dialog.setHeaderText("Create an isolated mod folder");
        dialog.setContentText("Instance name:");
        dialog.showAndWait().filter(s -> !s.isBlank()).ifPresent(name -> runTask(
                () -> instances.create(name.trim(), selectedInstance() == null ? "1.21.1" : selectedInstance().gameVersion(), selectedInstance() == null ? "fabric" : selectedInstance().loader()),
                profile -> { reloadInstances(profile.id()); log("Created " + profile.name()); }));
    }

    private void importInstanceDialog() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose an existing Minecraft game directory");
        java.io.File selected = chooser.showDialog(window());
        if (selected == null) return;
        TextInputDialog name = new TextInputDialog("Imported game");
        name.setTitle("Import instance");
        name.setHeaderText("Name this instance");
        name.setContentText("Instance name:");
        name.showAndWait().filter(s -> !s.isBlank()).ifPresent(value -> runTask(
                () -> instances.importDirectory(value.trim(), "1.21.1", "fabric", selected.toPath()),
                profile -> { reloadInstances(profile.id()); log("Imported " + profile.gameDirectory()); }));
    }

    private void reloadInstances(String selectId) {
        runTask(instances::list, profiles -> {
            instanceChoice.getItems().setAll(profiles);
            if (selectId != null) profiles.stream().filter(p -> p.id().equals(selectId)).findFirst().ifPresent(instanceChoice::setValue);
            else if (!profiles.isEmpty()) instanceChoice.setValue(profiles.get(0));
            refreshInstalledMods();
        });
    }

    private void refreshInstalledMods() {
        InstanceStore.InstanceProfile profile = instanceChoice.getValue();
        if (profile == null) { installedMods.getItems().clear(); return; }
        runTask(() -> {
            try (var files = Files.list(instances.modsDirectory(profile))) {
                return files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".jar") || p.toString().toLowerCase().endsWith(".zip"))
                        .sorted().map(metadata::describe).toList();
            }
        }, installedMods.getItems()::setAll);
    }

    private void searchModrinth() {
        InstanceStore.InstanceProfile profile = instanceChoice.getValue();
        if (profile == null) { status.setText("Create an instance to search compatible mods."); return; }
        status.setText("Searching Modrinth…");
        String loader = selectedLoader(profile);
        runTask(() -> modrinth.search(searchField.getText().trim(), profile.gameVersion(), loader), projects -> {
            searchResults.getItems().setAll(projects);
            status.setText("Found " + projects.size() + " compatible project(s) · Minecraft " + profile.gameVersion() + " · " + loader);
        });
    }

    private String selectedLoader(InstanceStore.InstanceProfile profile) {
        String selected = loaderFilter.getValue();
        if (selected == null || selected.equals("Use instance loader") || selected.endsWith("(server)")) return profile.loader();
        return selected;
    }

    private void installSelectedMod() {
        InstanceStore.InstanceProfile profile = instanceChoice.getValue();
        ModrinthClient.Project project = searchResults.getSelectionModel().getSelectedItem();
        if (profile == null || project == null) { status.setText("Select an instance and a mod first."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Install " + project.title() + " and any required Modrinth dependencies into " + profile.name() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Mod downloads come from Modrinth");
        confirm.showAndWait().filter(ButtonType.YES::equals).ifPresent(button -> {
            setBusy(true, "Installing " + project.title() + "…");
            runTask(() -> modrinth.installProject(project.id(), profile.gameVersion(), selectedLoader(profile), instances.modsDirectory(profile)), paths -> {
                setBusy(false, "Installed " + paths.size() + " file(s) into " + profile.name());
                paths.forEach(path -> log("Installed " + path.getFileName()));
                refreshInstalledMods();
            });
        });
    }

    private void showProjectDetails() {
        ModrinthClient.Project project = searchResults.getSelectionModel().getSelectedItem();
        if (project == null) { status.setText("Choose a project first."); return; }
        setBusy(true, "Loading project details…");
        runTask(() -> modrinth.details(project.id()), details -> {
            setBusy(false, "Ready");
            TextArea text = new TextArea(details.body());
            text.setEditable(false);
            text.setWrapText(true);
            text.setPrefColumnCount(70);
            text.setPrefRowCount(22);
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle(details.title());
            dialog.setHeaderText(details.title() + " · " + project.author() + " · " + project.downloads() + " downloads");
            dialog.getDialogPane().setContent(text);
            dialog.getDialogPane().setPrefWidth(700);
            dialog.showAndWait();
        });
    }

    private void addLocalMod() {
        InstanceStore.InstanceProfile profile = instanceChoice.getValue();
        if (profile == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select mod archive");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Mod archives", "*.jar", "*.zip"));
        java.io.File file = chooser.showOpenDialog(window());
        if (file == null) return;
        runTask(() -> {
            Path dir = instances.modsDirectory(profile);
            Path target = dir.resolve(file.getName()).normalize();
            if (!target.getParent().equals(dir.toAbsolutePath().normalize())) throw new IOException("Invalid filename");
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        }, path -> { log("Added " + path.getFileName()); refreshInstalledMods(); });
    }

    private void removeSelectedMod() {
        String selected = installedMods.getSelectionModel().getSelectedItem();
        InstanceStore.InstanceProfile profile = instanceChoice.getValue();
        if (selected == null || profile == null) { status.setText("Select an installed mod first."); return; }
        String filename = selected.split("  ·  ", 2)[0];
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove " + filename + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(ButtonType.YES::equals).ifPresent(button -> runTask(() -> Files.deleteIfExists(instances.modsDirectory(profile).resolve(filename).normalize()), removed -> {
            log("Removed " + filename);
            refreshInstalledMods();
        }));
    }

    private void backupMods() {
        InstanceStore.InstanceProfile profile = instanceChoice.getValue();
        if (profile == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save mods backup");
        chooser.setInitialFileName(profile.name().replaceAll("[^A-Za-z0-9._-]", "_") + "-mods.zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
        java.io.File file = chooser.showSaveDialog(window());
        if (file != null) runTask(() -> { backups.backup(instances.modsDirectory(profile), file.toPath()); return file.toPath(); }, path -> log("Backup saved: " + path));
    }

    private void restoreMods() {
        InstanceStore.InstanceProfile profile = instanceChoice.getValue();
        if (profile == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose backup ZIP");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
        java.io.File file = chooser.showOpenDialog(window());
        if (file != null) runTask(() -> backups.restore(file.toPath(), instances.modsDirectory(profile)), count -> { log("Restored " + count + " file(s)"); refreshInstalledMods(); });
    }

    private void openDirectory(Path path) {
        try { Files.createDirectories(path); if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(path.toFile()); }
        catch (IOException ex) { status.setText("Could not open folder: " + ex.getMessage()); }
    }
    private void withInstance(java.util.function.Consumer<InstanceStore.InstanceProfile> action) { if (instanceChoice.getValue() != null) action.accept(instanceChoice.getValue()); }
    private Window window() { return root.getScene() == null ? null : root.getScene().getWindow(); }
    private void setBusy(boolean busy, String message) { progress.setVisible(busy); status.setText(message); }
    private void log(String message) { Platform.runLater(() -> activity.appendText(message + System.lineSeparator())); }
    private <T> void runTask(CheckedSupplier<T> action, java.util.function.Consumer<T> success) {
        worker.submit(() -> {
            try { T result = action.get(); Platform.runLater(() -> success.accept(result)); }
            catch (Exception ex) { Platform.runLater(() -> { setBusy(false, "Action failed"); activity.appendText("Error: " + ex.getMessage() + System.lineSeparator()); }); }
        });
    }
    @FunctionalInterface private interface CheckedSupplier<T> { T get() throws Exception; }
    private VBox panel() { VBox box = new VBox(10); box.setPadding(new Insets(16)); box.setStyle("-fx-background-color: #171C24;"); return box; }
    private Tab tab(String title, VBox panel) { Tab tab = new Tab(title); tab.setContent(panel); return tab; }
    private Label heading(String text) { Label l = new Label(text); l.setTextFill(Color.web("#F5F7FF")); l.setFont(Font.font("Segoe UI", 21)); return l; }
    private Label muted(String text) { Label l = new Label(text); l.setTextFill(Color.web("#B8C1D9")); l.setWrapText(true); return l; }
    private Button button(String text, String bg, String fg) { Button b = new Button(text); b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-font-weight: bold;"); return b; }
}
