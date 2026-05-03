package com.recetea.core.shared.domain.utils;

/**
 * Utility class for masking Personally Identifiable Information (PII) in logs.
 *
 * <p>All methods are stateless and null-safe. The class is not instantiable.
 */
public final class MaskingUtils {

    private MaskingUtils() {}

    /**
     * Masks a login identifier (email or username) for safe inclusion in log output.
     *
     * <ul>
     *   <li><b>Email</b> (contains {@code @}): preserves the first character of the local part
     *       and the full domain — e.g. {@code victor@example.com} → {@code v***@example.com}.
     *       An empty local part becomes {@code ***@domain}.</li>
     *   <li><b>Username, length &gt; 3</b>: preserves the first and last characters —
     *       e.g. {@code victor} → {@code v***r}.</li>
     *   <li><b>Username, length ≤ 3</b>: fully replaced by {@code ***} to avoid leaking
     *       short identifiers whose first/last characters expose the full value.</li>
     * </ul>
     *
     * @param identifier the raw username or email address; {@code null} returns {@code "***"}.
     * @return the masked identifier, never {@code null}.
     */
    public static String mask(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "***";
        }
        int atIndex = identifier.indexOf('@');
        if (atIndex >= 0) {
            String localPart = identifier.substring(0, atIndex);
            String domain = identifier.substring(atIndex); // includes '@'
            if (localPart.isEmpty()) {
                return "***" + domain;
            }
            return localPart.charAt(0) + "***" + domain;
        }
        if (identifier.length() <= 3) {
            return "***";
        }
        return identifier.charAt(0) + "***" + identifier.charAt(identifier.length() - 1);
    }

    /**
     * Strips embedded credentials from a JDBC connection string before logging.
     * Two layers of defense for cloud-provider URLs (Neon, Supabase, AWS RDS):
     *
     * <ul>
     *   <li><b>Authority-form credentials</b>:
     *       {@code jdbc:proto://user:pass@host/db} → {@code jdbc:proto://***@host/db}</li>
     *   <li><b>Query-string passwords</b>:
     *       {@code ...?password=secret&sslmode=require} → {@code ...?password=***&sslmode=require}.
     *       Some cloud providers expose credentials this way instead of the authority.</li>
     * </ul>
     *
     * Non-credential query parameters (e.g. {@code sslmode}, {@code channel_binding},
     * {@code sslrootcert}) are preserved verbatim — operators need them visible to
     * diagnose connectivity issues.
     *
     * @param url the raw JDBC URL; {@code null} or empty returns {@code "***"}.
     */
    public static String maskJdbcUrl(String url) {
        if (url == null || url.isEmpty()) return "***";
        String authorityMasked = url.replaceAll("(jdbc:[^:]+://)[^@/]+@", "$1***@");
        // Mask any password= query param value, regardless of position. Case-insensitive
        // because some drivers accept "Password=" or "PWD=".
        return authorityMasked.replaceAll("(?i)([?&])(password|pwd)=[^&]*", "$1$2=***");
    }
}
