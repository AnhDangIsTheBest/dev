package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.network.protocol.SocketMessage;
import com.auction.network.protocol.SocketMessage.Action;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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
        priceLineChart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(auction.getItem().getName());

        List<BidTransaction> history = auction.getBidHistory();

        series.getData().add(new XYChart.Data<>(0, auction.getItem().getStartingPrice()));

        for (int i = 0; i < history.size(); i++) {
            series.getData().add(new XYChart.Data<>(i + 1, history.get(i).getAmount()));
        }

        // Tooltip hiện tên bidder + giá khi hover
        for (int i = 0; i < history.size(); i++) {
            BidTransaction t = history.get(i);
            XYChart.Data<Number, Number> point = series.getData().get(i + 1);
            Tooltip tooltip = new Tooltip(
                    t.getBidderName() + "\n" + VND.format((long) t.getAmount()) + " đ\n" + t.getFormattedTimeHour()
            );
            Tooltip.install(point.getNode() != null ? point.getNode() : new javafx.scene.shape.Circle(), tooltip);
        }

        // Căn trục X đúng số điểm, tránh float
        if (xAxis != null) {
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(history.size());
            xAxis.setTickUnit(Math.max(1, history.size() / 10.0));
        }

        priceLineChart.getData().add(series);
        priceLineChart.setTitle("Giá đấu: " + auction.getItem().getName());

        // Gắn tooltip sau khi node đã được render
        Platform.runLater(() -> {
            for (int i = 0; i < history.size(); i++) {
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
        });
    }
}