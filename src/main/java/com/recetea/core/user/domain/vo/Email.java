package com.recetea.core.user.domain.vo;

import java.util.regex.Pattern;

/**
 * Validated email address. The compact constructor enforces the format at
 * construction time so an invalid string cannot circulate inside the domain
 * — DB row hydration also goes through this constructor, which is acceptable
 * because seeded data is controlled.
 *
 * <p><b>{@link #toString()} masks the local-part</b> ("{@code ***@host.tld}")
 * to keep PII out of log lines that interpolate the record. The compact
 * constructor's regex guarantees a non-empty local-part and a literal {@code @},
 * so the substring lookup is safe.
 *
 * <p>The regex is intentionally pragmatic — it rejects obvious typos but
 * does not implement RFC 5322 in full. Final delivery is the only authority
 * on whether an address actually works; structural validation here is just
 * a fast-fail signal for the registration form.
 *
 * <p><b>ES — </b>Dirección de correo validada. El constructor compacto
 * aplica el formato en tiempo de construcción para que una cadena inválida
 * no pueda circular dentro del dominio — la hidratación de filas desde la
 * BD también pasa por este constructor, lo cual es aceptable porque los
 * datos semilla están controlados.
 *
 * <p><b>{@link #toString()} enmascara la parte local</b>
 * ("{@code ***@host.tld}") para mantener los datos personales (PII) fuera
 * de las líneas de log que interpolen el record. El regex del constructor
 * compacto garantiza una parte local no vacía y un {@code @} literal, así
 * que el substring es seguro.
 *
 * <p>El regex es deliberadamente pragmático — rechaza tipeos obvios pero
 * no implementa la RFC 5322 al completo. La entrega real es la única
 * autoridad sobre si una dirección funciona; la validación estructural
 * aquí es solo una señal de fallo rápido para el formulario de registro.
 */
public record Email(String value) {

    public static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    public Email {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Email is required.");
        if (!EMAIL_PATTERN.matcher(value.trim()).matches())
            throw new IllegalArgumentException("Invalid email format: " + value);
    }

    /**
     * Shows only the domain part to prevent PII leakage in logs.
     * The compact constructor's regex guarantees a non-empty local-part and a
     * literal '@', so {@code value.indexOf('@')} is always positive here.
     */
    @Override
    public String toString() {
        return "Email[value=***" + value.substring(value.indexOf('@')) + "]";
    }
}
