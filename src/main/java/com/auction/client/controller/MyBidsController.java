package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.Auction;
import com.auction.shared.network.protocol.SocketMessage;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller cho my-bids.fxml
 * Hiển thị tất cả phiên mà user hiện tại đã đặt giá.
 */
public class MyBidsController {

    @FXML
    private TableView<Auction> table;
    @FXML
    private TableColumn<Auction, String> colName;
    @FXML
    private TableColumn<Auction, String> colMyBid;
    @FXML
    private TableColumn<Auction, String> colCurrent;
    @FXML
    private TableColumn<Auction, String> colStatus;
    @FXML
    private TableColumn<Auction, String> colResult;
    @FXML
    private Label summaryLabel;

    private final ObservableList<Auction> rows = FXCollections.observableArrayList();
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    /**
     * Map<auctionId, giá cao nhất của user> — nhận từ server cùng response
     */
    private Map<String, Double> myBestBids = Collections.emptyMap();

    @FXML
    public void initialize() {
        setupTable();
        loadMyBids();
    }

    private void setupTable() {
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getName()));

        // Đọc giá cao nhất từ map server trả về, không phụ thuộc bidHistory
        colMyBid.setCellValueFactory(d -> {
            Double myBest = myBestBids.get(d.getValue().getAuctionId());
            return new SimpleStringProperty(myBest != null && myBest > 0 ? fmtVND(myBest) : "--");
        });

        colCurrent.setCellValueFactory(d ->
                new SimpleStringProperty(fmtVND(d.getValue().getCurrentPrice())));

        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus().name()));

        colResult.setCellValueFactory(d -> {
            String userId = ClientContext.getInstance().getCurrentUser().getId();
            Auction a = d.getValue();
            boolean finished = a.getStatus() == Auction.AuctionStatus.FINISHED
                    || a.getStatus() == Auction.AuctionStatus.PAID;
            if (!finished) return new SimpleStringProperty("Đang diễn ra");
            boolean won = userId.equals(a.getLeadBidderId());
            return new SimpleStringProperty(won ? "🏆 Thắng!" : "Thua");
        });

        // Tô màu cột kết quả
        colResult.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(val);
                setStyle(val.contains("Thắng")
                        ? "-fx-text-fill: #10b981; -fx-font-weight: bold;"
                        : val.contains("Thua")
                          ? "-fx-text-fill: #ef4444;"
                          : "-fx-text-fill: #94a3b8;");
            }
        });

        table.setItems(rows);
    }

    private void loadMyBids() {
        summaryLabel.setText("Đang tải...");
        new Thread(() -> {
            // Dùng GET_MY_BIDS — server tự lọc theo bidderId, trả đúng phiên + giá
            SocketMessage res = ClientContext.getInstance().getClient().getMyBids();
            Platform.runLater(() -> {
                if (res.isOk()) {
                    @SuppressWarnings("unchecked")
                    List<Auction> myAuctions = (List<Auction>) res.get("auctions");

                    @SuppressWarnings("unchecked")
                    Map<String, Double> bids = (Map<String, Double>) res.get("myBestBids");
                    if (bids != null) myBestBids = bids;

                    rows.setAll(myAuctions);

                    // Thống kê số phiên thắng
                    String userId = ClientContext.getInstance().getCurrentUser().getId();
                    long won = myAuctions.stream().filter(a ->
                            userId.equals(a.getLeadBidderId())
                                    && (a.getStatus() == Auction.AuctionStatus.FINISHED
                                    || a.getStatus() == Auction.AuctionStatus.PAID)).count();

                    summaryLabel.setText(String.format(
                            "Tham gia %d phiên | Thắng %d phiên", myAuctions.size(), won));
                }
            });
        }).start();
    }

    private String fmtVND(double amount) {
        return VND.format((long) amount) + " đ";
    }
}