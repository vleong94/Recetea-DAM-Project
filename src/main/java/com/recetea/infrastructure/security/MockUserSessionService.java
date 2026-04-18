package com.recetea.infrastructure.security;

import com.recetea.core.shared.application.ports.in.IUserSessionService;

/**
 * Adaptador de infraestructura que implementa un mecanismo de sesión simulado.
 * Actúa como un placeholder técnico que retorna una identidad estática, facilitando
 * el desarrollo y las pruebas del sistema antes de la integración del módulo real
 * de autenticación e Identity.
 */
public class MockUserSessionService implements IUserSessionService {

    /**
     * Retorna el identificador de usuario predeterminado para el entorno de desarrollo.
     */
    @Override
    public int getCurrentUserId() {
        return 1;
    }
}