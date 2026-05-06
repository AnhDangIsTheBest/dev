package com.auction.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        changeScene("login2");
        stage.setTitle("Auction76");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.show();
    }

    public void changeScene(String fxml) throws IOException {
        String path = "/fxml/" + fxml + ".fxml";
        Parent root = FXMLLoader.load(Main.class.getResource(path));
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(
                    Main.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
    }
}