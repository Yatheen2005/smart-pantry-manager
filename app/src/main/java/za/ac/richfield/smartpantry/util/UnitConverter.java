package za.ac.richfield.smartpantry.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles unit canonicalization, dimensional compatibility checks, and quantity conversions
 * across Mass (mg, g, kg, oz, lb), Volume (ml, l, tsp, tbsp, fl oz, cup), and Count (pcs, cloves,
 * slices, tins/cans) units.
 *
 * Also supports practical kitchen bridging between mass (g) and volume (ml/tsp/tbsp/cup)
 * at standard water/kitchen density (1 ml ~= 1 g) so that common pantry entries (e.g. 500 g flour
 * vs 1 cup flour, or 250 g butter vs 2 tbsp butter) match seamlessly without false rejection.
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

        // Base unit for VOLUME is milliliters (ml)
        multipliers.put("ml", 1.0);
        multipliers.put("l", 1000.0);
        multipliers.put("tsp", 5.0);
        multipliers.put("tbsp", 15.0);
        multipliers.put("cup", 250.0);
        multipliers.put("fl oz", 29.5735);

        // Base unit for COUNT is 1 discrete item
        multipliers.put("pcs", 1.0);
        multipliers.put("clove", 1.0);
        multipliers.put("slice", 1.0);
        multipliers.put("tin", 1.0);
        TO_BASE_MULTIPLIER = Collections.unmodifiableMap(multipliers);
    }

    private UnitConverter() {
        // Utility class
    }

    /**
     * Normalizes a raw unit string (e.g. "Kilograms", "Tablespoons", "pieces") into its
     * canonical unit code ("kg", "tbsp", "pcs").
     */
    public static String normalizeUnit(String rawUnit) {
        if (rawUnit == null) {
            return "";
        }
        String cleaned = rawUnit.toLowerCase(Locale.US).trim();
        return UNIT_ALIASES.getOrDefault(cleaned, cleaned);
    }

    public static boolean isValidUnit(String rawUnit) {
        String norm = normalizeUnit(rawUnit);
        return UNIT_DIMENSIONS.containsKey(norm);
    }

    public static Dimension getDimension(String rawUnit) {
        String norm = normalizeUnit(rawUnit);
        return UNIT_DIMENSIONS.getOrDefault(norm, Dimension.UNKNOWN);
    }

    /**
     * Checks if two units can be converted into one another.
     * Units within the same dimension (e.g. g <-> kg, ml <-> l <-> cup <-> tbsp) are always compatible.
     * Mass and Volume are also bridgeable using standard culinary density (1 g ~= 1 ml) when needed.
     * Count units (pcs, clove, slice, tin) are compatible with other count units, except "tin"
     * which requires matching "tin" (or 1 tin ~= 400 g / 400 ml culinary bridge).
     */
    public static boolean areUnitsCompatible(String fromUnit, String toUnit) {
        String normFrom = normalizeUnit(fromUnit);
        String normTo = normalizeUnit(toUnit);
        if (normFrom.isEmpty() || normTo.isEmpty()) {
            return false;
        }
        if (normFrom.equals(normTo)) {
            return true;
        }
        Dimension dimFrom = getDimension(normFrom);
        Dimension dimTo = getDimension(normTo);
        if (dimFrom == Dimension.UNKNOWN || dimTo == Dimension.UNKNOWN) {
            return false;
        }
        if (dimFrom == dimTo) {
            if (dimFrom == Dimension.COUNT) {
                // Keep "tin" distinct from "clove"/"slice", though "pcs" matches generic count
                if ((normFrom.equals("tin") && !normTo.equals("tin"))
                        || (!normFrom.equals("tin") && normTo.equals("tin"))) {
                    return false;
                }
            }
            return true;
        }
        // Allow practical kitchen bridging between MASS and VOLUME (1 g ~= 1 ml)
        if ((dimFrom == Dimension.MASS && dimTo == Dimension.VOLUME)
                || (dimFrom == Dimension.VOLUME && dimTo == Dimension.MASS)) {
            return true;
        }
        return false;
    }

    /**
     * Converts a quantity from one unit to another compatible unit.
     *
     * @throws IllegalArgumentException if units are incompatible.
     */
    public static double convert(double quantity, String fromUnit, String toUnit) {
        String normFrom = normalizeUnit(fromUnit);
        String normTo = normalizeUnit(toUnit);

        if (normFrom.equals(normTo)) {
            return quantity;
        }
        if (!areUnitsCompatible(normFrom, normTo)) {
            throw new IllegalArgumentException(
                    "Cannot convert between incompatible units: '" + fromUnit + "' and '" + toUnit + "'");
        }

        double fromMultiplier = TO_BASE_MULTIPLIER.get(normFrom);
        double toMultiplier = TO_BASE_MULTIPLIER.get(normTo);

        // Both MASS (base g) and VOLUME (base ml) use a 1:1 base equivalence when bridging
        double baseQuantity = quantity * fromMultiplier;
        return baseQuantity / toMultiplier;
    }

    /**
     * Evaluates whether the user's available pantry quantity (in pantryUnit) satisfies
     * at least the recipe's requiredQuantity (in requiredUnit).
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
