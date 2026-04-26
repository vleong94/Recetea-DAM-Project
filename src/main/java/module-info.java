module com.recetea {

    // --- requires (alphabetical) ---
    requires ch.qos.logback.classic;
    requires com.github.librepdf.openpdf;
    requires com.zaxxer.hikari;
    requires jakarta.xml.bind;
    requires java.desktop;
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires jbcrypt;
    requires org.json;
    requires org.postgresql.jdbc;
    requires org.slf4j;

    // --- exports (alphabetical) ---
    // Only the composition root and UserId, which is a cross-cutting domain type
    // referenced from recipe domain, application DTOs, and session ports.
    exports com.recetea;
    exports com.recetea.core.user.domain;

    // --- opens (alphabetical) ---
    // JAXB runtime needs deep reflection to marshal/unmarshal XML DTOs.
    opens com.recetea.core.recipe.application.ports.out.interop.dto to jakarta.xml.bind;
    // FXMLLoader needs deep reflection to instantiate controllers and inject @FXML fields.
    opens com.recetea.infrastructure.ui.javafx.features.identity.controllers to javafx.fxml;
    opens com.recetea.infrastructure.ui.javafx.features.recipe.components      to javafx.fxml;
    opens com.recetea.infrastructure.ui.javafx.features.recipe.controllers     to javafx.fxml;
}
