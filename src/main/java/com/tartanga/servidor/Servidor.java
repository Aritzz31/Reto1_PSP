package com.tartanga.servidor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.Collections;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.LogManager;

import com.tartanga.cliente.model.*;
import com.tartanga.cliente.view.Chat;

/**
 * Inicia el servidor de chat y acepta conexiones de clientes.
 */
public class Servidor {

    private static final Logger LOGGER = Logger.getLogger(Servidor.class.getName());
    private static final int MAX_CLIENTES = 10;
    private static final long INTERVALO_MONITOR_MS = 60_000;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Configura el chat, inicia el monitor y atiende las conexiones entrantes.
     *
     * @param args argumentos de inicio (no utilizados)
     */
    public static void main(String[] args) {
        configurarLogs();
        int puerto = 5000;
        Chat chat = new Chat(MAX_CLIENTES);
        Set<HiloCliente> clientes = Collections.synchronizedSet(new java.util.HashSet<>());

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            Thread monitor = crearMonitor(chat);
            monitor.start();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                /**
                 * Detiene el monitor y cierra las conexiones durante el apagado.
                 */
                @Override
                public void run() {
                    monitor.interrupt();
                    synchronized (clientes) {
                        for (HiloCliente cliente : clientes) {
                            cliente.detener();
                        }
                    }
                    LOGGER.info("Servidor detenido correctamente.");
                }
            }, "apagado-servidor"));
            String ipServidor = "Desconocida";
            try {
                ipServidor = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ex) {
                System.err.println("No se pudo determinar la IP de la máquina local.");
            }

            System.out.println("=========================================");
            System.out.println("       Servidor de Chat Iniciado       ");
            System.out.println("=========================================");
            System.out.println("IP del Servidor : " + ipServidor);
            System.out.println("Puerto          : " + puerto);
            System.out.println("=========================================");
            System.out.println("Esperando clientes...");

            while (true) {
                Socket socketCliente = serverSocket.accept();
                LOGGER.log(Level.INFO, "Nueva conexi\u00f3n recibida desde: {0}",
                        socketCliente.getInetAddress().getHostAddress());

                HiloCliente hilo = new HiloCliente(socketCliente, chat);
                clientes.add(hilo);
                hilo.start();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error en el servidor: {0}", e.getMessage());
        }
    }

    /**
     * Crea un hilo que registra periódicamente las estadísticas del chat.
     *
     * @param chat gestor del chat cuyas estadísticas se consultan
     * @return hilo monitor del servidor
     */
    private static Thread crearMonitor(Chat chat) {
        return new Thread(new Runnable() {
            /**
             * Registra el estado del chat hasta que se interrumpe el hilo.
             */
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(INTERVALO_MONITOR_MS);
                        LOGGER.log(Level.INFO, "Estado del servidor: {0}", chat.obtenerEstadisticas());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, "monitor-servidor");
    }

    /**
     * Configura el formato y los destinos de los registros del servidor.
     */
    private static void configurarLogs() {
        Logger loggerRaiz = LogManager.getLogManager().getLogger("");
        Formatter formatter = new Formatter() {
            /**
             * Formatea un registro con fecha, nivel, clase y mensaje.
             *
             * @param registro registro que se va a formatear
             * @return línea de registro formateada
             */
            @Override
            public String format(LogRecord registro) {
                String clase = registro.getLoggerName();
                int ultimaSeparacion;
                if (clase == null) {
                    ultimaSeparacion = -1;
                } else {
                    ultimaSeparacion = clase.lastIndexOf('.');
                }

                if (ultimaSeparacion >= 0) {
                    clase = clase.substring(ultimaSeparacion + 1);
                }
                return String.format("[%s] [%s] [%s] %s%n",
                        LocalDateTime.now().format(FORMATO_FECHA),
                        registro.getLevel(),
                        clase == null ? "Aplicación" : clase,
                        formatMessage(registro));
            }
        };

        for (Handler handler : loggerRaiz.getHandlers()) {
            handler.setFormatter(formatter);
            handler.setLevel(Level.ALL);
        }
        try {
            FileHandler fileHandler = new FileHandler("servidor.log", true);
            fileHandler.setFormatter(formatter);
            fileHandler.setLevel(Level.ALL);
            loggerRaiz.addHandler(fileHandler);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el archivo servidor.log.", e);
        }
        loggerRaiz.setLevel(Level.INFO);
    }
}
