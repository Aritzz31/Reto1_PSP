package com.tartanga;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Chat {

    private final ConcurrentHashMap<String, ObjectOutputStream> clientes = new ConcurrentHashMap<>();
    private final List<Mensaje> historial = new ArrayList<>();

    public synchronized boolean agregarCliente(String usuario, ObjectOutputStream out) {
        if (clientes.putIfAbsent(usuario, out) != null) {
            return false;
        }

        historial.forEach(msj -> enviar(out, msj));
        enviarListaUsuariosActivos();
        return true;
    }

    public synchronized void eliminarCliente(String usuario) {
        clientes.remove(usuario);
        enviarListaUsuariosActivos();
    }

    public synchronized void enviarMensajePublico(Mensaje msj) {
        historial.add(msj);
        difusion(msj);
    }

    public synchronized void enviarMensajePrivado(Mensaje msj) {
        enviar(clientes.get(msj.getDestinatario()), msj);
        if (!msj.getRemitente().equals(msj.getDestinatario())) {
            enviar(clientes.get(msj.getRemitente()), msj);
        }
    }

    private void enviarListaUsuariosActivos() {
        Mensaje msj = new Mensaje(Mensaje.Tipo.LISTA_USUARIOS, "Servidor", "Todos", null);
        msj.setUsuariosActivos(new ArrayList<>(clientes.keySet()));
        difusion(msj);
    }

    private synchronized void difusion(Mensaje msj) {
        clientes.values().forEach(out -> enviar(out, msj));
    }

    private void enviar(ObjectOutputStream out, Mensaje msj) {
        if (out == null) {
            return;
        }
        try {
            out.writeObject(msj);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
        }
    }
}
