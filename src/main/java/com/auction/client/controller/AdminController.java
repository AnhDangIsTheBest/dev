package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.shared.model.Auction;
import com.auction.shared.model.User.User;
import com.auction.shared.network.protocol.SocketMessage;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminController {

    // Stats
    @FXML private Label totalUsersLabel;
    @FXML private Label totalAuctionsLabel;
    @FXML private Label totalBidsLabel;
    @FXML private Label adminStatusLabel;

    // Tabs
    @FXML private Button tabUserBtn;
    @FXML private Button tabAucBtn;
    @FXML private VBox   panelUsers;
    @FXML private VBox   panelAuctions;

    // Users table
    @FXML private TableView<User>          userTable;
    @FXML private TableColumn<User,String> colUserName;
    @FXML private TableColumn<User,String> colUserRole;
    @FXML private TableColumn<User,String> colUserEmail;

    // Auctions table
    @FXML private TableView<Auction>            adminAucTable;
    @FXML private TableColumn<Auction,String>   colAdminAucName;
    @FXML private TableColumn<Auction,String>   colAdminAucStatus;
    @FXML private TableColumn<Auction,String>   colAdminAucPrice;
    @FXML private TableColumn<Auction,String>   colAdminAucBids;

    private final ObservableList<User>    users    = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctions = FXCollections.observableArrayList();
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi","VN"));

    // ── Tab styles ───────────────────────────────────────────────
    private static final String TAB_ACTIVE =
            "-fx-background-color: #0d0f14; -fx-text-fill: #f1f5f9; -fx-font-size: 13; -fx-font-weight: bold;" +
                    "-fx-padding: 12 24; -fx-border-color: transparent transparent #f59e0b transparent; -fx-border-width: 0 0 2 0; -fx-cursor: hand;";
    private static final String TAB_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 13;" +
                    "-fx-padding: 12 24; -fx-border-color: transparent; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        setupUserTable();
        setupAuctionTable();
        loadAll();
    }

    // ── Tab switching ────────────────────────────────────────────

    @FXML
    private void handleTabUser(ActionEvent e) {
        tabUserBtn.setStyle(TAB_ACTIVE);
        tabAucBtn.setStyle(TAB_INACTIVE);
        panelUsers.setVisible(true);    panelUsers.setManaged(true);
        panelAuctions.setVisible(false); panelAuctions.setManaged(false);
        hideStatus();
    }

    @FXML
    private void handleTabAuction(ActionEvent e) {
        tabAucBtn.setStyle(TAB_ACTIVE);
        tabUserBtn.setStyle(TAB_INACTIVE);
        panelAuctions.setVisible(true);  panelAuctions.setManaged(true);
        panelUsers.setVisible(false);    panelUsers.setManaged(false);
        hideStatus();
    }

    // ── Setup tables ─────────────────────────────────────────────

    private void setupUserTable() {
        colUserName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullname()));
        colUserRole.setCellValueFactory(d -> new SimpleStringProperty(formatRole(d.getValue())));
        colUserEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));

        // Tô màu vai trò
        colUserRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                setStyle("ADMIN".equals(val)
                        ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;"
                        : "-fx-text-fill: #10b981;");
            }
        });

        userTable.setItems(users);
    }

    private void setupAuctionTable() {
        colAdminAucName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getName()));
        colAdminAucStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus().name()));
        colAdminAucPrice.setCellValueFactory(d ->
                new SimpleStringProperty(VND.format((long) d.getValue().getCurrentPrice()) + " đ"));
        colAdminAucBids.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getBidHistory().size())));

        // Tô màu trạng thái
        colAdminAucStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                setStyle(switch (val) {
                    case "RUNNING"  -> "-fx-text-fill: #10b981; -fx-font-weight: bold;";
                    case "OPEN"     -> "-fx-text-fill: #3b82f6;";
                    case "FINISHED" -> "-fx-text-fill: #64748b;";
                    case "PAID"     -> "-fx-text-fill: #f59e0b;";
                    case "CANCELED" -> "-fx-text-fill: #ef4444;";
                    default         -> "-fx-text-fill: #94a3b8;";
                });
            }
        });

        adminAucTable.setItems(auctions);
    }

    // ── Load ─────────────────────────────────────────────────────

    @FXML
    private void handleRefresh(ActionEvent e) { loadAll(); }

    private void loadAll() {
        new Thread(() -> {
            SocketMessage resUsers = ClientContext.getInstance().getClient().getAllUsers();
            SocketMessage resAucs  = ClientContext.getInstance().getClient().getAllAuctions();
            Platform.runLater(() -> {
                if (resUsers.isOk()) {
                    @SuppressWarnings("unchecked")
                    List<User> list = (List<User>) resUsers.get("users");
                    users.setAll(list);
                    totalUsersLabel.setText(String.valueOf(list.size()));
                }
                if (resAucs.isOk()) {
                    @SuppressWarnings("unchecked")
                    List<Auction> list = (List<Auction>) resAucs.get("auctions");
                    auctions.setAll(list);
                    totalAuctionsLabel.setText(String.valueOf(list.size()));
                    long totalBids = list.stream().mapToLong(a -> a.getBidHistory().size()).sum();
                    totalBidsLabel.setText(String.valueOf(totalBids));
                }
            });
        }).start();
    }

    // ── Xóa tài khoản ────────────────────────────────────────────

    @FXML
    private void handleDeleteUser(ActionEvent e) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("⚠️  Chọn tài khoản cần xóa trước.", false); return;
        }
        if ("ADMIN".equals(selected.getRole())) {
            showStatus("❌  Không thể xóa tài khoản Admin.", false); return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa tài khoản: " + selected.getFullname());
        confirm.setContentText("Hành động này không thể hoàn tác. Bạn có chắc chắn?");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient().deleteUser(selected.getId());
            Platform.runLater(() -> {
                if (res.isOk()) {
                    showStatus("✅  Đã xóa tài khoản: " + selected.getFullname(), true);
                    loadAll();
                } else {
                    showStatus("❌  " + res.getMessage(), false);
                }
            });
        }).start();
    }

    // ── Dừng phiên ───────────────────────────────────────────────

    @FXML
    private void handleEndSelectedAuction(ActionEvent e) {
        Auction selected = adminAucTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("⚠️  Chọn phiên cần dừng trước.", false); return;
        }
        if (selected.getStatus() == Auction.AuctionStatus.FINISHED
                || selected.getStatus() == Auction.AuctionStatus.CANCELED
                || selected.getStatus() == Auction.AuctionStatus.PAID) {
            showStatus("⚠️  Phiên này đã kết thúc rồi.", false); return;
        }

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient()
                    .finishAuction(selected.getAuctionId());
            Platform.runLater(() -> {
                if (res.isOk()) {
                    showStatus("🏁  Đã kết thúc phiên: " + selected.getItem().getName(), true);
                    loadAll();
                } else {
                    showStatus("❌  " + res.getMessage(), false);
                }
            });
        }).start();
    }

    // ── Xóa phiên ────────────────────────────────────────────────

    @FXML
    private void handleDeleteAuction(ActionEvent e) {
        Auction selected = adminAucTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("⚠️  Chọn phiên cần xóa trước.", false); return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa phiên: " + selected.getItem().getName());
        confirm.setContentText("Toàn bộ lịch sử bid của phiên này cũng sẽ bị xóa. Bạn có chắc chắn?");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        new Thread(() -> {
            SocketMessage res = ClientContext.getInstance().getClient()
                    .deleteAuction(selected.getAuctionId());
            Platform.runLater(() -> {
                if (res.isOk()) {
                    showStatus("🗑️  Đã xóa phiên: " + selected.getItem().getName(), true);
                    loadAll();
                } else {
                    showStatus("❌  " + res.getMessage(), false);
                }
            });
        }).start();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void hideStatus() {
        if (adminStatusLabel == null) return;
        adminStatusLabel.setText("");
        adminStatusLabel.setVisible(false);
        adminStatusLabel.setManaged(false);
    }

    private void showStatus(String msg, boolean success) {
        adminStatusLabel.setVisible(true);
        adminStatusLabel.setManaged(true);
        adminStatusLabel.setStyle(success
                ? "-fx-background-color: rgba(16,185,129,0.12); -fx-border-color: rgba(16,185,129,0.35); -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #34d399; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 9 12;"
                : "-fx-background-color: rgba(239,68,68,0.12); -fx-border-color: rgba(239,68,68,0.35); -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #f87171; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 9 12;");
        adminStatusLabel.setText(msg);
    }

    private void styleAlert(Alert alert) {
        alert.getDialogPane().setStyle(
                "-fx-background-color: #161820; -fx-border-color: rgba(255,255,255,0.1);"
        );
        javafx.scene.Node contentLabel = alert.getDialogPane().lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: #cbd5e1;");
        }
    }

    private String formatRole(User user) {
        if (user == null) return "";
        return "ADMIN".equals(user.getRole()) ? "ADMIN" : "T\u00e0i kho\u1ea3n \u0111\u1ea5u gi\u00e1";
    }
}
