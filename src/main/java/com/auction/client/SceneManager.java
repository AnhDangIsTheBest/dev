package com.auction.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Quản lý toàn bộ việc chuyển màn hình.
 * Sử dụng: SceneManager.getInstance().switchTo("main.fxml")
 */
public class SceneManager {

    private static volatile SceneManager instance;
    private Stage primaryStage;

    private SceneManager() {
    }

    public static SceneManager getInstance() {
        if (instance == null) {
            synchronized (SceneManager.class) {
                if (instance == null) instance = new SceneManager();
            }
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Chuyển scene sang FXML mới, thay toàn bộ scene.
     */
    public void switchTo(String fxmlFile, double width, double height) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/" + fxmlFile));
            Scene scene = new Scene(root, width, height);
            installStylesheet(scene);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("[SceneManager] Không load được " + fxmlFile + ": " + e.getMessage());
        }
    }

    public void switchTo(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/" + fxmlFile));
            primaryStage.getScene().setRoot(root);
            installStylesheet(primaryStage.getScene());
        } catch (IOException e) {
            System.err.println("[SceneManager] Không load được " + fxmlFile + ": " + e.getMessage());
        }
    }

    /**
     * Load FXML và lấy controller để inject data trước khi show.
     */
    public <T> T loadController(String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/" + fxmlFile));
        Parent root = loader.load();
        primaryStage.getScene().setRoot(root);
        installStylesheet(primaryStage.getScene());
        return loader.getController();
    }

    private void installStylesheet(Scene scene) {
        String css = getClass().getResource("/css/style.css").toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }
}
