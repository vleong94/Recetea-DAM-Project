package com.recetea.core.shared.application.ports.in;

/**
 * Puerto de entrada que define el contrato para la gestión de la sesión de usuario activa.
 * Provee una abstracción para recuperar la identidad del sujeto autenticado, permitiendo
 * que los casos de uso y controladores permanezcan desacoplados de la implementación
 * específica del sistema de seguridad (Identity).
 */
public interface IUserSessionService {

    /**
     * Recupera el identificador único del usuario que mantiene la sesión activa.
     * @return El ID numérico del usuario autenticado.
     */
    int getCurrentUserId();
}