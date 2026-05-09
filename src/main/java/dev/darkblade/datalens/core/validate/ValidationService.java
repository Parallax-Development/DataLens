package dev.darkblade.datalens.core.validate;

import dev.darkblade.datalens.model.DataType;
import dev.darkblade.datalens.model.validation.ValidationResult;

import java.util.UUID;

/**
 * Validates raw string input against a target {@link DataType} before committing edits.
 */
public final class ValidationService {

    /**
     * Validates and coerces {@code rawValue} to the expected {@code type}.
     *
     * @param type     the DataType of the target node
     * @param rawValue the raw string input from the player / command
     * @return a ValidationResult (ok or error with message)
     */
    public ValidationResult validate(DataType type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return ValidationResult.error("Value must not be empty");
        }
        return switch (type) {
            case STRING  -> ValidationResult.ok();
            case INT     -> validateInt(rawValue);
            case LONG    -> validateLong(rawValue);
            case DOUBLE  -> validateDouble(rawValue);
            case FLOAT   -> validateFloat(rawValue);
            case BYTE    -> validateByte(rawValue);
            case SHORT   -> validateShort(rawValue);
            case BOOLEAN -> validateBoolean(rawValue);
            case UUID    -> validateUuid(rawValue);
            case COMPOUND, LIST -> ValidationResult.error("Cannot set a value on container type: " + type.label());
            case BYTE_ARRAY, INT_ARRAY, LONG_ARRAY ->
                    ValidationResult.error("Array editing is not supported via text input");
            case UNKNOWN -> ValidationResult.error("Cannot edit a node of unknown type");
        };
    }

    /**
     * Coerces a validated raw string to the proper Java type for the given DataType.
     * Call only after {@link #validate} returns ok.
     */
    public Object coerce(DataType type, String rawValue) {
        return switch (type) {
            case STRING  -> rawValue;
            case INT     -> Integer.parseInt(rawValue);
            case LONG    -> Long.parseLong(rawValue);
            case DOUBLE  -> Double.parseDouble(rawValue);
            case FLOAT   -> Float.parseFloat(rawValue);
            case BYTE    -> Byte.parseByte(rawValue);
            case SHORT   -> Short.parseShort(rawValue);
            case BOOLEAN -> Boolean.parseBoolean(rawValue);
            case UUID    -> UUID.fromString(rawValue);
            default -> throw new IllegalStateException("Cannot coerce type: " + type);
        };
    }

    // ── Type-specific validators ───────────────────────────────────────────────

    private ValidationResult validateInt(String v) {
        try { Integer.parseInt(v); return ValidationResult.ok(); }
        catch (NumberFormatException e) { return ValidationResult.error("Not a valid integer: " + v); }
    }

    private ValidationResult validateLong(String v) {
        try { Long.parseLong(v); return ValidationResult.ok(); }
        catch (NumberFormatException e) { return ValidationResult.error("Not a valid long: " + v); }
    }

    private ValidationResult validateDouble(String v) {
        try { Double.parseDouble(v); return ValidationResult.ok(); }
        catch (NumberFormatException e) { return ValidationResult.error("Not a valid double: " + v); }
    }

    private ValidationResult validateFloat(String v) {
        try { Float.parseFloat(v); return ValidationResult.ok(); }
        catch (NumberFormatException e) { return ValidationResult.error("Not a valid float: " + v); }
    }

    private ValidationResult validateByte(String v) {
        try { Byte.parseByte(v); return ValidationResult.ok(); }
        catch (NumberFormatException e) { return ValidationResult.error("Not a valid byte (-128..127): " + v); }
    }

    private ValidationResult validateShort(String v) {
        try { Short.parseShort(v); return ValidationResult.ok(); }
        catch (NumberFormatException e) { return ValidationResult.error("Not a valid short: " + v); }
    }

    private ValidationResult validateBoolean(String v) {
        if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false")) return ValidationResult.ok();
        return ValidationResult.error("Expected 'true' or 'false', got: " + v);
    }

    private ValidationResult validateUuid(String v) {
        try { UUID.fromString(v); return ValidationResult.ok(); }
        catch (IllegalArgumentException e) { return ValidationResult.error("Not a valid UUID: " + v); }
    }
}
