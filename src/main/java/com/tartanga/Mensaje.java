package com.tartanga;

import java.io.Serializable;
import java.util.List;

public class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Tipo {
        CONEXION, DESCONEXION, MENSAJE_PUBLICO, MENSAJE_PRIVADO, LISTA_USUARIOS
    }

    private final Tipo tipo;
    private final String remitente;
    private final String destinatario;
    private final String contenido;
    private List<String> usuariosActivos;

    public Mensaje(Tipo tipo, String remitente, String destinatario, String contenido) {
        this.tipo = tipo;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.contenido = contenido;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getRemitente() {
        return remitente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getContenido() {
        return contenido;
    }

    public List<String> getUsuariosActivos() {
        return usuariosActivos;
    }

    public void setUsuariosActivos(List<String> usuariosActivos) {
        this.usuariosActivos = usuariosActivos;
    }
}
