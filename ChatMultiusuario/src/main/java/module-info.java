module com.tartanga.chatmultiusuario {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.tartanga.chatmultiusuario to javafx.fxml;
    exports com.tartanga.chatmultiusuario;
}
