package dev.darkblade.datalens.model.validation;

import java.util.Optional;

/**
 * Wraps the result of a validation check.
 * Either represents success or carries an error message.
 */
public final class ValidationResult {

    private static final ValidationResult OK = new ValidationResult(true, null);

    private final boolean valid;
    private final String error;

    private ValidationResult(boolean valid, String error) {
        this.valid = valid;
        this.error = error;
    }

    public static ValidationResult ok() {
        return OK;
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(false, message);
    }

    public boolean isValid() { return valid; }

    public Optional<String> getError() { return Optional.ofNullable(error); }

    @Override
    public String toString() {
        return valid ? "ValidationResult[OK]" : "ValidationResult[ERROR: " + error + "]";
    }
}
