package com.tartanga;

public interface ClienteListener {

    void actualizarUsuarios(Mensaje mensaje);

    void procesarMensaje(Mensaje mensaje);

    void notificarError(String mensaje);

    void notificarDesconexion();
}
