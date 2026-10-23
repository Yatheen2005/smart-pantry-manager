package za.ac.richfield.smartpantry.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Validates user data entry on the Add/Edit Ingredient screen (Section 3.1).
 * Returns the first problem found together with the field it belongs to, so the screen can
 * show the message next to the right input.
 */
public final class InputValidator {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_QUANTITY = "quantity";
    public static final String FIELD_UNIT = "unit";
    public static final String FIELD_EXPIRY = "expiry";

    public static final int MAX_NAME_LENGTH = 50;
    public static final double MAX_QUANTITY = 100000.0;

    public static class ValidationResult {
        private final boolean valid;
        private final String field;
        private final String errorMessage;

        private ValidationResult(boolean valid, String field, String errorMessage) {
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
     * Parses a quantity typed by the user. Accepts a decimal comma ("1,5") as well as a dot.
     *
     * @throws NumberFormatException if the text is not a number
     */
    public static double parseQuantity(String rawQuantity) {
        return Double.parseDouble(rawQuantity.trim().replace(',', '.'));
    }

    /**
     * Validates all fields of a pantry item form submission.
     */
    public static ValidationResult validatePantryInput(String rawName,
                                                       String rawQuantity,
                                                       String rawUnit,
                                                       String rawExpiryDate) {
        // 1. Ingredient name
        if (rawName == null || rawName.trim().isEmpty()) {
            return ValidationResult.error(FIELD_NAME, "Ingredient name is required.");
        }
        String trimmedName = rawName.trim();
        if (trimmedName.length() < 2) {
            return ValidationResult.error(FIELD_NAME, "Ingredient name must be at least 2 characters.");
        }
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            return ValidationResult.error(FIELD_NAME,
                    "Ingredient name must be " + MAX_NAME_LENGTH + " characters or fewer.");
        }
        if (!trimmedName.matches(".*[a-zA-Z].*")) {
            return ValidationResult.error(FIELD_NAME, "Ingredient name must contain letters.");
        }

        // 2. Quantity
        if (rawQuantity == null || rawQuantity.trim().isEmpty()) {
            return ValidationResult.error(FIELD_QUANTITY, "Quantity is required.");
        }
        double parsedQty;
        try {
            parsedQty = parseQuantity(rawQuantity);
        } catch (NumberFormatException e) {
            return ValidationResult.error(FIELD_QUANTITY, "Please enter a valid number (e.g. 2 or 250).");
        }
        if (Double.isNaN(parsedQty) || Double.isInfinite(parsedQty) || parsedQty <= 0.0) {
            return ValidationResult.error(FIELD_QUANTITY, "Quantity must be greater than zero.");
        }
        if (parsedQty > MAX_QUANTITY) {
            return ValidationResult.error(FIELD_QUANTITY, "Quantity cannot exceed 100,000.");
        }

        // 3. Unit
        if (rawUnit == null || !UnitConverter.isValidUnit(rawUnit)) {
            return ValidationResult.error(FIELD_UNIT, "Please select a valid measurement unit.");
        }

        // 4. Optional expiry date (YYYY-MM-DD, must be a real calendar date)
        if (rawExpiryDate != null && !rawExpiryDate.trim().isEmpty()) {
            String trimmedDate = rawExpiryDate.trim();
            if (!trimmedDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return ValidationResult.error(FIELD_EXPIRY, "Expiry date must use the YYYY-MM-DD format.");
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false); // rejects impossible dates such as 2026-02-30
            try {
                sdf.parse(trimmedDate);
            } catch (ParseException e) {
                return ValidationResult.error(FIELD_EXPIRY, "Invalid calendar date (check the month and day).");
            }
        }

        return ValidationResult.success();
    }
}
