package za.ac.richfield.smartpantry.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles unit canonicalization, compatibility checks and quantity conversions.
 *
 * Supported dimensions:
 * - MASS   (base unit g):  mg, g, kg, oz, lb
 * - VOLUME (base unit ml): ml, l, tsp, tbsp, cup, fl oz
 * - COUNT  (no base unit): pcs, clove, slice, tin
 *
 * Deliberate strictness (Section 2.3 - a recipe must never be suggested unless the user
 * genuinely has enough):
 * - Mass and volume are NOT converted into each other. Density differs per ingredient
 *   (1 cup of flour is roughly 125 g, 1 cup of milk roughly 250 g), so a generic bridge would
 *   over-estimate some ingredients and suggest recipes the user cannot actually cook.
 * - Count units only match the same count unit. "6 pcs garlic" (bulbs?) is not assumed to be
 *   "6 cloves", and "1 slice onion" is not assumed to be a whole onion.
 */
public final class UnitConverter {

    public enum Dimension {
        MASS,
        VOLUME,
        COUNT,
        UNKNOWN
    }

    private static final double EPSILON = 1e-6;

    private static final Map<String, String> UNIT_ALIASES;
    private static final Map<String, Dimension> UNIT_DIMENSIONS;
    private static final Map<String, Double> TO_BASE_MULTIPLIER;

    /** Units offered in the Add/Edit Ingredient spinner (all canonical codes). */
    public static final String[] SUPPORTED_DISPLAY_UNITS = {
        "g", "kg", "ml", "l", "tsp", "tbsp", "cup", "pcs", "clove", "slice", "tin", "oz", "lb"
    };

    static {
        Map<String, String> aliases = new HashMap<>();
        // Mass aliases -> canonical unit
        aliases.put("g", "g");
        aliases.put("gram", "g");
        aliases.put("grams", "g");
        aliases.put("gm", "g");
        aliases.put("kg", "kg");
        aliases.put("kilogram", "kg");
        aliases.put("kilograms", "kg");
        aliases.put("kilo", "kg");
        aliases.put("kilos", "kg");
        aliases.put("mg", "mg");
        aliases.put("milligram", "mg");
        aliases.put("milligrams", "mg");
        aliases.put("oz", "oz");
        aliases.put("ounce", "oz");
        aliases.put("ounces", "oz");
        aliases.put("lb", "lb");
        aliases.put("lbs", "lb");
        aliases.put("pound", "lb");
        aliases.put("pounds", "lb");

        // Volume aliases -> canonical unit
        aliases.put("ml", "ml");
        aliases.put("milliliter", "ml");
        aliases.put("milliliters", "ml");
        aliases.put("millilitre", "ml");
        aliases.put("millilitres", "ml");
        aliases.put("l", "l");
        aliases.put("liter", "l");
        aliases.put("liters", "l");
        aliases.put("litre", "l");
        aliases.put("litres", "l");
        aliases.put("tsp", "tsp");
        aliases.put("teaspoon", "tsp");
        aliases.put("teaspoons", "tsp");
        aliases.put("tbsp", "tbsp");
        aliases.put("tablespoon", "tbsp");
        aliases.put("tablespoons", "tbsp");
        aliases.put("cup", "cup");
        aliases.put("cups", "cup");
        aliases.put("fl oz", "fl oz");
        aliases.put("floz", "fl oz");

        // Discrete count aliases -> canonical unit
        aliases.put("pcs", "pcs");
        aliases.put("pc", "pcs");
        aliases.put("piece", "pcs");
        aliases.put("pieces", "pcs");
        aliases.put("unit", "pcs");
        aliases.put("units", "pcs");
        aliases.put("item", "pcs");
        aliases.put("items", "pcs");
        aliases.put("whole", "pcs");
        aliases.put("clove", "clove");
        aliases.put("cloves", "clove");
        aliases.put("slice", "slice");
        aliases.put("slices", "slice");
        aliases.put("tin", "tin");
        aliases.put("tins", "tin");
        aliases.put("can", "tin");
        aliases.put("cans", "tin");
        UNIT_ALIASES = Collections.unmodifiableMap(aliases);

        Map<String, Dimension> dims = new HashMap<>();
        dims.put("mg", Dimension.MASS);
        dims.put("g", Dimension.MASS);
        dims.put("kg", Dimension.MASS);
        dims.put("oz", Dimension.MASS);
        dims.put("lb", Dimension.MASS);

        dims.put("ml", Dimension.VOLUME);
        dims.put("l", Dimension.VOLUME);
        dims.put("tsp", Dimension.VOLUME);
        dims.put("tbsp", Dimension.VOLUME);
        dims.put("cup", Dimension.VOLUME);
        dims.put("fl oz", Dimension.VOLUME);

        dims.put("pcs", Dimension.COUNT);
        dims.put("clove", Dimension.COUNT);
        dims.put("slice", Dimension.COUNT);
        dims.put("tin", Dimension.COUNT);
        UNIT_DIMENSIONS = Collections.unmodifiableMap(dims);

        Map<String, Double> multipliers = new HashMap<>();
        // Base unit for MASS is grams (g)
        multipliers.put("mg", 0.001);
        multipliers.put("g", 1.0);
        multipliers.put("kg", 1000.0);
        multipliers.put("oz", 28.3495);
        multipliers.put("lb", 453.592);

        // Base unit for VOLUME is milliliters (ml); metric kitchen measures
        multipliers.put("ml", 1.0);
        multipliers.put("l", 1000.0);
        multipliers.put("tsp", 5.0);
        multipliers.put("tbsp", 15.0);
        multipliers.put("cup", 250.0);
        multipliers.put("fl oz", 29.5735);
        TO_BASE_MULTIPLIER = Collections.unmodifiableMap(multipliers);
    }

    private UnitConverter() {
        // Utility class
    }

    /**
     * Normalizes a raw unit string (e.g. "Kilograms", "Tablespoons", "pieces") into its
     * canonical unit code ("kg", "tbsp", "pcs"). Unknown units are returned lower-cased.
     */
    public static String normalizeUnit(String rawUnit) {
        if (rawUnit == null) {
            return "";
        }
        String cleaned = rawUnit.toLowerCase(Locale.US).trim();
        String canonical = UNIT_ALIASES.get(cleaned);
        return canonical != null ? canonical : cleaned;
    }

    public static boolean isValidUnit(String rawUnit) {
        return UNIT_DIMENSIONS.containsKey(normalizeUnit(rawUnit));
    }

    public static Dimension getDimension(String rawUnit) {
        Dimension dimension = UNIT_DIMENSIONS.get(normalizeUnit(rawUnit));
        return dimension != null ? dimension : Dimension.UNKNOWN;
    }

    /**
     * Two units are compatible when they are the same unit, or when both are MASS units
     * or both are VOLUME units. Count units are only compatible with themselves.
     */
    public static boolean areUnitsCompatible(String fromUnit, String toUnit) {
        String normFrom = normalizeUnit(fromUnit);
        String normTo = normalizeUnit(toUnit);
        if (!isValidUnit(normFrom) || !isValidUnit(normTo)) {
            return false;
        }
        if (normFrom.equals(normTo)) {
            return true;
        }
        Dimension dimension = getDimension(normFrom);
        return dimension == getDimension(normTo)
                && (dimension == Dimension.MASS || dimension == Dimension.VOLUME);
    }

    /**
     * Converts a quantity from one unit to another compatible unit.
     *
     * @throws IllegalArgumentException if the units are not compatible.
     */
    public static double convert(double quantity, String fromUnit, String toUnit) {
        if (!areUnitsCompatible(fromUnit, toUnit)) {
            throw new IllegalArgumentException(
                    "Cannot convert between incompatible units: '" + fromUnit + "' and '" + toUnit + "'");
        }
        String normFrom = normalizeUnit(fromUnit);
        String normTo = normalizeUnit(toUnit);
        if (normFrom.equals(normTo)) {
            return quantity;
        }
        // Both units share a base unit (g or ml): convert to base, then to the target unit
        return quantity * TO_BASE_MULTIPLIER.get(normFrom) / TO_BASE_MULTIPLIER.get(normTo);
    }

    /**
     * Returns true when pantryQty (in pantryUnit) is at least requiredQty (in requiredUnit).
     * Incompatible units are never treated as sufficient.
     */
    public static boolean hasSufficientQuantity(double pantryQty, String pantryUnit,
                                                double requiredQty, String requiredUnit) {
        if (!areUnitsCompatible(pantryUnit, requiredUnit)) {
            return false;
        }
        double convertedAvailable = convert(pantryQty, pantryUnit, requiredUnit);
        return (convertedAvailable + EPSILON) >= requiredQty;
    }
}
