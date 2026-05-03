// JPMS module descriptor for Recetea. Sections appear in this order:
//   1. requires        — every module the application reads directly,
//                        plus a few that ServiceLoader / jlink need surfaced.
//   2. exports         — only what the composition root + cross-domain
//                        UserId need; infrastructure stays hidden.
//   3. opens           — targeted reflection grants for JAXB, FXMLLoader,
//                        Logback's conversionRule loader, and ResourceBundle.
//   4. uses / provides — SPI plumbing for the report and password-encoder
//                        ports (Main.start resolves both via ServiceLoader).
//
// Most entries carry inline comments explaining why they're shaped the way
// they are — keep the per-line rationale current when editing this file.
//
// ES — Descriptor de módulo JPMS para Recetea. Las secciones aparecen
// en este orden:
//   1. requires        — cada módulo que la aplicación lee directamente,
//                        más algunos que ServiceLoader / jlink necesitan
//                        que estén expuestos.
//   2. exports         — sólo lo que necesitan la composition root y
//                        UserId (cruzado entre dominios); la
//                        infraestructura queda oculta.
//   3. opens           — concesiones de reflexión dirigidas a JAXB,
//                        FXMLLoader, el loader de conversionRule de
//                        Logback y ResourceBundle.
//   4. uses / provides — fontanería SPI para los puertos de informes
//                        y password-encoder (Main.start los resuelve
//                        a ambos vía ServiceLoader).
//
// La mayoría de entradas llevan comentarios inline explicando por qué
// tienen la forma que tienen — mantener actualizada la justificación
// línea-a-línea al editar este archivo.
module com.recetea {

    // --- requires (alphabetical) ---
    requires ch.qos.logback.classic;
    // logback.core is needed for the SensitiveDataMaskingConverter type chain:
    // ClassicConverter (logback-classic) extends DynamicConverter (logback-core).
    // Without this requires, Logback's reflective loading of conversionRule classes
    // fails at class-init time and the pattern collapses to %PARSER_ERROR.
    requires ch.qos.logback.core;
    requires com.github.librepdf.openpdf;
    requires com.zaxxer.hikari;
    requires jakarta.xml.bind;
    // Glassfish JAXB Runtime. The legacy javax.xml.bind module was named
    // 'com.sun.xml.bind'; the Jakarta-namespace 4.x line renames it to
    // 'org.glassfish.jaxb.runtime'. The application never references these
    // classes directly (JAXBContext.newInstance discovers them via ServiceLoader),
    // but jlink only walks the explicit `requires` graph — without this line
    // the implementation is dropped and startup fails with "Implementation of
    // Jakarta XML Binding-API has not been found on module path".
    requires org.glassfish.jaxb.runtime;
    requires java.desktop;
    requires java.sql;
    // jdk.jfr surfaces the Java Flight Recorder programmatic API (Recording,
    // RecordingFile, EventStream). Used by VirtualThreadPinningTest to capture
    // VirtualThreadPinned events during the 500-task stress phase. Marked
    // 'static' so it's a compile-time-only requires — production startup stays
    // free of JFR overhead and jlink can omit the jdk.jfr module from the
    // packaged runtime image. Surefire's argLine adds --add-modules jdk.jfr
    // so test execution still resolves the module.
    requires static jdk.jfr;
    requires javafx.controls;
    requires javafx.fxml;
    // javafx.graphics is transitively required by javafx.controls; declaring it
    // explicitly makes the link-time module set unambiguous for jlink and surfaces
    // the dependency in --list-modules output of the generated runtime image.
    requires javafx.graphics;
    requires atlantafx.base;
    requires at.favre.lib.bcrypt;
    // OkHttp 5.x ships its own MR-jar module-info under META-INF/versions/9/
    // (module name: okhttp3), so it is a real named module — no Moditect patch
    // needed on the okhttp jar itself. Its transitive okio dependency IS patched
    // by Moditect (see pom.xml `patch-okio` execution) because okio-jvm-3.16.4
    // ships only Automatic-Module-Name=okio. kotlin-stdlib 2.x is already a real
    // module. Both okio + kotlin.stdlib reach this module via okhttp3's transitive
    // requires, so no explicit `requires okio` is needed here.
    requires okhttp3;
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
    opens com.recetea.infrastructure.interop.xml.dto to jakarta.xml.bind;
    // FXMLLoader needs deep reflection to instantiate controllers and inject @FXML fields.
    opens com.recetea.infrastructure.ui.javafx.features.identity.controllers to javafx.fxml;
    opens com.recetea.infrastructure.ui.javafx.features.recipe.components      to javafx.fxml;
    opens com.recetea.infrastructure.ui.javafx.features.recipe.controllers     to javafx.fxml;
    opens com.recetea.infrastructure.ui.javafx.shared.components               to javafx.fxml;
    // Logback resolves <conversionRule> classes via reflection. The actual instantiation
    // happens inside ch.qos.logback.core (Joran parses the rule, PatternLayoutBase.start()
    // calls the no-arg constructor); ch.qos.logback.classic only orchestrates. Both targets
    // are listed explicitly so neither module's reflection path can be blocked. Without
    // ch.qos.logback.core in this list the runtime fails with IllegalAccessException as
    // soon as Joran reaches the <conversionRule> element. Targeted opens (not unconditional)
    // keeps the package hidden from every other module.
    opens com.recetea.infrastructure.logging                                  to ch.qos.logback.classic, ch.qos.logback.core;
    // ResourceBundle in a named module needs the bundle's package open to the
    // resource-bundle loader. Unconditional open: java.base reads the .properties.
    opens i18n;

    // --- Service consumption (uses) ---
    // Main.start() loads each via ServiceLoader.findFirst().orElseThrow(...). The media
    // storage port is wired explicitly via MediaStorageFactory (which routes between
    // local-fs and Supabase based on STORAGE_BASE_PATH shape) and is NOT discovered via SPI.
    uses com.recetea.core.recipe.application.ports.out.report.IRecipeReportPort;
    uses com.recetea.core.user.application.ports.out.IPasswordEncoder;

    // --- Service provision (provides ... with ...) ---
    // Concrete adapters discovered by ServiceLoader. Each requires a public no-arg constructor.
    provides com.recetea.core.recipe.application.ports.out.report.IRecipeReportPort
            with com.recetea.infrastructure.reports.openpdf.OpenPdfRecipeAdapter;
    provides com.recetea.core.user.application.ports.out.IPasswordEncoder
            with com.recetea.infrastructure.security.PasswordHasher;
}
