package za.ac.richfield.smartpantry.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the detailed evaluation result of comparing a single Recipe against the user's pantry.
 * Enforces the assignment's strict-matching rule: a recipe is a strict match if and only if
 * totalIngredientsCount > 0 and missingOrInsufficientCount == 0.
 */
public class RecipeMatchResult implements Serializable {

    private final Recipe recipe;
    private final int totalIngredientsCount;
    private final int matchedIngredientsCount;
    private final int missingOrInsufficientCount;
    private final List<String> matchedBreakdown;
    private final List<String> missingBreakdown;

    public RecipeMatchResult(Recipe recipe,
                             List<String> matchedBreakdown,
                             List<String> missingBreakdown) {
        this.recipe = recipe;
        this.matchedBreakdown = new ArrayList<>(matchedBreakdown);
        this.missingBreakdown = new ArrayList<>(missingBreakdown);
        this.matchedIngredientsCount = this.matchedBreakdown.size();
        this.missingOrInsufficientCount = this.missingBreakdown.size();
        this.totalIngredientsCount = this.matchedIngredientsCount + this.missingIngredientsCount();
    }

    private int missingIngredientsCount() {
        return this.missingOrInsufficientCount;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    /**
     * Strict-matching predicate (Section 2.3):
     * Returns true ONLY when every single required ingredient is present in the pantry
     * in at least the required quantity.
     */
    public boolean isStrictMatch() {
        return totalIngredientsCount > 0 && missingOrInsufficientCount == 0;
    }

    /**
     * Optional stretch predicate (Section 2.3 & Section 8):
     * Returns true when a recipe is missing (or short on) exactly 1 required ingredient.
     */
    public boolean isAlmostThere() {
        return totalIngredientsCount > 0 && missingOrInsufficientCount == 1;
    }

    public int getTotalIngredientsCount() {
        return totalIngredientsCount;
    }

    public int getMatchedIngredientsCount() {
        return matchedIngredientsCount;
    }

    public int getMissingOrInsufficientCount() {
        return missingOrInsufficientCount;
    }

    public List<String> getMatchedBreakdown() {
        return Collections.unmodifiableList(matchedBreakdown);
    }

    public List<String> getMissingBreakdown() {
        return Collections.unmodifiableList(missingBreakdown);
    }
}
