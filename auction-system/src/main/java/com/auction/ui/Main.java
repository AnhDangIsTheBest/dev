package com.auction.ui;

import com.auction.network.client.AuctionClient;
import com.auction.network.server.AuctionServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

public class Main extends Application {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static Stage primaryStage;
    public static final AuctionClient CONNECTION = new AuctionClient("localhost",9090);
    @Override
    public void start(Stage stage) throws Exception {
        boolean connected = CONNECTION.connect();
        if (!connected) {
            // Nếu chưa có server, khởi động embedded server
            logger.info("Không có server, khởi động embedded server...");
            Thread serverThread = new Thread(() -> new AuctionServer(9090).start(), "embedded-server");
            serverThread.setDaemon(true);
            serverThread.start();
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            CONNECTION.connect();
        }
        primaryStage = stage;
        changeScene("login2");
        stage.setTitle("Auction76");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.show();
        stage.setOnCloseRequest(e -> {
            CONNECTION.disconnect();
            System.exit(0);
        });
    }
    public static Stage getPrimaryStage() { return primaryStage; }
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
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void stop() {
        CONNECTION.disconnect();
    }
}