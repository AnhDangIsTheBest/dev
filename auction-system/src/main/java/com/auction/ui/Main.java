package com.auction.ui;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;

public class Main extends Application {
    private static Stage stage;
    private static Stage stg;

    @Override
    public void start(Stage primaryStage) throws Exception{
        stg = primaryStage;
        primaryStage.setResizable(false);
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        primaryStage.setTitle("76 Auction");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    public void changeScene(String fxml) throws IOException {
        Parent pane = FXMLLoader.load(getClass().getResource("/fxml/" + fxml));
        stg.getScene().setRoot(pane);
    }


//    @Override
//    public void start(Stage stage) {
//
//        // Tạo nút
//        Button btn = new Button("Click me");
//
//        // Tạo scene (nội dung)
//        Scene scene = new Scene(btn, 400, 300);
//
//        // Gán scene vào cửa sổ
//        stage.setScene(scene);
//
//        // Tiêu đề,
//        stage.setTitle("My App");
//
//        // Hiển thị cửa sổ
//        stage.show();
//    }

    public static void main(String[] args) {
        launch(args);
    }
}