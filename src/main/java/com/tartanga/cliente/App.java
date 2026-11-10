package com.tartanga.cliente;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Inicia y configura la ventana principal de la aplicación JavaFX.
 */
public class App extends Application {

    private static Scene scene;

    /**
     * Crea y muestra la ventana principal del chat.
     *
     * @param stage ventana principal proporcionada por JavaFX
     * @throws IOException si no se puede cargar la interfaz FXML
     */
    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("main"), 950, 500);
        stage.setScene(scene);
        stage.setTitle("Chat multiusuario");
        stage.setResizable(false);
        stage.getIcons().add(new Image(App.class.getResourceAsStream("/com/tartanga/icon/favicon.png")));
        stage.show();
    }

    /**
     * Carga una vista FXML del paquete de recursos de la aplicación.
     *
     * @param fxml nombre del archivo FXML sin su extensión
     * @return nodo raíz de la vista cargada
     * @throws IOException si no se puede cargar el archivo FXML
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * Inicia la aplicación JavaFX.
     *
     * @param args argumentos de inicio
     */
    public static void main(String[] args) {
        launch();
    }
}
