package com.tartanga.cliente.controller;

import com.tartanga.cliente.model.Mensaje;

/**
 * Define las notificaciones que el cliente envía al controlador de la interfaz.
 */
public interface ClienteListener {

    /**
     * Actualiza la interfaz con la lista de usuarios activos recibida.
     *
     * @param mensaje mensaje que contiene los usuarios conectados
     */
    void actualizarUsuarios(Mensaje mensaje);

    /**
     * Entrega a la interfaz un mensaje recibido del servidor.
     *
     * @param mensaje mensaje recibido
     */
    void procesarMensaje(Mensaje mensaje);

    /**
     * Notifica un error que debe mostrarse al usuario.
     *
     * @param mensaje descripción del error
     */
    void notificarError(String mensaje);

    /**
     * Notifica que la conexión con el servidor se ha cerrado.
     */
    void notificarDesconexion();
}
