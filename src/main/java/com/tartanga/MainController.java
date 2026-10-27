package com.tartanga;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

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

        lstMensajes.setStyle("-fx-control-inner-background-alt: -fx-control-inner-background;");

        usuariosActivos = FXCollections.observableArrayList();
        cmbPrivateUser.setItems(usuariosActivos);
        cmbPrivateUser.disableProperty().bind(chkPrivate.disabledProperty().or(chkPrivate.selectedProperty().not()));

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

        cliente = new Cliente();

        cliente.setOnMensajeRecibido(msj -> {
            String miUsuario = txtUser.getText();
            String prefix;

            if (msj.getRemitente().equals(miUsuario)) {
                if (msj.getTipo() == Mensaje.Tipo.MENSAJE_PRIVADO) {
                    prefix = "[Privado para " + msj.getDestinatario() + "]";
                } else {
                    prefix = "[Tú]";
                }
            } else {
                if (msj.getTipo() == Mensaje.Tipo.MENSAJE_PRIVADO) {
                    prefix = "[Privado de " + msj.getRemitente() + "]";
                } else {
                    prefix = "[" + msj.getRemitente() + "]";
                }
            }

            agregarMensajeUI(prefix + ": " + msj.getContenido());
        });

        cliente.setOnListaUsuariosActualizada(msj -> {
            String seleccionadoAnterior = cmbPrivateUser.getValue();
            usuariosActivos.clear();

            for (String u : msj.getUsuariosActivos()) {
                if (!u.equals(txtUser.getText())) {
                    usuariosActivos.add(u);
                }
            }

            if (!usuariosActivos.isEmpty()) {
                cmbPrivateUser.setPromptText("");
                if (seleccionadoAnterior != null && usuariosActivos.contains(seleccionadoAnterior)) {
                    cmbPrivateUser.setValue(seleccionadoAnterior);
                } else {
                    cmbPrivateUser.getSelectionModel().selectFirst();
                }
            } else {
                cmbPrivateUser.setPromptText("Usuario");
            }
        });

        cliente.setOnDesconexion(() -> {
            agregarMensajeUI("Sistema: Desconectado del servidor.");
            estadoInterfaz(false);
        });

        if (cliente.conectar(ip, puerto, usuario)) {
            lblConnected.setText("Conectado");
            lblConnected.setTextFill(javafx.scene.paint.Color.GREEN);
            estadoInterfaz(true);
            mensajesObList.clear();
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
        chkPrivate.setDisable(!conectado);

        if (!conectado) {
            lblConnected.setText("No conectado");
            lblConnected.setTextFill(javafx.scene.paint.Color.RED);
            usuariosActivos.clear();
            cmbPrivateUser.setPromptText("Usuario");
            chkPrivate.setSelected(false);
        }
    }
}
