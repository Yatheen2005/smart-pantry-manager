package za.ac.richfield.smartpantry;

import org.junit.Test;

import za.ac.richfield.smartpantry.util.UnitConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnitConverterTest {

    @Test
    public void aliases_normalizeToCanonicalUnits() {
        assertEquals("kg", UnitConverter.normalizeUnit("Kilograms"));
        assertEquals("tbsp", UnitConverter.normalizeUnit(" Tablespoons "));
        assertEquals("pcs", UnitConverter.normalizeUnit("pieces"));
        assertEquals("tin", UnitConverter.normalizeUnit("can"));
        assertEquals("l", UnitConverter.normalizeUnit("Litres"));
    }

    @Test
    public void massConversions() {
        assertEquals(1000.0, UnitConverter.convert(1, "kg", "g"), 1e-9);
        assertEquals(0.25, UnitConverter.convert(250, "g", "kg"), 1e-9);
        assertEquals(453.592, UnitConverter.convert(1, "lb", "g"), 1e-9);
    }

    @Test
    public void volumeConversions() {
        assertEquals(0.5, UnitConverter.convert(500, "ml", "l"), 1e-9);
        assertEquals(250.0, UnitConverter.convert(1, "cup", "ml"), 1e-9);
        assertEquals(30.0, UnitConverter.convert(2, "tbsp", "ml"), 1e-9);
        assertEquals(3.0, UnitConverter.convert(1, "tbsp", "tsp"), 1e-9);
    }

    @Test
    public void sameDimension_isCompatible() {
        assertTrue(UnitConverter.areUnitsCompatible("g", "kg"));
        assertTrue(UnitConverter.areUnitsCompatible("cup", "ml"));
        assertTrue(UnitConverter.areUnitsCompatible("slice", "slices"));
    }

    @Test
    public void massAndVolume_areNotCompatible() {
        assertFalse(UnitConverter.areUnitsCompatible("g", "ml"));
        assertFalse(UnitConverter.areUnitsCompatible("cup", "g"));
    }

    @Test
    public void differentCountUnits_areNotCompatible() {
        assertFalse(UnitConverter.areUnitsCompatible("pcs", "clove"));
        assertFalse(UnitConverter.areUnitsCompatible("slice", "pcs"));
        assertFalse(UnitConverter.areUnitsCompatible("tin", "pcs"));
    }

    @Test
    public void unknownUnits_areNeverCompatible() {
        assertFalse(UnitConverter.isValidUnit("handful"));
        assertFalse(UnitConverter.areUnitsCompatible("handful", "handful"));
        assertFalse(UnitConverter.areUnitsCompatible("", "g"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertingIncompatibleUnits_throws() {
        UnitConverter.convert(1, "cup", "g");
    }

    @Test
    public void sufficientQuantityCheck() {
        assertTrue(UnitConverter.hasSufficientQuantity(0.5, "kg", 250, "g"));
        assertTrue(UnitConverter.hasSufficientQuantity(0.25, "kg", 250, "g")); // exact boundary
        assertFalse(UnitConverter.hasSufficientQuantity(0.1, "l", 250, "ml"));
        assertFalse(UnitConverter.hasSufficientQuantity(10, "cup", 1, "g")); // incompatible
    }

    @Test
    public void everySpinnerUnit_isValid() {
        for (String unit : UnitConverter.SUPPORTED_DISPLAY_UNITS) {
            assertTrue(unit, UnitConverter.isValidUnit(unit));
        }
    }
}
