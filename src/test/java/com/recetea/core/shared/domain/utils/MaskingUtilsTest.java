package com.recetea.core.shared.domain.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("MaskingUtils — PII masking for safe log output")
class MaskingUtilsTest {

    // -------------------------------------------------------------------------
    // Null / empty
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("null input should return ***")
    void mask_ShouldReturnPlaceholder_WhenInputIsNull() {
        assertEquals("***", MaskingUtils.mask(null));
    }

    @Test
    @DisplayName("empty string should return ***")
    void mask_ShouldReturnPlaceholder_WhenInputIsEmpty() {
        assertEquals("***", MaskingUtils.mask(""));
    }

    // -------------------------------------------------------------------------
    // Email masking
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @DisplayName("Email: should preserve first local-part character and full domain")
    @CsvSource({
            "victor@example.com,   v***@example.com",
            "ab@example.com,       a***@example.com",
            "a@example.com,        a***@example.com",
            "user@sub.domain.org,  u***@sub.domain.org",
            "x@x.io,               x***@x.io",
    })
    void mask_ShouldMaskEmailLocalPart(String input, String expected) {
        assertEquals(expected.strip(), MaskingUtils.mask(input.strip()));
    }

    @Test
    @DisplayName("Email with empty local part should replace local part with ***")
    void mask_ShouldReplaceLocalPart_WhenLocalPartIsEmpty() {
        assertEquals("***@domain.com", MaskingUtils.mask("@domain.com"));
    }

    // -------------------------------------------------------------------------
    // Username masking — length > 3
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @DisplayName("Username longer than 3 chars: should preserve first and last character")
    @CsvSource({
            "victor, v***r",
            "abcd,   a***d",
            "abcde,  a***e",
            "user,   u***r",
    })
    void mask_ShouldPreserveFirstAndLastChar_WhenUsernameLongerThanThree(String input, String expected) {
        assertEquals(expected.strip(), MaskingUtils.mask(input.strip()));
    }

    // -------------------------------------------------------------------------
    // Username masking — length ≤ 3
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "\"{0}\" → ***")
    @DisplayName("Username of 3 chars or fewer should be fully replaced by ***")
    @CsvSource({"abc", "ab", "a"})
    void mask_ShouldFullyReplace_WhenUsernameThreeCharsOrFewer(String input) {
        assertEquals("***", MaskingUtils.mask(input));
    }

    // -------------------------------------------------------------------------
    // JDBC URL masking
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("maskJdbcUrl: null returns ***")
    void maskJdbcUrl_ShouldReturnPlaceholder_WhenNull() {
        assertEquals("***", MaskingUtils.maskJdbcUrl(null));
    }

    @Test
    @DisplayName("maskJdbcUrl: URL without embedded credentials is returned unchanged")
    void maskJdbcUrl_ShouldReturnUnchanged_WhenNoCredentialsEmbedded() {
        String url = "jdbc:postgresql://localhost:5432/recetea";
        assertEquals(url, MaskingUtils.maskJdbcUrl(url));
    }

    @Test
    @DisplayName("maskJdbcUrl: embedded user:pass authority is replaced by ***@")
    void maskJdbcUrl_ShouldStripCredentials_WhenAuthorityPresent() {
        String url = "jdbc:postgresql://admin:s3cr3t@host:5432/recetea";
        assertEquals("jdbc:postgresql://***@host:5432/recetea",
                MaskingUtils.maskJdbcUrl(url));
    }

    @Test
    @DisplayName("maskJdbcUrl: cloud URL with sslmode query param is preserved (no embedded creds to strip)")
    void maskJdbcUrl_ShouldPreserveCloudUrl_WhenSslmodeOnly() {
        String url = "jdbc:postgresql://ep-cool-shape-123.us-east-2.aws.neon.tech:5432/main?sslmode=require&channel_binding=require";
        assertEquals(url, MaskingUtils.maskJdbcUrl(url),
                "Operators need sslmode visible to diagnose cloud connectivity; only credentials should be masked");
    }

    @Test
    @DisplayName("maskJdbcUrl: password= query param value is masked while other params remain")
    void maskJdbcUrl_ShouldMaskPasswordQueryParam() {
        String url = "jdbc:postgresql://host:5432/db?user=app&password=hunter2&sslmode=require";
        String masked = MaskingUtils.maskJdbcUrl(url);
        assertEquals("jdbc:postgresql://host:5432/db?user=app&password=***&sslmode=require", masked);
    }

    @Test
    @DisplayName("maskJdbcUrl: case-insensitive Password= and PWD= query params are masked")
    void maskJdbcUrl_ShouldMaskCaseInsensitivePasswordVariants() {
        // Mixed-case Password=
        assertEquals("jdbc:postgresql://host/db?Password=***&ssl=true",
                MaskingUtils.maskJdbcUrl("jdbc:postgresql://host/db?Password=secret&ssl=true"));
        // Alternate key name PWD= (Oracle / DB2 style)
        assertEquals("jdbc:postgresql://host/db?PWD=***",
                MaskingUtils.maskJdbcUrl("jdbc:postgresql://host/db?PWD=secret"));
    }

    @Test
    @DisplayName("maskJdbcUrl: combines authority + query-string masking on a hybrid URL")
    void maskJdbcUrl_ShouldMaskBothLayersSimultaneously() {
        String url = "jdbc:postgresql://admin:authpass@host:5432/db?password=querypass&sslmode=verify-full";
        String masked = MaskingUtils.maskJdbcUrl(url);
        assertEquals("jdbc:postgresql://***@host:5432/db?password=***&sslmode=verify-full", masked);
    }
}
