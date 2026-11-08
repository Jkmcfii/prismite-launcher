package com.prismite.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrismiteApp {
    private final BorderPane root = new BorderPane();
    private final ListView<String> modList = new ListView<>();
    private final TextArea output = new TextArea();
    private final Label statusLabel = new Label("No Minecraft directory selected");
    private final ComboBox<String> instanceCombo = new ComboBox<>();
    private Path minecraftRoot;

    public PrismiteApp() {
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #0E1117;");

        VBox top = new VBox(8);
        top.setPadding(new Insets(0, 0, 12, 0));

        Label title = new Label("Prismite");
        title.setTextFill(Color.web("#F5F7FF"));
        title.setFont(Font.font("Segoe UI", 30));

        Label subtitle = new Label("Local-only Minecraft mod manager");
        subtitle.setTextFill(Color.web("#B8C1D9"));
        subtitle.setFont(Font.font("Segoe UI", 16));

        top.getChildren().addAll(title, subtitle);

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button chooseDir = new Button("Choose Minecraft folder");
        chooseDir.setStyle("-fx-background-color: #52D7FF; -fx-text-fill: #0E1117; -fx-font-weight: bold;");

        Button refresh = new Button("Refresh");
        refresh.setStyle("-fx-background-color: #9A6BFF; -fx-text-fill: #F5F7FF; -fx-font-weight: bold;");

        Button addMod = new Button("Add Mod");
        addMod.setStyle("-fx-background-color: #FFD166; -fx-text-fill: #0E1117; -fx-font-weight: bold;");

        Button removeMod = new Button("Remove Selected");
        removeMod.setStyle("-fx-background-color: #EA4335; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        Button openModsFolder = new Button("Open Mods Folder");
        openModsFolder.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        controls.getChildren().addAll(chooseDir, refresh, addMod, removeMod, openModsFolder);

        instanceCombo.setPromptText("Instance");
        instanceCombo.setPrefWidth(220);
        instanceCombo.getItems().addAll("Default");
        instanceCombo.setValue("Default");

        statusLabel.setTextFill(Color.web("#F5F7FF"));
        statusLabel.setFont(Font.font("Segoe UI", 13));

        modList.setPrefHeight(300);
        output.setEditable(false);
        output.setWrapText(true);
        output.setPrefRowCount(6);

        chooseDir.setOnAction(e -> chooseMinecraftFolder());
        refresh.setOnAction(e -> refreshMods());
        addMod.setOnAction(e -> addMod());
        removeMod.setOnAction(e -> removeSelectedMod());
        openModsFolder.setOnAction(e -> openModsDirectory());

        VBox center = new VBox(10);
        center.getChildren().addAll(
                controls,
                instanceCombo,
                statusLabel,
                modList,
                output
        );

        root.setTop(top);
        root.setCenter(center);

        minecraftRoot = findMinecraftDirectory();
        if (minecraftRoot != null) {
            statusLabel.setText("Detected Minecraft folder: " + minecraftRoot);
            refreshMods();
        }
    }

    public BorderPane getRoot() {
        return root;
    }

    private void chooseMinecraftFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select your .minecraft folder");
        Path folder = chooser.showDialog(root.getScene().getWindow()) != null
                ? chooser.showDialog(root.getScene().getWindow()).toPath()
                : null;

        if (folder == null) {
            output.appendText("No folder selected.\n");
            return;
        }

        minecraftRoot = folder;
        statusLabel.setText("Selected Minecraft folder: " + minecraftRoot);
        output.appendText("Using Minecraft root at: " + minecraftRoot + "\n");
        refreshMods();
    }

    private void refreshMods() {
        if (minecraftRoot == null || !Files.exists(minecraftRoot)) {
            modList.getItems().clear();
            output.appendText("Minecraft folder not found.\n");
            return;
        }

        Path modsDir = minecraftRoot.resolve("mods");
        if (!Files.exists(modsDir)) {
            try {
                Files.createDirectories(modsDir);
                output.appendText("Created mods folder at: " + modsDir + "\n");
            } catch (IOException ex) {
                output.appendText("Could not create mods folder: " + ex.getMessage() + "\n");
                return;
            }
        }

        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir)) {
            for (Path p : stream) {
                if (p.getFileName().toString().toLowerCase().endsWith(".jar") || p.getFileName().toString().toLowerCase().endsWith(".zip")) {
                    names.add(p.getFileName().toString());
                }
            }
        } catch (IOException e) {
            output.appendText("Failed to list mods: " + e.getMessage() + "\n");
            return;
        }

        Collections.sort(names);
        modList.getItems().setAll(names);
        output.appendText("Loaded " + names.size() + " mod file(s).\n");
    }

    private void addMod() {
        if (minecraftRoot == null) {
            output.appendText("Choose your Minecraft folder first.\n");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a mod to install");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Mod files", "*.jar", "*.zip"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        java.io.File selected = chooser.showOpenDialog(root.getScene().getWindow());
        if (selected == null) {
            return;
        }

        Path modsDir = minecraftRoot.resolve("mods");
        try {
            Files.createDirectories(modsDir);
            Path target = modsDir.resolve(selected.getName());
            Files.copy(selected.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            output.appendText("Installed mod: " + selected.getName() + "\n");
            refreshMods();
        } catch (IOException e) {
            output.appendText("Could not add mod: " + e.getMessage() + "\n");
        }
    }

    private void removeSelectedMod() {
        String selected = modList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            output.appendText("Select a mod to remove.\n");
            return;
        }

        if (minecraftRoot == null) {
            output.appendText("No Minecraft folder selected.\n");
            return;
        }

        Path target = minecraftRoot.resolve("mods").resolve(selected);
        try {
            Files.deleteIfExists(target);
            output.appendText("Removed mod: " + selected + "\n");
            refreshMods();
        } catch (IOException e) {
            output.appendText("Could not remove mod: " + e.getMessage() + "\n");
        }
    }

    private void openModsDirectory() {
        if (minecraftRoot == null) {
            output.appendText("Choose your Minecraft folder first.\n");
            return;
        }

        Path modsDir = minecraftRoot.resolve("mods");
        try {
            Files.createDirectories(modsDir);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(modsDir.toFile());
            } else {
                output.appendText("Desktop open is not supported on this platform.\n");
            }
        } catch (IOException e) {
            output.appendText("Could not open mods folder: " + e.getMessage() + "\n");
        }
    }

    private Path findMinecraftDirectory() {
        List<String> candidates = new ArrayList<>();
        String home = System.getProperty("user.home");

        candidates.add(Paths.get(home, ".minecraft").toString());
        candidates.add(Paths.get(home, "AppData", "Roaming", ".minecraft").toString());
        candidates.add(Paths.get(home, "AppData", "Local", ".minecraft").toString());

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            candidates.add(Paths.get(System.getenv().getOrDefault("APPDATA", home), ".minecraft").toString());
        } else if (os.contains("mac")) {
            candidates.add(Paths.get(home, "Library", "Application Support", "minecraft").toString());
        }

        for (String c : candidates) {
            Path p = Paths.get(c);
            if (Files.exists(p) && Files.isDirectory(p)) {
                return p;
            }
        }

        return null;
    }
}
