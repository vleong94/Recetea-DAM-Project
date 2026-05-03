# 🖥️ Informe Técnico de Entorno de Ejecución: Proyecto Recetea

## 1. Definición y Justificación del Entorno (*Environment*)
La infraestructura de **Recetea** se ha diseñado para ejecutarse bajo un modelo de **Workstation de Usuario** (escritorio), priorizando la fluidez de la interfaz y la autonomía del dato.

*   **Modelo de Distribución**: Se ha implementado un despliegue mediante un **Portable Bundle**. Este formato permite que la aplicación sea autocontenida, integrando su propia máquina virtual optimizada, lo que facilita su ejecución inmediata en cualquier equipo compatible sin instalaciones previas.
*   **Justificación**: Este enfoque garantiza que el software funcione con la versión exacta de la JVM para la que fue desarrollado (Java 24), eliminando conflictos de versiones en el sistema operativo del evaluador.

## 2. Especificaciones de Hardware
El sistema aprovecha la tecnología de **Virtual Threads** de Java 24 para minimizar el consumo de recursos de CPU mientras gestiona múltiples tareas asíncronas.

| Componente | Requisitos Mínimos (*Baseline*) | Especificaciones Recomendadas |
| :--- | :--- | :--- |
| **Procesador (CPU)** | 2 Cores @ 2.0 GHz | 4+ Cores (Arquitectura x64) |
| **Memoria (RAM)** | 4 GB (256MB reservados para la JVM) | 8 GB DDR4 (Gestión de hilos masiva) |
| **Almacenamiento** | 200 MB para el binario portable | 1 GB en SSD (Optimización de caché de imágenes) |
| **Interfaz** | Resolución 1024x768 | 1920x1080 (Full HD) |

## 3. Stack de Software y Componentes Críticos
*   **Sistema Operativo**: Windows 10/11 (64 bits), aprovechando el pipeline gráfico nativo **Prism**.
*   **Runtime**: **Java 24 (Amazon Corretto)** con soporte para características de **Preview**.
*   **Motor de Persistencia**: **PostgreSQL 18**, utilizando el pool de conexiones de alto rendimiento **HikariCP**.
*   **Middleware**: Controlador JDBC de PostgreSQL integrado mediante parche modular.

## 4. Proceso de Creación del Ejecutable (*Packaging*)
Para garantizar la portabilidad absoluta del proyecto, el empaquetado se ha realizado siguiendo estos pasos técnicos:

1.  **Modularización con Moditect**: Debido a que algunas librerías externas (como el driver JDBC o OpenPDF) no son nativamente modulares, he utilizado el plugin **Moditect** para inyectarles descriptores `module-info` en tiempo de compilación.
2.  **Generación de Imagen con jlink**: He creado una **Custom Runtime Image** (JRE reducida) que solo contiene los módulos esenciales del JDK, optimizando el tamaño final a aproximadamente **67 MB**.
3.  **Empaquetado con jpackage**: Finalmente, se ha generado un directorio portable con el ejecutable `Recetea.exe`, el cual lanza la aplicación con los parámetros de acceso nativo y previsualización de Java 24 necesarios para el motor gráfico.

## 5. Guía de Provisión y Configuración (*Setup*)
Para replicar el entorno o ejecutar el binario, el flujo de trabajo es el siguiente:

1.  **Base de Datos**: Instalar PostgreSQL y ejecutar los scripts de la carpeta `/sql` siguiendo este orden correlativo:
    *   `01_schema_definition.sql` (Estructura de tablas).
    *   `02_data_seeding.sql` (Carga de catálogos e ingredientes).
    *   `04_migracion_recipe_media.sql` (Soporte para almacenamiento de archivos).
2.  **Variables de Entorno**: Configurar las credenciales necesarias para que `AppConfig` conecte el sistema:
    *   `DB_URL`, `DB_USER`, `DB_PASSWORD`.
    *   `STORAGE_BASE_PATH` (Ruta para las imágenes de las recetas).
3.  **Lanzamiento**: Ejecutar `Recetea.exe` en la ruta `target/installer/Recetea`.

## 6. Seguridad y Protección de Datos
*   **Hardening de Logs**: Se ha integrado un `SensitiveDataMaskingConverter` en la infraestructura de registro que utiliza **Regex** para identificar y ocultar tokens JWT y hashes de contraseñas automáticamente.
*   **Privacidad de Usuario (PII)**: El sistema implementa utilidades de enmascaramiento que ocultan parcialmente los correos electrónicos y nombres de usuario en las trazas de depuración de la consola.
*   **Aislamiento de Base de Datos**: El usuario de aplicación está restringido exclusivamente a operaciones **DML**, impidiendo modificaciones estructurales accidentales durante el uso normal.

## 7. Mantenimiento y Auditoría
*   **Monitoreo de Operaciones**: El adaptador `LogMetricsAdapter` audita constantemente el tiempo de respuesta de la base de datos, emitiendo una alerta de nivel **WARN** si alguna consulta excede los **500 ms**.
*   **Salud de Hilos**: El sistema incluye soporte para **Java Flight Recorder (JFR)**, permitiendo realizar capturas de rendimiento para detectar posibles bloqueos (*pinning*) en los hilos de plataforma.
*   **Actualización del Software**: El mantenimiento se simplifica mediante el reemplazo atómico de la carpeta de la aplicación portable, sin afectar a la configuración persistente en variables de entorno.

## 8. Plan de Contingencia (*Troubleshooting*)
| Incidencia | Protocolo de Resolución |
| :--- | :--- |
| **Fallo de Conexión JDBC** | Validar que el servicio PostgreSQL está activo y el puerto 5432 habilitado en el cortafuegos. |
| **Error en Visualización de Medios** | Comprobar que la ruta definida en `STORAGE_BASE_PATH` existe y tiene permisos de lectura. |
| **Latencia Excesiva** | Consultar los logs del sistema para identificar la sentencia SQL reportada por el monitor de métricas. |

## 9. Evidencias de Validación
*   **Smoke Test de Persistencia**: Registro de logs confirmando la inicialización del pool HikariCP y la carga exitosa del catálogo de ingredientes.
*   **Test de Concurrencia**: Validación de estabilidad ejecutando 500 transacciones simultáneas sobre hilos virtuales sin degradación de la memoria.