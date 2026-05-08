package com.auction.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class AfterLogin {
    @FXML
    private Button logout;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private BorderPane mainPane;
    @FXML private VBox sidebar;
    @FXML private Button btnDashboard;
    @FXML private Button btnAuctions;
    @FXML private Button btnMyBids;
    @FXML private Button btnAutoBid;
    @FXML private Button btnSeller;
    @FXML private Button btnAdmin;
    @FXML private Button btnHistory;
    @FXML private Label statusLabel;

    @FXML private void showDashboard() { loadPage("dashboard"); }
    @FXML private void showAuctions() { loadPage("auctions"); }
    @FXML private void showMyBids() { loadPage("my-bids"); }
    @FXML private void showAutoBid() { loadPage("auto-bid"); }
    @FXML private void showSeller() { loadPage("seller"); }
    @FXML private void showAdmin() { loadPage("admin"); }
    @FXML private void showHistory() { loadPage("history"); }

    public void loadPage(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/"+fxml+".fxml"));
            Parent page = loader.load();
            mainPane.setCenter(page);
        } catch (IOException e) {
            statusLabel.setText("Lỗi load trang: "+e.getMessage());
        }
    }

    public void userLogout(ActionEvent event) throws IOException {
        Main m = new Main();
        m.changeScene("login2");
    }
}
