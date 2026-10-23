package za.ac.richfield.smartpantry.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of comparing a single Recipe against the user's pantry: one IngredientCheck per
 * required ingredient, in the recipe's own order.
 *
 * Strict-matching rule (Section 2.3): a recipe is a strict match if and only if it has at least
 * one required ingredient and EVERY ingredient check is satisfied.
 */
public class RecipeMatchResult {

    private final Recipe recipe;
    private final List<IngredientCheck> checks;
    private final int missingOrInsufficientCount;

    public RecipeMatchResult(Recipe recipe, List<IngredientCheck> checks) {
        this.recipe = recipe;
        this.checks = new ArrayList<>(checks);
        int unsatisfied = 0;
        for (IngredientCheck check : this.checks) {
            if (!check.isSatisfied()) {
                unsatisfied++;
            }
        }
        this.missingOrInsufficientCount = unsatisfied;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    /** True ONLY when every required ingredient is present in at least the required quantity. */
    public boolean isStrictMatch() {
        return !checks.isEmpty() && missingOrInsufficientCount == 0;
    }

    /** Optional stretch goal: exactly one ingredient is missing or short. */
    public boolean isAlmostThere() {
        return !checks.isEmpty() && missingOrInsufficientCount == 1;
    }

    public int getTotalIngredientsCount() {
        return checks.size();
    }

    public int getMatchedIngredientsCount() {
        return checks.size() - missingOrInsufficientCount;
    }

    public int getMissingOrInsufficientCount() {
        return missingOrInsufficientCount;
    }

    /** Every ingredient check, in recipe order. */
    public List<IngredientCheck> getChecks() {
        return Collections.unmodifiableList(checks);
    }

    /** Only the checks that failed (missing, insufficient or incompatible unit). */
    public List<IngredientCheck> getUnsatisfiedChecks() {
        List<IngredientCheck> unsatisfied = new ArrayList<>();
        for (IngredientCheck check : checks) {
            if (!check.isSatisfied()) {
                unsatisfied.add(check);
            }
        }
        return unsatisfied;
    }
}
