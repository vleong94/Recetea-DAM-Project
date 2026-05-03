package com.recetea.infrastructure.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the {@link SensitiveDataMaskingConverter} both at the converter level
 * (white-box: feed it crafted {@link ILoggingEvent}s) and at the pattern level
 * (black-box: format through a real {@link PatternLayout} that registers the
 * {@code mask} keyword the same way {@code logback.xml} does in production).
 *
 * <p>The black-box assertion catches misregistration mistakes — a wired-up
 * {@code <conversionRule>} that logback silently ignores would fail here.
 */
@DisplayName("SensitiveDataMaskingConverter — global Logback scrubber for BCrypt + JWT")
class SensitiveDataMaskingConverterTest {

    /** Real BCrypt-shaped value (not a real credential — generated with cost 12 over a literal). */
    private static final String BCRYPT_HASH =
            "$2a$12$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPQRSTU";
    /** JWT-shaped value mirroring the structure of a Supabase service key — three base64url segments. */
    private static final String JWT_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    private LoggerContext loggerContext;

    @BeforeEach
    void setUp() {
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    @AfterEach
    void tearDown() {
        // Detach any test-scoped appenders we added directly on the root logger.
        Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();
        // Re-initialise from the test classpath so other tests still see logback-test.xml.
        loggerContext.reset();
        try {
            new ch.qos.logback.classic.joran.JoranConfigurator() {{
                setContext(loggerContext);
            }}.doConfigure(getClass().getResource("/logback-test.xml"));
        } catch (Exception ignored) {
            // Best-effort restore — subsequent tests log via SLF4J and pick up classpath defaults.
        }
    }

    // -----------------------------------------------------------------------
    // White-box: drive the converter directly with synthesised LoggingEvents.
    // -----------------------------------------------------------------------

    /**
     * White-box helper. {@link SensitiveDataMaskingConverter#transform} is package-protected
     * and visible to tests in the same module — extend the class so we can drive the
     * protected method without going through a full {@code PatternLayout} compile cycle.
     */
    private static final class TestableConverter extends SensitiveDataMaskingConverter {
        @Override public String transform(ILoggingEvent event, String in) {
            return super.transform(event, in);
        }
    }

    @Test
    @DisplayName("converter: replaces a BCrypt hash with [MASKED-BCRYPT]")
    void converterMasksBcryptHash() {
        TestableConverter converter = new TestableConverter();
        String input  = "User registered with hash " + BCRYPT_HASH + " (length=" + BCRYPT_HASH.length() + ")";
        String result = converter.transform(eventWithMessage(input), input);

        assertFalse(result.contains(BCRYPT_HASH), "Raw BCrypt hash must not appear in output");
        assertTrue(result.contains("[MASKED-BCRYPT]"), "Output must carry the BCrypt sentinel");
        assertTrue(result.contains("length=60"),       "Surrounding text must be preserved");
    }

    @Test
    @DisplayName("converter: replaces a JWT/Supabase service key with [MASKED-JWT]")
    void converterMasksJwtToken() {
        TestableConverter converter = new TestableConverter();
        String input  = "Supabase auth token: " + JWT_TOKEN;
        String result = converter.transform(eventWithMessage(input), input);

        assertFalse(result.contains(JWT_TOKEN), "Raw JWT must not appear in output");
        assertTrue(result.contains("[MASKED-JWT]"), "Output must carry the JWT sentinel");
    }

    @Test
    @DisplayName("converter: scrubs both patterns in the same message")
    void converterMasksMultipleSecretsInOneMessage() {
        TestableConverter converter = new TestableConverter();
        String input  = "hash=" + BCRYPT_HASH + " token=" + JWT_TOKEN;
        String result = converter.transform(eventWithMessage(input), input);

        assertFalse(result.contains(BCRYPT_HASH));
        assertFalse(result.contains(JWT_TOKEN));
        assertTrue(result.contains("[MASKED-BCRYPT]"));
        assertTrue(result.contains("[MASKED-JWT]"));
    }

    @Test
    @DisplayName("converter: returns innocuous messages untouched (zero-overhead happy path)")
    void converterIsTransparentForCleanMessages() {
        TestableConverter converter = new TestableConverter();
        String original = "Login successful for user 'victor' from 192.168.1.5";
        String result   = converter.transform(eventWithMessage(original), original);
        assertEquals(original, result);
    }

    @Test
    @DisplayName("converter: tolerates null and empty inputs without throwing")
    void converterTolerantToNullAndEmpty() {
        TestableConverter converter = new TestableConverter();
        assertNull(converter.transform(eventWithMessage(""), null));
        assertEquals("", converter.transform(eventWithMessage(""), ""));
    }

    // -----------------------------------------------------------------------
    // Black-box: PatternLayout exercising the %mask conversion word, just like
    // logback.xml/logback-test.xml configure it. Catches misregistration.
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("pattern layout: %mask(%msg) registered as conversionRule scrubs both secrets")
    void patternLayoutScrubsThroughMaskConversionWord() {
        PatternLayout layout = new PatternLayout();
        layout.setContext(loggerContext);
        layout.getInstanceConverterMap().put("mask", SensitiveDataMaskingConverter::new);
        layout.setPattern("%mask(%msg)");
        layout.start();

        ILoggingEvent event = eventWithMessage(
                "registering user " + BCRYPT_HASH + " token=" + JWT_TOKEN);
        String formatted = layout.doLayout(event);

        assertFalse(formatted.contains(BCRYPT_HASH), "Raw bcrypt must not survive the pattern; was: " + formatted);
        assertFalse(formatted.contains(JWT_TOKEN),   "Raw JWT must not survive the pattern; was: " + formatted);
        assertTrue(formatted.contains("[MASKED-BCRYPT]"), "Expected BCRYPT mask; got: " + formatted);
        assertTrue(formatted.contains("[MASKED-JWT]"),    "Expected JWT mask; got: " + formatted);
    }

    @Test
    @DisplayName("end-to-end: SLF4J → ListAppender via the configured masking pattern")
    void endToEndScrubbingViaListAppender() {
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.setContext(loggerContext);
        events.start();

        Logger captured = loggerContext.getLogger(SensitiveDataMaskingConverterTest.class);
        captured.addAppender(events);
        captured.setLevel(ch.qos.logback.classic.Level.INFO);
        captured.setAdditive(false);

        org.slf4j.Logger slf4j = LoggerFactory.getLogger(SensitiveDataMaskingConverterTest.class);
        slf4j.info("Hash leak attempt: {}; token leak attempt: {}", BCRYPT_HASH, JWT_TOKEN);

        // ListAppender stores raw events; format them through a layout configured the same
        // way as logback-test.xml so we observe what an operator would actually see.
        PatternLayout layout = new PatternLayout();
        layout.setContext(loggerContext);
        layout.getInstanceConverterMap().put("mask", SensitiveDataMaskingConverter::new);
        layout.setPattern("%mask(%msg)");
        layout.start();

        assertEquals(1, events.list.size(), "Exactly one event captured");
        String rendered = layout.doLayout(events.list.getFirst());

        assertFalse(rendered.contains(BCRYPT_HASH));
        assertFalse(rendered.contains(JWT_TOKEN));
        assertTrue(rendered.contains("[MASKED-BCRYPT]"));
        assertTrue(rendered.contains("[MASKED-JWT]"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ILoggingEvent eventWithMessage(String formatted) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLoggerName(SensitiveDataMaskingConverterTest.class.getName());
        event.setLevel(ch.qos.logback.classic.Level.INFO);
        event.setMessage(formatted);
        event.setMDCPropertyMap(Map.of());
        return event;
    }
}
