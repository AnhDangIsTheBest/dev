package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.client.SceneManager;
import com.auction.network.protocol.SocketMessage;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         messageLabel;
    @FXML private Button        registerButton;
    @FXML private ToggleButton  btnBidder;
    @FXML private ToggleButton  btnSeller;

    // Vai trò đang chọn — mặc định BIDDER
    private String selectedRole = "BIDDER";
    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 9090;

    @FXML
    public void initialize() {
        messageLabel.setText("");

        ClientContext ctx = ClientContext.getInstance();
        if (ctx.getClient() == null || !ctx.getClient().isConnected()) {
            boolean ok = ctx.connect(SERVER_HOST, SERVER_PORT);
            if (!ok) {
                showMsg("Không thể kết nối server. Hãy khởi động AuctionServer trước.", false);
            }
        }


        // Toggle Bidder
        btnBidder.setOnAction(e -> selectRole("BIDDER"));

        // Toggle Seller
        btnSeller.setOnAction(e -> selectRole("SELLER"));

        // Mặc định chọn BIDDER
        selectRole("BIDDER");
    }

    private void selectRole(String role) {
        selectedRole = role;

        String activeStyle   = "-fx-background-color: #f59e0b; -fx-text-fill: #000; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: #1a1d26; -fx-text-fill: #94a3b8; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8; -fx-padding: 8; -fx-cursor: hand;";

        if ("BIDDER".equals(role)) {
            btnBidder.setStyle(activeStyle);
            btnSeller.setStyle(inactiveStyle);
        } else {
            btnSeller.setStyle(activeStyle);
            btnBidder.setStyle(inactiveStyle);
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showMsg("Tài khoản và mật khẩu không được trống.", false);
            return;
        }
        if (fullName.isEmpty()){
            showMsg("Họ và tên không được trống.", false);
            return;
        }
        if (email.isEmpty()){
            showMsg("Email không được trống.", false);
            return;
        }
        if (password.length() < 6) {
            showMsg("Mật khẩu phải có ít nhất 6 ký tự.", false);
            return;
        }

        registerButton.setDisable(true);

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance()
                    .getClient()
                    .register(selectedRole, username,email, password,fullName);

            Platform.runLater(() -> {
                registerButton.setDisable(false);
                if (res.isOk()) {
                    showMsg("Đăng ký thành công! Đang chuyển về đăng nhập...", true);
                    new Thread(() -> {
                        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                        Platform.runLater(() ->
                                SceneManager.getInstance().switchTo("login2.fxml"));
                    }).start();
                } else {
                    showMsg(res.getMessage() != null
                            ? res.getMessage() : "Đăng ký thất bại.", false);
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