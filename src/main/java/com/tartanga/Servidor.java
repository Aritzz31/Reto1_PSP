package com.tartanga;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Servidor {

    public static void main(String[] args) {
        int puerto = 5000;
        Chat chat = new Chat();

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            String ipLocal = "Desconocida";
            try {
                ipLocal = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ex) {
                System.err.println("No se pudo determinar la IP de la máquina local.");
            }

            System.out.println("=========================================");
            System.out.println("       Servidor de Chat Iniciado       ");
            System.out.println("=========================================");
            System.out.println("IP del Servidor : " + ipLocal);
            System.out.println("Puerto          : " + puerto);
            System.out.println("=========================================");
            System.out.println("Esperando clientes...");

            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Nueva conexión recibida desde: " + socketCliente.getInetAddress().getHostAddress());

                HiloCliente hilo = new HiloCliente(socketCliente, chat);
                hilo.start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
}
