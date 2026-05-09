package dev.darkblade.datalens.core.validate;

import dev.darkblade.datalens.model.DataType;
import dev.darkblade.datalens.model.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private ValidationService service;

    @BeforeEach
    void setUp() {
        service = new ValidationService();
    }

    // ── String ────────────────────────────────────────────────────────────────

    @Test
    void string_acceptsAnyNonEmptyValue() {
        assertTrue(service.validate(DataType.STRING, "hello").isValid());
        assertTrue(service.validate(DataType.STRING, "123").isValid());
    }

    @Test
    void string_rejectsBlank() {
        assertFalse(service.validate(DataType.STRING, "").isValid());
        assertFalse(service.validate(DataType.STRING, "  ").isValid());
    }

    // ── Int ───────────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "2147483647", "-2147483648"})
    void int_acceptsValidValues(String v) {
        assertTrue(service.validate(DataType.INT, v).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "2147483648", ""})
    void int_rejectsInvalidValues(String v) {
        assertFalse(service.validate(DataType.INT, v).isValid());
    }

    // ── Long ──────────────────────────────────────────────────────────────────

    @Test
    void long_acceptsLargeValue() {
        assertTrue(service.validate(DataType.LONG, "9999999999999").isValid());
    }

    // ── Double ────────────────────────────────────────────────────────────────

    @Test
    void double_acceptsDecimal() {
        assertTrue(service.validate(DataType.DOUBLE, "3.14").isValid());
    }

    @Test
    void double_rejectsAlpha() {
        assertFalse(service.validate(DataType.DOUBLE, "pi").isValid());
    }

    // ── Boolean ───────────────────────────────────────────────────────────────

    @Test
    void boolean_acceptsTrueAndFalse() {
        assertTrue(service.validate(DataType.BOOLEAN, "true").isValid());
        assertTrue(service.validate(DataType.BOOLEAN, "FALSE").isValid());
    }

    @Test
    void boolean_rejectsOther() {
        assertFalse(service.validate(DataType.BOOLEAN, "yes").isValid());
    }

    // ── UUID ─────────────────────────────────────────────────────────────────

    @Test
    void uuid_acceptsValidUUID() {
        assertTrue(service.validate(DataType.UUID, "550e8400-e29b-41d4-a716-446655440000").isValid());
    }

    @Test
    void uuid_rejectsInvalidUUID() {
        assertFalse(service.validate(DataType.UUID, "not-a-uuid").isValid());
    }

    // ── Container and array types ─────────────────────────────────────────────

    @Test
    void compound_alwaysRejectsWithMessage() {
        ValidationResult r = service.validate(DataType.COMPOUND, "anything");
        assertFalse(r.isValid());
        assertTrue(r.getError().isPresent());
    }

    @Test
    void list_alwaysRejectsWithMessage() {
        ValidationResult r = service.validate(DataType.LIST, "anything");
        assertFalse(r.isValid());
    }

    // ── Coercion ──────────────────────────────────────────────────────────────

    @Test
    void coerce_convertsToCorrectType() {
        assertEquals(42, service.coerce(DataType.INT, "42"));
        assertEquals(3.14, (double) service.coerce(DataType.DOUBLE, "3.14"), 0.001);
        assertEquals(true, service.coerce(DataType.BOOLEAN, "true"));
        assertEquals("hello", service.coerce(DataType.STRING, "hello"));
    }
}
