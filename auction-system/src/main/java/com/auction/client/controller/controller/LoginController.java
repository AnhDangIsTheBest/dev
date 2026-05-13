package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.client.SceneManager;
import com.auction.model.User.User;
import com.auction.network.protocol.SocketMessage;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller cho login2.fxml
 * Kết nối tới AuctionServer, đăng nhập, chuyển sang main.fxml
 */
public class LoginController {

    // ── Server config (có thể đọc từ config file sau) ────────────
    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 9090;

    @FXML private TextField       usernameField;
    @FXML private PasswordField   passwordField;
    @FXML private Label           errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button          loginButton;
    @FXML private Button          registerButton;

    @FXML
    public void initialize() {
        errorLabel.setText("");
        // Kết nối tới server một lần duy nhất
        ClientContext ctx = ClientContext.getInstance();
        if (ctx.getClient() == null || !ctx.getClient().isConnected()) {
            boolean ok = ctx.connect(SERVER_HOST, SERVER_PORT);
            if (!ok) {
                showError("Không thể kết nối server " + SERVER_HOST + ":" + SERVER_PORT
                        + "\nHãy khởi động AuctionServer trước.");
            }
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
            return;
        }

        setLoading(true);

        // Chạy trên thread khác để không block UI
        new Thread(() -> {
            ClientContext ctx = ClientContext.getInstance();

            if (ctx.getClient() == null || !ctx.getClient().isConnected()) {
                boolean ok = ctx.connect(SERVER_HOST, SERVER_PORT);
                if (!ok) {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError("Không thể kết nối server. Kiểm tra AuctionServer.");
                    });
                    return;
                }
            }

            SocketMessage res = ctx.getClient().login(username, password);

            Platform.runLater(() -> {
                setLoading(false);
                if (res.isOk()) {
                    User user = (User) res.get("user");
                    ctx.setCurrentUser(user);
                    SceneManager.getInstance().switchTo("main.fxml", 1050, 650);
                    // MainController.initialize() sẽ nhận user tự động
                } else {
                    showError(res.getMessage() != null
                            ? res.getMessage() : "Sai tên đăng nhập hoặc mật khẩu.");
                }
            });
        }).start();
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        SceneManager.getInstance().switchTo("register.fxml");
    }

    // ── helpers ──────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12;");
        errorLabel.setText(msg);
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loginButton.setDisable(loading);
        registerButton.setDisable(loading);
    }
}
