module com.tartanga.cliente {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;

    opens com.tartanga.cliente.controller to javafx.fxml;
    exports com.tartanga.cliente;
}