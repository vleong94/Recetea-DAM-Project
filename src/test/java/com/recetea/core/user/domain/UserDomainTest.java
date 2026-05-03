package com.recetea.core.user.domain;

import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserDomainTest {

    private static final String VALID_HASH = "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    @DisplayName("Should create a valid user with all fields")
    void shouldCreateValidUser() {
        User user = new User(
                new UserId(1),
                new Username("victor"),
                new Email("victor@example.com"),
                new PasswordHash(VALID_HASH)
        );

        assertEquals(1, user.id().value());
        assertEquals("victor", user.username().value());
        assertEquals("victor@example.com", user.email().value());
        assertEquals(VALID_HASH, user.passwordHash().value());
    }

    @Test
    @DisplayName("Should reject non-positive UserId")
    void shouldRejectNonPositiveUserId() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(0));
        assertThrows(IllegalArgumentException.class, () -> new UserId(-1));
    }

    @Test
    @DisplayName("Should reject blank or null username")
    void shouldRejectBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> new Username(null));
        assertThrows(IllegalArgumentException.class, () -> new Username("  "));
    }

    @Test
    @DisplayName("Should reject username shorter than three characters")
    void shouldRejectUsernameShorterThanThreeChars() {
        assertThrows(IllegalArgumentException.class, () -> new Username("ab"));
        assertThrows(IllegalArgumentException.class, () -> new Username("x"));
    }

    @Test
    @DisplayName("Should reject blank or null email")
    void shouldRejectBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
        assertThrows(IllegalArgumentException.class, () -> new Email(""));
    }

    @Test
    @DisplayName("Should reject invalid email formats")
    void shouldRejectInvalidEmailFormat() {
        assertThrows(IllegalArgumentException.class, () -> new Email("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> new Email("missing@tld"));
        assertThrows(IllegalArgumentException.class, () -> new Email("@nodomain.com"));
    }

    @Test
    @DisplayName("Should reject blank or null password hash")
    void shouldRejectBlankPasswordHash() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash(null));
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash("  "));
    }

    @Test
    @DisplayName("Should reject non-BCrypt password hashes")
    void shouldRejectNonBcryptPasswordHash() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash("plaintext_password"));
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash("md5hashvalue"));
    }

    // -------------------------------------------------------------------
    // User record compact-constructor coverage (kills mutations on the
    // requireNonNull guards, the id-nullable design, the create-path
    // delegation, and the credential-masking toString override).
    // -------------------------------------------------------------------

    @Test
    @DisplayName("User: rejects null username with NullPointerException")
    void userRejectsNullUsername() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new User(new UserId(1), null,
                        new Email("a@b.com"), new PasswordHash(VALID_HASH)));
        assertEquals("username is required.", ex.getMessage());
    }

    @Test
    @DisplayName("User: rejects null email with NullPointerException")
    void userRejectsNullEmail() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new User(new UserId(1), new Username("victor"),
                        null, new PasswordHash(VALID_HASH)));
        assertEquals("email is required.", ex.getMessage());
    }

    @Test
    @DisplayName("User: rejects null passwordHash with NullPointerException")
    void userRejectsNullPasswordHash() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new User(new UserId(1), new Username("victor"),
                        new Email("a@b.com"), null));
        assertEquals("passwordHash is required.", ex.getMessage());
    }

    @Test
    @DisplayName("User: id is intentionally nullable to support the create-path before persistence")
    void userAllowsNullIdOnCreatePath() {
        User user = new User(null,
                new Username("victor"),
                new Email("victor@example.com"),
                new PasswordHash(VALID_HASH));
        assertNull(user.id());
    }

    @Test
    @DisplayName("User: 3-arg create-path constructor delegates with null id")
    void userThreeArgCreatePathConstructor() {
        User user = new User(
                new Username("victor"),
                new Email("victor@example.com"),
                new PasswordHash(VALID_HASH));
        assertNull(user.id());
        assertEquals("victor", user.username().value());
        assertEquals("victor@example.com", user.email().value());
        assertEquals(VALID_HASH, user.passwordHash().value());
    }

    @Test
    @DisplayName("User: equals + hashCode honour value-semantics across all components")
    void userEqualsAndHashCode() {
        User a = new User(new UserId(1),
                new Username("victor"),
                new Email("a@b.com"),
                new PasswordHash(VALID_HASH));
        User b = new User(new UserId(1),
                new Username("victor"),
                new Email("a@b.com"),
                new PasswordHash(VALID_HASH));
        User differentId = new User(new UserId(2),
                new Username("victor"),
                new Email("a@b.com"),
                new PasswordHash(VALID_HASH));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, differentId);
        assertNotEquals(a, null);
        assertNotEquals(a, "not a user");
        // Distinct components must produce distinct hashes — kills the "return 0" mutation.
        assertNotEquals(a.hashCode(), differentId.hashCode());
    }

    @Test
    @DisplayName("UserId, Username, Email, PasswordHash: equals + hashCode value-semantics")
    void valueObjectIdentity() {
        // UserId
        assertEquals(new UserId(1), new UserId(1));
        assertEquals(new UserId(1).hashCode(), new UserId(1).hashCode());
        assertNotEquals(new UserId(1), new UserId(2));
        assertNotEquals(new UserId(1).hashCode(), new UserId(2).hashCode());
        assertNotEquals(new UserId(1), null);
        assertNotEquals(new UserId(1), "not a UserId");

        // Username
        assertEquals(new Username("alice"), new Username("alice"));
        assertEquals(new Username("alice").hashCode(), new Username("alice").hashCode());
        assertNotEquals(new Username("alice"), new Username("victor"));
        assertNotEquals(new Username("alice").hashCode(), new Username("victor").hashCode());
        assertNotEquals(new Username("alice"), null);
        assertNotEquals(new Username("alice"), "alice");

        // Email
        assertEquals(new Email("a@b.com"), new Email("a@b.com"));
        assertEquals(new Email("a@b.com").hashCode(), new Email("a@b.com").hashCode());
        assertNotEquals(new Email("a@b.com"), new Email("c@d.com"));
        assertNotEquals(new Email("a@b.com").hashCode(), new Email("c@d.com").hashCode());
        assertNotEquals(new Email("a@b.com"), null);
        assertNotEquals(new Email("a@b.com"), "a@b.com");

        // PasswordHash — VALID_HASH starts with $2a$12$, so generate a second valid hash.
        String otherHash = "$2b$12$bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        assertEquals(new PasswordHash(VALID_HASH), new PasswordHash(VALID_HASH));
        assertEquals(new PasswordHash(VALID_HASH).hashCode(),
                     new PasswordHash(VALID_HASH).hashCode());
        assertNotEquals(new PasswordHash(VALID_HASH), new PasswordHash(otherHash));
        assertNotEquals(new PasswordHash(VALID_HASH).hashCode(),
                        new PasswordHash(otherHash).hashCode());
        assertNotEquals(new PasswordHash(VALID_HASH), null);
        assertNotEquals(new PasswordHash(VALID_HASH), "not a hash");
    }

    @Test
    @DisplayName("User.toString omits passwordHash and exposes id, username, email")
    void userToStringDoesNotLeakHash() {
        User user = new User(new UserId(7),
                new Username("victor"),
                new Email("victor@example.com"),
                new PasswordHash(VALID_HASH));

        String s = user.toString();

        assertFalse(s.contains(VALID_HASH),  "toString must not expose the BCrypt hash");
        assertFalse(s.contains("passwordHash"), "toString must not even mention passwordHash");
        assertTrue(s.contains("victor"),     "toString should include the username");
        assertTrue(s.contains("victor@example.com"), "toString should include the email");
        assertTrue(s.contains("7"),          "toString should include the id");
    }

    // -------------------------------------------------------------------
    // InvalidUserDataException coverage — kills mutations in formatMessage,
    // errors() override, and getErrors().
    // -------------------------------------------------------------------

    @Test
    @DisplayName("InvalidUserDataException: aggregates errors and formats a non-empty summary message")
    void invalidUserDataException_FormatsMessageAndExposesErrors() {
        List<String> errors = List.of("first error", "second error");

        InvalidUserDataException ex = new InvalidUserDataException(errors);

        assertTrue(ex.getMessage().contains("2 validation error(s)"),
                "Message should announce the count");
        assertTrue(ex.getMessage().contains("first error"),
                "Message should embed every individual error");
        assertTrue(ex.getMessage().contains("second error"),
                "Message should embed every individual error");
        assertEquals(errors, ex.getErrors(),
                "getErrors should return the original list");
        assertEquals(errors, ex.errors(),
                "errors() polymorphic override should return the same list");
    }

    @Test
    @DisplayName("InvalidUserDataException.from(ValidationResult): wraps invalid result errors")
    void invalidUserDataException_FromValidationResult() {
        com.recetea.core.shared.domain.ValidationResult<String> result =
                com.recetea.core.shared.domain.ValidationResult.invalid(List.of("only error"));

        InvalidUserDataException ex = InvalidUserDataException.from(result);

        assertEquals(List.of("only error"), ex.getErrors());
        assertTrue(ex.getMessage().contains("1 validation error(s)"));
        assertTrue(ex.getMessage().contains("only error"));
    }

    @Test
    @DisplayName("DuplicateIdentityException: carries the supplied message")
    void duplicateIdentityException_CarriesMessage() {
        DuplicateIdentityException ex = new DuplicateIdentityException("username taken");
        assertEquals("username taken", ex.getMessage());
    }

    // -------------------------------------------------------------------
    // VO toString masking — kills the "replaced return with empty string"
    // and the at-position boundary mutations on Email.toString.
    // -------------------------------------------------------------------

    @Test
    @DisplayName("Email.toString: masks the local-part and exposes only the domain")
    void emailToStringMasksLocalPart() {
        String s = new Email("victor@example.com").toString();

        assertTrue(s.startsWith("Email[value=***@"),
                "Email.toString must replace local-part with three asterisks before '@'");
        assertTrue(s.contains("@example.com"),
                "Email.toString must preserve the full domain part for diagnostics");
        assertFalse(s.contains("victor"),
                "Email.toString must not leak the local-part PII");
        assertTrue(s.endsWith("]"),
                "Email.toString must close with the canonical bracket");
    }

    @Test
    @DisplayName("Username.toString: includes the username value (record default)")
    void usernameToStringExposesValue() {
        String s = new Username("alice123").toString();
        assertTrue(s.contains("alice123"),
                "Username.toString must surface the value for log diagnostics");
    }

    @Test
    @DisplayName("PasswordHash.toString: redacts the hash to PROTECTED")
    void passwordHashToStringRedacts() {
        String s = new PasswordHash(VALID_HASH).toString();

        assertEquals("PasswordHash[value=PROTECTED]", s,
                "PasswordHash.toString must emit a fixed redacted token");
        assertFalse(s.contains(VALID_HASH),
                "PasswordHash.toString must never leak the BCrypt hash");
    }
}
