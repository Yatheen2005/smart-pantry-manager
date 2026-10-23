package za.ac.richfield.smartpantry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import za.ac.richfield.smartpantry.database.RecipeSeeder;
import za.ac.richfield.smartpantry.database.SamplePantryData;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;
import za.ac.richfield.smartpantry.util.IngredientNormalizer;
import za.ac.richfield.smartpantry.util.StrictRecipeMatcher;
import za.ac.richfield.smartpantry.util.UnitConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks the 18 seeded recipes and the demo pantry against the real matching code.
 */
public class SeededRecipesTest {

    private final List<Recipe> recipes = RecipeSeeder.buildPreloadedRecipes();

    @Test
    public void between15And20RecipesWithUniqueTitles() {
        assertTrue(recipes.size() >= 15 && recipes.size() <= 20);
        assertEquals(18, recipes.size());
        Set<String> titles = new HashSet<>();
        for (Recipe recipe : recipes) {
            assertTrue("Duplicate title: " + recipe.getTitle(), titles.add(recipe.getTitle()));
        }
    }

    @Test
    public void everyRecipeHasIngredientsStepsAndValidUnits() {
        for (Recipe recipe : recipes) {
            assertFalse(recipe.getTitle(), recipe.getIngredients().isEmpty());
            assertFalse(recipe.getTitle(), recipe.getInstructions().trim().isEmpty());
            assertTrue("Steps should be on separate lines: " + recipe.getTitle(),
                    recipe.getInstructions().contains("\n"));
            for (RecipeIngredient ing : recipe.getIngredients()) {
                assertTrue(ing.getIngredientName(), ing.getRequiredQuantity() > 0);
                assertTrue(ing.getRequiredUnit(), UnitConverter.isValidUnit(ing.getRequiredUnit()));
                assertFalse(IngredientNormalizer.normalize(ing.getIngredientName()).isEmpty());
            }
        }
    }

    /**
     * For EVERY recipe: exactly its own ingredients is a strict match; removing any single
     * ingredient, or having slightly too little of any single ingredient, is not.
     */
    @Test
    public void everyRecipe_matchesOnlyWhenEveryIngredientIsFullyAvailable() {
        for (Recipe recipe : recipes) {
            List<RecipeIngredient> needed = recipe.getIngredients();
            assertTrue(recipe.getTitle(),
                    StrictRecipeMatcher.evaluateRecipe(recipe, pantryFor(needed, -1, 1.0), true).isStrictMatch());

            for (int i = 0; i < needed.size(); i++) {
                String context = recipe.getTitle() + " / " + needed.get(i).getIngredientName();
                assertFalse("missing " + context, StrictRecipeMatcher.evaluateRecipe(
                        recipe, pantryFor(needed, i, 0.0), true).isStrictMatch());
                assertFalse("short " + context, StrictRecipeMatcher.evaluateRecipe(
                        recipe, pantryFor(needed, i, 0.99), true).isStrictMatch());
            }
        }
    }

    @Test
    public void samplePantry_givesTheDocumentedSuggestions() {
        List<PantryItem> pantry = SamplePantryData.build(Calendar.getInstance());

        List<String> strict = titles(StrictRecipeMatcher.getStrictMatches(recipes, pantry, true));
        List<RecipeMatchResult> almost = StrictRecipeMatcher.getAlmostThereMatches(recipes, pantry, true);

        assertEquals(6, strict.size());
        assertTrue(strict.contains("Classic Scrambled Eggs on Toast"));
        assertTrue(strict.contains("Golden Grilled Cheese Toastie"));
        assertTrue(strict.contains("Tomato & Onion Braai Relish (Smoor)"));
        assertTrue(strict.contains("Fluffy Homemade Pancakes"));
        assertTrue(strict.contains("Cheddar & Tomato Omelette"));
        assertTrue(strict.contains("Welsh Rarebit Style Cheese Sauce on Toast"));
        assertEquals(9, almost.size());
    }

    /** The scenario used in the video: adding sugar unlocks French Toast, removing it hides it. */
    @Test
    public void addingTheOneMissingIngredient_makesARecipeAppear() {
        List<PantryItem> pantry = new ArrayList<>(SamplePantryData.build(Calendar.getInstance()));
        assertFalse(titles(StrictRecipeMatcher.getStrictMatches(recipes, pantry, true))
                .contains("Classic French Toast"));

        pantry.add(new PantryItem("Sugar", null, 100, "g", null));
        assertTrue(titles(StrictRecipeMatcher.getStrictMatches(recipes, pantry, true))
                .contains("Classic French Toast"));
    }

    @Test
    public void samplePantryExpiryDates_areRelativeToToday() {
        for (PantryItem item : SamplePantryData.build(Calendar.getInstance())) {
            if (item.getName().equals("Plain Yoghurt")) {
                assertTrue(item.isExpired());
            } else {
                assertFalse(item.getName() + " should not be expired", item.isExpired());
            }
        }
    }

    /**
     * Builds a pantry containing each needed ingredient. The ingredient at changedIndex is
     * scaled by factor (0 = left out entirely).
     */
    private static List<PantryItem> pantryFor(List<RecipeIngredient> needed, int changedIndex, double factor) {
        List<PantryItem> pantry = new ArrayList<>();
        for (int i = 0; i < needed.size(); i++) {
            RecipeIngredient ing = needed.get(i);
            double qty = ing.getRequiredQuantity() * (i == changedIndex ? factor : 1.0);
            if (qty > 0) {
                pantry.add(new PantryItem(ing.getIngredientName(), null, qty, ing.getRequiredUnit(), null));
            }
        }
        return pantry;
    }

    private static List<String> titles(List<RecipeMatchResult> results) {
        List<String> titles = new ArrayList<>();
        for (RecipeMatchResult result : results) {
            titles.add(result.getRecipe().getTitle());
        }
        return titles;
    }
}
