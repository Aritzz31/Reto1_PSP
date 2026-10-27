package com.tartanga;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HiloCliente extends Thread {

    private final Socket socketCliente;
    private final Chat chat;
    private String nombreUsuario;

    public HiloCliente(Socket socketCliente, Chat chat) {
        this.socketCliente = socketCliente;
        this.chat = chat;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream salida = new ObjectOutputStream(socketCliente.getOutputStream());
            salida.flush();
            ObjectInputStream entrada = new ObjectInputStream(socketCliente.getInputStream());

            Mensaje msjInicial = (Mensaje) entrada.readObject();

            if (msjInicial.getTipo() != Mensaje.Tipo.CONEXION) {
                return;
            }

            nombreUsuario = msjInicial.getRemitente();

            if (!chat.agregarCliente(nombreUsuario, salida)) {
                salida.writeObject(new Mensaje(Mensaje.Tipo.DESCONEXION, "Servidor", nombreUsuario, "El usuario ya está en uso."));
                salida.flush();
                return;
            }

            chat.enviarMensajePublico(new Mensaje(Mensaje.Tipo.MENSAJE_PUBLICO, "Servidor", "Todos", nombreUsuario + " se ha unido al chat."));

            while (true) {
                Mensaje msj = (Mensaje) entrada.readObject();
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
            System.out.println("Cliente " + (nombreUsuario != null ? nombreUsuario : "desconocido") + " desconectado.");
        } finally {
            if (nombreUsuario != null) {
                chat.eliminarCliente(nombreUsuario);
                chat.enviarMensajePublico(new Mensaje(Mensaje.Tipo.MENSAJE_PUBLICO, "Servidor", "Todos", nombreUsuario + " ha abandonado el chat."));
            }
            try {
                if (socketCliente != null) {
                    socketCliente.close();
                }
            } catch (IOException e) {
                System.out.println("Error cerrando socket: " + e.getMessage());
            }
        }
    }
}
