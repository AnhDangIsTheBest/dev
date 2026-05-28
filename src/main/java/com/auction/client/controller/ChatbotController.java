package com.auction.client.controller;

import com.auction.client.controller.ai.AuctionChatContextBuilder;
import com.auction.client.controller.ai.GeminiChatClient;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ChatbotController {

    private static final String AI_BUBBLE_STYLE =
            "-fx-background-color: #1a1d26; -fx-text-fill: #e2e8f0; -fx-font-size: 13; " +
                    "-fx-padding: 9 12; -fx-background-radius: 14 14 14 4;";
    private static final String USER_BUBBLE_STYLE =
            "-fx-background-color: #f59e0b; -fx-text-fill: #111827; -fx-font-size: 13; " +
                    "-fx-padding: 9 12; -fx-background-radius: 14 14 4 14;";
    private static final String ERROR_BUBBLE_STYLE =
            "-fx-background-color: #3f1d22; -fx-text-fill: #fecaca; -fx-font-size: 13; " +
                    "-fx-padding: 9 12; -fx-background-radius: 14 14 14 4;";
    private static final Duration MESSAGE_ANIMATION_DURATION = Duration.millis(140);
    private static final Duration STREAM_FLUSH_INTERVAL = Duration.millis(33);

    private final GeminiChatClient chatClient = new GeminiChatClient();
    private final AuctionChatContextBuilder contextBuilder = new AuctionChatContextBuilder();
    private final Object streamLock = new Object();
    private final StringBuilder pendingStreamText = new StringBuilder();
    private Timeline streamFlushTimeline;

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox messageList;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        addMessage("Xin chào! Mình có thể xem dữ liệu đấu giá của bạn để hỗ trợ.", false);
        if (chatClient.hasApiKey()) {
            setStatus("Ready");
        } else {
            setStatus("Missing GEMINI_API_KEY");
        }
    }

    @FXML
    private void handleSend() {
        String message = messageField.getText();
        if (message == null || message.isBlank()) {
            return;
        }

        String trimmedMessage = message.trim();
        messageField.clear();
        addMessage(trimmedMessage, true);
        Label aiBubble = addMessage("Đang trả lời...", false);
        setLoading(true);
        startStreamFlush(aiBubble);

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("Đang đọc dữ liệu..."));
                String dataContext = contextBuilder.build(trimmedMessage);
                Platform.runLater(() -> setStatus("Đang trả lời..."));
                chatClient.sendMessage(trimmedMessage, dataContext, chunk ->
                        queueAssistantChunk(chunk));
                Platform.runLater(() -> {
                    flushPendingAssistantText(aiBubble);
                    stopStreamFlush();
                    if ("Đang trả lời...".equals(aiBubble.getText())) {
                        aiBubble.setText("Không có phản hồi.");
                    }
                    setStatus("Ready");
                    setLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    stopStreamFlush();
                    clearPendingAssistantText();
                    aiBubble.setStyle(ERROR_BUBBLE_STYLE);
                    aiBubble.setText("Lỗi: " + e.getMessage());
                    setStatus("Error");
                    setLoading(false);
                });
            }
        }, "gemini-chat-stream").start();
    }

    private Label addMessage(String text, boolean fromUser) {
        HBox row = new HBox();
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(fromUser ? 250 : 270);
        bubble.setStyle(fromUser ? USER_BUBBLE_STYLE : AI_BUBBLE_STYLE);

        row.getChildren().add(bubble);
        messageList.getChildren().add(row);
        animateMessageRow(row);
        scrollToBottom();
        return bubble;
    }

    private void appendToBubble(Label bubble, String text) {
        if ("Đang trả lời...".equals(bubble.getText())) {
            bubble.setText("");
        }
        bubble.setText(bubble.getText() + text);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (Platform.isFxApplicationThread()) {
            chatScrollPane.setVvalue(1.0);
        } else {
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        }
    }

    private void setLoading(boolean loading) {
        sendButton.setDisable(loading);
        messageField.setDisable(loading);
        if (loading) {
            setStatus("Đang chuẩn bị...");
        } else {
            messageField.requestFocus();
        }
    }

    private void setStatus(String status) {
        statusLabel.setText(status);
    }

    private void startStreamFlush(Label bubble) {
        clearPendingAssistantText();
        stopStreamFlush();

        streamFlushTimeline = new Timeline(
                new KeyFrame(STREAM_FLUSH_INTERVAL, event -> flushPendingAssistantText(bubble)));
        streamFlushTimeline.setCycleCount(Timeline.INDEFINITE);
        streamFlushTimeline.play();
    }

    private void queueAssistantChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        synchronized (streamLock) {
            pendingStreamText.append(chunk);
        }
    }

    private void flushPendingAssistantText(Label bubble) {
        String text;
        synchronized (streamLock) {
            if (pendingStreamText.length() == 0) {
                return;
            }
            text = pendingStreamText.toString();
            pendingStreamText.setLength(0);
        }
        appendToBubble(bubble, text);
    }

    private void stopStreamFlush() {
        if (streamFlushTimeline != null) {
            streamFlushTimeline.stop();
            streamFlushTimeline = null;
        }
    }

    private void clearPendingAssistantText() {
        synchronized (streamLock) {
            pendingStreamText.setLength(0);
        }
    }

    private void animateMessageRow(Node row) {
        row.setOpacity(0);
        row.setTranslateY(8);
        row.setCache(true);
        row.setCacheHint(CacheHint.SPEED);

        FadeTransition fade = new FadeTransition(MESSAGE_ANIMATION_DURATION, row);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition translate = new TranslateTransition(MESSAGE_ANIMATION_DURATION, row);
        translate.setToY(0);
        translate.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition transition = new ParallelTransition(fade, translate);
        transition.setOnFinished(event -> row.setCache(false));
        transition.play();
    }
}
