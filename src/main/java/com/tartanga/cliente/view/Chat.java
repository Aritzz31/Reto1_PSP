package com.tartanga.cliente.view;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.tartanga.cliente.model.Mensaje;

/**
 * Gestiona los clientes conectados, el historial y la distribución de mensajes.
 */
public class Chat {

    private static final Logger LOGGER = Logger.getLogger(Chat.class.getName());
    private final ConcurrentHashMap<String, ObjectOutputStream> clientes = new ConcurrentHashMap<>();
    private final List<Mensaje> historial = new ArrayList<>();
    private final int maxClientes;
    private final long inicio = System.currentTimeMillis();
    private long ultimoMensaje = inicio;

    /**
     * Crea un chat con el límite predeterminado de diez clientes.
     */
    public Chat() {
        this(10);
    }

    /**
     * Crea un chat con un límite máximo de clientes.
     *
     * @param maxClientes número máximo de conexiones simultáneas; debe ser positivo
     * @throws IllegalArgumentException si el límite no es positivo
     */
    public Chat(int maxClientes) {
        if (maxClientes < 1) {
            throw new IllegalArgumentException("El máximo de clientes debe ser positivo.");
        }
        this.maxClientes = maxClientes;
    }

    /**
     * Indica si el registro de un usuario se aceptó o por qué se rechazó.
     */
    public enum ResultadoConexion {
        ACEPTADA, USUARIO_EN_USO, LIMITE_ALCANZADO
    }

    /**
     * Registra un cliente y le envía el historial y la lista de usuarios activos.
     *
     * @param usuario nombre del usuario que se conecta
     * @param out flujo de salida del cliente
     * @return resultado del intento de registro
     */
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
        LOGGER.log(Level.INFO, "Cliente conectado: {0} ({1}/{2})",
                new Object[] { usuario, clientes.size(), maxClientes });
        return ResultadoConexion.ACEPTADA;
    }

    /**
     * Registra un cliente e indica si la operación tuvo éxito.
     *
     * @param usuario nombre del usuario que se conecta
     * @param out flujo de salida del cliente
     * @return {@code true} si el cliente se registró
     */
    public synchronized boolean agregarCliente(String usuario, ObjectOutputStream out) {
        return registrarCliente(usuario, out) == ResultadoConexion.ACEPTADA;
    }

    /**
     * Elimina un cliente y comunica la nueva lista de usuarios conectados.
     *
     * @param usuario nombre del usuario que se desconecta
     */
    public synchronized void eliminarCliente(String usuario) {
        if (clientes.remove(usuario) != null) {
            LOGGER.log(Level.INFO, "Cliente desconectado: {0} ({1}/{2})",
                    new Object[] { usuario, clientes.size(), maxClientes });
            enviarListaUsuariosActivos();
        }
    }

    /**
     * Guarda un mensaje público en el historial y lo envía a todos los clientes.
     *
     * @param msj mensaje público que se distribuirá
     */
    public synchronized void enviarMensajePublico(Mensaje msj) {
        historial.add(msj);
        ultimoMensaje = System.currentTimeMillis();
        difusion(msj);
    }

    /**
     * Envía un mensaje privado a su destinatario y confirma al remitente.
     *
     * @param msj mensaje privado que se distribuirá
     */
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

    /**
     * Construye y difunde la lista actual de usuarios conectados.
     */
    private void enviarListaUsuariosActivos() {
        Mensaje msj = new Mensaje(Mensaje.Tipo.LISTA_USUARIOS, "Servidor", "Todos", null);
        msj.setUsuariosActivos(new ArrayList<>(clientes.keySet()));
        difusion(msj);
    }

    /**
     * Envía un mensaje a todos los clientes registrados.
     *
     * @param msj mensaje que se difundirá
     */
    private synchronized void difusion(Mensaje msj) {
        for (ObjectOutputStream out : clientes.values()) {
            enviar(out, msj);
        }
    }

    /**
     * Envía un mensaje de error al flujo indicado.
     *
     * @param out flujo de salida del cliente
     * @param contenido descripción del error
     */
    private void enviarError(ObjectOutputStream out, String contenido) {
        enviar(out, new Mensaje(Mensaje.Tipo.ERROR, "Servidor", "Todos", contenido));
    }

    /**
     * Serializa y envía un mensaje por el flujo indicado.
     *
     * @param out flujo de salida del cliente
     * @param msj mensaje que se enviará
     */
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

    /**
     * Genera un resumen del número de clientes y la actividad del servidor.
     *
     * @return estadísticas actuales del chat
     */
    public synchronized String obtenerEstadisticas() {
        long activo = (System.currentTimeMillis() - inicio) / 1000;
        String ultimo;
        if (ultimoMensaje == inicio) {
            ultimo = "ninguno";
        } else {
            long segundosDesdeUltimo = (System.currentTimeMillis() - ultimoMensaje) / 1000;
            ultimo = segundosDesdeUltimo + " s atrás";
        }

        return "clientes=" + clientes.size() + "/" + maxClientes
                + ", tiempoActivo=" + activo + " s, últimoMensaje=" + ultimo;
    }
}
