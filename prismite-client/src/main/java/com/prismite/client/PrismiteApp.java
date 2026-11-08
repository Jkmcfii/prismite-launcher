package com.prismite.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class PrismiteApp {
    private final VBox root = new VBox();
    private final ApiClient apiClient = new ApiClient("http://localhost:8080");
    private String token;

    public PrismiteApp() {
        root.setPadding(new Insets(30));
        root.setSpacing(18);
        root.setStyle("-fx-background-color: #0E1117; -fx-text-fill: #F5F7FF;");

        Label title = new Label("Prismite");
        title.setTextFill(Color.web("#F5F7FF"));
        title.setFont(Font.font("Segoe UI", 30));

        Label subtitle = new Label("Build brighter worlds");
        subtitle.setTextFill(Color.web("#B8C1D9"));
        subtitle.setFont(Font.font("Segoe UI", 16));

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button registerButton = new Button("Create account");
        Button loginButton = new Button("Sign in");
        Button googleButton = new Button("Continue with Google");
        Button discordButton = new Button("Continue with Discord");
        Button microsoftButton = new Button("Continue with Microsoft");

        registerButton.setStyle("-fx-background-color: #52D7FF; -fx-text-fill: #0E1117; -fx-font-weight: bold;");
        loginButton.setStyle("-fx-background-color: #9A6BFF; -fx-text-fill: #F5F7FF; -fx-font-weight: bold;");
        googleButton.setStyle("-fx-background-color: #EA4335; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        discordButton.setStyle("-fx-background-color: #5865F2; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        microsoftButton.setStyle("-fx-background-color: #FFD166; -fx-text-fill: #0E1117; -fx-font-weight: bold;");

        TextArea output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);

        registerButton.setOnAction(e -> {
            apiClient.register(emailField.getText(), usernameField.getText(), passwordField.getText(),
                    result -> output.setText("Registered successfully: " + result),
                    error -> output.setText("Register failed: " + error));
        });

        loginButton.setOnAction(e -> {
            apiClient.login(emailField.getText(), passwordField.getText(),
                    result -> {
                        output.setText("Login successful: " + result);
                        token = parseToken(result);
                        if (!token.isBlank()) {
                            root.getChildren().setAll(new DashboardView(apiClient, token).getRoot());
                        }
                    },
                    error -> output.setText("Login failed: " + error));
        });

        googleButton.setOnAction(e -> {
            apiClient.oauthStart("google",
                    result -> output.setText("Google auth URL: " + result),
                    error -> output.setText("Google auth failed: " + error));
        });

        discordButton.setOnAction(e -> {
            apiClient.oauthStart("discord",
                    result -> output.setText("Discord auth URL: " + result),
                    error -> output.setText("Discord auth failed: " + error));
        });

        microsoftButton.setOnAction(e -> {
            apiClient.oauthStart("microsoft",
                    result -> output.setText("Microsoft auth URL: " + result),
                    error -> output.setText("Microsoft auth failed: " + error));
        });

        VBox form = new VBox(10,
                title,
                subtitle,
                emailField,
                usernameField,
                passwordField,
                registerButton,
                loginButton,
                googleButton,
                discordButton,
                microsoftButton,
                output
        );
        form.setAlignment(Pos.CENTER_LEFT);
        form.setMaxWidth(500);

        root.getChildren().add(form);
        root.setAlignment(Pos.CENTER);
    }

    private String parseToken(String json) {
        try {
            int start = json.indexOf("\"token\":\"");
            if (start == -1) return "";
            start += 9;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "";
        }
    }

    public VBox getRoot() {
        return root;
    }
}
