package com.tartanga.cliente.view;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.tartanga.cliente.model.Mensaje;

public class Chat {

    private static final Logger LOGGER = Logger.getLogger(Chat.class.getName());
    private final ConcurrentHashMap<String, ObjectOutputStream> clientes = new ConcurrentHashMap<>();
    private final List<Mensaje> historial = new ArrayList<>();
    private final int maxClientes;
    private final long inicio = System.currentTimeMillis();
    private long ultimoMensaje = inicio;

    public Chat() {
        this(10);
    }

    public Chat(int maxClientes) {
        if (maxClientes < 1) {
            throw new IllegalArgumentException("El máximo de clientes debe ser positivo.");
        }
        this.maxClientes = maxClientes;
    }

    public enum ResultadoConexion {
        ACEPTADA, USUARIO_EN_USO, LIMITE_ALCANZADO
    }

    public synchronized ResultadoConexion registrarCliente(String usuario, ObjectOutputStream out) {
        if (clientes.containsKey(usuario)) {
            return ResultadoConexion.USUARIO_EN_USO;
        }
        if (clientes.size() >= maxClientes) {
            return ResultadoConexion.LIMITE_ALCANZADO;
        }
        if (clientes.putIfAbsent(usuario, out) != null) {
            return ResultadoConexion.USUARIO_EN_USO;
        }

        for (Mensaje msj : historial) {
            enviar(out, msj);
        }
        enviarListaUsuariosActivos();
        LOGGER.log(Level.INFO, "Cliente conectado: {0} ({1}/{2})", new Object[]{usuario, clientes.size(), maxClientes});
        return ResultadoConexion.ACEPTADA;
    }

    public synchronized boolean agregarCliente(String usuario, ObjectOutputStream out) {
        return registrarCliente(usuario, out) == ResultadoConexion.ACEPTADA;
    }

    public synchronized void eliminarCliente(String usuario) {
        if (clientes.remove(usuario) != null) {
            LOGGER.log(Level.INFO, "Cliente desconectado: {0} ({1}/{2})", new Object[]{usuario, clientes.size(), maxClientes});
            enviarListaUsuariosActivos();
        }
    }

    public synchronized void enviarMensajePublico(Mensaje msj) {
        historial.add(msj);
        ultimoMensaje = System.currentTimeMillis();
        difusion(msj);
    }

    public synchronized void enviarMensajePrivado(Mensaje msj) {
        ObjectOutputStream destinatario = clientes.get(msj.getDestinatario());
        ObjectOutputStream remitente = clientes.get(msj.getRemitente());
        if (destinatario == null) {
            enviarError(remitente, "El usuario '" + msj.getDestinatario() + "' no está conectado.");
            return;
        }
        ultimoMensaje = System.currentTimeMillis();
        enviar(destinatario, msj);
        if (!msj.getRemitente().equals(msj.getDestinatario())) {
            enviar(remitente, msj);
        }
    }

    private void enviarListaUsuariosActivos() {
        Mensaje msj = new Mensaje(Mensaje.Tipo.LISTA_USUARIOS, "Servidor", "Todos", null);
        msj.setUsuariosActivos(new ArrayList<>(clientes.keySet()));
        difusion(msj);
    }

    private synchronized void difusion(Mensaje msj) {
        for (ObjectOutputStream out : clientes.values()) {
            enviar(out, msj);
        }
    }

    private void enviarError(ObjectOutputStream out, String contenido) {
        enviar(out, new Mensaje(Mensaje.Tipo.ERROR, "Servidor", "Todos", contenido));
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
            LOGGER.log(Level.WARNING, "Error al enviar mensaje al cliente", e);
        }
    }

    public synchronized String obtenerEstadisticas() {
        long activo = (System.currentTimeMillis() - inicio) / 1000;
        String ultimo = ultimoMensaje == inicio ? "ninguno"
                : (System.currentTimeMillis() - ultimoMensaje) / 1000 + " s atrás";
        return "clientes=" + clientes.size() + "/" + maxClientes
                + ", tiempoActivo=" + activo + " s, últimoMensaje=" + ultimo;
    }
}
