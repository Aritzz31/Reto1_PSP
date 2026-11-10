package com.tartanga.cliente.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import com.tartanga.cliente.controller.*;

public class Cliente {

    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private String usuario;
    private Thread hilo;
    private boolean conectado = false;
    private final ClienteListener listener;

    public Cliente(ClienteListener listener) {
        this.listener = listener;
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

            hilo = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (conectado) {
                            Mensaje msj = (Mensaje) entrada.readObject();

                            switch (msj.getTipo()) {
                                case LISTA_USUARIOS:
                                    ejecutarHilo(new Runnable() {
                                        @Override
                                        public void run() {
                                            listener.actualizarUsuarios(msj);
                                        }
                                    });
                                    break;
                                case ERROR:
                                    ejecutarHilo(new Runnable() {
                                        @Override
                                        public void run() {
                                            listener.procesarMensaje(msj);
                                        }
                                    });
                                    break;
                                case DESCONEXION:
                                    ejecutarHilo(new Runnable() {
                                        @Override
                                        public void run() {
                                            listener.procesarMensaje(msj);
                                        }
                                    });
                                    cerrarConexion();
                                    break;
                                default:
                                    ejecutarHilo(new Runnable() {
                                        @Override
                                        public void run() {
                                            listener.procesarMensaje(msj);
                                        }
                                    });
                                    break;
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        cerrarConexion();
                    }
                }
            });
            hilo.setDaemon(true);
            hilo.start();
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
            final String error = "No se pudo enviar el mensaje: " + e.getMessage();
            ejecutarHilo(new Runnable() {
                @Override
                public void run() {
                    listener.notificarError(error);
                }
            });
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
            ejecutarHilo(new Runnable() {
                @Override
                public void run() {
                    listener.notificarDesconexion();
                }
            });
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión.");
        }
    }

    private void ejecutarHilo(Runnable tarea) {
        javafx.application.Platform.runLater(tarea);
    }
}
