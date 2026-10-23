package za.ac.richfield.smartpantry.model;

import java.util.Locale;

/**
 * The result of checking ONE required recipe ingredient against the user's pantry.
 */
public class IngredientCheck {

    public enum Status {
        /** Present with at least the required quantity. */
        AVAILABLE,
        /** Present in a compatible unit, but not enough of it. */
        INSUFFICIENT,
        /** Present, but only in a unit that cannot be converted (e.g. "cup" vs "g"). */
        INCOMPATIBLE_UNIT,
        /** Not in the pantry at all (or only expired stock when expired items are excluded). */
        MISSING
    }

    private final RecipeIngredient ingredient;
    private final Status status;
    private final double availableQuantity; // expressed in the recipe's required unit
    private final String incompatiblePantryQuantity; // e.g. "2 cup", only for INCOMPATIBLE_UNIT

    public IngredientCheck(RecipeIngredient ingredient, Status status,
                           double availableQuantity, String incompatiblePantryQuantity) {
        this.ingredient = ingredient;
        this.status = status;
        this.availableQuantity = availableQuantity;
        this.incompatiblePantryQuantity = incompatiblePantryQuantity;
    }

    public RecipeIngredient getIngredient() {
        return ingredient;
    }

    public Status getStatus() {
        return status;
    }

    public double getAvailableQuantity() {
        return availableQuantity;
    }

    public boolean isSatisfied() {
        return status == Status.AVAILABLE;
    }

    /** Human-readable explanation shown on the Recipe Detail and Suggested Recipes screens. */
    public String describe() {
        String name = ingredient.getIngredientName();
        String unit = ingredient.getRequiredUnit();
        String required = PantryItem.formatQuantityValue(ingredient.getRequiredQuantity()) + " " + unit;
        String available = PantryItem.formatQuantityValue(availableQuantity) + " " + unit;
        switch (status) {
            case AVAILABLE:
                return String.format(Locale.US, "%s: needs %s (you have %s)", name, required, available);
            case INSUFFICIENT:
                double shortBy = ingredient.getRequiredQuantity() - availableQuantity;
                return String.format(Locale.US, "%s: needs %s, you have %s (short by %s %s)",
                        name, required, available, PantryItem.formatQuantityValue(shortBy), unit);
            case INCOMPATIBLE_UNIT:
                return String.format(Locale.US,
                        "%s: needs %s, but your pantry lists %s (units cannot be converted)",
                        name, required, incompatiblePantryQuantity);
            case MISSING:
            default:
                return String.format(Locale.US, "%s: missing (needs %s)", name, required);
        }
    }
}
