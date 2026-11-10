package com.tartanga.cliente.controller;

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
import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Callback;
import com.tartanga.cliente.model.*;

/**
 * Controla la interfaz gráfica del chat y coordina sus acciones con el cliente.
 */
public class MainController implements ClienteListener {

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

    /**
     * Inicializa las listas, los estilos y los eventos de los controles gráficos.
     */
    @FXML
    public void initialize() {
        mensajesObList = FXCollections.observableArrayList();
        lstMensajes.setItems(mensajesObList);

        lstMensajes.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            /**
             * Crea celdas que presentan los mensajes con estilos según su tipo.
             *
             * @param listView lista a la que pertenece la celda
             * @return celda configurada para mostrar mensajes
             */
            @Override
            public ListCell<String> call(ListView<String> listView) {
                return new ListCell<String>() {
                    /**
                     * Actualiza el texto y el estilo de una celda de mensaje.
                     *
                     * @param item mensaje que se muestra
                     * @param empty indica si la celda está vacía
                     */
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
                };
            }
        });

        lstMensajes.setStyle("-fx-control-inner-background-alt: -fx-control-inner-background;");

        usuariosActivos = FXCollections.observableArrayList();
        cmbPrivateUser.setItems(usuariosActivos);
        cmbPrivateUser.disableProperty().bind(chkPrivate.disabledProperty().or(chkPrivate.selectedProperty().not()));

        usuariosActivos.addListener(new ListChangeListener<String>() {
            /**
             * Habilita o deshabilita el envío privado según los usuarios disponibles.
             *
             * @param cambio cambio producido en la lista de usuarios
             */
            @Override
            public void onChanged(ListChangeListener.Change<? extends String> cambio) {
                boolean sinUsuarios = usuariosActivos.isEmpty();
                chkPrivate.setDisable(sinUsuarios);
                if (sinUsuarios) {
                    chkPrivate.setSelected(false);
                }
            }
        });

        chkPrivate.setDisable(true);

        btnConnect.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * Inicia la conexión al activar el botón correspondiente.
             *
             * @param evento evento de acción recibido
             */
            @Override
            public void handle(ActionEvent evento) {
                conectar();
            }
        });
        btnDisconnect.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * Desconecta al cliente al activar el botón correspondiente.
             *
             * @param evento evento de acción recibido
             */
            @Override
            public void handle(ActionEvent evento) {
                desconectar();
            }
        });
        txtSend.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * Envía el mensaje al activar el botón de envío.
             *
             * @param evento evento de acción recibido
             */
            @Override
            public void handle(ActionEvent evento) {
                enviarMensaje();
            }
        });

        txtIP.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * Intenta conectar al confirmar la dirección IP.
             *
             * @param evento evento de acción recibido
             */
            @Override
            public void handle(ActionEvent evento) {
                conectar();
            }
        });
        txtPort.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * Intenta conectar al confirmar el puerto.
             *
             * @param evento evento de acción recibido
             */
            @Override
            public void handle(ActionEvent evento) {
                conectar();
            }
        });
        txtUser.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * Intenta conectar al confirmar el nombre de usuario.
             *
             * @param evento evento de acción recibido
             */
            @Override
            public void handle(ActionEvent evento) {
                conectar();
            }
        });
        txtMessage.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * Envía el mensaje al confirmar su texto.
             *
             * @param evento evento de acción recibido
             */
            @Override
            public void handle(ActionEvent evento) {
                enviarMensaje();
            }
        });

        estadoInterfaz(false);
    }

    /**
     * Valida los datos introducidos e intenta conectar con el servidor.
     */
    private void conectar() {
        String ip;
        if (txtIP.getText().isEmpty()) {
            ip = "127.0.0.1";
        } else {
            ip = txtIP.getText();
        }

        int puerto;
        if (txtPort.getText().isEmpty()) {
            puerto = 5000;
        } else {
            puerto = Integer.parseInt(txtPort.getText());
        }

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

    /**
     * Valida y envía el texto como mensaje público o privado.
     */
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

    /**
     * Solicita la desconexión del cliente actual.
     */
    private void desconectar() {
        if (cliente != null) {
            cliente.desconectar();
        }
    }

    /**
     * Añade un mensaje a la lista y desplaza la vista hasta el último elemento.
     *
     * @param mensaje texto que se mostrará en la lista
     */
    private void agregarMensajeUI(String mensaje) {
        mensajesObList.add(mensaje);
        lstMensajes.scrollTo(mensajesObList.size() - 1);
    }

    /**
     * Actualiza la disponibilidad de los controles según el estado de conexión.
     *
     * @param conectado indica si el cliente está conectado
     */
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

    /**
     * Presenta en la interfaz un mensaje recibido del servidor.
     *
     * @param msj mensaje recibido
     */
    public void procesarMensaje(Mensaje msj) {
        if (msj.getTipo() == Mensaje.Tipo.ERROR) {
            notificarError(msj.getContenido());
            return;
        }
        boolean soyYo = msj.getRemitente().equals(txtUser.getText());

        String prefix;
        switch (msj.getTipo()) {
            case MENSAJE_PRIVADO:
                if (soyYo) {
                    prefix = "[Privado para " + msj.getDestinatario() + "]";
                } else {
                    prefix = "[Privado de " + msj.getRemitente() + "]";
                }
                break;

            default:
                if (soyYo) {
                    prefix = "[Tú]";
                } else {
                    prefix = "[" + msj.getRemitente() + "]";
                }
                break;
        }

        agregarMensajeUI(prefix + ": " + msj.getContenido());
    }

    /**
     * Actualiza los destinatarios privados con los usuarios conectados.
     *
     * @param msj mensaje que contiene la lista de usuarios activos
     */
    public void actualizarUsuarios(Mensaje msj) {
        String seleccionado = cmbPrivateUser.getValue();
        usuariosActivos.clear();

        for (String usuario : msj.getUsuariosActivos()) {
            if (!usuario.equals(txtUser.getText())) {
                usuariosActivos.add(usuario);
            }
        }

        if (!usuariosActivos.isEmpty()) {
            cmbPrivateUser.setPromptText("");
            if (usuariosActivos.contains(seleccionado)) {
                cmbPrivateUser.setValue(seleccionado);
            } else {
                cmbPrivateUser.setValue(usuariosActivos.get(0));
            }
        } else {
            cmbPrivateUser.setPromptText("Usuario");
        }
    }

    /**
     * Informa de la desconexión y restaura el estado inicial de la interfaz.
     */
    public void notificarDesconexion() {
        agregarMensajeUI("Sistema: Desconectado del servidor.");
        estadoInterfaz(false);
    }

    /**
     * Muestra un error recibido o producido por el cliente.
     *
     * @param mensaje descripción del error
     */
    public void notificarError(String mensaje) {
        agregarMensajeUI("Sistema: " + mensaje);
    }
}
