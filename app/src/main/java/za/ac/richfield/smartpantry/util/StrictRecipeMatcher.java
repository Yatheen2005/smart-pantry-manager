package za.ac.richfield.smartpantry.util;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.model.IngredientCheck;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;

/**
 * Core business logic implementing Section 2.3, "The Strict-Matching Rule".
 *
 * A recipe is suggested IF AND ONLY IF, for every required ingredient:
 * 1. the pantry contains an item with the same canonical name (IngredientNormalizer), and
 * 2. the total quantity of those items, converted into the recipe's unit (UnitConverter),
 *    is at least the required quantity.
 *
 * If several pantry rows have the same ingredient (e.g. two separate batches of eggs) their
 * quantities are added together. Rows in a unit that cannot be converted are ignored, and
 * expired rows are ignored when the "exclude expired" setting is on.
 *
 * This class has no Android dependencies so it can be unit tested on the JVM.
 */
public final class StrictRecipeMatcher {

    private static final double EPSILON = 1e-6;

    private StrictRecipeMatcher() {
        // Utility class
    }

    /**
     * Returns the pantry items that can be used for a required ingredient: same canonical name,
     * positive quantity, convertible unit, and not expired (when excludeExpired is true).
     * Shared by the matcher and by PantryDatabaseHelper.consumeRecipeIngredients().
     */
    public static List<PantryItem> findUsablePantryItems(RecipeIngredient required,
                                                         List<PantryItem> pantryItems,
                                                         boolean excludeExpired) {
        List<PantryItem> usable = new ArrayList<>();
        if (required == null || pantryItems == null) {
            return usable;
        }
        String requiredName = IngredientNormalizer.normalize(required.getIngredientName());
        for (PantryItem item : pantryItems) {
            if (isSameUsableIngredient(item, requiredName, excludeExpired)
                    && UnitConverter.areUnitsCompatible(item.getUnit(), required.getRequiredUnit())) {
                usable.add(item);
            }
        }
        return usable;
    }

    /**
     * Checks one required ingredient against the pantry.
     */
    public static IngredientCheck checkIngredient(RecipeIngredient required,
                                                  List<PantryItem> pantryItems,
                                                  boolean excludeExpired) {
        String requiredUnit = UnitConverter.normalizeUnit(required.getRequiredUnit());

        double available = 0.0;
        for (PantryItem item : findUsablePantryItems(required, pantryItems, excludeExpired)) {
            available += UnitConverter.convert(item.getQuantity(), item.getUnit(), requiredUnit);
        }

        if (available + EPSILON >= required.getRequiredQuantity()) {
            return new IngredientCheck(required, IngredientCheck.Status.AVAILABLE, available, null);
        }
        if (available > 0) {
            return new IngredientCheck(required, IngredientCheck.Status.INSUFFICIENT, available, null);
        }

        // Nothing usable: report whether the ingredient exists but in an unconvertible unit
        String requiredName = IngredientNormalizer.normalize(required.getIngredientName());
        if (pantryItems != null) {
            for (PantryItem item : pantryItems) {
                if (isSameUsableIngredient(item, requiredName, excludeExpired)) {
                    return new IngredientCheck(required, IngredientCheck.Status.INCOMPATIBLE_UNIT,
                            0, item.getFormattedQuantity());
                }
            }
        }
        return new IngredientCheck(required, IngredientCheck.Status.MISSING, 0, null);
    }

    /**
     * Evaluates a single recipe against the user's pantry.
     *
     * @param recipe         Recipe to evaluate (including its required ingredients).
     * @param pantryItems    All current pantry items.
     * @param excludeExpired Whether expired pantry items are ignored.
     */
    public static RecipeMatchResult evaluateRecipe(Recipe recipe,
                                                   List<PantryItem> pantryItems,
                                                   boolean excludeExpired) {
        List<IngredientCheck> checks = new ArrayList<>();
        if (recipe != null && recipe.getIngredients() != null) {
            for (RecipeIngredient required : recipe.getIngredients()) {
                checks.add(checkIngredient(required, pantryItems, excludeExpired));
            }
        }
        return new RecipeMatchResult(recipe, checks);
    }

    /**
     * Returns ONLY recipes whose every ingredient is available in sufficient quantity.
     */
    public static List<RecipeMatchResult> getStrictMatches(List<Recipe> allRecipes,
                                                           List<PantryItem> pantryItems,
                                                           boolean excludeExpired) {
        List<RecipeMatchResult> strictMatches = new ArrayList<>();
        if (allRecipes == null) {
            return strictMatches;
        }
        for (Recipe recipe : allRecipes) {
            RecipeMatchResult result = evaluateRecipe(recipe, pantryItems, excludeExpired);
            if (result.isStrictMatch()) {
                strictMatches.add(result);
            }
        }
        return strictMatches;
    }

    /**
     * Returns recipes missing (or short on) exactly ONE ingredient, for the separate optional
     * "Almost There" list. These are never included in getStrictMatches().
     */
    public static List<RecipeMatchResult> getAlmostThereMatches(List<Recipe> allRecipes,
                                                                List<PantryItem> pantryItems,
                                                                boolean excludeExpired) {
        List<RecipeMatchResult> almostThere = new ArrayList<>();
        if (allRecipes == null) {
            return almostThere;
        }
        for (Recipe recipe : allRecipes) {
            RecipeMatchResult result = evaluateRecipe(recipe, pantryItems, excludeExpired);
            if (result.isAlmostThere()) {
                almostThere.add(result);
            }
        }
        return almostThere;
    }

    private static boolean isSameUsableIngredient(PantryItem item, String requiredName,
                                                  boolean excludeExpired) {
        if (item == null || item.getQuantity() <= 0) {
            return false;
        }
        if (excludeExpired && item.isExpired()) {
            return false;
        }
        // Always normalize from the display name so stored values can never be stale
        return requiredName.equals(IngredientNormalizer.normalize(item.getName()));
    }
}
