package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.Item.ItemFactory;
import com.auction.shared.model.Item.ItemFactory.ItemType;
import com.auction.shared.network.protocol.SocketMessage;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SellerController {

    // ── Items ────────────────────────────────────────────────────
    @FXML
    private ComboBox<String> itemTypeCombo;
    @FXML
    private ComboBox<String> itemCondCombo;
    @FXML
    private TextField itemNameField;
    @FXML
    private TextInputControl itemDescField;
    @FXML
    private TextField itemPriceField;
    @FXML
    private TableView<Item> itemTable;
    @FXML
    private TableColumn<Item, String> colItemName;
    @FXML
    private TableColumn<Item, String> colItemCat;
    @FXML
    private TableColumn<Item, String> colItemPrice;
    @FXML
    private TableColumn<Item, String> colItemCond;
    @FXML
    private Label selectedImageLabel;
    @FXML
    private ImageView selectedImagePreview;

    // ── Auctions ─────────────────────────────────────────────────
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private Spinner<Integer> startHourSpinner;
    @FXML
    private Spinner<Integer> startMinSpinner;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Spinner<Integer> endHourSpinner;
    @FXML
    private Spinner<Integer> endMinSpinner;
    @FXML
    private CheckBox antiSnipeCheck;

    @FXML
    private TableView<Auction> auctionTable;
    @FXML
    private TableColumn<Auction, String> colAucName;
    @FXML
    private TableColumn<Auction, String> colAucStatus;
    @FXML
    private TableColumn<Auction, String> colAucPrice;

    @FXML
    private Label sellerResultLabel;

    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctions = FXCollections.observableArrayList();
    private File selectedImageFile;

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML
    public void initialize() {
        itemTypeCombo.setItems(FXCollections.observableArrayList(
                "ELECTRONICS", "ART", "VEHICLE", "OTHER"));
        itemTypeCombo.getSelectionModel().selectFirst();

        if (itemCondCombo != null) {
            itemCondCombo.setItems(FXCollections.observableArrayList(
                    "NEW", "LIKE_NEW", "USED", "REFURBISHED"));
            itemCondCombo.getSelectionModel().selectFirst();
        }

        // Khởi tạo Spinner giờ/phút
        startHourSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 23, 0));
        startMinSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 59, 0));
        endHourSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 23, 1));
        endMinSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 59, 0));

        // Mặc định start = hôm nay, end = hôm nay + 1 giờ
        LocalDateTime now = LocalDateTime.now();
        startDatePicker.setValue(now.toLocalDate());
        startHourSpinner.getValueFactory().setValue(now.getHour());
        startMinSpinner.getValueFactory().setValue(now.getMinute());
        endDatePicker.setValue(now.toLocalDate());
        endHourSpinner.getValueFactory().setValue((now.getHour() + 1) % 24);
        endMinSpinner.getValueFactory().setValue(now.getMinute());

        setupItemTable();
        setupAuctionTable();
        loadMyItems();
        loadMyAuctions();
    }

    // ── Nút tiện ích thời gian ───────────────────────────────────

    /**
     * Đặt thời gian bắt đầu = ngay bây giờ
     */
    @FXML
    private void handleSetStartNow(ActionEvent event) {
        LocalDateTime now = LocalDateTime.now();
        startDatePicker.setValue(now.toLocalDate());
        startHourSpinner.getValueFactory().setValue(now.getHour());
        startMinSpinner.getValueFactory().setValue(now.getMinute());
    }

    /**
     * Cộng thêm 1 giờ vào thời gian kết thúc
     */
    @FXML
    private void handleAddOneHour(ActionEvent event) {
        LocalDateTime current = getEndTime();
        LocalDateTime plusOne = current.plusHours(1);
        endDatePicker.setValue(plusOne.toLocalDate());
        endHourSpinner.getValueFactory().setValue(plusOne.getHour());
        endMinSpinner.getValueFactory().setValue(plusOne.getMinute());
    }

    // ── Helpers lấy LocalDateTime từ picker + spinner ────────────

    private LocalDateTime getStartTime() {
        LocalDate date = startDatePicker.getValue();
        if (date == null) date = LocalDate.now();
        int h = startHourSpinner.getValue();
        int m = startMinSpinner.getValue();
        return date.atTime(h, m);
    }

    private LocalDateTime getEndTime() {
        LocalDate date = endDatePicker.getValue();
        if (date == null) date = LocalDate.now();
        int h = endHourSpinner.getValue();
        int m = endMinSpinner.getValue();
        return date.atTime(h, m);
    }

    // ── TableView setup ──────────────────────────────────────────

    private void setupItemTable() {
        colItemName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colItemCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        colItemPrice.setCellValueFactory(d ->
                new SimpleStringProperty(VND.format((long) d.getValue().getStartingPrice()) + " đ"));
        if (colItemCond != null) {
            colItemCond.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        }
        itemTable.setItems(items);
    }

    private void setupAuctionTable() {
        colAucName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getName()));
        colAucStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus().name()));
        colAucPrice.setCellValueFactory(d ->
                new SimpleStringProperty(VND.format((long) d.getValue().getCurrentPrice()) + " đ"));
        auctionTable.setItems(auctions);
    }

    // ── Load data ────────────────────────────────────────────────

    private void loadMyItems() {
        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().getMyItems();
            Platform.runLater(() -> {
                if (res.isOk()) {
                    @SuppressWarnings("unchecked")
                    List<Item> list = (List<Item>) res.get("items");
                    items.setAll(list);
                }
            });
        }).start();
    }

    private void loadMyAuctions() {
        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().getAllAuctions();
            Platform.runLater(() -> {
                if (res.isOk()) {
                    @SuppressWarnings("unchecked")
                    List<Auction> all = (List<Auction>) res.get("auctions");
                    auctions.setAll(all.stream()
                            .filter(a -> a.getItem() != null)
                            .toList());
                }
            });
        }).start();
    }

    // ── Thêm sản phẩm ────────────────────────────────────────────

    @FXML
    private void handleAddItem(ActionEvent event) {
        String name = itemNameField.getText().trim();
        String desc = itemDescField.getText().trim();
        String type = itemTypeCombo.getValue();
        String cond = itemCondCombo != null ? itemCondCombo.getValue() : "NEW";
        String priceStr = itemPriceField.getText().trim().replaceAll("[^0-9]", "");

        if (name.isEmpty() || priceStr.isEmpty()) {
            showResult("❌ Điền đủ tên và giá khởi điểm.", false);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            showResult("❌ Giá không hợp lệ.", false);
            return;
        }

        ItemType itemType = "OTHER".equals(type) ? ItemType.OTHERITEM : ItemType.valueOf(type);
        Item item = ItemFactory.createItem(
                itemType, name, desc, price, cond, price);
        attachSelectedImageToItem(item);

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().createItem(item);
            Platform.runLater(() -> {
                if (res.isOk()) {
                    showResult("✅ Đã thêm sản phẩm: " + name, true);
                    clearItemForm();
                    loadMyItems();
                } else {
                    showResult("❌ " + res.getMessage(), false);
                }
            });
        }).start();
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh sản phẩm");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File file = chooser.showOpenDialog(itemNameField.getScene().getWindow());
        if (file == null) return;

        selectedImageFile = file;
        if (selectedImageLabel != null) selectedImageLabel.setText(file.getName());
        if (selectedImagePreview != null) selectedImagePreview.setImage(new Image(file.toURI().toString()));
    }

    private void attachSelectedImageToItem(Item item) {
        if (selectedImageFile == null || item == null) return;
        try {
            item.setImageData(Files.readAllBytes(selectedImageFile.toPath()));
        } catch (IOException e) {
            Platform.runLater(() -> showResult("⚠️ Không đọc được ảnh: " + e.getMessage(), false));
        }
    }

    @FXML
    private void handleAddItemAndCreateAuction(ActionEvent event) {
        String name = itemNameField.getText().trim();
        String desc = itemDescField.getText().trim();
        String type = itemTypeCombo.getValue();
        String priceStr = itemPriceField.getText().trim().replaceAll("[^0-9]", "");

        if (name.isEmpty() || priceStr.isEmpty()) {
            showResult("❌ Điền đủ tên sản phẩm và giá khởi điểm.", false);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            showResult("❌ Giá không hợp lệ.", false);
            return;
        }

        LocalDateTime start = getStartTime();
        LocalDateTime end = getEndTime();
        if (!end.isAfter(start)) {
            showResult("❌ Thời gian kết thúc phải sau thời gian bắt đầu.", false);
            return;
        }
        if (end.isBefore(LocalDateTime.now())) {
            showResult("❌ Thời gian kết thúc đã qua.", false);
            return;
        }

        ItemType itemType = "OTHER".equals(type) ? ItemType.OTHERITEM : ItemType.valueOf(type);
        Item item = ItemFactory.createItem(itemType, name, desc, price, "NEW", price);
        attachSelectedImageToItem(item);
        boolean antiSnipe = antiSnipeCheck.isSelected();

        new Thread(() -> {
            SocketMessage itemRes = ClientContext.getInstance().getClient().createItem(item);
            if (!itemRes.isOk()) {
                Platform.runLater(() -> showResult("❌ " + itemRes.getMessage(), false));
                return;
            }

            SocketMessage aucRes = ClientContext.getInstance().getClient()
                    .createAuction(item, start, end, antiSnipe, 30, 60);
            Platform.runLater(() -> {
                if (aucRes.isOk()) {
                    String auctionId = aucRes.getString("auctionId");
                    showResult("✅ Đã đăng sản phẩm và tạo phiên " + auctionId + " thành công!", true);
                    clearItemForm();
                    loadMyItems();
                    loadMyAuctions();
                } else {
                    showResult("✅ Đã thêm sản phẩm, nhưng tạo phiên lỗi: " + aucRes.getMessage(), false);
                    loadMyItems();
                }
            });
        }).start();
    }

    // ── Tạo phiên đấu giá ────────────────────────────────────────

    @FXML
    private void handleCreateAuction(ActionEvent event) {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showResult("❌ Chọn sản phẩm từ danh sách.", false);
            return;
        }

        LocalDateTime start = getStartTime();
        LocalDateTime end = getEndTime();

        if (!end.isAfter(start)) {
            showResult("❌ Thời gian kết thúc phải sau thời gian bắt đầu.", false);
            return;
        }
        if (end.isBefore(LocalDateTime.now())) {
            showResult("❌ Thời gian kết thúc đã qua.", false);
            return;
        }

        boolean antiSnipe = antiSnipeCheck.isSelected();

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient()
                    .createAuction(selectedItem, start, end, antiSnipe, 30, 60);
            Platform.runLater(() -> {
                if (res.isOk()) {
                    String auctionId = res.getString("auctionId");
                    showResult("✅ Tạo phiên " + auctionId + " thành công!\n"
                            + "Bắt đầu: " + start.format(FMT)
                            + "  →  Kết thúc: " + end.format(FMT), true);
                    loadMyAuctions();
                } else {
                    showResult("❌ " + res.getMessage(), false);
                }
            });
        }).start();
    }

    // ── Kết thúc phiên ───────────────────────────────────────────

    @FXML
    private void handleEndAuction(ActionEvent event) {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showResult("❌ Chọn phiên đấu giá cần kết thúc.", false);
            return;
        }

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient()
                    .finishAuction(selected.getAuctionId());
            Platform.runLater(() -> {
                if (res.isOk()) {
                    showResult("🏁 Đã kết thúc phiên: " + selected.getAuctionId(), true);
                    loadMyAuctions();
                } else {
                    showResult("❌ " + res.getMessage(), false);
                }
            });
        }).start();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void clearItemForm() {
        itemNameField.clear();
        itemDescField.clear();
        itemPriceField.clear();
        selectedImageFile = null;
        if (selectedImageLabel != null) selectedImageLabel.setText("Chưa chọn ảnh");
        if (selectedImagePreview != null) selectedImagePreview.setImage(null);
    }

    private void showResult(String msg, boolean success) {
        sellerResultLabel.setStyle(success
                ? "-fx-text-fill: #10b981; -fx-font-size: 12;"
                : "-fx-text-fill: #ef4444; -fx-font-size: 12;");
        sellerResultLabel.setText(msg);
    }
}