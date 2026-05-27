package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.client.SceneManager;
import com.auction.shared.network.protocol.SocketMessage;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         messageLabel;
    @FXML private Button        registerButton;

    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 9090;

    @FXML
    public void initialize() {
        messageLabel.setText("");

        ClientContext ctx = ClientContext.getInstance();
        if (ctx.getClient() == null || !ctx.getClient().isConnected()) {
            boolean ok = ctx.connect(SERVER_HOST, SERVER_PORT);
            if (!ok) {
                showMsg("Khong the ket noi server. Hay khoi dong AuctionServer truoc.", false);
            }
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showMsg("Tai khoan va mat khau khong duoc trong.", false);
            return;
        }
        if (fullName.isEmpty()){
            showMsg("Ho va ten khong duoc trong.", false);
            return;
        }
        if (email.isEmpty()){
            showMsg("Email khong duoc trong.", false);
            return;
        }
        if (password.length() < 6) {
            showMsg("Mat khau phai co it nhat 6 ky tu.", false);
            return;
        }

        registerButton.setDisable(true);

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance()
                    .getClient()
                    .register("BIDDER", username, email, password, fullName);

            Platform.runLater(() -> {
                registerButton.setDisable(false);
                if (res.isOk()) {
                    showMsg("Dang ky thanh cong! Dang chuyen ve dang nhap...", true);
                    new Thread(() -> {
                        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                        Platform.runLater(() ->
                                SceneManager.getInstance().switchTo("login2.fxml"));
                    }).start();
                } else {
                    showMsg(res.getMessage() != null
                            ? res.getMessage() : "Dang ky that bai.", false);
                }
            });
        }).start();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        SceneManager.getInstance().switchTo("login2.fxml");
    }

    private void showMsg(String msg, boolean success) {
        messageLabel.setStyle(success
                ? "-fx-text-fill: #10b981; -fx-font-size: 12;"
                : "-fx-text-fill: #ef4444; -fx-font-size: 12;");
        messageLabel.setText(msg);
    }
}
