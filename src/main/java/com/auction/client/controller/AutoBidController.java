package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.Auction;
import com.auction.shared.network.protocol.SocketMessage;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Controller cho auto-bid.fxml
 * Cho phép Bidder đăng ký auto-bid (maxBid + increment) cho một phiên.
 */
public class AutoBidController {

    @FXML
    private ComboBox<Auction> auctionCombo;
    @FXML
    private TextField maxBidField;
    @FXML
    private TextField incrementField;
    @FXML
    private Label resultLabel;

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        loadActiveAuctions();

        // Hiển thị tên phiên trong ComboBox
        auctionCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Auction a, boolean empty) {
                super.updateItem(a, empty);
                setText(empty || a == null ? null
                        : a.getItem().getName() + " — " + fmtVND(a.getCurrentPrice()));
            }
        });
        auctionCombo.setButtonCell(auctionCombo.getCellFactory().call(null));
    }

    private void loadActiveAuctions() {
        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().getAllAuctions();
            Platform.runLater(() -> {
                if (res.isOk()) {
                    @SuppressWarnings("unchecked")
                    List<Auction> all = (List<Auction>) res.get("auctions");
                    List<Auction> active = all.stream()
                            .filter(a -> a.getStatus() == Auction.AuctionStatus.OPEN
                                    || a.getStatus() == Auction.AuctionStatus.RUNNING)
                            .toList();
                    auctionCombo.setItems(FXCollections.observableArrayList(active));
                    if (!active.isEmpty()) auctionCombo.getSelectionModel().selectFirst();
                }
            });
        }).start();
    }

    @FXML
    private void handleSetAutoBid(ActionEvent event) {
        Auction selected = auctionCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showResult("❌ Chọn phiên đấu giá.", false);
            return;
        }

        String maxStr = maxBidField.getText().trim().replaceAll("[^0-9]", "");
        String incStr = incrementField.getText().trim().replaceAll("[^0-9]", "");

        if (maxStr.isEmpty() || incStr.isEmpty()) {
            showResult("❌ Điền đủ giá tối đa và bước giá.", false);
            return;
        }

        double maxBid, increment;
        try {
            maxBid = Double.parseDouble(maxStr);
            increment = Double.parseDouble(incStr);
        } catch (NumberFormatException e) {
            showResult("❌ Số tiền không hợp lệ.", false);
            return;
        }

        if (maxBid <= selected.getCurrentPrice()) {
            showResult("❌ Giá tối đa phải lớn hơn giá hiện tại: " + fmtVND(selected.getCurrentPrice()), false);
            return;
        }
        if (increment <= 0) {
            showResult("❌ Bước giá phải > 0.", false);
            return;
        }

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient()
                    .registerAutoBid(selected.getAuctionId(), maxBid, increment);
            Platform.runLater(() -> {
                if (res.isOk()) {
                    showResult("🤖 Đã kích hoạt Auto-Bid!\nMax: " + fmtVND(maxBid)
                            + " | Bước: " + fmtVND(increment), true);
                    maxBidField.clear();
                    incrementField.clear();
                } else {
                    showResult("❌ " + res.getMessage(), false);
                }
            });
        }).start();
    }

    private void showResult(String msg, boolean success) {
        resultLabel.setStyle(success
                ? "-fx-text-fill: #10b981; -fx-font-size: 12;"
                : "-fx-text-fill: #ef4444; -fx-font-size: 12;");
        resultLabel.setText(msg);
    }

    private String fmtVND(double amount) {
        return VND.format((long) amount) + " đ";
    }
}
