/**
 * Definición arquitectónica del Module System (JPMS).
 * Establece los Boundaries del sistema aislando las dependencias transitivas
 * y concediendo permisos explícitos de Reflection en tiempo de ejecución.
 */
module com.recetea {

    // Declaración de dependencias del View Framework.
    requires javafx.controls;
    requires javafx.fxml;

    // Declaración de dependencias de la Persistence Layer.
    requires java.sql;
    requires org.postgresql.jdbc;
    requires com.zaxxer.hikari;

    // Declaración de dependencias de la Security Layer.
    requires jbcrypt;

    // Declaración de dependencias de la Interop Layer (XML).
    requires jakarta.xml.bind;

    // Expone el Composition Root para permitir el Bootstrapping de la aplicación por parte de la JVM.
    exports com.recetea;

    // Expone el dominio y puertos del módulo de usuario.
    exports com.recetea.core.user.domain;
    exports com.recetea.core.user.application.ports.out;

    // Expone los puertos del módulo social.
    exports com.recetea.core.social.application.ports.in;
    exports com.recetea.core.social.application.ports.out;

    // Apertura quirúrgica de paquetes para Reflection (Inbound Adapters y UI Components).
    // Obligatorio para que el FXMLLoader instancie y enlace los nodos FXML en los Fields anotados.
    // Los packages declarados aquí deben contener obligatoriamente clases compiladas (.class),
    // de lo contrario el JPMS lanzará un FindException durante el Boot Layer Initialization.
    opens com.recetea.infrastructure.ui.javafx.features.recipe.controllers to javafx.fxml;
    opens com.recetea.infrastructure.ui.javafx.features.recipe.components to javafx.fxml;
    opens com.recetea.infrastructure.ui.javafx.features.identity.controllers to javafx.fxml;

    // JAXB runtime needs reflective access to marshal/unmarshal the XML-DTOs.
    opens com.recetea.infrastructure.interop.xml.dto to jakarta.xml.bind;
}