package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item.Item;
import com.auction.shared.network.protocol.SocketMessage;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.CacheHint;
import javafx.scene.Node;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;


public class MySellController {
    @FXML private DatePicker startDatePicker;
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private Spinner<Integer> startMinSpinner;
    @FXML private DatePicker  endDatePicker;
    @FXML private Spinner<Integer> endHourSpinner;
    @FXML private Spinner<Integer> endMinSpinner;
    @FXML private CheckBox    antiSnipeCheck;
    @FXML private TableView<Item>           itemTable;

    @FXML private TableColumn<Item,String>  colItemName;
    @FXML private TableColumn<Item,String>  colItemType;
    @FXML private TableColumn<Item,String>  colItemPrice;
    @FXML private TableColumn<Item,String>  colItemCPrice;
    @FXML private TableColumn<Item,String>  colItemStatus;
    @FXML private TableColumn<Item,Void>    colAction;
    @FXML private Label summaryLabel;


    private final ObservableList<Auction> rows = FXCollections.observableArrayList();
    private final ObservableList<Item>    items    = FXCollections.observableArrayList();
    private final Map<String, Auction> auctionByItemId = new HashMap<>();

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));
    private static final javafx.util.Duration QUICK = javafx.util.Duration.millis(120);
    private static final javafx.util.Duration SMOOTH = javafx.util.Duration.millis(190);

    @FXML
    public void initialize() {
        setupItemTable();
        loadMyBids();
    }

    private void setupItemTable() {
        colItemName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colItemType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        colItemPrice.setCellValueFactory(d ->
                new SimpleStringProperty(VND.format((long) d.getValue().getStartingPrice()) + " đ"));
        colItemCPrice.setCellValueFactory(d ->
                new SimpleStringProperty(VND.format((long) d.getValue().getCurrentPrice()) + " đ"));
        colItemStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        setupActionColumn();
        setupRowMotion();
        itemTable.setItems(items);
    }

    private void setupActionColumn() {
        if (colAction == null) return;
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button();

            {
                viewButton.getStyleClass().add("auction-see-all-button");
                viewButton.setMinWidth(118);
                viewButton.setFocusTraversable(false);
                viewButton.setOnAction(event -> {
                    Item item = getTableView().getItems().get(getIndex());
                    openAuctionForItem(item, viewButton);
                });
            }

            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                Item item = getTableView().getItems().get(getIndex());
                boolean hasAuction = getAuctionForItem(item) != null;
                viewButton.setText(hasAuction ? "Xem đấu giá  ›" : "Chưa có phiên");
                viewButton.setDisable(!hasAuction);
                setGraphic(viewButton);
            }
        });
    }

    private void setupRowMotion() {
        if (itemTable == null) return;
        itemTable.setRowFactory(table -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseEntered(event -> {
                if (!row.isEmpty()) animateRow(row, 1.006, -2);
            });
            row.setOnMouseExited(event -> animateRow(row, 1.0, 0));
            row.setOnMousePressed(event -> {
                if (!row.isEmpty()) animateRow(row, 0.994, 0);
            });
            row.setOnMouseReleased(event -> {
                if (!row.isEmpty()) animateRow(row, row.isHover() ? 1.006 : 1.0, row.isHover() ? -2 : 0);
            });
            return row;
        });
    }

    private void loadMyBids() {
        summaryLabel.setText("Đang tải...");
        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().getMyItems();
            SocketMessage auctionRes = ClientContext.getInstance().getClient().getMySellerAuctions();
            Platform.runLater(() -> {
                if (res.isOk()) {
                    @SuppressWarnings("unchecked")
                    List<Item> list = (List<Item>) res.get("items");
                    items.setAll(list);
                    auctionByItemId.clear();
                    if (auctionRes.isOk()) {
                        @SuppressWarnings("unchecked")
                        List<Auction> auctions = (List<Auction>) auctionRes.get("auctions");
                        for (Auction auction : auctions) {
                            if (auction.getItem() != null && auction.getItem().getId() != null) {
                                auctionByItemId.put(auction.getItem().getId(), auction);
                            }
                        }
                    }
                    itemTable.refresh();
                    animateTableRefresh();
                    Platform.runLater(() -> updateSummary(list.size()));
                    summaryLabel.setText(String.format("Tổng %d Sản phẩm", list.size()));
                }

            });
        }).start();

    }
    private LocalDateTime getStartTime() {
        LocalDate date = startDatePicker.getValue();
        if (date == null) date = LocalDate.now();
        int h = startHourSpinner.getValue();
        int m = startMinSpinner.getValue();
        return date.atTime(h, m);
    }

    private Auction getAuctionForItem(Item item) {
        return item == null || item.getId() == null ? null : auctionByItemId.get(item.getId());
    }

    private void openAuctionForItem(Item item, Button sourceButton) {
        Auction auction = getAuctionForItem(item);
        if (auction == null) {
            summaryLabel.setText("Sản phẩm này chưa có phiên đấu giá.");
            shakeNode(sourceButton);
            return;
        }

        sourceButton.setDisable(true);
        sourceButton.setText("Đang mở...");
        summaryLabel.setText("Đang mở phiên đấu giá của sản phẩm...");
        animateLaunch(sourceButton);

        PauseTransition delay = new PauseTransition(javafx.util.Duration.millis(150));
        delay.setOnFinished(event -> {
            MainController mainController = MainController.getActiveInstance();
            if (mainController != null) {
                mainController.openAuctionDetail(auction);
            } else {
                sourceButton.setDisable(false);
                sourceButton.setText("Xem đấu giá  ›");
                summaryLabel.setText("Không thể mở trang đấu giá từ màn hình hiện tại.");
            }
        });
        delay.play();
    }

    private void updateSummary(int itemCount) {
        summaryLabel.setText(String.format("Tổng %d sản phẩm | %d phiên đấu giá",
                itemCount, auctionByItemId.size()));
    }

    private void animateTableRefresh() {
        if (itemTable == null) return;
        itemTable.setOpacity(0.72);
        itemTable.setTranslateY(8);
        FadeTransition fade = new FadeTransition(SMOOTH, itemTable);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition move = new TranslateTransition(SMOOTH, itemTable);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, move).play();
    }

    private void animateLaunch(Node node) {
        if (node == null) return;
        setAnimatedCache(node, true);
        ScaleTransition press = new ScaleTransition(QUICK, node);
        press.setToX(0.94);
        press.setToY(0.94);
        press.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition lift = new ScaleTransition(QUICK, node);
        lift.setToX(1.05);
        lift.setToY(1.05);
        lift.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(SMOOTH, node);
        settle.setToX(1);
        settle.setToY(1);
        settle.setInterpolator(Interpolator.EASE_BOTH);

        SequentialTransition sequence = new SequentialTransition(press, lift, settle);
        sequence.setOnFinished(event -> setAnimatedCache(node, false));
        sequence.play();
    }

    private void animateRow(Node row, double scale, double translateY) {
        if (row == null) return;
        ScaleTransition scaleTransition = new ScaleTransition(QUICK, row);
        scaleTransition.setToX(scale);
        scaleTransition.setToY(scale);
        scaleTransition.setInterpolator(Interpolator.EASE_BOTH);
        TranslateTransition translate = new TranslateTransition(QUICK, row);
        translate.setToY(translateY);
        translate.setInterpolator(Interpolator.EASE_BOTH);
        new ParallelTransition(scaleTransition, translate).play();
    }

    private void shakeNode(Node node) {
        if (node == null) return;
        TranslateTransition left = new TranslateTransition(javafx.util.Duration.millis(40), node);
        left.setByX(-6);
        TranslateTransition right = new TranslateTransition(javafx.util.Duration.millis(55), node);
        right.setByX(12);
        TranslateTransition back = new TranslateTransition(javafx.util.Duration.millis(55), node);
        back.setByX(-12);
        TranslateTransition settle = new TranslateTransition(javafx.util.Duration.millis(40), node);
        settle.setByX(6);
        new SequentialTransition(left, right, back, settle).play();
    }

    private void setAnimatedCache(Node node, boolean enabled) {
        node.setCache(enabled);
        node.setCacheHint(enabled ? CacheHint.SPEED : CacheHint.DEFAULT);
    }

    private String fmtVND(double amount) {
        return VND.format((long) amount) + " đ";
    }
}
