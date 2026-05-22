package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.network.protocol.SocketMessage;
import com.auction.shared.network.protocol.SocketMessage.Action;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auction-timer");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> timerTask;

    private static final NumberFormat VND =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    // ── Init ─────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        loadAuctions();

        // Lắng nghe broadcast từ server
        ClientContext.getInstance().getClient().setBroadcastListener(msg -> {
            if (msg.getAction() == Action.BROADCAST_BID_UPDATE
                    || msg.getAction() == Action.BROADCAST_AUCTION_END) {
                Platform.runLater(() -> handleBroadcast(msg));
            }
        });

        // Bắt đầu ở màn hình danh sách
        showList();
    }

    // ── Navigation ────────────────────────────────────────s───────

    /** Hiện danh sách, ẩn chi tiết */
    private void showList() {
        // listView/detailPanel chỉ có trong auctions.fxml, không có trong dashboard.fxml
        if (listView != null) {
            listView.setVisible(true);
            listView.setManaged(true);
        }
        if (detailPanel != null) {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
        }
        if (timerTask != null) timerTask.cancel(false);
        selectedAuction = null;
    }

    /** Hiện chi tiết, ẩn danh sách */
    private void showDetail() {
        if (listView != null) {
            listView.setVisible(false);
            listView.setManaged(false);
        }
        if (detailPanel != null) {
            detailPanel.setVisible(true);
            detailPanel.setManaged(true);
        }
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
                if (!row.isEmpty()) onSelectAuction(row.getItem());
            });
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
        card.setStyle("-fx-background-color: #15191d; -fx-border-color: #31363c;"
                + "-fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14; -fx-cursor: hand;");
        card.setOnMouseClicked(e -> onSelectAuction(a));

        ImageView image = new ImageView();
        image.setImage(loadItemImage(a.getItem()));
        image.setFitWidth(270);
        image.setFitHeight(145);
        image.setPreserveRatio(false);
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
        imageWrap.setStyle("-fx-alignment: center; -fx-background-color: #0b0f13; -fx-background-radius: 10;");

        Label name = new Label(shortText(a.getItem().getName(), 70));
        name.setWrapText(true);
        name.setMinHeight(45);
        name.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15; -fx-font-weight: bold;");

        HBox row1 = infoRow("Giá khởi điểm:", fmtVND(a.getItem().getStartingPrice()));
        HBox row2 = infoRow("Tiền đặt trước:", fmtVND(Math.max(a.getItem().getStartingPrice() * 0.1, 0)));
        HBox row3 = infoRow("Thời gian tổ chức:", formatAuctionTime(a));

        card.getChildren().addAll(imageWrap, badge, name, row1, row2, row3);
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
        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance()
                    .getClient().getAuction(auction.getAuctionId());
            Platform.runLater(() -> {
                if (res.isOk()) {
                    selectedAuction = (Auction) res.get("auction");
                    renderDetail(selectedAuction);
                    showDetail(); // chuyển sang màn hình chi tiết
                    startTimerRefresh();
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

        boolean canBid = a.getStatus() == Auction.AuctionStatus.OPEN
                || a.getStatus() == Auction.AuctionStatus.RUNNING;
        if (placeBidButton != null) placeBidButton.setDisable(!canBid);
        if (bidResultLabel != null) bidResultLabel.setText("");
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

        priceChart.getData().clear();
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        series.setName("Giá đấu");

        List<BidTransaction> history = a.getBidHistory();
        series.getData().add(new XYChart.Data<>(0, a.getItem().getStartingPrice()));
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                series.getData().add(new XYChart.Data<>(i + 1, history.get(i).getAmount()));
            }
        }
        priceChart.getData().add(series);
    }

    // ── Đặt giá ──────────────────────────────────────────────────

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        if (selectedAuction == null) return;

        String raw = bidAmountField.getText().trim().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) {
            bidResultLabel.setStyle("-fx-text-fill: #ef4444;");
            bidResultLabel.setText("Nhập số tiền hợp lệ.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            bidResultLabel.setText("Số tiền không hợp lệ.");
            return;
        }

        placeBidButton.setDisable(true);
        final String auctionId = selectedAuction.getAuctionId();

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance()
                    .getClient().placeBid(auctionId, amount);
            Platform.runLater(() -> {
                placeBidButton.setDisable(false);
                if (res.isOk()) {
                    bidResultLabel.setStyle("-fx-text-fill: #10b981;");
                    bidResultLabel.setText("✅ Đặt giá " + fmtVND(amount) + " thành công!");
                    bidAmountField.clear();
                    onSelectAuction(selectedAuction);
                } else {
                    bidResultLabel.setStyle("-fx-text-fill: #ef4444;");
                    bidResultLabel.setText("❌ " + res.getMessage());
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