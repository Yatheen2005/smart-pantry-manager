package za.ac.richfield.smartpantry;

import org.junit.Test;

import za.ac.richfield.smartpantry.util.InputValidator;
import za.ac.richfield.smartpantry.util.InputValidator.ValidationResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputValidatorTest {

    private static ValidationResult validate(String name, String qty, String unit, String expiry) {
        return InputValidator.validatePantryInput(name, qty, unit, expiry);
    }

    @Test
    public void validInput_isAccepted() {
        assertTrue(validate("Tomatoes", "4", "pcs", "2026-10-25").isValid());
        assertTrue(validate("Milk", "1.5", "l", null).isValid());
        assertTrue(validate("Milk", "1.5", "l", "  ").isValid()); // expiry is optional
    }

    @Test
    public void decimalComma_isAccepted() {
        assertTrue(validate("Milk", "1,5", "l", null).isValid());
        assertEquals(1.5, InputValidator.parseQuantity("1,5"), 1e-9);
    }

    @Test
    public void invalidName_isRejectedOnNameField() {
        assertField(InputValidator.FIELD_NAME, validate("", "2", "pcs", null));
        assertField(InputValidator.FIELD_NAME, validate("   ", "2", "pcs", null));
        assertField(InputValidator.FIELD_NAME, validate("E", "2", "pcs", null));
        assertField(InputValidator.FIELD_NAME, validate("1234", "2", "pcs", null));
        assertField(InputValidator.FIELD_NAME, validate(repeat('a', 51), "2", "pcs", null));
    }

    @Test
    public void invalidQuantity_isRejectedOnQuantityField() {
        assertField(InputValidator.FIELD_QUANTITY, validate("Eggs", "", "pcs", null));
        assertField(InputValidator.FIELD_QUANTITY, validate("Eggs", "abc", "pcs", null));
        assertField(InputValidator.FIELD_QUANTITY, validate("Eggs", "0", "pcs", null));
        assertField(InputValidator.FIELD_QUANTITY, validate("Eggs", "-3", "pcs", null));
        assertField(InputValidator.FIELD_QUANTITY, validate("Eggs", "NaN", "pcs", null));
        assertField(InputValidator.FIELD_QUANTITY, validate("Eggs", "100001", "pcs", null));
    }

    @Test
    public void invalidUnit_isRejected() {
        assertField(InputValidator.FIELD_UNIT, validate("Eggs", "2", "handful", null));
        assertField(InputValidator.FIELD_UNIT, validate("Eggs", "2", null, null));
    }

    @Test
    public void invalidExpiryDate_isRejectedOnExpiryField() {
        assertField(InputValidator.FIELD_EXPIRY, validate("Eggs", "2", "pcs", "2026-02-30")); // no 30 Feb
        assertField(InputValidator.FIELD_EXPIRY, validate("Eggs", "2", "pcs", "2026-13-01"));
        assertField(InputValidator.FIELD_EXPIRY, validate("Eggs", "2", "pcs", "25/10/2026"));
        assertField(InputValidator.FIELD_EXPIRY, validate("Eggs", "2", "pcs", "tomorrow"));
    }

    private static void assertField(String expectedField, ValidationResult result) {
        assertFalse(result.isValid());
        assertEquals(expectedField, result.getField());
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
