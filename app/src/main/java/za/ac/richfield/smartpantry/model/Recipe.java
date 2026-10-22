package za.ac.richfield.smartpantry.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a pre-loaded or stored recipe with its required ingredients
 * and step-by-step preparation method.
 */
public class Recipe implements Serializable {

    private long id;
    private String title;
    private String category;
    private int prepTimeMinutes;
    private int servings;
    private String instructions;
    private List<RecipeIngredient> ingredients;

    public Recipe() {
        this.ingredients = new ArrayList<>();
    }

    public Recipe(long id, String title, String category,
                  int prepTimeMinutes, int servings, String instructions) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.prepTimeMinutes = prepTimeMinutes;
        this.servings = servings;
        this.instructions = instructions;
        this.ingredients = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(int prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = (ingredients != null) ? ingredients : new ArrayList<>();
    }

    public void addIngredient(RecipeIngredient ingredient) {
        if (this.ingredients == null) {
            this.ingredients = new ArrayList<>();
        }
        this.ingredients.add(ingredient);
    }
}
