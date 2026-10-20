package com.tartanga;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MainController {

    @FXML
    private TextField txtIP;
    @FXML
    private TextField txtPort;
    @FXML
    private TextField txtUser;
    @FXML
    private Button btnConnect;
    @FXML
    private Button btnDisconnect;
    @FXML
    private Label lblConnected;
    @FXML
    private CheckBox chkPrivate;
    @FXML
    private ComboBox<?> cmbPrivateUser;
    @FXML
    private TextField txtMessage;
    @FXML
    private Button txtSend;
}
