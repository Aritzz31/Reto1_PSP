package com.tartanga.cliente.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import com.tartanga.cliente.controller.*;

/**
 * Gestiona la conexión del cliente con el servidor y el intercambio de mensajes.
 */
public class Cliente {

    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private String usuario;
    private Thread hilo;
    private boolean conectado = false;
    private final ClienteListener listener;

    /**
     * Crea un cliente que notificará los eventos de red al listener indicado.
     *
     * @param listener receptor de mensajes, errores y cambios de conexión
     */
    public Cliente(ClienteListener listener) {
        this.listener = listener;
    }

    /**
     * Abre la conexión con el servidor y comienza a escuchar sus mensajes.
     *
     * @param ip dirección del servidor
     * @param puerto puerto del servidor
     * @param usuario nombre del usuario que se conecta
     * @return {@code true} si se pudo iniciar la conexión
     */
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
                /**
                 * Lee los mensajes del servidor y los entrega al listener en JavaFX.
                 */
                @Override
                public void run() {
                    try {
                        while (conectado) {
                            Mensaje msj = (Mensaje) entrada.readObject();

                            switch (msj.getTipo()) {
                                case LISTA_USUARIOS:
                                    ejecutarHilo(new Runnable() {
                                        /**
                                         * Actualiza la lista de usuarios en la interfaz.
                                         */
                                        @Override
                                        public void run() {
                                            listener.actualizarUsuarios(msj);
                                        }
                                    });
                                    break;
                                case ERROR:
                                    ejecutarHilo(new Runnable() {
                                        /**
                                         * Muestra en la interfaz el error recibido.
                                         */
                                        @Override
                                        public void run() {
                                            listener.procesarMensaje(msj);
                                        }
                                    });
                                    break;
                                case DESCONEXION:
                                    ejecutarHilo(new Runnable() {
                                        /**
                                         * Procesa la notificación de desconexión recibida.
                                         */
                                        @Override
                                        public void run() {
                                            listener.procesarMensaje(msj);
                                        }
                                    });
                                    cerrarConexion();
                                    break;
                                default:
                                    ejecutarHilo(new Runnable() {
                                        /**
                                         * Entrega a la interfaz cualquier otro mensaje recibido.
                                         */
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

    /**
     * Envía un mensaje público o privado al servidor.
     *
     * @param destinatario usuario o grupo destinatario
     * @param contenido texto que se enviará
     * @param esPrivado indica si el mensaje debe enviarse de forma privada
     */
    public void enviarTexto(String destinatario, String contenido, boolean esPrivado) {
        Mensaje.Tipo tipo;
        if (esPrivado) {
            tipo = Mensaje.Tipo.MENSAJE_PRIVADO;
        } else {
            tipo = Mensaje.Tipo.MENSAJE_PUBLICO;
        }
        Mensaje msj = new Mensaje(tipo, usuario, destinatario, contenido);
        enviarMensaje(msj);
    }

    /**
     * Serializa y envía un mensaje por la conexión activa.
     *
     * @param msj mensaje que se enviará
     */
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
                /**
                 * Notifica en la interfaz que no se pudo enviar el mensaje.
                 */
                @Override
                public void run() {
                    listener.notificarError(error);
                }
            });
        }
    }

    /**
     * Solicita al servidor la desconexión y cierra la conexión local.
     */
    public void desconectar() {
        enviarMensaje(new Mensaje(Mensaje.Tipo.DESCONEXION, usuario, "Servidor", "Saliendo..."));
        cerrarConexion();
    }

    /**
     * Cierra la conexión una sola vez y notifica el cambio al listener.
     */
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
                /**
                 * Notifica en la interfaz que la conexión se ha cerrado.
                 */
                @Override
                public void run() {
                    listener.notificarDesconexion();
                }
            });
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión.");
        }
    }

    /**
     * Programa una tarea para ejecutarla en el hilo de la interfaz JavaFX.
     *
     * @param tarea acción que se ejecutará en el hilo de JavaFX
     */
    private void ejecutarHilo(Runnable tarea) {
        javafx.application.Platform.runLater(tarea);
    }
}
