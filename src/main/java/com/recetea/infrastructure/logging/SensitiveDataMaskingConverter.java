package com.recetea.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import java.util.regex.Pattern;

/**
 * Last-line-of-defence Logback converter that scrubs sensitive tokens out of any log
 * message before it reaches an appender. Catches values that slipped past the per-callsite
 * masking helpers ({@code MaskingUtils.mask}, {@code maskJdbcUrl}) — for example a hash
 * accidentally passed as an SLF4J parameter, or a service key embedded in a pre-formatted
 * SQL error string from a JDBC driver.
 *
 * <p>Two patterns are scrubbed:
 * <ul>
 *   <li><b>BCrypt hash</b> — {@code $2[ayb]$<cost>$<22-char salt><31-char hash>} (60 chars total).
 *       Replaced with {@code [MASKED-BCRYPT]}.</li>
 *   <li><b>JWT-shaped token</b> — three base64url-encoded segments separated by dots
 *       (the shape Supabase service-role keys take). Replaced with {@code [MASKED-JWT]}.</li>
 * </ul>
 *
 * <p>Both patterns are precompiled and the {@link Pattern#matcher} chain runs lazily — if the
 * formatted message contains neither sigil ({@code $2} for bcrypt, {@code eyJ} for JWT)
 * the converter returns the message reference unchanged, so the steady-state cost is one
 * {@code String.indexOf} per log line.
 *
 * <p><b>Logback shape</b>: extends {@link CompositeConverter} so the pattern syntax
 * {@code %mask(%msg)} (or {@code %mask(%-30logger - %msg)}, etc.) wraps an arbitrary child
 * pattern and the regex is applied to the rendered child output. Registered as a
 * {@code <conversionRule>} under the keyword {@code mask} in {@code logback.xml} and the
 * test variant.
 *
 * <p>Class is public so Logback can reflectively instantiate it; package is opened to the
 * {@code ch.qos.logback.classic} module via {@code module-info.java}.
 */
public class SensitiveDataMaskingConverter extends CompositeConverter<ILoggingEvent> {

    /**
     * BCrypt hash. Standard format: {@code $2x$<cost>$<53 base64-ish chars>}.
     * - {@code 2x} is one of {@code 2a}, {@code 2b}, {@code 2y} (all bcrypt revisions in the wild).
     * - cost is two decimal digits.
     * - salt+hash payload is 53 characters from BCrypt's modified base64 alphabet
     *   ({@code [./A-Za-z0-9]}); restricting to that exact alphabet avoids false positives on
     *   regular base64 strings that happen to contain {@code +} or {@code =}.
     */
    private static final Pattern BCRYPT_HASH = Pattern.compile(
            "\\$2[ayb]\\$\\d{2}\\$[A-Za-z0-9./]{53}");

    /**
     * JWT compact serialisation: three base64url segments separated by dots.
     * The header almost always begins with {@code eyJ} (the base64url encoding of {@code {"}).
     * Anchoring on that prefix keeps the regex cheap and avoids matching any random
     * dot-separated triple. Length floor of 8 chars per segment rejects implausibly small inputs.
     */
    private static final Pattern JWT_TOKEN = Pattern.compile(
            "eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}");

    private static final String BCRYPT_REPLACEMENT = "[MASKED-BCRYPT]";
    private static final String JWT_REPLACEMENT    = "[MASKED-JWT]";

    /**
     * Applies the regex chain to the already-rendered child output. Cheap guards
     * ({@code String.contains} on the prefix sigils) keep the no-secret path
     * close to a single read.
     */
    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (in == null || in.isEmpty()) return in;

        String result = in;
        if (result.contains("$2")) {
            result = BCRYPT_HASH.matcher(result).replaceAll(BCRYPT_REPLACEMENT);
        }
        if (result.contains("eyJ")) {
            result = JWT_TOKEN.matcher(result).replaceAll(JWT_REPLACEMENT);
        }
        return result;
    }
}
