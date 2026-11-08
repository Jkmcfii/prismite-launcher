package com.prismite.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        PrismiteApp app = new PrismiteApp();
        Scene scene = new Scene(app.getRoot(), 980, 700);
        stage.setTitle("Prismite");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
