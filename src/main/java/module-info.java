module com.recetea {
    // Requerimientos de librerías externas
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.postgresql.jdbc;

    // Abrimos el paquete UI a JavaFX para que pueda inyectar los archivos FXML
    //opens com.recetea.infrastructure.ui to javafx.fxml;

    // Exportamos el root para el Main y las vistas
    exports com.recetea;
}