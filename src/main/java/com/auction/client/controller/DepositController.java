package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.User;
import com.auction.shared.network.protocol.SocketMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.text.NumberFormat;
import java.util.Locale;

public class DepositController {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML private Label balanceLabel;
    @FXML private TextField amountField;
    @FXML private Button btnDeposit;
    @FXML private Label resultLabel;

    @FXML
    public void initialize() {
        renderBalance();
        showInfo("");
    }

    @FXML
    private void handleDeposit() {
        double amount;
        try {
            amount = parseAmount(amountField.getText());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        ClientContext ctx = ClientContext.getInstance();
        User user = ctx.getCurrentUser();
        if (user == null) {
            showError("Ban can dang nhap de nap tien.");
            return;
        }

        setLoading(true);
        showInfo("Dang xu ly...");

        new Thread(() -> {
            SocketMessage res = ctx.getClient().depositUser(user.getId(), amount);

            Platform.runLater(() -> {
                setLoading(false);

                if (res != null && res.isOk()) {
                    Object updatedUser = res.get("user");
                    if (updatedUser instanceof User u) {
                        ctx.setCurrentUser(u);
                    }

                    amountField.clear();
                    renderBalance();
                    showSuccess(res.getMessage() != null ? res.getMessage() : "Nap tien thanh cong.");
                } else {
                    showError(res != null && res.getMessage() != null
                            ? res.getMessage()
                            : "Nap tien that bai.");
                }
            });
        }, "deposit-user").start();
    }

    private double parseAmount(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Vui long nhap so tien.");
        }

        String normalized = text.trim().replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("So tien khong hop le.");
        }

        double amount = Double.parseDouble(normalized);
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien phai lon hon 0.");
        }

        return amount;
    }

    private void renderBalance() {
        User user = ClientContext.getInstance().getCurrentUser();
        double balance = user instanceof Bidder bidder ? bidder.getBalance() : 0;
        balanceLabel.setText(fmtVND(balance));
    }

    private void setLoading(boolean loading) {
        btnDeposit.setDisable(loading);
        amountField.setDisable(loading);
    }

    private void showSuccess(String message) {
        resultLabel.setStyle("-fx-text-fill: #22c55e;");
        resultLabel.setText(message);
    }

    private void showError(String message) {
        resultLabel.setStyle("-fx-text-fill: #ef4444;");
        resultLabel.setText(message);
    }

    private void showInfo(String message) {
        resultLabel.setStyle("-fx-text-fill: #94a3b8;");
        resultLabel.setText(message);
    }

    private String fmtVND(double amount) {
        return VND.format((long) amount) + " d";
    }
}
