# 🖥️ Informe Técnico de Entorno de Ejecución: Proyecto Recetea

## 1. Definición y Justificación del Entorno (*Environment*)
La infraestructura de **Recetea** se ha diseñado bajo un modelo de **Workstation de Desarrollo** (PC de usuario) para su fase de implementación inicial.

* **Justificación de Arquitectura**: Se opta por este entorno para garantizar la soberanía de los datos y una latencia mínima en el **Throughput** de la **Base de Datos**.
* **Escalabilidad**: Este diseño local sirve como nodo de validación antes de un posible salto a un entorno de contenedores o **Cloud**, alineándose con los objetivos de desarrollo integral de aplicaciones empresariales multiplataforma.

## 2. Especificaciones de Hardware
El hardware seleccionado debe garantizar un **Uptime** constante durante las sesiones de desarrollo y pruebas de carga.

| Componente | Requisitos Mínimos (*Baseline*) | Especificaciones Recomendadas |
| :--- | :--- | :--- |
| **Procesador (CPU)** | 2 Cores @ 2.5 GHz | 4+ Cores @ 3.2 GHz (Intel i5 / Ryzen 5) |
| **Memoria (RAM)** | 8 GB DDR4 | 16 GB DDR4 (Gestión eficiente de la JVM) |
| **Almacenamiento** | 2 GB libres en HDD | 10 GB en SSD NVMe (Optimización de logs) |
| **Interfaz (Monitor)**| Resolución 720p | Resolución 1080p (FHD) |

**Periféricos y Conectividad**: Se requiere teclado extendido y ratón óptico para el flujo de trabajo en el IDE, además de una conexión a internet estable para la resolución de dependencias y sincronización con el repositorio remoto.

## 3. Stack de Software y Sistema Operativo
* **Sistema Operativo**: **Windows 11 Pro** (64 bits). Justificado por su gestión avanzada de servicios y compatibilidad con el ecosistema Java.
* **Lenguaje y Runtime**: **Java 24 (JDK)**. Motor principal de ejecución.
* **Gestor de Base de Datos**: **PostgreSQL 18**. Sistema de persistencia relacional.
* **Control de Versiones (VCS)**: **Git**. Herramienta crítica para el seguimiento de cambios y despliegue.

## 4. Arquitectura Lógica de Componentes
El sistema se organiza en cuatro capas de ejecución concurrentes dentro del mismo entorno:
1.  **Capa de Presentación**: Interfaz de usuario (Consola/GUI) ejecutada sobre la JVM.
2.  **Capa de Aplicación**: Lógica de negocio y controladores de datos.
3.  **Middleware de Conexión**: Controlador JDBC de PostgreSQL para la traducción de peticiones.
4.  **Capa de Persistencia**: Servicio de base de datos PostgreSQL gestionando el almacenamiento físico.

## 5. Guía de Instalación y Provisión (*Setup*)
Para garantizar que el entorno sea replicable, se debe seguir estrictamente este **Workflow**:

1.  **JDK 24**: Instalación y configuración de la variable de sistema `JAVA_HOME`.
2.  **PostgreSQL 18**: Configuración del servidor en el puerto 5432 con codificación regional UTF-8.
3.  **Git**: Instalación del cliente para la gestión del repositorio local.
4.  **JDBC Driver**: Incorporación del binario `.jar` en el **Build Path** del proyecto.
5.  **Variables de Entorno**: Configuración de credenciales de acceso como variables de sistema para evitar el almacenamiento de texto plano en el código.

## 6. Seguridad y *Hardening* del Entorno
Se aplica el principio de **Mínimo Privilegio** para proteger la integridad del sistema:
* **Seguridad de Red**: Configuración del cortafuegos para permitir tráfico local (*Loopback*) exclusivamente al puerto 5432.
* **Gestión de Identidades**: Uso de un usuario de aplicación dedicado con permisos restringidos a operaciones **DML** (Data Manipulation Language).
* **Cifrado**: Las contraseñas de la base de datos se gestionan mediante variables de entorno del sistema operativo, nunca "hardcodeadas" en los archivos fuente.

## 7. Mantenimiento y Disponibilidad
* **Actualizaciones**: Revisión quincenal de parches de seguridad (*Patches*) del JDK.
* **Estrategia de Backup**: Ejecución programada de volcados lógicos mediante la utilidad `pg_dump`.
* **Auditoría**: Revisión mensual de los archivos de **Log** del sistema para identificar posibles cuellos de botella en las consultas SQL.

## 8. Plan de Contingencia (*Troubleshooting*)
| Incidencia | Protocolo de Resolución |
| :--- | :--- |
| **Fallo de conexión JDBC** | Validar estado del servicio en el administrador de tareas y conectividad del puerto 5432. |
| **Error de Memoria (JVM)** | Ajustar los parámetros de inicio (-Xmx / -Xms) en la configuración de ejecución del IDE. |
| **Corrupción de Datos** | Ejecutar un **Restore** total desde el último punto de restauración válido en `/docs/backups/`. |

## 9. Evidencias de Validación
Se requiere la inclusión de las siguientes pruebas en la documentación final:
1.  **Estado del Servicio**: Captura de pantalla del motor de base de datos activo.
2.  **Smoke Test**: Registro de consola que confirme el arranque del sistema y la respuesta positiva del *ping* a la base de datos.