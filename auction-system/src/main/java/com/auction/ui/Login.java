package com.auction.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.event.ActionEvent;
import java.io.IOException;

public class Login {

    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Button button;
    @FXML
    private Label checkPass;
    @FXML
    private Button registerButton;
    
    public void userLogin(ActionEvent event) throws IOException {
        checkLogin();
    }

    private void checkLogin() throws IOException {
        Main m = new Main();
        if (username.getText().toString().equals("nam") && password.getText().toString().equals("123")) {
            checkPass.setText("Login success!");
            m.changeScene("main");
        } else if ( username.getText().isEmpty() || password.getText().isEmpty()){
            checkPass.setText("Nhập đủ vào");
        }
        else{
            checkPass.setText("Thông tin không chính xác");
        }
    }
    @FXML
    private void handleRegister() {
    }
}
