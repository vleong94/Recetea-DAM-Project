/**
 * Definición del Module System (JPMS) de la aplicación.
 * Gestiona las dependencias de librerías externas y los permisos de Reflection
 * requeridos por el framework de UI y la infraestructura de persistencia.
 */
module com.recetea {

    // Dependencias del framework UI
    requires javafx.controls;
    requires javafx.fxml;

    // Dependencias de Infraestructura y Data Access (JDBC & Connection Pool)
    requires java.sql;
    requires java.naming;
    requires org.postgresql.jdbc;
    requires com.zaxxer.hikari;

    // Exposición del Root Package para el entorno de ejecución
    exports com.recetea;

    // Apertura de packages al framework JavaFX.
    // Habilita al FXMLLoader para acceder dinámicamente mediante Reflection a los
    // Controllers, Custom Components y variables inyectadas con la anotación @FXML.
    opens com.recetea.infrastructure.ui.javafx.recipe to javafx.fxml;
    opens com.recetea.infrastructure.ui.javafx.shared to javafx.fxml;
    opens com.recetea.infrastructure.ui.javafx.components to javafx.fxml;
}