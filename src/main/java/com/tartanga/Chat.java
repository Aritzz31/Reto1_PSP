package com.tartanga;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Chat {

    private final ConcurrentHashMap<String, ObjectOutputStream> clientes = new ConcurrentHashMap<>();

    public synchronized boolean agregarCliente(String usuario, ObjectOutputStream out) {
        if (clientes.containsKey(usuario)) {
            return false;
        }
        clientes.put(usuario, out);
        enviarListaUsuariosActivos();
        return true;
    }

    public synchronized void eliminarCliente(String usuario) {
        clientes.remove(usuario);
        enviarListaUsuariosActivos();
    }

    public synchronized void enviarMensajePublico(Mensaje msj) {
        for (ObjectOutputStream out : clientes.values()) {
            enviarPorFlujo(out, msj);
        }
    }

    public synchronized void enviarMensajePrivado(Mensaje msj) {
        ObjectOutputStream outDestino = clientes.get(msj.getDestinatario());
        if (outDestino != null) {
            enviarPorFlujo(outDestino, msj);
        }

        if (!msj.getRemitente().equals(msj.getDestinatario())) {
            ObjectOutputStream outRemitente = clientes.get(msj.getRemitente());
            if (outRemitente != null) {
                enviarPorFlujo(outRemitente, msj);
            }
        }
    }

    private void enviarListaUsuariosActivos() {
        Mensaje msj = new Mensaje(Mensaje.Tipo.LISTA_USUARIOS, "Servidor", "Todos", null);
        msj.setUsuariosActivos(new ArrayList<>(clientes.keySet()));
        enviarMensajePublico(msj);
    }

    private void enviarPorFlujo(ObjectOutputStream out, Mensaje msj) {
        try {
            out.writeObject(msj);
            out.flush();
            out.reset();
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
        }
    }
}
