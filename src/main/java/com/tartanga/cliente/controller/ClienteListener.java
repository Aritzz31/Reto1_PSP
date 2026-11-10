package com.tartanga.cliente.controller;

import com.tartanga.cliente.model.Mensaje;

public interface ClienteListener {

    void actualizarUsuarios(Mensaje mensaje);

    void procesarMensaje(Mensaje mensaje);

    void notificarError(String mensaje);

    void notificarDesconexion();
}
