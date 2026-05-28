package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.client.SceneManager;
import com.auction.shared.model.Auction;
import com.auction.shared.model.User.Admin;
import com.auction.shared.model.User.User;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class MainController {

    private static MainController activeInstance;

    @FXML private Label       userNameLabel;
    @FXML private Label       userRoleLabel;
    @FXML private Label       statusLabel;
    @FXML private StackPane   rootPane;
    @FXML private BorderPane  mainPane;
    @FXML private VBox        sidebar;
    @FXML private VBox        chatPanel;
    @FXML private Button      chatToggleButton;

    @FXML private Button btnDashboard;
    @FXML private Button btnMyBids;
    @FXML private Button btnSeller;
    @FXML private Button btnAdmin;
    @FXML private Button btnHistory;
    @FXML private Button btnMySell;
    @FXML private Button btnDeposit;

    private Button activeBtn;

    private static final String ACTIVE_STYLE =
            "-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: #f59e0b;" +
                    "-fx-font-weight: bold; -fx-padding: 10 16; -fx-cursor: hand; " +
                    "-fx-alignment: CENTER_LEFT; -fx-font-size: 13; -fx-border-color: transparent;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: #94a3b8;" +
                    "-fx-padding: 10 16; -fx-cursor: hand; " +
                    "-fx-alignment: CENTER_LEFT; -fx-font-size: 13;";
    private static final Duration CHAT_ANIMATION_DURATION = Duration.millis(220);
    private static final Duration CONTENT_ANIMATION_DURATION = Duration.millis(150);
    private static final Duration BUTTON_ANIMATION_DURATION = Duration.millis(90);
    private static final String BUTTON_ANIMATION_KEY = "auction.buttonAnimationInstalled";
    private static final String BUTTON_SCALE_TRANSITION_KEY = "auction.buttonScaleTransition";

    private ParallelTransition chatAnimation;
    private ParallelTransition toggleAnimation;
    private boolean chatVisible;

    @FXML
    public void initialize() {
        activeInstance = this;
        User user = ClientContext.getInstance().getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getUsername());
            userRoleLabel.setText(formatRole(user));
        }

        setupChatbotAnimationState();
        installButtonFeedback(rootPane);

        btnAdmin.setVisible(user instanceof Admin);
        btnAdmin.setManaged(user instanceof Admin);

        btnSeller.setVisible(true);
        btnSeller.setManaged(true);
        btnMySell.setVisible(true);
        btnMySell.setManaged(true);
        btnMyBids.setVisible(true);
        btnMyBids.setManaged(true);
        btnDeposit.setVisible(true);
        btnDeposit.setManaged(true);

        setStatus("\u0110\u00e3 k\u1ebft n\u1ed1i \u0111\u1ebfn server.");
        showDashboard(null);
    }

    @FXML public void showDashboard(ActionEvent e) { loadContent("dashboard.fxml", btnDashboard); }
    @FXML public void showMyBids(ActionEvent e)    { loadContent("my-bids.fxml",   btnMyBids); }
    @FXML public void showSeller(ActionEvent e)    { loadContent("seller.fxml",    btnSeller); }
    @FXML public void showMySell(ActionEvent e)    { loadContent("my-sell.fxml",   btnMySell); }
    @FXML public void showAdmin(ActionEvent e)     { loadContent("admin.fxml",     btnAdmin); }
    @FXML public void showHistory(ActionEvent e)   { loadContent("history.fxml",   btnHistory); }
    @FXML public void showDeposit(ActionEvent e)   { loadContent("deposit.fxml",   btnDeposit); }

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
    private void toggleChatbot(ActionEvent event) {
        setChatbotVisible(!chatVisible);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        new Thread(() -> {
            ClientContext.getInstance().logout();
            Platform.runLater(() ->
                    SceneManager.getInstance().switchTo("login2.fxml", 600, 520));
        }).start();
    }

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

    private void setChatbotVisible(boolean visible) {
        if (chatPanel == null || visible == chatVisible) {
            return;
        }

        chatVisible = visible;
        if (chatAnimation != null) {
            chatAnimation.stop();
        }

        chatPanel.setManaged(true);
        chatPanel.setVisible(true);
        chatPanel.setMouseTransparent(!visible);

        if (visible) {
            chatPanel.toFront();
            animateChatPanel(1.0, 1.0, 0, () -> {
                if (chatToggleButton != null) {
                    chatToggleButton.setVisible(false);
                    chatToggleButton.setManaged(false);
                    chatToggleButton.setDisable(false);
                }
            });
            animateToggleButton(false);
        } else {
            if (chatToggleButton != null) {
                chatToggleButton.setManaged(true);
                chatToggleButton.setVisible(true);
                chatToggleButton.setDisable(true);
                chatToggleButton.setOpacity(0);
                chatToggleButton.setScaleX(0.82);
                chatToggleButton.setScaleY(0.82);
                chatToggleButton.toFront();
            }
            chatPanel.toFront();
            animateChatPanel(0, 0.94, 18, () -> {
                chatPanel.setVisible(false);
                chatPanel.setManaged(false);
                chatPanel.setMouseTransparent(true);
                animateToggleButton(true);
            });
        }
    }

    private void setupChatbotAnimationState() {
        chatVisible = false;
        if (chatPanel != null) {
            chatPanel.setManaged(false);
            chatPanel.setVisible(false);
            chatPanel.setMouseTransparent(true);
            chatPanel.setOpacity(0);
            chatPanel.setScaleX(0.94);
            chatPanel.setScaleY(0.94);
            chatPanel.setTranslateY(18);
            chatPanel.setCache(true);
            chatPanel.setCacheHint(CacheHint.SPEED);
        }
        if (chatToggleButton != null) {
            chatToggleButton.setManaged(true);
            chatToggleButton.setVisible(true);
            chatToggleButton.setOpacity(1);
            chatToggleButton.setScaleX(1);
            chatToggleButton.setScaleY(1);
            chatToggleButton.setDisable(false);
        }
    }

    private void animateChatPanel(double opacity, double scale, double translateY, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(CHAT_ANIMATION_DURATION, chatPanel);
        fade.setToValue(opacity);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scaleTransition = new ScaleTransition(CHAT_ANIMATION_DURATION, chatPanel);
        scaleTransition.setToX(scale);
        scaleTransition.setToY(scale);
        scaleTransition.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition translate = new TranslateTransition(CHAT_ANIMATION_DURATION, chatPanel);
        translate.setToY(translateY);
        translate.setInterpolator(Interpolator.EASE_BOTH);

        chatAnimation = new ParallelTransition(fade, scaleTransition, translate);
        chatAnimation.setOnFinished(event -> {
            chatAnimation = null;
            if (onFinished != null) {
                onFinished.run();
            }
        });
        chatAnimation.play();
    }

    private void animateToggleButton(boolean visible) {
        if (chatToggleButton == null) {
            return;
        }
        if (toggleAnimation != null) {
            toggleAnimation.stop();
        }

        if (visible) {
            chatToggleButton.setManaged(true);
            chatToggleButton.setVisible(true);
            chatToggleButton.setDisable(true);
            chatToggleButton.toFront();
        }

        FadeTransition fade = new FadeTransition(Duration.millis(140), chatToggleButton);
        fade.setToValue(visible ? 1 : 0);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scale = new ScaleTransition(Duration.millis(140), chatToggleButton);
        scale.setToX(visible ? 1 : 0.82);
        scale.setToY(visible ? 1 : 0.82);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        toggleAnimation = new ParallelTransition(fade, scale);
        toggleAnimation.setOnFinished(event -> {
            toggleAnimation = null;
            chatToggleButton.setDisable(false);
            if (!visible) {
                chatToggleButton.setVisible(false);
                chatToggleButton.setManaged(false);
            }
        });
        toggleAnimation.play();
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

    private String formatRole(User user) {
        if (user instanceof Admin) return "ADMIN";
        return "T\u00c0I KHO\u1ea2N \u0110\u1ea4U GI\u00c1";
    }

    public void setStatus(String msg) {
        Platform.runLater(() -> statusLabel.setText(msg));
    }
}
