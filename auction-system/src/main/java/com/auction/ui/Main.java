package com.auction.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import static javafx.application.Application.launch;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception{
        primaryStage = stage;
        changeScene("login");
        stage.setTitle("AUCTION76 - Đấu giá bằng cả tính mạng!");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.show();
    }
    public void changeScene(String fxml) throws IOException {
        String path = "/fxml/"+fxml+".fxml";
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
    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        launch(args);
    }
}
