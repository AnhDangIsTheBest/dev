package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.User.User;
import com.auction.shared.network.protocol.SocketMessage;
import com.auction.shared.network.protocol.SocketMessage.Action;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller cho auctions.fxml.
 * Layout dùng StackPane với 2 lớp:
 *   - listView:   danh sách phiên đấu giá (mặc định hiện)
 *   - detailPanel: chi tiết phiên (hiện khi click row, ẩn khi ấn "Quay lại")
 */
public class AuctionsController {

    // ── FXML: Views ──────────────────────────────────────────────
    @FXML private VBox listView;
    @FXML private Region detailPanel;

    // ── FXML: Stat cards (chỉ có trong dashboard.fxml) ──────────
    @FXML private Label statRunning;  // PHIÊN ĐANG CHẠY
    @FXML private Label statTotal;    // TỔNG PHIÊN
    @FXML private Label statBids;     // TỔNG BID

    // ── FXML: Danh sách ──────────────────────────────────────────
    @FXML private TableView<Auction>          auctionTable;
    @FXML private TilePane                    upcomingCards;
    @FXML private TilePane                    runningCards;
    @FXML private TilePane                    pastCards;
    @FXML private Button                      upcomingAllButton;
    @FXML private Button                      runningAllButton;
    @FXML private Button                      pastAllButton;
    @FXML private TableColumn<Auction,String> colName;
    @FXML private TableColumn<Auction,String> colCategory;
    @FXML private TableColumn<Auction,String> colPrice;
    @FXML private TableColumn<Auction,String> colStatus;
    @FXML private TableColumn<Auction,String> colTimer;
    @FXML private TableColumn<Auction,String> colLeader;
    @FXML private Label                       statusLabel;

    // ── FXML: Chi tiết ───────────────────────────────────────────
    @FXML private Label   detailName;
    @FXML private Label   detailTitleLarge;
    @FXML private Label   detailAssetCode;
    @FXML private Label   detailStartingPrice;
    @FXML private Label   detailStepPrice;
    @FXML private Label   detailDeposit;
    @FXML private Label   detailFee;
    @FXML private Label   detailStartTime;
    @FXML private Label   detailEndTime;
    @FXML private Label   detailAddress;
    @FXML private Label   detailType;
    @FXML private Label   countdownDays;
    @FXML private Label   countdownHours;
    @FXML private Label   countdownMinutes;
    @FXML private Label   countdownSeconds;
    @FXML private ImageView detailImage;
    @FXML private Label   detailPrice;
    @FXML private Label   detailTimer;
    @FXML private Label   detailStatus;
    @FXML private Label   detailLeader;
    @FXML private Label   detailDesc;
    @FXML private Label   detailAntiSnipe;
    @FXML private Label   countdownTitle;
    @FXML private Label   bidResultLabel;
    @FXML private VBox     auctionLiveBox;
    @FXML private VBox     auctionSuccessBox;
    @FXML private VBox     currentInfoBox;
    @FXML private Label   successStartingPrice;
    @FXML private Label   successWinningPrice;
    @FXML private Label   successWinnerCode;

    // ── FXML: Đặt giá ────────────────────────────────────────────
    @FXML private TextField bidAmountField;
    @FXML private Button    placeBidButton;
    @FXML private TextField autoMaxBidField;
    @FXML private TextField autoIncrementField;
    @FXML private Button    autoBidButton;
    @FXML private Button    cancelAutoBidButton;
    @FXML private Label     autoBidResultLabel;

    // ── FXML: Chart ──────────────────────────────────────────────
    @FXML private LineChart<Number,Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private VBox chartBox;

    // ── FXML: Lịch sử ────────────────────────────────────────────
    @FXML private ListView<String> bidHistoryList;

    // ── State ─────────────────────────────────────────────────────
    private final ObservableList<Auction> auctions = FXCollections.observableArrayList();
    private Auction selectedAuction;
    private String renderedChartKey;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auction-timer");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> timerTask;

    private static final NumberFormat VND =
            NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final javafx.util.Duration VIEW_ANIMATION_DURATION = javafx.util.Duration.millis(260);
    private static final javafx.util.Duration VIEW_OUT_DURATION = javafx.util.Duration.millis(120);
    private static final javafx.util.Duration CARD_ANIMATION_DURATION = javafx.util.Duration.millis(120);
    private static final javafx.util.Duration FIELD_FEEDBACK_DURATION = javafx.util.Duration.millis(95);
    private static final javafx.util.Duration PANEL_STAGGER_DURATION = javafx.util.Duration.millis(70);
    private static final javafx.util.Duration RESULT_ANIMATION_DURATION = javafx.util.Duration.millis(180);
    private static final String CARD_BASE_STYLE = "-fx-background-color: #15191d; -fx-border-color: #31363c;"
            + "-fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14; -fx-cursor: hand;";
    private static final String CARD_HOVER_STYLE = "-fx-background-color: #1b2026; -fx-border-color: rgba(245,158,11,0.55);"
            + "-fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14; -fx-cursor: hand;"
            + "-fx-effect: dropshadow(gaussian, rgba(245,158,11,0.18), 18, 0.18, 0, 7);";
    private static final String MOTION_INSTALLED_KEY = "auction.motionInstalled";

    private ParallelTransition viewTransition;
    private boolean detailVisible;

    // ── Init ─────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        setupInitialViews();
        installBidFormMotion();
        installSeeAllButtonMotion();
        loadAuctions();

        // Lắng nghe broadcast từ server
        ClientContext.getInstance().getClient().setBroadcastListener(msg -> {
            if (msg.getAction() == Action.BROADCAST_BID_UPDATE
                    || msg.getAction() == Action.BROADCAST_AUCTION_END) {
                Platform.runLater(() -> handleBroadcast(msg));
            }
        });

        // Bắt đầu ở màn hình danh sách
        detailVisible = false;
    }

    // ── Navigation ────────────────────────────────────────s───────

    /** Hiện danh sách, ẩn chi tiết */
    private void showList() {
        // listView/detailPanel chỉ có trong auctions.fxml, không có trong dashboard.fxml
        switchAuctionView(false);
        if (timerTask != null) timerTask.cancel(false);
        selectedAuction = null;
    }

    /** Hiện chi tiết, ẩn danh sách */
    private void showDetail() {
        switchAuctionView(true);
    }

    private void setupInitialViews() {
        detailVisible = false;
        if (listView != null) {
            setViewState(listView, true, 1.0, 0, 1.0);
            listView.setMouseTransparent(false);
        }
        if (detailPanel != null) {
            setViewState(detailPanel, false, 0, 18, 0.985);
            detailPanel.setMouseTransparent(true);
        }
    }

    private void switchAuctionView(boolean showDetailView) {
        if (listView == null || detailPanel == null) {
            if (listView != null) {
                listView.setVisible(!showDetailView);
                listView.setManaged(!showDetailView);
            }
            if (detailPanel != null) {
                detailPanel.setVisible(showDetailView);
                detailPanel.setManaged(showDetailView);
            }
            detailVisible = showDetailView;
            return;
        }

        if (detailVisible == showDetailView
                && ((showDetailView && detailPanel.isVisible())
                || (!showDetailView && listView.isVisible()))) {
            return;
        }

        if (viewTransition != null) {
            viewTransition.stop();
        }

        Node incoming = showDetailView ? detailPanel : listView;
        Node outgoing = showDetailView ? listView : detailPanel;
        detailVisible = showDetailView;

        incoming.setManaged(true);
        incoming.setVisible(true);
        incoming.setMouseTransparent(false);
        incoming.toFront();
        outgoing.setMouseTransparent(true);

        if (incoming instanceof ScrollPane scrollPane) {
            scrollPane.setVvalue(0);
        }

        double incomingOffset = showDetailView ? 24 : -18;
        double outgoingOffset = showDetailView ? -14 : 18;
        setAnimatedCache(incoming, true);
        setAnimatedCache(outgoing, true);
        incoming.setOpacity(0);
        incoming.setTranslateY(incomingOffset);
        incoming.setScaleX(0.985);
        incoming.setScaleY(0.985);

        FadeTransition fadeOut = new FadeTransition(VIEW_OUT_DURATION, outgoing);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition moveOut = new TranslateTransition(VIEW_OUT_DURATION, outgoing);
        moveOut.setToY(outgoingOffset);
        moveOut.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition shrinkOut = new ScaleTransition(VIEW_OUT_DURATION, outgoing);
        shrinkOut.setToX(0.99);
        shrinkOut.setToY(0.99);
        shrinkOut.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(VIEW_ANIMATION_DURATION, incoming);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition moveIn = new TranslateTransition(VIEW_ANIMATION_DURATION, incoming);
        moveIn.setToY(0);
        moveIn.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scaleIn = new ScaleTransition(VIEW_ANIMATION_DURATION, incoming);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.EASE_BOTH);

        viewTransition = new ParallelTransition(fadeOut, moveOut, shrinkOut, fadeIn, moveIn, scaleIn);
        viewTransition.setOnFinished(event -> {
            outgoing.setVisible(false);
            outgoing.setManaged(false);
            outgoing.setOpacity(0);
            outgoing.setTranslateY(outgoingOffset);
            outgoing.setScaleX(0.99);
            outgoing.setScaleY(0.99);
            incoming.setOpacity(1);
            incoming.setTranslateY(0);
            incoming.setScaleX(1);
            incoming.setScaleY(1);
            setAnimatedCache(incoming, false);
            setAnimatedCache(outgoing, false);
            viewTransition = null;
        });
        viewTransition.play();

        if (showDetailView) {
            animateDetailSections();
        }
    }

    private void setViewState(Node node, boolean visible, double opacity, double translateY, double scale) {
        node.setVisible(visible);
        node.setManaged(visible);
        node.setOpacity(opacity);
        node.setTranslateY(translateY);
        node.setScaleX(scale);
        node.setScaleY(scale);
    }

    private void setAnimatedCache(Node node, boolean enabled) {
        if (node == null) return;
        node.setCache(enabled);
        node.setCacheHint(enabled ? CacheHint.SPEED : CacheHint.DEFAULT);
    }

    private void animateDetailSections() {
        double stagger = PANEL_STAGGER_DURATION.toMillis();
        animateNodeEntrance(detailName, 0, 12);
        animateNodeEntrance(detailImage != null ? detailImage.getParent() : null, stagger, 18);
        animateNodeEntrance(detailTitleLarge, stagger * 1.5, 12);
        animateNodeEntrance(auctionLiveBox != null && auctionLiveBox.isVisible() ? auctionLiveBox : auctionSuccessBox, stagger * 2, 18);
        animateNodeEntrance(currentInfoBox != null && currentInfoBox.isVisible() ? currentInfoBox : null, stagger * 2.5, 14);
        animateNodeEntrance(chartBox != null ? chartBox.getParent() : priceChart != null ? priceChart.getParent() : null, stagger * 3, 16);
        animateNodePop(detailPrice);
    }

    private void animateDetailRefresh() {
        animateNodePop(detailStatus);
        animateNodePop(detailPrice);
        animateNodePop(detailTimer);
        animateNodePop(detailLeader);
    }

    private void animateNodeEntrance(Node node, double delayMillis, double offsetY) {
        if (node == null || !node.isVisible()) return;
        setAnimatedCache(node, true);
        node.setOpacity(0);
        node.setTranslateY(offsetY);

        FadeTransition fade = new FadeTransition(RESULT_ANIMATION_DURATION, node);
        fade.setToValue(1);
        fade.setDelay(javafx.util.Duration.millis(delayMillis));
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition move = new TranslateTransition(RESULT_ANIMATION_DURATION, node);
        move.setToY(0);
        move.setDelay(javafx.util.Duration.millis(delayMillis));
        move.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition transition = new ParallelTransition(fade, move);
        transition.setOnFinished(event -> setAnimatedCache(node, false));
        transition.play();
    }

    private void animateNodePop(Node node) {
        if (node == null || !node.isVisible()) return;
        setAnimatedCache(node, true);

        ScaleTransition up = new ScaleTransition(FIELD_FEEDBACK_DURATION, node);
        up.setToX(1.045);
        up.setToY(1.045);
        up.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition down = new ScaleTransition(FIELD_FEEDBACK_DURATION, node);
        down.setToX(1.0);
        down.setToY(1.0);
        down.setInterpolator(Interpolator.EASE_BOTH);

        SequentialTransition sequence = new SequentialTransition(up, down);
        sequence.setOnFinished(event -> setAnimatedCache(node, false));
        sequence.play();
    }

    private void animateResultLabel(Label label, boolean success) {
        if (label == null) return;
        label.setOpacity(0);
        label.setTranslateY(success ? 8 : 0);

        FadeTransition fade = new FadeTransition(RESULT_ANIMATION_DURATION, label);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition move = new TranslateTransition(RESULT_ANIMATION_DURATION, label);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition transition = new ParallelTransition(fade, move);
        transition.play();

        if (success) {
            animateNodePop(label);
        } else {
            shakeNode(label);
        }
    }

    private void shakeNode(Node node) {
        if (node == null) return;
        TranslateTransition left = new TranslateTransition(javafx.util.Duration.millis(38), node);
        left.setByX(-7);
        TranslateTransition right = new TranslateTransition(javafx.util.Duration.millis(58), node);
        right.setByX(14);
        TranslateTransition back = new TranslateTransition(javafx.util.Duration.millis(58), node);
        back.setByX(-14);
        TranslateTransition settle = new TranslateTransition(javafx.util.Duration.millis(38), node);
        settle.setByX(7);
        new SequentialTransition(left, right, back, settle).play();
    }

    private void installBidFormMotion() {
        installFocusMotion(bidAmountField);
        installFocusMotion(autoMaxBidField);
        installFocusMotion(autoIncrementField);
    }

    private void installSeeAllButtonMotion() {
        prepareSeeAllButton(upcomingAllButton);
        prepareSeeAllButton(runningAllButton);
        prepareSeeAllButton(pastAllButton);
    }

    private void prepareSeeAllButton(Button button) {
        if (button == null) return;
        button.setMinHeight(32);
        button.setFocusTraversable(false);
    }

    private void installFocusMotion(Node node) {
        if (node == null || Boolean.TRUE.equals(node.getProperties().get(MOTION_INSTALLED_KEY))) {
            return;
        }
        node.getProperties().put(MOTION_INSTALLED_KEY, true);
        node.focusedProperty().addListener((obs, oldValue, focused) ->
                animateScale(node, focused ? 1.012 : 1.0, FIELD_FEEDBACK_DURATION));
    }

    private void installCardMotion(VBox card) {
        if (card == null) return;
        card.setOnMouseEntered(event -> {
            card.setStyle(CARD_HOVER_STYLE);
            animateCard(card, 1.018, -4);
        });
        card.setOnMouseExited(event -> {
            card.setStyle(CARD_BASE_STYLE);
            animateCard(card, 1.0, 0);
        });
        card.setOnMousePressed(event -> animateCard(card, 0.982, 0));
        card.setOnMouseReleased(event -> {
            boolean hover = card.isHover();
            card.setStyle(hover ? CARD_HOVER_STYLE : CARD_BASE_STYLE);
            animateCard(card, hover ? 1.018 : 1.0, hover ? -4 : 0);
        });
    }

    private void animateCard(Node node, double scale, double translateY) {
        setAnimatedCache(node, true);

        ScaleTransition scaleTransition = new ScaleTransition(CARD_ANIMATION_DURATION, node);
        scaleTransition.setToX(scale);
        scaleTransition.setToY(scale);
        scaleTransition.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition translate = new TranslateTransition(CARD_ANIMATION_DURATION, node);
        translate.setToY(translateY);
        translate.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition transition = new ParallelTransition(scaleTransition, translate);
        transition.setOnFinished(event -> setAnimatedCache(node, false));
        transition.play();
    }

    private void animateScale(Node node, double scale, javafx.util.Duration duration) {
        if (node == null) return;
        ScaleTransition transition = new ScaleTransition(duration, node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.play();
    }

    private void animateSelectionFlash(Node node) {
        if (node == null) return;
        ScaleTransition up = new ScaleTransition(javafx.util.Duration.millis(75), node);
        up.setToX(1.025);
        up.setToY(1.025);
        up.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition down = new ScaleTransition(javafx.util.Duration.millis(95), node);
        down.setToX(node.isHover() ? 1.018 : 1.0);
        down.setToY(node.isHover() ? 1.018 : 1.0);
        down.setInterpolator(Interpolator.EASE_BOTH);
        new SequentialTransition(up, down).play();
    }

    private void animateSeeAllButton(Node node) {
        if (node == null) return;
        setAnimatedCache(node, true);

        ScaleTransition press = new ScaleTransition(javafx.util.Duration.millis(70), node);
        press.setToX(0.94);
        press.setToY(0.94);
        press.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition lift = new ScaleTransition(javafx.util.Duration.millis(95), node);
        lift.setToX(1.055);
        lift.setToY(1.055);
        lift.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(javafx.util.Duration.millis(120), node);
        settle.setToX(1.0);
        settle.setToY(1.0);
        settle.setInterpolator(Interpolator.EASE_BOTH);

        SequentialTransition sequence = new SequentialTransition(press, lift, settle);
        sequence.setOnFinished(event -> setAnimatedCache(node, false));
        sequence.play();
    }

    private void animateFocusedSection(Node source) {
        TilePane pane = null;
        if (source == upcomingAllButton) {
            pane = upcomingCards;
        } else if (source == runningAllButton) {
            pane = runningCards;
        } else if (source == pastAllButton) {
            pane = pastCards;
        }
        if (pane == null) return;

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(120), pane);
        fade.setFromValue(0.72);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition move = new TranslateTransition(javafx.util.Duration.millis(140), pane);
        move.setFromY(6);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, move).play();
    }

    /** Nút "← Quay lại" trong màn hình chi tiết */
    @FXML
    private void handleBack(ActionEvent event) {
        showList();
    }

    // ── TableView setup ──────────────────────────────────────────

    private void setupTable() {
        if (auctionTable == null) return; // dashboard.fxml dùng card grid, auctions.fxml dùng table
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getName()));
        colCategory.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getType()));
        colPrice.setCellValueFactory(d ->
                new SimpleStringProperty(fmtVND(d.getValue().getCurrentPrice())));
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(statusDisplay(d.getValue().getStatus())));
        colTimer.setCellValueFactory(d ->
                new SimpleStringProperty(fmtTimer(d.getValue().getSecondRemaining())));
        colLeader.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getLeadBidderName()));

        auctionTable.setItems(auctions);

        // Click vào row → mở chi tiết
        auctionTable.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) {
                    animateSelectionFlash(row);
                    onSelectAuction(row.getItem());
                }
            });
            row.setOnMousePressed(e -> {
                if (!row.isEmpty()) animateScale(row, 0.994, CARD_ANIMATION_DURATION);
            });
            row.setOnMouseReleased(e -> {
                if (!row.isEmpty()) animateScale(row, 1.0, CARD_ANIMATION_DURATION);
            });
            row.setOnMouseExited(e -> animateScale(row, 1.0, CARD_ANIMATION_DURATION));
            return row;
        });

        // Color status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                String color = switch (val) {
                    case "🟢 RUNNING"  -> "#10b981";
                    case "⬜ OPEN"     -> "#94a3b8";
                    case "🏁 FINISHED" -> "#f59e0b";
                    case "✅ PAID"     -> "#3b82f6";
                    default            -> "#ef4444";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });
    }

    // ── Load data ────────────────────────────────────────────────

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadAuctions();
    }

    @FXML
    private void handleShowAllAuctions(ActionEvent event) {
        Node source = event.getSource() instanceof Node node ? node : null;
        animateSeeAllButton(source);
        animateFocusedSection(source);
        setStatus("Đang tải tất cả phiên đấu giá...");
        loadAuctions();
    }

    private void loadAuctions() {
        setStatus("Đang tải...");
        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().getAllAuctions();
            Platform.runLater(() -> {
                if (res.isOk()) {
                    @SuppressWarnings("unchecked")
                    List<Auction> list = (List<Auction>) res.get("auctions");
                    auctions.setAll(list);
                    setStatus("Tổng " + list.size() + " phiên đấu giá");
                    updateStatCards(list);
                    renderDashboardCards(list);
                } else {
                    setStatus("Lỗi tải dữ liệu: " + res.getMessage());
                }
            });
        }).start();
    }

    // ── Dashboard card grid ──────────────────────────────────────

    private void renderDashboardCards(List<Auction> list) {
        if (upcomingCards == null || runningCards == null || pastCards == null) return;

        upcomingCards.getChildren().clear();
        runningCards.getChildren().clear();
        pastCards.getChildren().clear();

        for (Auction a : list) {
            if (isPastAuction(a)) {
                pastCards.getChildren().add(createAuctionCard(a, "past"));
            } else if (isRunningAuction(a)) {
                runningCards.getChildren().add(createAuctionCard(a, "running"));
            } else {
                upcomingCards.getChildren().add(createAuctionCard(a, "upcoming"));
            }
        }

        if (upcomingCards.getChildren().isEmpty()) {
            upcomingCards.getChildren().add(emptyLabel("Chưa có phiên sắp diễn ra."));
        }
        if (runningCards.getChildren().isEmpty()) {
            runningCards.getChildren().add(emptyLabel("Chưa có phiên đang diễn ra."));
        }
        if (pastCards.getChildren().isEmpty()) {
            pastCards.getChildren().add(emptyLabel("Chưa có phiên đã diễn ra."));
        }
    }

    private boolean isPastAuction(Auction a) {
        if (a == null) return false;
        Auction.AuctionStatus status = a.getStatus();
        if (status == Auction.AuctionStatus.FINISHED
                || status == Auction.AuctionStatus.PAID
                || status == Auction.AuctionStatus.CANCELED) {
            return true;
        }
        return a.getEndTime() != null && a.getEndTime().isBefore(LocalDateTime.now());
    }

    private boolean isRunningAuction(Auction a) {
        if (a == null || isPastAuction(a)) return false;
        if (a.getStatus() == Auction.AuctionStatus.RUNNING) return true;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = a.getStartTime();
        LocalDateTime end = a.getEndTime();
        return start != null && end != null
                && !start.isAfter(now)
                && !end.isBefore(now);
    }

    private Label emptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13;");
        return label;
    }

    private Image loadItemImage(com.auction.shared.model.Item.Item item) {
        if (item != null && item.getImageData() != null && item.getImageData().length > 0) {
            return new Image(new ByteArrayInputStream(item.getImageData()));
        }

        String itemId = item == null ? null : item.getId();
        String[] exts = {".png", ".jpg", ".jpeg", ".gif", ".webp"};
        if (itemId != null) {
            for (String ext : exts) {
                File f = new File("src/main/resources/img/uploads/" + itemId + ext);
                if (f.exists()) return new Image(f.toURI().toString());
            }
            for (String ext : exts) {
                try {
                    var in = getClass().getResourceAsStream("/img/uploads/" + itemId + ext);
                    if (in != null) return new Image(in);
                } catch (Exception ignored) {}
            }
        }
        try {
            return new Image(getClass().getResourceAsStream("/img/76.jpg"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private VBox createAuctionCard(Auction a, String group) {
        VBox card = new VBox(10);
        card.setPrefWidth(320);
        card.setMinHeight(330);
        card.setStyle(CARD_BASE_STYLE);
        installCardMotion(card);
        card.setOnMouseClicked(e -> {
            animateSelectionFlash(card);
            onSelectAuction(a);
        });

        ImageView image = new ImageView();
        image.setImage(loadItemImage(a.getItem()));
        image.setFitWidth(270);
        image.setFitHeight(145);
        image.setPreserveRatio(true);
        image.setSmooth(true);

        String badgeText = switch (group) {
            case "running" -> "🟢 ĐANG DIỄN RA";
            case "past" -> statusDisplay(a.getStatus());
            default -> "🟡 SẮP DIỄN RA";
        };
        String badgeColor = switch (group) {
            case "running" -> "#059669";
            case "past" -> "#475569";
            default -> "#b45309";
        };
        Label badge = new Label(badgeText);
        badge.setStyle("-fx-background-color: " + badgeColor + ";"
                + "-fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;"
                + "-fx-background-radius: 999; -fx-padding: 3 8;");

        HBox imageWrap = new HBox(image);
        imageWrap.setMinSize(270, 145);
        imageWrap.setPrefSize(270, 145);
        imageWrap.setMaxSize(270, 145);
        imageWrap.setStyle("-fx-alignment: center; -fx-background-color: #0b0f13; -fx-background-radius: 10;");

        Label name = new Label(shortText(a.getItem().getName(), 70));
        name.setWrapText(true);
        name.setMinHeight(45);
        name.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15; -fx-font-weight: bold;");

        HBox row1 = infoRow("Giá khởi điểm:", fmtVND(a.getItem().getStartingPrice()));
        HBox row2 = infoRow("Giá hiện tại:", fmtVND(a.getCurrentPrice()));
        HBox row3 = infoRow("Thời gian tổ chức:", formatAuctionTime(a));

        card.getChildren().addAll(imageWrap, badge, name, row1, row2, row3);
        animateNodeEntrance(card, 0, 12);
        return card;
    }

    private HBox infoRow(String left, String right) {
        Label l = new Label(left);
        l.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12;");
        Label r = new Label(right);
        r.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 12; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox row = new HBox(l, spacer, r);
        row.setStyle("-fx-alignment: center-left;");
        return row;
    }

    private String shortText(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private String formatAuctionTime(Auction a) {
        if (a.getStartTime() == null) return fmtTimer(a.getSecondRemaining());
        return String.format("%02d:%02d - %02d/%02d/%04d",
                a.getStartTime().getHour(), a.getStartTime().getMinute(),
                a.getStartTime().getDayOfMonth(), a.getStartTime().getMonthValue(), a.getStartTime().getYear());
    }

    // ── Chọn phiên → hiện chi tiết ───────────────────────────────

    private void onSelectAuction(Auction auction) {
        if (auction == null) return;

        selectedAuction = auction;
        renderDetail(auction);
        showDetail();
        startTimerRefresh();
        setStatus("Äang cáº­p nháº­t chi tiáº¿t phiÃªn...");

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance()
                    .getClient().getAuction(auction.getAuctionId());
            Platform.runLater(() -> {
                if (res.isOk() && res.get("auction") instanceof Auction fresh) {
                    if (selectedAuction != null
                            && selectedAuction.getAuctionId().equals(fresh.getAuctionId())) {
                        selectedAuction = fresh;
                        renderDetail(fresh);
                        animateDetailRefresh();
                        setStatus("ÄÃ£ cáº­p nháº­t chi tiáº¿t phiÃªn.");
                    }
                } else {
                    setStatus("KhÃ´ng táº£i Ä‘Æ°á»£c chi tiáº¿t má»›i nháº¥t.");
                }
            });
        }).start();
    }

    public void openAuctionById(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) return;

        setStatus("Đang mở phiên đấu giá...");
        Auction existing = auctions.stream()
                .filter(a -> auctionId.equals(a.getAuctionId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            onSelectAuction(existing);
            return;
        }

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().getAuction(auctionId);
            Platform.runLater(() -> {
                if (res.isOk() && res.get("auction") instanceof Auction auction) {
                    onSelectAuction(auction);
                } else {
                    setStatus("Không tìm thấy phiên đấu giá: " + auctionId);
                }
            });
        }, "open-auction-detail").start();
    }

    public void openAuction(Auction auction) {
        onSelectAuction(auction);
    }

    private void renderDetail(Auction a) {
        String itemName = a.getItem().getName();
        if (detailName != null) detailName.setText(itemName);
        if (detailTitleLarge != null) detailTitleLarge.setText(itemName);
        if (detailAssetCode != null) detailAssetCode.setText("Mã tài sản: " + a.getItem().getId());

        if (detailImage != null) {
            detailImage.setImage(loadItemImage(a.getItem()));
        }

        double startPrice = a.getItem().getStartingPrice();
        if (detailStartingPrice != null) detailStartingPrice.setText(fmtVND(startPrice));
        if (detailStepPrice != null) detailStepPrice.setText(fmtVND(Math.max(startPrice * 0.01, 100_000)));
        if (detailDeposit != null) detailDeposit.setText(fmtVND(startPrice * 0.20));
        if (detailFee != null) detailFee.setText(fmtVND(200_000));
        if (detailStartTime != null) detailStartTime.setText(formatDateTime(a.getStartTime()));
        if (detailEndTime != null) detailEndTime.setText(formatDateTime(a.getEndTime()));
        if (detailAddress != null) detailAddress.setText(safeText(a.getItem().getDescription(), "Chưa cập nhật địa điểm"));
        if (detailType != null) detailType.setText(a.getItem().getType());

        if (detailPrice != null) detailPrice.setText(fmtVND(a.getCurrentPrice()));
        if (detailTimer != null) detailTimer.setText(fmtTimer(a.getSecondRemaining()));
        if (detailStatus != null) detailStatus.setText(statusDisplay(a.getStatus()));
        if (detailLeader != null) {
            detailLeader.setText(safeText(a.getLeadBidderName(), "Chưa có người dẫn"));
        }
        if (detailDesc != null) detailDesc.setText(safeText(a.getItem().getDescription(), "Chưa cập nhật thông tin xem tài sản."));

        updateCountdown(a);

        if (detailAntiSnipe != null) {
            if (a.isAntiSnipingEnabled()) {
                detailAntiSnipe.setText(String.format(
                        "⚠ Anti-Snipe: bid trong %ds cuối → gia hạn thêm %ds",
                        a.snipeWindowSeconds(), a.snipeExtendSeconds()));
            } else {
                detailAntiSnipe.setText("⚠ Thời gian nộp hồ sơ sẽ kết thúc trước phiên đấu giá. Vui lòng kiểm tra điều kiện tham gia.");
            }
        }

        if (chartBox != null || priceChart != null) renderChart(a);

        ObservableList<String> history = FXCollections.observableArrayList();
        for (BidTransaction t : a.getBidHistory()) {
            history.add(0, t.toString());
        }
        if (bidHistoryList != null) bidHistoryList.setItems(history);

        updateBidControls(a);
        if (bidResultLabel != null) bidResultLabel.setText("");
        if (autoBidResultLabel != null) autoBidResultLabel.setText("");
        if (isOwnAuction(a)) {
            if (bidResultLabel != null) {
                bidResultLabel.setStyle("-fx-text-fill: #ef4444;");
                bidResultLabel.setText("Bạn không được tự đặt giá ở phiên đấu giá của mình.");
            }
            if (autoBidResultLabel != null) {
                autoBidResultLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12;");
                autoBidResultLabel.setText("Bạn không được tự đặt giá ở phiên đấu giá của mình.");
            }
        }
    }

    private boolean canAcceptBid(Auction a) {
        return a != null
                && !isOwnAuction(a)
                && (a.getStatus() == Auction.AuctionStatus.OPEN
                || a.getStatus() == Auction.AuctionStatus.RUNNING);
    }

    private boolean isOwnAuction(Auction a) {
        User user = ClientContext.getInstance().getCurrentUser();
        return a != null
                && a.getItem() != null
                && a.getItem().getSellerId() != null
                && user != null
                && a.getItem().getSellerId().equals(user.getId());
    }

    private void updateBidControls(Auction a) {
        boolean canBid = canAcceptBid(a);
        if (bidAmountField != null) bidAmountField.setDisable(!canBid);
        if (placeBidButton != null) placeBidButton.setDisable(!canBid);
        if (autoMaxBidField != null) autoMaxBidField.setDisable(!canBid);
        if (autoIncrementField != null) autoIncrementField.setDisable(!canBid);
        if (autoBidButton != null) autoBidButton.setDisable(!canBid);
        if (cancelAutoBidButton != null) cancelAutoBidButton.setDisable(!canBid);
    }

    private void setAutoBidLoading(boolean loading) {
        boolean disabled = loading || !canAcceptBid(selectedAuction);
        if (autoMaxBidField != null) autoMaxBidField.setDisable(disabled);
        if (autoIncrementField != null) autoIncrementField.setDisable(disabled);
        if (autoBidButton != null) {
            autoBidButton.setDisable(disabled);
            autoBidButton.setText(loading ? "Đang xử lý..." : "Kích hoạt");
        }
        if (cancelAutoBidButton != null) cancelAutoBidButton.setDisable(disabled);
    }

    private double parseMoney(TextField field, String fieldName) {
        if (field == null) throw new IllegalArgumentException("Thiếu ô nhập " + fieldName + ".");
        String raw = field.getText().trim().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Nhập " + fieldName + " hợp lệ.");
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " không hợp lệ.");
        }
    }

    private void showBidResult(String msg, boolean success) {
        if (bidResultLabel == null) return;
        bidResultLabel.setStyle(success
                ? "-fx-text-fill: #10b981; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-text-fill: #ef4444; -fx-font-size: 12;");
        bidResultLabel.setText((success ? "OK: " : "Lỗi: ") + safeText(msg, ""));
        animateResultLabel(bidResultLabel, success);
    }

    private void showAutoBidResult(String msg, boolean success) {
        if (autoBidResultLabel == null) return;
        autoBidResultLabel.setStyle(success
                ? "-fx-text-fill: #10b981; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-text-fill: #ef4444; -fx-font-size: 12;");
        autoBidResultLabel.setText((success ? "OK: " : "Lỗi: ") + safeText(msg, ""));
        animateResultLabel(autoBidResultLabel, success);
    }

    private void animateBidSuccess() {
        animateNodePop(detailPrice);
        animateNodePop(currentInfoBox);
        animateNodePop(priceChart);
        animateNodeEntrance(bidResultLabel, 0, 8);
    }

    private void animateAutoBidSuccess() {
        animateNodePop(autoBidButton);
        animateNodePop(auctionLiveBox);
        animateNodeEntrance(autoBidResultLabel, 0, 8);
    }

    private void updateCurrentUser(SocketMessage res) {
        Object updatedUser = res.get("user");
        if (updatedUser instanceof User user) {
            ClientContext.getInstance().setCurrentUser(user);
        }
    }

    private void applyAuctionUpdate(Auction updated) {
        selectedAuction = updated;
        for (int i = 0; i < auctions.size(); i++) {
            if (auctions.get(i).getAuctionId().equals(updated.getAuctionId())) {
                auctions.set(i, updated);
                break;
            }
        }
        renderDetail(updated);
    }


    private String safeText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private String formatDateTime(LocalDateTime time) {
        if (time == null) return "--";
        return String.format("%02d:%02d - %02d/%02d/%04d",
                time.getHour(), time.getMinute(),
                time.getDayOfMonth(), time.getMonthValue(), time.getYear());
    }

    private void updateCountdown(Auction a) {
        LocalDateTime target;

        boolean finished = a.getStatus() == Auction.AuctionStatus.FINISHED
                || a.getStatus() == Auction.AuctionStatus.PAID
                || a.getStatus() == Auction.AuctionStatus.CANCELED
                || (a.getEndTime() != null && !a.getEndTime().isAfter(LocalDateTime.now()));

        if (finished) {
            showAuctionSuccess(a);
            return;
        }

        showAuctionLive();

        if (a.getStatus() == Auction.AuctionStatus.PENDING
                || (a.getStartTime() != null && a.getStartTime().isAfter(LocalDateTime.now()))) {
            if (countdownTitle != null) {
                countdownTitle.setText("Thời gian bắt đầu đấu giá");
            }
            target = a.getStartTime();
        } else {
            if (countdownTitle != null) {
                countdownTitle.setText("Thời gian kết thúc đấu giá");
            }
            target = a.getEndTime();
        }

        long seconds = target == null
                ? a.getSecondRemaining()
                : Math.max(0, Duration.between(LocalDateTime.now(), target).getSeconds());

        setCountdown(seconds);
    }

    private void showAuctionLive() {
        if (auctionLiveBox != null) {
            auctionLiveBox.setVisible(true);
            auctionLiveBox.setManaged(true);
        }
        if (auctionSuccessBox != null) {
            auctionSuccessBox.setVisible(false);
            auctionSuccessBox.setManaged(false);
        }
        if (currentInfoBox != null) {
            currentInfoBox.setVisible(true);
            currentInfoBox.setManaged(true);
        }
    }

    private void showAuctionSuccess(Auction a) {
        if (auctionLiveBox != null) {
            auctionLiveBox.setVisible(false);
            auctionLiveBox.setManaged(false);
        }
        if (auctionSuccessBox != null) {
            auctionSuccessBox.setVisible(true);
            auctionSuccessBox.setManaged(true);
        }
        if (currentInfoBox != null) {
            currentInfoBox.setVisible(false);
            currentInfoBox.setManaged(false);
        }

        if (successStartingPrice != null) {
            successStartingPrice.setText(fmtVND(a.getItem().getStartingPrice()));
        }
        if (successWinningPrice != null) {
            successWinningPrice.setText(fmtVND(a.getCurrentPrice()));
        }
        if (successWinnerCode != null) {
            successWinnerCode.setText(safeText(a.getLeadBidderName(), "Chưa có người thắng"));
        }

        setCountdown(0);
    }

    private void setCountdown(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (countdownDays != null) countdownDays.setText(String.valueOf(days));
        if (countdownHours != null) countdownHours.setText(String.valueOf(hours));
        if (countdownMinutes != null) countdownMinutes.setText(String.valueOf(minutes));
        if (countdownSeconds != null) countdownSeconds.setText(String.valueOf(secs));
    }

    // ── Biểu đồ ──────────────────────────────────────────────────

    private void renderChart(Auction a) {
        List<BidTransaction> history = a.getBidHistory();
        int historySize = history != null ? history.size() : 0;
        String chartKey = a.getAuctionId() + ":" + historySize + ":" + Math.round(a.getCurrentPrice());
        if (chartKey.equals(renderedChartKey)) {
            return;
        }
        renderedChartKey = chartKey;

        if (priceChart == null) {
            xAxis = new NumberAxis();
            yAxis = new NumberAxis();
            xAxis.setLabel("Lượt bid");
            yAxis.setLabel("Giá đấu");
            xAxis.setForceZeroInRange(true);
            yAxis.setForceZeroInRange(false);
            priceChart = new LineChart<>(xAxis, yAxis);
            priceChart.setLegendVisible(false);
            priceChart.setAnimated(false);
            priceChart.setCreateSymbols(true);
            priceChart.setPrefHeight(240);
            priceChart.setStyle("-fx-background-color: transparent;");
            if (chartBox != null) {
                chartBox.getChildren().setAll(priceChart);
            }
        }

        if (xAxis != null) {
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(Math.max(1, historySize));
            xAxis.setTickUnit(Math.max(1, historySize / 8.0));
        }
        if (yAxis != null) {
            yAxis.setAutoRanging(true);
        }
        priceChart.setCreateSymbols(historySize <= 24);
        priceChart.setAnimated(false);
        priceChart.getData().clear();
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        series.setName("Giá đấu");

        series.getData().add(new XYChart.Data<>(0, a.getItem().getStartingPrice()));
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                series.getData().add(new XYChart.Data<>(i + 1, history.get(i).getAmount()));
            }
        }
        priceChart.getData().add(series);
        Platform.runLater(() -> {
            installChartTooltips(series, history);
            animatePriceHistoryChart(series);
        });
    }

    private void installChartTooltips(XYChart.Series<Number, Number> series, List<BidTransaction> history) {
        for (int i = 0; i < series.getData().size(); i++) {
            XYChart.Data<Number, Number> point = series.getData().get(i);
            String text;
            if (i == 0) {
                text = "Giá khởi điểm: " + fmtVND(point.getYValue().doubleValue());
            } else {
                BidTransaction bid = history != null && i - 1 < history.size() ? history.get(i - 1) : null;
                text = bid == null
                        ? "Giá: " + fmtVND(point.getYValue().doubleValue())
                        : "Lượt " + i + ": " + fmtVND(bid.getAmount());
            }
            installPointTooltip(point, text);
        }
    }

    private void installPointTooltip(XYChart.Data<Number, Number> point, String text) {
        if (point.getNode() == null) return;

        Tooltip tooltip = new Tooltip(text);
        tooltip.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #f8fafc;"
                + "-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 6 9;"
                + "-fx-background-radius: 6;");

        point.getNode().setOnMouseEntered(event -> {
            Bounds bounds = point.getNode().localToScreen(point.getNode().getBoundsInLocal());
            tooltip.show(point.getNode(),
                    bounds.getMinX() + bounds.getWidth() / 2 - 48,
                    bounds.getMinY() - 36);
        });
        point.getNode().setOnMouseExited(event -> tooltip.hide());
    }

    private void animatePriceHistoryChart(XYChart.Series<Number, Number> series) {
        Node chartNode = chartBox != null ? chartBox : priceChart;
        if (chartNode != null) {
            setAnimatedCache(chartNode, true);
            chartNode.setOpacity(0.76);
            chartNode.setTranslateY(8);

            FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(220), chartNode);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition move = new TranslateTransition(javafx.util.Duration.millis(220), chartNode);
            move.setToY(0);
            move.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition transition = new ParallelTransition(fade, move);
            transition.setOnFinished(event -> setAnimatedCache(chartNode, false));
            transition.play();
        }

        if (series.getNode() != null) {
            series.getNode().setOpacity(0);
            FadeTransition lineFade = new FadeTransition(javafx.util.Duration.millis(240), series.getNode());
            lineFade.setToValue(1);
            lineFade.setInterpolator(Interpolator.EASE_OUT);
            lineFade.play();
        }

        for (int i = 0; i < series.getData().size(); i++) {
            Node pointNode = series.getData().get(i).getNode();
            if (pointNode == null) continue;

            pointNode.setOpacity(0);
            pointNode.setScaleX(0.55);
            pointNode.setScaleY(0.55);

            FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(150), pointNode);
            fade.setDelay(javafx.util.Duration.millis(Math.min(i * 18L, 180)));
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(javafx.util.Duration.millis(150), pointNode);
            scale.setDelay(fade.getDelay());
            scale.setToX(1);
            scale.setToY(1);
            scale.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fade, scale).play();
        }
    }

    // ── Đặt giá ──────────────────────────────────────────────────

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        if (selectedAuction == null) return;

        if (isOwnAuction(selectedAuction)) {
            showBidResult("Bạn không được tự đặt giá ở phiên đấu giá của mình.", false);
            shakeNode(placeBidButton);
            updateBidControls(selectedAuction);
            return;
        }

        String raw = bidAmountField.getText().trim().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) {
            showBidResult("Nhập số tiền hợp lệ.", false);
            shakeNode(bidAmountField);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            showBidResult("Số tiền không hợp lệ.", false);
            shakeNode(bidAmountField);
            return;
        }

        placeBidButton.setDisable(true);
        placeBidButton.setText("Đang đặt giá...");
        animateNodePop(placeBidButton);
        final String auctionId = selectedAuction.getAuctionId();

        new Thread(() -> {
            var client = ClientContext.getInstance().getClient();
            SocketMessage res = client.placeBid(auctionId, amount);
            SocketMessage detailRes = null;
            if (res.isOk() && !(res.get("auction") instanceof Auction)) {
                detailRes = client.getAuction(auctionId);
            }
            final SocketMessage detailResponse = detailRes;

            Platform.runLater(() -> {
                placeBidButton.setDisable(false);
                placeBidButton.setText("Đặt giá ngay");
                if (res.isOk()) {
                    updateCurrentUser(res);

                    Auction updated = null;
                    Object auctionPayload = res.get("auction");
                    if (auctionPayload instanceof Auction auction) {
                        updated = auction;
                    } else if (detailResponse != null && detailResponse.isOk()
                            && detailResponse.get("auction") instanceof Auction auction) {
                        updated = auction;
                    }
                    if (updated != null) {
                        applyAuctionUpdate(updated);
                    }

                    showBidResult("Đặt giá " + fmtVND(amount) + " thành công!", true);
                    bidAmountField.clear();
                    animateBidSuccess();
                } else {
                    showBidResult(res.getMessage(), false);
                    shakeNode(placeBidButton);
                }
            });
        }).start();
    }

    @FXML
    private void handleSetAutoBid(ActionEvent event) {
        if (selectedAuction == null) return;

        if (isOwnAuction(selectedAuction)) {
            showAutoBidResult("Bạn không được tự đặt giá ở phiên đấu giá của mình.", false);
            shakeNode(autoBidButton);
            updateBidControls(selectedAuction);
            return;
        }

        if (!canAcceptBid(selectedAuction)) {
            showAutoBidResult("Phiên này không còn nhận Auto-Bid.", false);
            shakeNode(autoBidButton);
            return;
        }

        final double maxBid;
        final double increment;
        try {
            maxBid = parseMoney(autoMaxBidField, "giá tối đa");
            increment = parseMoney(autoIncrementField, "bước giá");
        } catch (IllegalArgumentException ex) {
            showAutoBidResult(ex.getMessage(), false);
            shakeNode(autoMaxBidField);
            shakeNode(autoIncrementField);
            return;
        }

        if (maxBid <= selectedAuction.getCurrentPrice()) {
            showAutoBidResult("Giá tối đa phải lớn hơn giá hiện tại: " + fmtVND(selectedAuction.getCurrentPrice()), false);
            shakeNode(autoMaxBidField);
            return;
        }
        if (increment <= 0) {
            showAutoBidResult("Bước giá phải lớn hơn 0.", false);
            shakeNode(autoIncrementField);
            return;
        }

        setAutoBidLoading(true);
        animateNodePop(autoBidButton);
        final String auctionId = selectedAuction.getAuctionId();

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient()
                    .registerAutoBid(auctionId, maxBid, increment);
            SocketMessage detailRes = res.isOk()
                    ? ClientContext.getInstance().getClient().getAuction(auctionId)
                    : null;

            Platform.runLater(() -> {
                setAutoBidLoading(false);
                if (res.isOk()) {
                    updateCurrentUser(res);
                    if (detailRes != null && detailRes.isOk()) {
                        Auction updated = (Auction) detailRes.get("auction");
                        if (updated != null) applyAuctionUpdate(updated);
                    }
                    autoMaxBidField.clear();
                    autoIncrementField.clear();
                    showAutoBidResult("Đã kích hoạt Auto-Bid. Max: "
                            + fmtVND(maxBid) + " | Bước: " + fmtVND(increment), true);
                    animateAutoBidSuccess();
                } else {
                    showAutoBidResult(res.getMessage(), false);
                    shakeNode(autoBidButton);
                }
            });
        }).start();
    }

    @FXML
    private void handleCancelAutoBid(ActionEvent event) {
        if (selectedAuction == null) return;

        setAutoBidLoading(true);
        animateNodePop(cancelAutoBidButton);
        final String auctionId = selectedAuction.getAuctionId();

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient()
                    .cancelAutoBid(auctionId);
            Platform.runLater(() -> {
                setAutoBidLoading(false);
                if (res.isOk()) {
                    showAutoBidResult("Đã hủy Auto-Bid cho phiên này.", true);
                    animateNodePop(cancelAutoBidButton);
                } else {
                    showAutoBidResult(res.getMessage(), false);
                    shakeNode(cancelAutoBidButton);
                }
            });
        }).start();
    }

    // ── Broadcast realtime ────────────────────────────────────────

    private void handleBroadcast(SocketMessage msg) {
        Auction updated = (Auction) msg.get("auction");
        if (updated == null) return;

        // Cập nhật danh sách
        for (int i = 0; i < auctions.size(); i++) {
            if (auctions.get(i).getAuctionId().equals(updated.getAuctionId())) {
                auctions.set(i, updated);
                break;
            }
        }

        // Cập nhật chi tiết nếu đang xem phiên này
        if (selectedAuction != null
                && selectedAuction.getAuctionId().equals(updated.getAuctionId())) {
            selectedAuction = updated;
            renderDetail(updated);
        }

        if (msg.getAction() == Action.BROADCAST_AUCTION_END) {
            setStatus("🏁 Phiên " + updated.getAuctionId() + " đã kết thúc!");
        }
    }

    // ── Timer countdown ──────────────────────────────────────────

    private void startTimerRefresh() {
        if (timerTask != null) timerTask.cancel(false);

        timerTask = scheduler.scheduleAtFixedRate(() -> {
            if (selectedAuction != null) {
                Platform.runLater(() -> {
                    updateCountdown(selectedAuction);

                    if (detailTimer != null) {
                        LocalDateTime target;
                        if (selectedAuction.getStatus() == Auction.AuctionStatus.PENDING
                                || (selectedAuction.getStartTime() != null
                                && selectedAuction.getStartTime().isAfter(LocalDateTime.now()))) {
                            target = selectedAuction.getStartTime();
                        } else {
                            target = selectedAuction.getEndTime();
                        }

                        long seconds = target == null
                                ? selectedAuction.getSecondRemaining()
                                : Math.max(0, Duration.between(LocalDateTime.now(), target).getSeconds());

                        detailTimer.setText(fmtTimer(seconds));
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String fmtVND(double amount) {
        return VND.format((long) amount) + " đ";
    }

    private String fmtTimer(long seconds) {
        if (seconds <= 0) return "Kết thúc";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%dh %dm", h, m);
        if (m > 0) return String.format("%dm %ds", m, s);
        return s + "s";
    }

    private String statusDisplay(Auction.AuctionStatus status) {
        return switch (status) {
            case PENDING -> "PENĐING";
            case OPEN     -> "⬜ OPEN";
            case RUNNING  -> "🟢 RUNNING";
            case FINISHED -> "🏁 FINISHED";
            case PAID     -> "✅ PAID";
            case CANCELED -> "❌ CANCELED";
        };
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private void updateStatCards(List<Auction> list) {
        if (statTotal   != null) statTotal.setText(String.valueOf(list.size()));
        if (statRunning != null) {
            long running = list.stream()
                    .filter(this::isRunningAuction)
                    .count();
            statRunning.setText(String.valueOf(running));
        }
        if (statBids != null) {
            long totalBids = list.stream()
                    .mapToLong(a -> a.getBidHistory() != null ? a.getBidHistory().size() : 0)
                    .sum();
            statBids.setText(String.valueOf(totalBids));
        }
    }
}
