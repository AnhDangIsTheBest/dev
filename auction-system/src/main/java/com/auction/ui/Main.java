package com.auction.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        // Tạo nút
        Button btn = new Button("Click me");

        // Tạo scene (nội dung)
        Scene scene = new Scene(btn, 400, 300);

        // Gán scene vào cửa sổ
        stage.setScene(scene);

        // Tiêu đề
        stage.setTitle("My App");

        // Hiển thị cửa sổ
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}