# 🍲 Recetea — Gestión Gastronómica Premium

Bienvenido a **Recetea**, una aplicación multiplataforma diseñada para la gestión integral de recetas de cocina. Este proyecto representa la culminación del primer año de **DAM**, integrando conceptos avanzados de arquitectura de software, persistencia relacional, concurrencia moderna y despliegue nativo.

> **🚀 Nota**: La aplicación se distribuye como un **Portable Bundle**. No requiere instalación de Java ni configuración previa; basta con ejecutar el archivo `.exe` incluido para comenzar la experiencia.

[![Descargar Portable](https://img.shields.io/badge/Descargar-Portable.zip-brightgreen?style=for-the-badge&logo=github)](https://github.com/vleong94/Recetea-DAM-Project/releases/download/v1.0.0_portable/Recetea-Portable.zip)

## 📍 Guía de Evaluación (Índice por Módulos)

Si eres docente de una materia específica, puedes saltar directamente a la documentación y componentes técnicos correspondientes:

| Módulo | Enlace Directo                                 | Puntos Clave a Evaluar |
| :--- |:-----------------------------------------------| :--- |
| **Bases de Datos** | [Consultar BBDD](#-bases-de-datos)             | E/R, Modelo Relacional, SQL DDL/DML, Índices. |
| **Programación / MPO** | [Consultar Código](#-programación-y-mpo)       | Java 24, Hexagonal, Virtual Threads, JDBC, Records. |
| **Sistemas Informáticos** | [Consultar Sistemas](#-sistemas-informáticos)  | Informe Técnico, Provisión, Hardening, Portable Bundle. |
| **Lenguajes de Marcas** | [Consultar XML](#-lenguajes-de-marcas)         | Interoperabilidad XML, Esquema XSD, JAXB. |
| **Entornos de Desarrollo** | [Consultar Entornos](#-entornos-de-desarrollo) | Git/GitHub, Packaging (jlink/jpackage), Moditect. |


## 🏗️ Arquitectura y Tecnologías
La aplicación utiliza una **Arquitectura Hexagonal (Puertos y Adaptadores)** para garantizar que la lógica de negocio permanezca pura y aislada de los detalles técnicos.

*   **Lenguaje**: Java 24 (con `--enable-preview`).
*   **Interfaz**: JavaFX con el sistema de diseño **AtlantaFX (PrimerLight)**.
*   **Persistencia**: PostgreSQL 18 con pool de conexiones **HikariCP**.
*   **Concurrencia**: Uso intensivo de **Virtual Threads** y **ScopedValue** para transacciones atómicas.


## 🗄️ Bases de Datos
El sistema de persistencia ha sido diseñado para maximizar el rendimiento en lecturas de catálogo mediante métricas desnormalizadas.
*   **Análisis**: [Viaje del Dato (Mermaid)](docs/diagramas/01_analisis_datos/01_analisis_datos.svg).
*   **Diagramas**: [Modelo E/R](docs/diagramas/02_diagrama_er/02_diagrama_er.svg) y [Modelo Relacional](docs/diagramas/03_modelo_relacional/03_relational_model.svg).
*   **Scripts**: Estructura (`01_schema.sql`), Semilla (`02_seeding.sql`) y Consultas de administración (`03_queries.sql`).

## 💻 Programación y MPO
El "corazón" de Recetea destaca por la aplicación de patrones de diseño profesionales:
*   **Inmutabilidad**: El agregado principal `Recipe` es un **Java Record** de 14 componentes con validación *fail-fast*.
*   **Integridad**: Gestión de transacciones avanzadas mediante `ITransactionManager` que propaga la conexión JDBC a través de hilos virtuales.
*   **MPO**: Implementación del patrón **Wrapper** para inyectar IDs de correlación (TraceID) en cada operación del usuario.

## 🖥️ Sistemas Informáticos
El despliegue ha sido optimizado para la portabilidad total en entornos de escritorio.
*   **Informe Técnico**: [Acceder al Informe Completo](docs/sistemas/informe_tecnico.md).
*   **Seguridad**: Enmascaramiento automático de credenciales y PII en logs del sistema mediante transformaciones Regex.

## 🏷️ Lenguajes de Marcas
Recetea permite la exportación e importación de recetas mediante un formato estructurado robusto.
*   **Validación**: Esquema [recipe.xsd](src/main/resources/com/recetea/infrastructure/interop/xml/recipe.xsd) que impone restricciones de integridad y tipos de datos.
*   **Integración**: Uso de JAXP con protecciones contra ataques XXE y procesamiento seguro de documentos.

## 🛠️ Entornos de Desarrollo
El ciclo de vida del software se gestiona con herramientas de automatización.
*   **Automatización**: Scripts en PowerShell (`package.ps1`) y Bash (`package.sh`) para la creación del instalador.
*   **Modularización**: Uso de **Moditect** para convertir dependencias *legacy* en módulos JPMS compatibles con la optimización de `jlink`.


## 🚀 Instrucciones de Instalación y Ejecución

### Opción A: Ejecutable Portable (Recomendado)
Para probar la aplicación de inmediato sin instalar Java ni configurar una base de datos local:
1.  Descargue el archivo **`Recetea-Portable.zip`** desde el repositorio.
2.  Descomprima el contenido en una carpeta local.
3.  Lanza el archivo **`Recetea.exe`**. La aplicación se conectará automáticamente a la base de datos remota en **Supabase**.

[![Descargar Portable](https://img.shields.io/badge/Descargar-Portable.zip-brightgreen?style=for-the-badge&logo=github)](https://github.com/vleong94/Recetea-DAM-Project/releases/download/v1.0.0_portable/Recetea-Portable.zip)

### Opción B: Entorno de Desarrollo
Para compilar y ejecutar el proyecto desde el código fuente:
1.  **Requisitos**: JDK 24 (con `--enable-preview`) y Maven 3.9+.
2.  **Base de Datos**: Cree dos bases de datos locales en **PostgreSQL**, una para el entorno de ejecución `recetea` y otra para las pruebas unitarias `recetea_test`.
3.  **Configuración**: Introduzca las credenciales de acceso en los siguientes archivos situados en `src/main/resources/`:
    *   **`application-local.properties`**: Para la base de datos de producción local.
    *   **`application-test.properties`**: Para la ejecución de los tests del sistema.
4.  **Ejecución**: Localice y ejecute la clase **`Main.java`** directamente desde su IDE de preferencia.

---

**Recetea** — Desarrollado por Víctor Roberto León Guerra.