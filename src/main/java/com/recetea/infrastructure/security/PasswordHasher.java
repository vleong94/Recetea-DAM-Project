package com.recetea.infrastructure.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;

/**
 * BCrypt-backed implementation of {@link IPasswordEncoder}. Uses the
 * Favre BCrypt library (pure-Java, no native bindings) so the runtime
 * image survives jlink without external libs.
 *
 * <p><b>Cost factor 12.</b> Targets ~250 ms verify-time on the
 * development hardware — slow enough to make brute-force attacks
 * impractical, fast enough that the user-visible login flow doesn't
 * feel laggy. Revisit if hardware budgets shift; the cost is embedded
 * in each generated hash so existing rows remain verifiable after a
 * cost change.
 *
 * <p>Two pairs of methods exist ({@code hash}/{@code verify} and
 * {@code encode}/{@code matches}) — the first pair is legacy from
 * pre-port code, the second satisfies the {@link IPasswordEncoder}
 * contract. They forward to each other; both are safe to call.
 *
 * <p><b>ES — </b>Implementación de {@link IPasswordEncoder} con
 * BCrypt como respaldo. Usa la librería BCrypt de Favre (Java puro,
 * sin bindings nativos) para que la imagen de runtime sobreviva a
 * jlink sin librerías externas.
 *
 * <p><b>Factor de coste 12.</b> Apunta a ~250 ms de tiempo de
 * verificación en el hardware de desarrollo — lo bastante lento
 * para hacer impracticables los ataques de fuerza bruta, lo
 * bastante rápido para que el flujo de login visible al usuario no
 * se sienta lento. Hay que revisarlo si cambian los presupuestos
 * de hardware; el coste se incrusta en cada hash generado para que
 * las filas existentes sigan siendo verificables tras un cambio de
 * coste.
 *
 * <p>Existen dos parejas de métodos
 * ({@code hash}/{@code verify} y {@code encode}/{@code matches}) —
 * la primera pareja es legado del código previo al porte, la
 * segunda satisface el contrato de {@link IPasswordEncoder}. Se
 * delegan entre sí; ambas son seguras de llamar.
 */
public class PasswordHasher implements IPasswordEncoder {

    private static final int COST = 12;

    public String hash(String plainPassword) {
        return encode(plainPassword);
    }

    public boolean verify(String plainPassword, String hashedPassword) {
        return matches(plainPassword, hashedPassword);
    }

    @Override
    public String encode(String plainText) {
        if (plainText == null || plainText.isEmpty())
            throw new IllegalArgumentException("Password must not be empty.");
        return BCrypt.withDefaults().hashToString(COST, plainText.toCharArray());
    }

    @Override
    public boolean matches(String plainText, String encodedText) {
        if (plainText == null || encodedText == null) return false;
        return BCrypt.verifyer().verify(plainText.toCharArray(), encodedText).verified;
    }
}
