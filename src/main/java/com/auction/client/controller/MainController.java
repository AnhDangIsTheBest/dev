package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.client.SceneManager;
import com.auction.shared.model.Auction;
import com.auction.shared.model.User.Admin;
import com.auction.shared.model.User.User;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller cho main.fxml - layout chính (sidebar + content area).
 * Mỗi mục sidebar load FXML con vào mainPane.
 */
public class MainController {

    private static MainController activeInstance;
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
    private static final String BUTTON_ANIMATION_KEY = "auction.buttonAnimationInstalled";
    private static final String BUTTON_SCALE_TRANSITION_KEY = "auction.buttonScaleTransition";
    private static final Duration CONTENT_ANIMATION_DURATION = Duration.millis(150);
    private static final Duration BUTTON_ANIMATION_DURATION = Duration.millis(90);

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

    public static MainController getActiveInstance() {
        return activeInstance;
    }

    public void openAuctionDetail(String auctionId) {
        openAuctionDetail(auctionId, null);
    }

    public void openAuctionDetail(Auction auction) {
        openAuctionDetail(auction != null ? auction.getAuctionId() : null, auction);
    }

    private void openAuctionDetail(String auctionId, Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/dashboard.fxml"));
            Node content = loader.load();
            content.setOpacity(0);
            content.setTranslateY(10);
            mainPane.setCenter(content);
            setActiveButton(btnDashboard);
            installButtonFeedback(content);
            animateContentIn(content);

            Object controller = loader.getController();
            if (controller instanceof AuctionsController auctionsController) {
                if (auction != null) {
                    auctionsController.openAuction(auction);
                } else {
                    auctionsController.openAuctionById(auctionId);
                }
            }
        } catch (IOException e) {
            setStatus("\u274c L\u1ed7i t\u1ea3i phi\u00ean \u0111\u1ea5u gi\u00e1.");
            System.err.println("[MainController] openAuctionDetail lỗi: " + e.getMessage());
        }
    }

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
            content.setOpacity(0);
            content.setTranslateY(10);
            mainPane.setCenter(content);
            setActiveButton(activeButton);
            installButtonFeedback(content);
            animateContentIn(content);
        } catch (IOException e) {
            setStatus("\u274c L\u1ed7i t\u1ea3i m\u00e0n h\u00ecnh: " + fxmlFile);
            System.err.println("[MainController] loadContent l\u1ed7i: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button btn) {
        if (activeBtn != null) activeBtn.setStyle(INACTIVE_STYLE);
        activeBtn = btn;
        if (btn != null) btn.setStyle(ACTIVE_STYLE);
    }

    private void animateContentIn(Node content) {
        content.setCache(true);
        content.setCacheHint(CacheHint.SPEED);

        FadeTransition fade = new FadeTransition(CONTENT_ANIMATION_DURATION, content);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition translate = new TranslateTransition(CONTENT_ANIMATION_DURATION, content);
        translate.setToY(0);
        translate.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition transition = new ParallelTransition(fade, translate);
        transition.setOnFinished(event -> content.setCache(false));
        transition.play();
    }

    private void installButtonFeedback(Node node) {
        if (node == null) {
            return;
        }

        if (node instanceof ButtonBase button
                && !Boolean.TRUE.equals(button.getProperties().get(BUTTON_ANIMATION_KEY))) {
            button.getProperties().put(BUTTON_ANIMATION_KEY, true);
            button.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> {
                if (!button.isDisabled()) animateButtonScale(button, 1.015);
            });
            button.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (!button.isDisabled()) animateButtonScale(button, 0.965);
            });
            button.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                if (!button.isDisabled()) animateButtonScale(button, button.isHover() ? 1.015 : 1.0);
            });
            button.addEventFilter(MouseEvent.MOUSE_EXITED, event -> animateButtonScale(button, 1.0));
            button.disabledProperty().addListener((obs, wasDisabled, isDisabled) -> {
                if (isDisabled) {
                    Object existing = button.getProperties().get(BUTTON_SCALE_TRANSITION_KEY);
                    if (existing instanceof ScaleTransition transition) {
                        transition.stop();
                    }
                    button.setScaleX(1.0);
                    button.setScaleY(1.0);
                }
            });
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                installButtonFeedback(child);
            }
        }
    }

    private void animateButtonScale(ButtonBase button, double scale) {
        Object existing = button.getProperties().get(BUTTON_SCALE_TRANSITION_KEY);
        if (existing instanceof ScaleTransition transition) {
            transition.stop();
        }

        ScaleTransition transition = new ScaleTransition(BUTTON_ANIMATION_DURATION, button);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        button.getProperties().put(BUTTON_SCALE_TRANSITION_KEY, transition);
        transition.play();
    }

    public void setStatus(String msg) {
        Platform.runLater(() -> statusLabel.setText(msg));
    }
}