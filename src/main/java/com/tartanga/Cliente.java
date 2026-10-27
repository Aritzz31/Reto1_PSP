package com.tartanga;

import java.io.IOException;
import javafx.application.Platform;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class Cliente {

    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private String usuario;
    private Thread hiloEscucha;
    private volatile boolean conectado = false;

    private Consumer<Mensaje> onMensajeRecibido;
    private Consumer<Mensaje> onListaUsuariosActualizada;
    private Runnable onDesconexion;

    public void setOnMensajeRecibido(Consumer<Mensaje> onMensajeRecibido) {
        this.onMensajeRecibido = onMensajeRecibido;
    }

    public void setOnListaUsuariosActualizada(Consumer<Mensaje> onListaUsuariosActualizada) {
        this.onListaUsuariosActualizada = onListaUsuariosActualizada;
    }

    public void setOnDesconexion(Runnable onDesconexion) {
        this.onDesconexion = onDesconexion;
    }

    public boolean conectar(String ip, int puerto, String usuario) {
        try {
            socket = new Socket(ip, puerto);
            this.usuario = usuario;
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());
            conectado = true;

            enviarMensaje(new Mensaje(Mensaje.Tipo.CONEXION, usuario, "Servidor", "Solicitud de conexión"));

            hiloEscucha = new Thread(() -> {
                try {
                    while (conectado) {
                        Mensaje msj = (Mensaje) entrada.readObject();

                        switch (msj.getTipo()) {
                            case Mensaje.Tipo.LISTA_USUARIOS -> {
                                if (onListaUsuariosActualizada != null) {
                                    Platform.runLater(() -> onListaUsuariosActualizada.accept(msj));
                                }
                            }
                            case Mensaje.Tipo.DESCONEXION -> cerrarConexion();
                            default -> {
                                if (onMensajeRecibido != null) {
                                    Platform.runLater(() -> onMensajeRecibido.accept(msj));
                                }
                            }
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    cerrarConexion();
                }
            });
            hiloEscucha.setDaemon(true);
            hiloEscucha.start();
            return true;
        } catch (IOException e) {
            System.err.println("Error al conectar: " + e.getMessage());
            return false;
        }
    }

    public void enviarTexto(String destinatario, String contenido, boolean esPrivado) {
        Mensaje.Tipo tipo = esPrivado ? Mensaje.Tipo.MENSAJE_PRIVADO : Mensaje.Tipo.MENSAJE_PUBLICO;
        Mensaje msj = new Mensaje(tipo, usuario, destinatario, contenido);
        enviarMensaje(msj);
    }

    private void enviarMensaje(Mensaje msj) {
        try {
            if (salida != null) {
                salida.writeObject(msj);
                salida.flush();
                salida.reset();
            }
        } catch (IOException e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
        }
    }

    public void desconectar() {
        enviarMensaje(new Mensaje(Mensaje.Tipo.DESCONEXION, usuario, "Servidor", "Saliendo..."));
        cerrarConexion();
    }

    private synchronized void cerrarConexion() {
        if (!conectado) {
            return;
        }
        conectado = false;
        try {
            if (socket != null) {
                socket.close();
            }
            if (onDesconexion != null) {
                Platform.runLater(onDesconexion);
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión.");
        }
    }
}
