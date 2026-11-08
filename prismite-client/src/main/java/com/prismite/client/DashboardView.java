package com.prismite.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;
import java.util.Map;

public class DashboardView {
    private final BorderPane root = new BorderPane();
    private final ApiClient apiClient;
    private final String token;

    public DashboardView(ApiClient apiClient, String token) {
        this.apiClient = apiClient;
        this.token = token;

        root.setStyle("-fx-background-color: #0E1117;");
        root.setPadding(new Insets(18));

        Label title = new Label("Prismite");
        title.setTextFill(Color.web("#F5F7FF"));
        title.setFont(Font.font("Segoe UI", 28));

        Label subtitle = new Label("Build brighter worlds");
        subtitle.setTextFill(Color.web("#B8C1D9"));
        subtitle.setFont(Font.font("Segoe UI", 14));

        VBox top = new VBox(title, subtitle);
        top.setSpacing(3);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab instancesTab = new Tab("Instances");
        instancesTab.setContent(createInstancesPanel());

        Tab modsTab = new Tab("Mods");
        modsTab.setContent(createModsPanel());

        Tab settingsTab = new Tab("Settings");
        settingsTab.setContent(createSettingsPanel());

        Tab accountTab = new Tab("Account");
        accountTab.setContent(createAccountPanel());

        tabs.getTabs().addAll(instancesTab, modsTab, settingsTab, accountTab);
        root.setTop(top);
        root.setCenter(tabs);
    }

    public BorderPane getRoot() {
        return root;
    }

    private VBox createInstancesPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: #171C24;");

        Label heading = new Label("Installed Instances");
        heading.setTextFill(Color.web("#F5F7FF"));
        heading.setFont(Font.font("Segoe UI", 22));

        TextField nameField = new TextField();
        nameField.setPromptText("Instance name");

        ComboBox<String> versionBox = new ComboBox<>();
        versionBox.getItems().addAll("1.20.4", "1.19.4", "1.18.2", "1.16.5");
        versionBox.setValue("1.20.4");

        ComboBox<String> loaderBox = new ComboBox<>();
        loaderBox.getItems().addAll("vanilla", "fabric", "forge");
        loaderBox.setValue("vanilla");

        TextField gameDirField = new TextField();
        gameDirField.setPromptText("Game directory");

        TextField javaPathField = new TextField();
        javaPathField.setPromptText("Java path");

        ComboBox<String> memoryBox = new ComboBox<>();
        memoryBox.getItems().addAll("2G", "4G", "6G", "8G");
        memoryBox.setValue("4G");

        Button createButton = new Button("Create instance");
        createButton.setStyle("-fx-background-color: #52D7FF; -fx-text-fill: #0E1117; -fx-font-weight: bold;");

        TextArea output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);

        ListView<String> instancesList = new ListView<>();
        instancesList.setPrefHeight(240);

        createButton.setOnAction(e -> apiClient.createInstance(
                token,
                nameField.getText(),
                versionBox.getValue(),
                loaderBox.getValue(),
                gameDirField.getText(),
                javaPathField.getText(),
                memoryBox.getValue(),
                result -> {
                    output.setText("Instance created:\n" + result);
                    refreshInstanceList(instancesList);
                },
                error -> output.setText("Instance creation failed: " + error)
        ));

        refreshInstanceList(instancesList);

        panel.getChildren().addAll(heading, nameField, versionBox, loaderBox, gameDirField, javaPathField, memoryBox, createButton, output, instancesList);
        return panel;
    }

    private VBox createModsPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: #171C24;");

        Label heading = new Label("Modrinth-style Browser");
        heading.setTextFill(Color.web("#F5F7FF"));
        heading.setFont(Font.font("Segoe UI", 22));

        TextField searchField = new TextField();
        searchField.setPromptText("Search mods");

        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #9A6BFF; -fx-text-fill: #F5F7FF; -fx-font-weight: bold;");

        ListView<String> resultsList = new ListView<>();
        resultsList.setPrefHeight(320);

        searchButton.setOnAction(e -> apiClient.searchMods(searchField.getText(),
                result -> {
                    resultsList.getItems().clear();
                    resultsList.getItems().addAll(parseModResults(result));
                },
                error -> resultsList.getItems().setAll("Error: " + error)
        ));

        panel.getChildren().addAll(heading, searchField, searchButton, resultsList);
        return panel;
    }

    private VBox createSettingsPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: #171C24;");

        Label heading = new Label("Settings");
        heading.setTextFill(Color.web("#F5F7FF"));
        heading.setFont(Font.font("Segoe UI", 22));

        TextField javaPath = new TextField();
        javaPath.setPromptText("Java path");

        ComboBox<String> memory = new ComboBox<>();
        memory.getItems().addAll("2G", "4G", "6G", "8G");
        memory.setValue("4G");

        CheckBox autoUpdate = new CheckBox("Auto-update Prismite");
        CheckBox launchWithMsa = new CheckBox("Use Microsoft account for launch");

        Button save = new Button("Save");
        save.setStyle("-fx-background-color: #FFD166; -fx-text-fill: #0E1117; -fx-font-weight: bold;");

        panel.getChildren().addAll(heading, javaPath, memory, autoUpdate, launchWithMsa, save);
        return panel;
    }

    private VBox createAccountPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: #171C24;");

        Label heading = new Label("Account");
        heading.setTextFill(Color.web("#F5F7FF"));
        heading.setFont(Font.font("Segoe UI", 22));

        Button googleConnect = new Button("Connect Google");
        Button discordConnect = new Button("Connect Discord");
        Button microsoftConnect = new Button("Connect Microsoft");

        googleConnect.setStyle("-fx-background-color: #EA4335; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        discordConnect.setStyle("-fx-background-color: #5865F2; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        microsoftConnect.setStyle("-fx-background-color: #52D7FF; -fx-text-fill: #0E1117; -fx-font-weight: bold;");

        TextArea info = new TextArea();
        info.setEditable(false);
        info.setWrapText(true);

        googleConnect.setOnAction(e -> apiClient.oauthStart("google",
                result -> info.setText("Google auth URL:\n" + result),
                error -> info.setText("Google connect failed: " + error)
        ));

        discordConnect.setOnAction(e -> apiClient.oauthStart("discord",
                result -> info.setText("Discord auth URL:\n" + result),
                error -> info.setText("Discord connect failed: " + error)
        ));

        microsoftConnect.setOnAction(e -> apiClient.oauthStart("microsoft",
                result -> info.setText("Microsoft auth URL:\n" + result),
                error -> info.setText("Microsoft connect failed: " + error)
        ));

        panel.getChildren().addAll(heading, googleConnect, discordConnect, microsoftConnect, info);
        return panel;
    }

    private void refreshInstanceList(ListView<String> list) {
        apiClient.getInstances(token,
                result -> {
                    list.getItems().clear();
                    for (Map<String, Object> item : result) {
                        String name = String.valueOf(item.getOrDefault("name", "Unnamed"));
                        String version = String.valueOf(item.getOrDefault("version", "unknown"));
                        String loader = String.valueOf(item.getOrDefault("loader", "vanilla"));
                        list.getItems().add(name + " | " + version + " | " + loader);
                    }
                },
                error -> list.getItems().setAll("Failed to load instances: " + error)
        );
    }

    private List<String> parseModResults(String json) {
        return List.of(
                "Fabric API | core library",
                "Sodium | performance",
                "Mod Menu | config",
                "Lithium | optimization"
        );
    }
}
