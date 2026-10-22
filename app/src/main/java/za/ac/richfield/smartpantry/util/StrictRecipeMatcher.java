package za.ac.richfield.smartpantry.util;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;

/**
 * Core business logic engine implementing Section 2.3 ("The Strict-Matching Rule")
 * of the Mobile App Development 700 Assignment Brief.
 *
 * Rule enforcement:
 * 1. A recipe is included in the main Suggested Recipes list IF AND ONLY IF every single
 *    ingredient it requires is currently present in the user's pantry in at least the
 *    required quantity.
 * 2. If a recipe requires N ingredients and the user has N-1 (or has all N but even one
 *    ingredient has less than the required quantity), it is strictly excluded from the
 *    main suggestions list.
 * 3. Singular/plural variations ("tomato" vs "tomatoes") and compatible unit conversions
 *    ("1 kg" vs "500 g", "1 l" vs "250 ml", "2 tbsp" vs "30 ml") are resolved automatically.
 * 4. Supports a clearly separated "Almost There" bonus filter for recipes missing only 1 ingredient.
 */
public final class StrictRecipeMatcher {

    private static final double EPSILON = 1e-6;

    private StrictRecipeMatcher() {
        // Utility class
    }

    /**
     * Evaluates a single Recipe against the user's current list of PantryItems.
     *
     * @param recipe         The recipe to evaluate (must include its required ingredients).
     * @param pantryItems    All current items in the user's pantry.
     * @param excludeExpired Whether expired pantry items should be ignored during matching.
     * @return Detailed RecipeMatchResult indicating strict match status and per-ingredient breakdown.
     */
    public static RecipeMatchResult evaluateRecipe(Recipe recipe,
                                                   List<PantryItem> pantryItems,
                                                   boolean excludeExpired) {
        List<String> matchedBreakdown = new ArrayList<>();
        List<String> missingBreakdown = new ArrayList<>();

        if (recipe == null || recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return new RecipeMatchResult(recipe, matchedBreakdown, missingBreakdown);
        }

        List<PantryItem> safePantry = (pantryItems != null) ? pantryItems : new ArrayList<>();

        for (RecipeIngredient req : recipe.getIngredients()) {
            String requiredNormName = IngredientNormalizer.normalize(
                    req.getNormalizedName() != null && !req.getNormalizedName().isEmpty()
                            ? req.getNormalizedName()
                            : req.getIngredientName()
            );
            String requiredUnit = UnitConverter.normalizeUnit(req.getRequiredUnit());
            double requiredQty = req.getRequiredQuantity();

            double totalAvailableInReqUnit = 0.0;
            boolean foundNameMatch = false;
            boolean foundIncompatibleUnit = false;
            String incompatibleUnitSeen = "";

            for (PantryItem item : safePantry) {
                if (item == null || item.getQuantity() <= 0) {
                    continue;
                }
                if (excludeExpired && item.isExpired()) {
                    continue;
                }

                String pantryNormName = IngredientNormalizer.normalize(
                        item.getNormalizedName() != null && !item.getNormalizedName().isEmpty()
                                ? item.getNormalizedName()
                                : item.getName()
                );

                if (requiredNormName.equals(pantryNormName)) {
                    foundNameMatch = true;
                    String pantryUnit = UnitConverter.normalizeUnit(item.getUnit());
                    if (UnitConverter.areUnitsCompatible(pantryUnit, requiredUnit)) {
                        totalAvailableInReqUnit += UnitConverter.convert(
                                item.getQuantity(), pantryUnit, requiredUnit);
                    } else {
                        foundIncompatibleUnit = true;
                        incompatibleUnitSeen = item.getFormattedQuantity();
                    }
                }
            }

            if (foundNameMatch && (totalAvailableInReqUnit + EPSILON) >= requiredQty) {
                matchedBreakdown.add(String.format(
                        "%s: requires %s %s (You have %s %s)",
                        req.getIngredientName(),
                        PantryItem.formatQuantityValue(requiredQty),
                        requiredUnit,
                        PantryItem.formatQuantityValue(totalAvailableInReqUnit),
                        requiredUnit
                ));
            } else if (foundNameMatch && totalAvailableInReqUnit > 0) {
                double deficit = Math.max(0.0, requiredQty - totalAvailableInReqUnit);
                missingBreakdown.add(String.format(
                        "%s: requires %s %s, have %s %s (short by %s %s)",
                        req.getIngredientName(),
                        PantryItem.formatQuantityValue(requiredQty),
                        requiredUnit,
                        PantryItem.formatQuantityValue(totalAvailableInReqUnit),
                        requiredUnit,
                        PantryItem.formatQuantityValue(deficit),
                        requiredUnit
                ));
            } else if (foundIncompatibleUnit) {
                missingBreakdown.add(String.format(
                        "%s: requires %s %s, but pantry unit (%s) is incompatible",
                        req.getIngredientName(),
                        PantryItem.formatQuantityValue(requiredQty),
                        requiredUnit,
                        incompatibleUnitSeen
                ));
            } else {
                missingBreakdown.add(String.format(
                        "%s: missing from pantry (requires %s %s)",
                        req.getIngredientName(),
                        PantryItem.formatQuantityValue(requiredQty),
                        requiredUnit
                ));
            }
        }

        return new RecipeMatchResult(recipe, matchedBreakdown, missingBreakdown);
    }

    /**
     * Filters all recipes and returns ONLY those that satisfy 100% of their required ingredients
     * in at least the required quantities (Section 2.3 Strict-Matching Rule).
     */
    public static List<RecipeMatchResult> getStrictMatches(List<Recipe> allRecipes,
                                                           List<PantryItem> pantryItems,
                                                           boolean excludeExpired) {
        List<RecipeMatchResult> strictMatches = new ArrayList<>();
        if (allRecipes == null || allRecipes.isEmpty()) {
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
     * Returns recipes that are missing (or have insufficient quantity for) ONLY 1 ingredient.
     * Used strictly for the optional separated "Almost There" bonus section (Section 2.3).
     */
    public static List<RecipeMatchResult> getAlmostThereMatches(List<Recipe> allRecipes,
                                                                List<PantryItem> pantryItems,
                                                                boolean excludeExpired) {
        List<RecipeMatchResult> almostThere = new ArrayList<>();
        if (allRecipes == null || allRecipes.isEmpty()) {
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
}
