package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.client.SceneManager;
import com.auction.shared.model.User.Admin;
import com.auction.shared.model.User.User;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Controller cho main.fxml - layout chính (sidebar + content area).
 * Mỗi mục sidebar load FXML con vào mainPane.
 */
public class MainController {

    @FXML private Label       userNameLabel;
    @FXML private Label       userRoleLabel;
    @FXML private Label       statusLabel;
    @FXML private BorderPane  mainPane;
    @FXML private VBox        sidebar;

    // Sidebar buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnMyBids;
    @FXML private Button btnAutoBid;
    @FXML private Button btnSeller;
    @FXML private Button btnAdmin;
    @FXML private Button btnHistory;
    @FXML private Button btnMySell;

    private Button activeBtn;

    // Style constants
    private static final String ACTIVE_STYLE =
            "-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: #f59e0b;" +
                    "-fx-font-weight: bold; -fx-padding: 10 16; -fx-cursor: hand; " +
                    "-fx-alignment: CENTER_LEFT; -fx-font-size: 13; -fx-border-color: transparent;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: #94a3b8;" +
                    "-fx-padding: 10 16; -fx-cursor: hand; " +
                    "-fx-alignment: CENTER_LEFT; -fx-font-size: 13;";

    @FXML
    public void initialize() {
        User user = ClientContext.getInstance().getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getUsername());
            userRoleLabel.setText(user.getRole());
        }

        // Ẩn nút Admin nếu không phải admin
        btnAdmin.setVisible(user instanceof Admin);
        btnAdmin.setManaged(user instanceof Admin);

        // Ẩn Seller nếu không phải seller
        boolean isSeller = user != null && "SELLER".equalsIgnoreCase(user.getRole());
        btnSeller.setVisible(isSeller);
        btnSeller.setManaged(isSeller);
        btnMySell.setVisible(isSeller);
        btnMySell.setManaged(isSeller);

        // Ẩn MyBids/AutoBid nếu là seller hoặc admin
        boolean showBidderMenus = !isSeller && !(user instanceof Admin);
        btnMyBids.setVisible(showBidderMenus);
        btnMyBids.setManaged(showBidderMenus);
        btnAutoBid.setVisible(showBidderMenus);
        btnAutoBid.setManaged(showBidderMenus);

        // Mở Dashboard mặc định
        setStatus("Đã kết nối đến server.");
        showDashboard(null);
    }

    // ── Điều hướng ───────────────────────────────────────────────

    @FXML public void showDashboard(ActionEvent e) { loadContent("dashboard.fxml", btnDashboard); }
    @FXML public void showMyBids(ActionEvent e)    { loadContent("my-bids.fxml",   btnMyBids); }
    @FXML public void showAutoBid(ActionEvent e)   { loadContent("auto-bid.fxml",  btnAutoBid); }
    @FXML public void showSeller(ActionEvent e)    { loadContent("seller.fxml",    btnSeller); }
    @FXML public void showMySell(ActionEvent e)    { loadContent("my-sell.fxml",   btnMySell); }
    @FXML public void showAdmin(ActionEvent e)     { loadContent("admin.fxml",     btnAdmin); }
    @FXML public void showHistory(ActionEvent e)   { loadContent("history.fxml",   btnHistory); }

    @FXML
    private void handleLogout(ActionEvent event) {
        new Thread(() -> {
            ClientContext.getInstance().logout();
            Platform.runLater(() ->
                    SceneManager.getInstance().switchTo("login2.fxml", 600, 520));
        }).start();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void loadContent(String fxmlFile, Button activeButton) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/" + fxmlFile));
            Node content = loader.load();
            mainPane.setCenter(content);
            setActiveButton(activeButton);
        } catch (IOException e) {
            setStatus("❌ Lỗi tải màn hình: " + fxmlFile);
            System.err.println("[MainController] loadContent lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button btn) {
        if (activeBtn != null) activeBtn.setStyle(INACTIVE_STYLE);
        activeBtn = btn;
        if (btn != null) btn.setStyle(ACTIVE_STYLE);
    }

    public void setStatus(String msg) {
        Platform.runLater(() -> statusLabel.setText(msg));
    }
}