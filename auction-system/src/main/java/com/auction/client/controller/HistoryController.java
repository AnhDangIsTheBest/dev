package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.network.protocol.SocketMessage;
import com.auction.shared.network.protocol.SocketMessage.Action;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class HistoryController {

    @FXML private ListView<String>           allBidsList;
    @FXML private ComboBox<Auction>          auctionFilter;
    @FXML private LineChart<Number, Number>  priceLineChart; // Đổi String→Number ở X
    @FXML private NumberAxis                 xAxis;
    @FXML private Label                      statsLabel;

    private final ObservableList<String>  bidLogs  = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctions = FXCollections.observableArrayList();

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));
    private String renderedChartKey;

    @FXML
    public void initialize() {
        allBidsList.setItems(bidLogs);

        auctionFilter.setItems(auctions);
        auctionFilter.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Auction a, boolean empty) {
                super.updateItem(a, empty);
                setText(empty || a == null ? null
                        : "[" + a.getAuctionId() + "] " + a.getItem().getName());
            }
        });
        auctionFilter.setButtonCell(auctionFilter.getCellFactory().call(null));
        auctionFilter.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, neu) -> { if (neu != null) renderChart(neu); });

        loadHistory();

        ClientContext.getInstance().getClient().setBroadcastListener(msg -> {
            if (msg.getAction() == Action.BROADCAST_BID_UPDATE) {
                Platform.runLater(this::loadHistory);
            }
        });
    }

    private void loadHistory() {
        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().getAllAuctions();
            Platform.runLater(() -> {
                if (!res.isOk()) return;
                @SuppressWarnings("unchecked")
                List<Auction> all = (List<Auction>) res.get("auctions");

                auctions.setAll(all);

                bidLogs.clear();
                long totalBids = 0;
                for (Auction a : all) {
                    for (BidTransaction t : a.getBidHistory()) {
                        bidLogs.add(String.format("[%s] %s — %s đặt %s%s",
                                a.getItem().getName(),
                                t.getFormattedTimeHour(),
                                t.getBidderName(),
                                VND.format((long) t.getAmount()) + " đ",
                                t.isAutoBid() ? " 🤖" : ""));
                        totalBids++;
                    }
                }
                FXCollections.reverse(bidLogs);
                animateHistoryList();

                statsLabel.setText("Tổng " + all.size() + " phiên | " + totalBids + " lượt đặt giá");

                Auction selected = auctionFilter.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    all.stream()
                            .filter(a -> a.getAuctionId().equals(selected.getAuctionId()))
                            .findFirst()
                            .ifPresent(this::renderChart);
                } else if (!auctions.isEmpty()) {
                    auctionFilter.getSelectionModel().selectFirst();
                }
            });
        }).start();
    }

    private void renderChart(Auction auction) {
        if (priceLineChart == null) return;

        List<BidTransaction> history = auction.getBidHistory();
        int historySize = history != null ? history.size() : 0;
        String chartKey = auction.getAuctionId() + ":" + historySize + ":" + Math.round(auction.getCurrentPrice());
        if (chartKey.equals(renderedChartKey)) {
            return;
        }
        renderedChartKey = chartKey;

        priceLineChart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(auction.getItem().getName());

        series.getData().add(new XYChart.Data<>(0, auction.getItem().getStartingPrice()));

        for (int i = 0; history != null && i < history.size(); i++) {
            series.getData().add(new XYChart.Data<>(i + 1, history.get(i).getAmount()));
        }

        // Tooltip hiện tên bidder + giá khi hover
        for (int i = 0; history != null && i < history.size(); i++) {
            BidTransaction t = history.get(i);
            XYChart.Data<Number, Number> point = series.getData().get(i + 1);
            Tooltip tooltip = new Tooltip(
                    t.getBidderName() + "\n" + VND.format((long) t.getAmount()) + " đ\n" + t.getFormattedTimeHour()
            );
            if (point.getNode() != null) {
                Tooltip.install(point.getNode(), tooltip);
            }
        }

        // Căn trục X đúng số điểm, tránh float
        if (xAxis != null) {
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(Math.max(1, historySize));
            xAxis.setTickUnit(Math.max(1, historySize / 8.0));
        }

        priceLineChart.getData().add(series);
        priceLineChart.setTitle("Giá đấu: " + auction.getItem().getName());

        // Gắn tooltip sau khi node đã được render
        Platform.runLater(() -> {
            for (int i = 0; history != null && i < history.size(); i++) {
                BidTransaction t = history.get(i);
                XYChart.Data<Number, Number> point = series.getData().get(i + 1);
                if (point.getNode() != null) {
                    Tooltip.install(point.getNode(), new Tooltip(
                            t.getBidderName() + "\n"
                                    + VND.format((long) t.getAmount()) + " đ\n"
                                    + t.getFormattedTimeHour()
                    ));
                }
            }
            animateChart(series);
        });
    }
    private void animateChart(XYChart.Series<Number, Number> series) {
        priceLineChart.setCache(true);
        priceLineChart.setCacheHint(CacheHint.SPEED);
        priceLineChart.setOpacity(0.76);
        priceLineChart.setTranslateY(8);

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(220), priceLineChart);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition move = new TranslateTransition(javafx.util.Duration.millis(220), priceLineChart);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition chartIn = new ParallelTransition(fade, move);
        chartIn.setOnFinished(event -> {
            priceLineChart.setCache(false);
            priceLineChart.setCacheHint(CacheHint.DEFAULT);
        });
        chartIn.play();

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

            FadeTransition pointFade = new FadeTransition(javafx.util.Duration.millis(150), pointNode);
            pointFade.setDelay(javafx.util.Duration.millis(Math.min(i * 18L, 180)));
            pointFade.setToValue(1);
            pointFade.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition pointScale = new ScaleTransition(javafx.util.Duration.millis(150), pointNode);
            pointScale.setDelay(pointFade.getDelay());
            pointScale.setToX(1);
            pointScale.setToY(1);
            pointScale.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(pointFade, pointScale).play();
        }
    }

    private void animateHistoryList() {
        if (allBidsList == null) return;
        allBidsList.setOpacity(0.82);
        allBidsList.setTranslateY(6);

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(180), allBidsList);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition move = new TranslateTransition(javafx.util.Duration.millis(180), allBidsList);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, move).play();
    }
}