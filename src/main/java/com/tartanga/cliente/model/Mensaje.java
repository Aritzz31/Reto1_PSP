package com.tartanga.cliente.model;

import java.io.Serializable;
import java.util.List;

/**
 * Representa un mensaje intercambiado entre el cliente y el servidor.
 */
public class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identifica la operación o el tipo de contenido del mensaje.
     */
    public enum Tipo {
        CONEXION, DESCONEXION, MENSAJE_PUBLICO, MENSAJE_PRIVADO, LISTA_USUARIOS, ERROR
    }

    private final Tipo tipo;
    private final String remitente;
    private final String destinatario;
    private final String contenido;
    private List<String> usuariosActivos;

    /**
     * Crea un mensaje con su tipo, emisor, destinatario y contenido.
     *
     * @param tipo tipo de mensaje
     * @param remitente usuario que envía el mensaje
     * @param destinatario usuario o grupo destinatario
     * @param contenido texto asociado al mensaje
     */
    public Mensaje(Tipo tipo, String remitente, String destinatario, String contenido) {
        this.tipo = tipo;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.contenido = contenido;
    }

    /**
     * Devuelve el tipo de este mensaje.
     *
     * @return tipo de mensaje
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * Devuelve el nombre del remitente.
     *
     * @return remitente del mensaje
     */
    public String getRemitente() {
        return remitente;
    }

    /**
     * Devuelve el destinatario del mensaje.
     *
     * @return destinatario del mensaje
     */
    public String getDestinatario() {
        return destinatario;
    }

    /**
     * Devuelve el contenido del mensaje.
     *
     * @return contenido del mensaje
     */
    public String getContenido() {
        return contenido;
    }

    /**
     * Devuelve la lista de usuarios activos incluida en el mensaje.
     *
     * @return usuarios activos, o {@code null} si el mensaje no contiene esa lista
     */
    public List<String> getUsuariosActivos() {
        return usuariosActivos;
    }

    /**
     * Establece la lista de usuarios activos asociada al mensaje.
     *
     * @param usuariosActivos usuarios conectados
     */
    public void setUsuariosActivos(List<String> usuariosActivos) {
        this.usuariosActivos = usuariosActivos;
    }
}
