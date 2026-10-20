module com.tartanga {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.tartanga to javafx.fxml;
    exports com.tartanga;
}
