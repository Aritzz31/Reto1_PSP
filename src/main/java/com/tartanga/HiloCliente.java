package com.tartanga;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloCliente extends Thread {

    private static final Logger LOGGER = Logger.getLogger(HiloCliente.class.getName());
    private final Socket socketCliente;
    private final Chat chat;
    private String nombreUsuario;
    private boolean registrado;

    public HiloCliente(Socket socketCliente, Chat chat) {
        this.socketCliente = socketCliente;
        this.chat = chat;
    }

    public void detener() {
        try {
            socketCliente.close();
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "El socket del cliente ya estaba cerrado", e);
        }
    }

    @Override
    public void run() {
        ObjectOutputStream salida = null;
        ObjectInputStream entrada = null;
        try {
            salida = new ObjectOutputStream(socketCliente.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socketCliente.getInputStream());

            Mensaje msjInicial = (Mensaje) entrada.readObject();

            if (msjInicial == null
                    || msjInicial.getTipo() != Mensaje.Tipo.CONEXION
                    || msjInicial.getRemitente() == null
                    || msjInicial.getRemitente().isBlank()) {
                enviarError(salida, "Solicitud de conexión no válida.");
                return;
            }

            nombreUsuario = msjInicial.getRemitente();

            Chat.ResultadoConexion resultado = chat.registrarCliente(nombreUsuario, salida);
            if (resultado != Chat.ResultadoConexion.ACEPTADA) {
                String motivo = resultado == Chat.ResultadoConexion.LIMITE_ALCANZADO
                        ? "Se ha alcanzado el máximo de conexiones."
                        : "El usuario ya está en uso.";
                enviarError(salida, motivo);
                return;
            }
            registrado = true;

            chat.enviarMensajePublico(new Mensaje(Mensaje.Tipo.MENSAJE_PUBLICO, "Servidor", "Todos",
                    nombreUsuario + " se ha unido al chat."));

            while (true) {
                Mensaje msj = (Mensaje) entrada.readObject();
                if (msj == null) {
                    continue;
                }
                switch (msj.getTipo()) {
                    case DESCONEXION -> {
                        return;
                    }
                    case MENSAJE_PRIVADO ->
                        chat.enviarMensajePrivado(msj);
                    case MENSAJE_PUBLICO ->
                        chat.enviarMensajePublico(msj);
                    default -> {
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.INFO, "Cliente " + (nombreUsuario != null ? nombreUsuario : "desconocido")
                    + " desconectado", e);
        } finally {
            if (registrado && nombreUsuario != null) {
                chat.eliminarCliente(nombreUsuario);
                chat.enviarMensajePublico(new Mensaje(Mensaje.Tipo.MENSAJE_PUBLICO, "Servidor", "Todos",
                        nombreUsuario + " ha abandonado el chat."));
            }
            try {
                if (entrada != null) {
                    entrada.close();
                }
                if (salida != null) {
                    salida.close();
                }
                if (socketCliente != null) {
                    socketCliente.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error cerrando el socket del cliente", e);
            }
        }
    }

    private void enviarError(ObjectOutputStream salida, String contenido) throws IOException {
        salida.writeObject(new Mensaje(Mensaje.Tipo.ERROR, "Servidor", nombreUsuario, contenido));
        salida.flush();
    }
}
