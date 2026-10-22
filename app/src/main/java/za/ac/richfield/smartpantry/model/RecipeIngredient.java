package za.ac.richfield.smartpantry.model;

import java.io.Serializable;

/**
 * Represents a single required ingredient and its minimum quantity for a Recipe.
 */
public class RecipeIngredient implements Serializable {

    private long id;
    private long recipeId;
    private String ingredientName;
    private String normalizedName;
    private double requiredQuantity;
    private String requiredUnit;

    public RecipeIngredient() {
    }

    public RecipeIngredient(long id, long recipeId, String ingredientName,
                            String normalizedName, double requiredQuantity, String requiredUnit) {
        this.id = id;
        this.recipeId = recipeId;
        this.ingredientName = ingredientName;
        this.normalizedName = normalizedName;
        this.requiredQuantity = requiredQuantity;
        this.requiredUnit = requiredUnit;
    }

    public RecipeIngredient(String ingredientName, String normalizedName,
                            double requiredQuantity, String requiredUnit) {
        this(0, 0, ingredientName, normalizedName, requiredQuantity, requiredUnit);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public double getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(double requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public String getRequiredUnit() {
        return requiredUnit;
    }

    public void setRequiredUnit(String requiredUnit) {
        this.requiredUnit = requiredUnit;
    }

    public String getFormattedRequirement() {
        return PantryItem.formatQuantityValue(requiredQuantity) + " " + requiredUnit + " " + ingredientName;
    }
}
