package com.auction.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import javafx.event.ActionEvent;
import java.io.IOException;

public class AfterLogin {
    @FXML
    private Button logout;
    @FXML
    public void userLogout(ActionEvent event) throws IOException {
        Main m = new Main();
        m.changeScene("login");
    }
}
