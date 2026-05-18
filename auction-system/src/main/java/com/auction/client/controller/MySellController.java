package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item.Item;
import com.auction.shared.network.protocol.SocketMessage;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
    @FXML private Label summaryLabel;


    private final ObservableList<Auction> rows = FXCollections.observableArrayList();
    private final ObservableList<Item>    items    = FXCollections.observableArrayList();

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

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
        itemTable.setItems(items);
    }

    private void loadMyBids() {
        summaryLabel.setText("Đang tải...");
        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().getMyItems();
            Platform.runLater(() -> {
                if (res.isOk()) {
                    @SuppressWarnings("unchecked")
                    List<Item> list = (List<Item>) res.get("items");
                    items.setAll(list);
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

    private String fmtVND(double amount) {
        return VND.format((long) amount) + " đ";
    }
}