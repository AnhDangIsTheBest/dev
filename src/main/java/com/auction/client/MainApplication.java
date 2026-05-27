package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point JavaFX.
 * Khởi động ứng dụng, show màn hình Login (login2.fxml).
 * AuctionServer phải được chạy riêng trước.
 */
public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Đăng ký stage với SceneManager để dùng ở khắp nơi
        SceneManager.getInstance().setPrimaryStage(primaryStage);

        Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/login2.fxml"));

        primaryStage.setTitle("⚡ AuctionX – Hệ thống đấu giá trực tuyến");
        primaryStage.setScene(new Scene(root, 600, 520));
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        primaryStage.show();

        // Đóng ứng dụng → ngắt kết nối socket
        primaryStage.setOnCloseRequest(e -> {
            com.auction.client.ClientContext.getInstance().disconnect();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
