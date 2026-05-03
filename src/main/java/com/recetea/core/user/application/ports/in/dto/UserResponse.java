package com.recetea.core.user.application.ports.in.dto;

import com.recetea.core.user.domain.UserId;

/**
 * Public projection of a user — never includes the password hash. Returned by
 * {@code ILoginUseCase} on success and by {@code IRegisterUserUseCase} after
 * the new account is persisted; the recipe + social use cases reference users
 * by id only and never need this DTO.
 *
 * <p>The {@code email} is included here because the post-login UI may want
 * to greet the user or surface the account address; the recipe-side flows
 * deliberately do <em>not</em> see emails (privacy: the dashboard's author
 * filter resolves usernames, never emails).
 *
 * <p><b>ES — </b>Proyección pública de un usuario — nunca incluye el hash
 * de contraseña. La devuelven {@code ILoginUseCase} en caso de éxito y
 * {@code IRegisterUserUseCase} después de persistir la cuenta nueva; los
 * casos de uso de receta y social referencian a los usuarios sólo por id
 * y nunca necesitan este DTO.
 *
 * <p>El {@code email} se incluye aquí porque la UI post-login puede
 * querer saludar al usuario o mostrar la dirección de la cuenta; los
 * flujos del lado de receta deliberadamente <em>no</em> ven emails
 * (privacidad: el filtro de autor del dashboard resuelve usernames,
 * nunca emails).
 */
public record UserResponse(UserId id, String username, String email) {}
