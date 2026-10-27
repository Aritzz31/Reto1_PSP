package com.tartanga;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

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
    private ListView<String> lstMensajes;
    @FXML
    private CheckBox chkPrivate;
    @FXML
    private ComboBox<String> cmbPrivateUser;
    @FXML
    private TextField txtMessage;
    @FXML
    private Button txtSend;

    private Cliente cliente;
    private ObservableList<String> mensajesObList;
    private ObservableList<String> usuariosActivos;

    @FXML
    public void initialize() {
        mensajesObList = FXCollections.observableArrayList();
        lstMensajes.setItems(mensajesObList);

        lstMensajes.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);

                    if (item.startsWith("Sistema:") || item.startsWith("[Servidor]:")) {
                        setTextFill(Color.GRAY);
                        setFont(Font.font("System", FontPosture.ITALIC, 12));
                    } else if (item.startsWith("[Privado")) {
                        setTextFill(Color.DARKMAGENTA);
                        setFont(Font.font("System", FontWeight.BOLD, 12));
                    } else if (item.startsWith("[Tú]")) {
                        setTextFill(Color.DARKBLUE);
                        setFont(Font.font("System", FontWeight.BOLD, 12));
                    } else {
                        setTextFill(Color.BLACK);
                        setFont(Font.font("System", FontWeight.NORMAL, 12));
                    }
                }
            }
        });

        lstMensajes.setStyle("-fx-control-inner-background-alt: -fx-control-inner-background;");

        usuariosActivos = FXCollections.observableArrayList();
        cmbPrivateUser.setItems(usuariosActivos);
        cmbPrivateUser.disableProperty().bind(chkPrivate.disabledProperty().or(chkPrivate.selectedProperty().not()));

        usuariosActivos.addListener((ListChangeListener.Change<? extends String> c) -> {
            boolean sinUsuarios = usuariosActivos.isEmpty();
            chkPrivate.setDisable(sinUsuarios);
            if (sinUsuarios) {
                chkPrivate.setSelected(false);
            }
        });

        chkPrivate.setDisable(true);

        btnConnect.setOnAction(e -> conectar());
        btnDisconnect.setOnAction(e -> desconectar());
        txtSend.setOnAction(e -> enviarMensaje());

        txtMessage.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                enviarMensaje();
            }
        });

        estadoInterfaz(false);
    }

    private void conectar() {
        String ip = txtIP.getText().isEmpty() ? "127.0.0.1" : txtIP.getText();
        int puerto = txtPort.getText().isEmpty() ? 5000 : Integer.parseInt(txtPort.getText());
        String usuario = txtUser.getText();

        if (usuario == null || usuario.trim().isEmpty()) {
            agregarMensajeUI("Sistema: El usuario no puede estar vacío.");
            return;
        }

        mensajesObList.clear();

        cliente = new Cliente(this);

        if (cliente.conectar(ip, puerto, usuario)) {
            lblConnected.setText("Conectado");
            lblConnected.setTextFill(javafx.scene.paint.Color.GREEN);
            estadoInterfaz(true);
        } else {
            agregarMensajeUI("Sistema: No se pudo conectar al servidor.");
        }
    }

    private void enviarMensaje() {
        String texto = txtMessage.getText();
        if (texto == null || texto.trim().isEmpty()) {
            return;
        }

        boolean esPrivado = chkPrivate.isSelected();
        String destinatario = "Todos";

        if (esPrivado) {
            destinatario = cmbPrivateUser.getValue();
            if (destinatario == null) {
                agregarMensajeUI("Sistema: Selecciona un usuario para el mensaje privado.");
                return;
            }
        }

        cliente.enviarTexto(destinatario, texto, esPrivado);
        txtMessage.clear();
    }

    private void desconectar() {
        if (cliente != null) {
            cliente.desconectar();
        }
    }

    private void agregarMensajeUI(String mensaje) {
        mensajesObList.add(mensaje);
        lstMensajes.scrollTo(mensajesObList.size() - 1);
    }

    private void estadoInterfaz(boolean conectado) {
        txtIP.setDisable(conectado);
        txtPort.setDisable(conectado);
        txtUser.setDisable(conectado);
        btnConnect.setDisable(conectado);

        btnDisconnect.setDisable(!conectado);
        txtMessage.setDisable(!conectado);
        txtSend.setDisable(!conectado);

        if (!conectado) {
            lblConnected.setText("No conectado");
            lblConnected.setTextFill(javafx.scene.paint.Color.RED);
            usuariosActivos.clear();
            cmbPrivateUser.setPromptText("Usuario");
            chkPrivate.setSelected(false);
        }
    }

    public void procesarMensaje(Mensaje msj) {
        boolean soyYo = msj.getRemitente().equals(txtUser.getText());

        String prefix = switch (msj.getTipo()) {
            case MENSAJE_PRIVADO ->
                soyYo ? "[Privado para " + msj.getDestinatario() + "]" : "[Privado de " + msj.getRemitente() + "]";
            default ->
                soyYo ? "[Tú]" : "[" + msj.getRemitente() + "]";
        };

        agregarMensajeUI(prefix + ": " + msj.getContenido());
    }

    public void actualizarUsuarios(Mensaje msj) {
        String seleccionado = cmbPrivateUser.getValue();
        usuariosActivos.clear();

        msj.getUsuariosActivos().stream()
                .filter(u -> !u.equals(txtUser.getText()))
                .forEach(usuariosActivos::add);

        if (!usuariosActivos.isEmpty()) {
            cmbPrivateUser.setPromptText("");
            cmbPrivateUser.setValue(usuariosActivos.contains(seleccionado) ? seleccionado : usuariosActivos.get(0));
        } else {
            cmbPrivateUser.setPromptText("Usuario");
        }
    }

    public void notificarDesconexion() {
        agregarMensajeUI("Sistema: Desconectado del servidor.");
        estadoInterfaz(false);
    }
}
