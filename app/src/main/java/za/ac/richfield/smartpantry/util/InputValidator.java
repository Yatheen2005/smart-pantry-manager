package za.ac.richfield.smartpantry.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Validates user data entry on the Add/Edit Ingredient screen (Section 3.1).
 * Provides structured validation results with field-specific error messages.
 */
public final class InputValidator {

    public static class ValidationResult {
        private final boolean valid;
        private final String field;
        private final String errorMessage;

        public ValidationResult(boolean valid, String field, String errorMessage) {
            this.valid = valid;
            this.field = field;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult error(String field, String message) {
            return new ValidationResult(false, field, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getField() {
            return field;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private InputValidator() {
        // Utility class
    }

    /**
     * Validates all fields of a pantry item form submission.
     */
    public static ValidationResult validatePantryInput(String rawName,
                                                       String rawQuantity,
                                                       String rawUnit,
                                                       String rawExpiryDate) {
        // 1. Validate ingredient name
        if (rawName == null || rawName.trim().isEmpty()) {
            return ValidationResult.error("name", "Ingredient name is required.");
        }
        String trimmedName = rawName.trim();
        if (trimmedName.length() < 2) {
            return ValidationResult.error("name", "Ingredient name must be at least 2 characters.");
        }
        if (!trimmedName.matches(".*[a-zA-Z]+.*")) {
            return ValidationResult.error("name", "Ingredient name must contain letters.");
        }

        // 2. Validate quantity
        if (rawQuantity == null || rawQuantity.trim().isEmpty()) {
            return ValidationResult.error("quantity", "Quantity is required.");
        }
        double parsedQty;
        try {
            parsedQty = Double.parseDouble(rawQuantity.trim());
        } catch (NumberFormatException e) {
            return ValidationResult.error("quantity", "Please enter a valid numeric quantity (e.g. 2 or 250).");
        }
        if (Double.isNaN(parsedQty) || Double.isInfinite(parsedQty) || parsedQty <= 0.0) {
            return ValidationResult.error("quantity", "Quantity must be greater than zero.");
        }
        if (parsedQty > 100000.0) {
            return ValidationResult.error("quantity", "Quantity cannot exceed 100,000.");
        }

        // 3. Validate unit
        if (rawUnit == null || rawUnit.trim().isEmpty() || !UnitConverter.isValidUnit(rawUnit)) {
            return ValidationResult.error("unit", "Please select a valid measurement unit.");
        }

        // 4. Validate optional expiry date (YYYY-MM-DD)
        if (rawExpiryDate != null && !rawExpiryDate.trim().isEmpty()) {
            String trimmedDate = rawExpiryDate.trim();
            if (!trimmedDate.matches("^\d{4}-\d{2}-\d{2}$")) {
                return ValidationResult.error("expiry", "Expiry date must use YYYY-MM-DD format.");
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            try {
                Date parsed = sdf.parse(trimmedDate);
                if (parsed == null) {
                    return ValidationResult.error("expiry", "Invalid calendar date.");
                }
            } catch (ParseException e) {
                return ValidationResult.error("expiry", "Invalid calendar date (check month and day).");
            }
        }

        return ValidationResult.success();
    }
}
