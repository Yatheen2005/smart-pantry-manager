package za.ac.richfield.smartpantry;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import za.ac.richfield.smartpantry.model.IngredientCheck;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;
import za.ac.richfield.smartpantry.util.StrictRecipeMatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * JVM unit tests for the Strict-Matching Rule (Section 2.3).
 */
public class StrictRecipeMatcherTest {

    private static final String FUTURE = "2999-12-31";
    private static final String PAST = "2000-01-01";

    private Recipe scrambledEggs; // 5 ingredients
    private Recipe toastie;       // 3 ingredients

    @Before
    public void setUp() {
        scrambledEggs = recipe("Scrambled Eggs",
                ing("Eggs", 2, "pcs"),
                ing("Milk", 30, "ml"),
                ing("Butter", 15, "g"),
                ing("Bread", 2, "slice"),
                ing("Salt", 2, "g"));
        toastie = recipe("Toastie",
                ing("Bread", 2, "slice"),
                ing("Cheddar Cheese", 60, "g"),
                ing("Butter", 15, "g"));
    }

    // ---------------------------------------------------------------- full / partial matches

    @Test
    public void allIngredientsSufficient_withUnitConversionAndPlurals_isStrictMatch() {
        List<PantryItem> pantry = Arrays.asList(
                item("Large Eggs", 6, "pcs"),
                item("Milk", 1, "l"),          // 1000 ml >= 30 ml
                item("Butter", 0.25, "kg"),    // 250 g >= 15 g
                item("Bread Slices", 4, "slice"),
                item("Salt", 50, "g"));

        RecipeMatchResult result = StrictRecipeMatcher.evaluateRecipe(scrambledEggs, pantry, true);

        assertTrue(result.isStrictMatch());
        assertEquals(5, result.getMatchedIngredientsCount());
        assertEquals(0, result.getMissingOrInsufficientCount());
    }

    @Test
    public void fourOfFiveIngredients_isExcludedFromSuggestions() {
        List<PantryItem> pantry = Arrays.asList( // no salt
                item("Eggs", 6, "pcs"),
                item("Milk", 250, "ml"),
                item("Butter", 100, "g"),
                item("Bread", 4, "slice"));

        List<RecipeMatchResult> suggestions = StrictRecipeMatcher.getStrictMatches(
                Collections.singletonList(scrambledEggs), pantry, true);
        RecipeMatchResult result = StrictRecipeMatcher.evaluateRecipe(scrambledEggs, pantry, true);

        assertTrue("4 of 5 must NOT be suggested", suggestions.isEmpty());
        assertFalse(result.isStrictMatch());
        assertTrue(result.isAlmostThere());
        assertEquals(IngredientCheck.Status.MISSING, result.getUnsatisfiedChecks().get(0).getStatus());
        assertEquals("Salt", result.getUnsatisfiedChecks().get(0).getIngredient().getIngredientName());
    }

    @Test
    public void oneIngredientShortByQuantity_isExcluded() {
        List<PantryItem> pantry = Arrays.asList(
                item("Bread", 2, "slice"),
                item("Cheddar Cheese", 40, "g"), // needs 60 g
                item("Butter", 50, "g"));

        RecipeMatchResult result = StrictRecipeMatcher.evaluateRecipe(toastie, pantry, true);

        assertFalse(result.isStrictMatch());
        IngredientCheck cheese = result.getUnsatisfiedChecks().get(0);
        assertEquals(IngredientCheck.Status.INSUFFICIENT, cheese.getStatus());
        assertEquals(40.0, cheese.getAvailableQuantity(), 1e-9);
    }

    @Test
    public void tinyShortfallAfterConversion_isStillExcluded() {
        // 0.059 kg = 59 g, one gram short of 60 g
        List<PantryItem> pantry = Arrays.asList(
                item("Bread", 2, "slice"),
                item("Cheddar Cheese", 0.059, "kg"),
                item("Butter", 15, "g"));
        assertFalse(StrictRecipeMatcher.evaluateRecipe(toastie, pantry, true).isStrictMatch());
    }

    @Test
    public void exactlyTheRequiredQuantity_isEnough() {
        List<PantryItem> pantry = Arrays.asList(
                item("Bread", 2, "slice"),
                item("Cheddar Cheese", 0.06, "kg"), // exactly 60 g (floating point safe)
                item("Butter", 15, "g"));
        assertTrue(StrictRecipeMatcher.evaluateRecipe(toastie, pantry, true).isStrictMatch());
    }

    @Test
    public void emptyPantry_returnsNoSuggestions() {
        List<RecipeMatchResult> matches = StrictRecipeMatcher.getStrictMatches(
                Arrays.asList(scrambledEggs, toastie), new ArrayList<>(), true);
        assertTrue(matches.isEmpty());
    }

    @Test
    public void recipeWithNoIngredients_isNeverAMatch() {
        Recipe empty = recipe("Nothing");
        assertFalse(StrictRecipeMatcher.evaluateRecipe(empty, toastiePantry(), true).isStrictMatch());
    }

    // ---------------------------------------------------------------- duplicates

    @Test
    public void duplicatePantryRows_areAddedTogether() {
        Recipe needsTwoEggs = recipe("Boiled eggs", ing("Eggs", 2, "pcs"));

        List<PantryItem> oneRow = Collections.singletonList(item("Egg", 1, "pcs"));
        List<PantryItem> twoRows = Arrays.asList(item("Egg", 1, "pcs"), item("Eggs", 1, "pcs"));

        assertFalse(StrictRecipeMatcher.evaluateRecipe(needsTwoEggs, oneRow, true).isStrictMatch());
        assertTrue(StrictRecipeMatcher.evaluateRecipe(needsTwoEggs, twoRows, true).isStrictMatch());
    }

    @Test
    public void duplicateRowsInDifferentCompatibleUnits_areConvertedAndAdded() {
        Recipe needsButter = recipe("Butter sauce", ing("Butter", 200, "g"));
        List<PantryItem> pantry = Arrays.asList(item("Butter", 100, "g"), item("Butter", 0.1, "kg"));
        assertTrue(StrictRecipeMatcher.evaluateRecipe(needsButter, pantry, true).isStrictMatch());
    }

    // ---------------------------------------------------------------- units

    @Test
    public void massVersusVolume_isNotConverted() {
        // 2 cups of flour is roughly 250 g, so a generic 1 g = 1 ml rule (500 g) would be wrong
        Recipe needsFlour = recipe("Pancakes", ing("Flour", 200, "g"));
        List<PantryItem> pantry = Collections.singletonList(item("Flour", 2, "cup"));

        RecipeMatchResult result = StrictRecipeMatcher.evaluateRecipe(needsFlour, pantry, true);

        assertFalse(result.isStrictMatch());
        assertEquals(IngredientCheck.Status.INCOMPATIBLE_UNIT, result.getChecks().get(0).getStatus());
    }

    @Test
    public void differentCountUnits_areNotInterchangeable() {
        Recipe needsCloves = recipe("Garlic bread", ing("Garlic", 2, "clove"));
        List<PantryItem> pantry = Collections.singletonList(item("Garlic", 5, "pcs"));
        assertFalse(StrictRecipeMatcher.evaluateRecipe(needsCloves, pantry, true).isStrictMatch());
    }

    @Test
    public void spoonAndCupMeasures_convertToMillilitres() {
        Recipe needsOil = recipe("Stir fry", ing("Cooking Oil", 30, "ml"));
        assertTrue(StrictRecipeMatcher.evaluateRecipe(needsOil,
                Collections.singletonList(item("Sunflower Oil", 2, "tbsp")), true).isStrictMatch());
        assertFalse(StrictRecipeMatcher.evaluateRecipe(needsOil,
                Collections.singletonList(item("Cooking Oil", 5, "tsp")), true).isStrictMatch()); // 25 ml
    }

    // ---------------------------------------------------------------- names

    @Test
    public void nameVariations_matchTheSameIngredient() {
        Recipe relish = recipe("Relish",
                ing("Tomatoes", 3, "pcs"),
                ing("Onion", 1, "pcs"),
                ing("Garlic", 2, "clove"),
                ing("Maize Meal", 100, "g"));
        List<PantryItem> pantry = Arrays.asList(
                item("fresh TOMATO", 3, "pcs"),
                item("Red Onions", 1, "pcs"),
                item("Garlic Cloves", 2, "clove"),
                item("Mielie Meal", 1, "kg"));
        assertTrue(StrictRecipeMatcher.evaluateRecipe(relish, pantry, true).isStrictMatch());
    }

    @Test
    public void noSubstringFalsePositives() {
        Recipe omelette = recipe("Omelette", ing("Eggs", 2, "pcs"));
        Recipe toast = recipe("Cheese toast", ing("Cheddar Cheese", 50, "g"));
        Recipe stew = recipe("Stew", ing("Bell Pepper", 1, "pcs"));

        assertFalse(StrictRecipeMatcher.evaluateRecipe(omelette,
                Collections.singletonList(item("Eggplant", 5, "pcs")), true).isStrictMatch());
        assertFalse(StrictRecipeMatcher.evaluateRecipe(toast,
                Collections.singletonList(item("Cheese", 500, "g")), true).isStrictMatch());
        assertFalse(StrictRecipeMatcher.evaluateRecipe(stew,
                Collections.singletonList(item("Black Pepper", 50, "pcs")), true).isStrictMatch());
    }

    // ---------------------------------------------------------------- expiry and zero stock

    @Test
    public void expiredItems_areIgnoredOnlyWhenSettingIsOn() {
        Recipe needsMilk = recipe("Milkshake", ing("Milk", 250, "ml"));
        List<PantryItem> pantry = Collections.singletonList(
                new PantryItem("Milk", null, 1, "l", PAST));

        assertFalse(StrictRecipeMatcher.evaluateRecipe(needsMilk, pantry, true).isStrictMatch());
        assertTrue(StrictRecipeMatcher.evaluateRecipe(needsMilk, pantry, false).isStrictMatch());
    }

    @Test
    public void zeroQuantityItems_doNotCount() {
        Recipe needsSalt = recipe("Salted water", ing("Salt", 1, "g"));
        assertFalse(StrictRecipeMatcher.evaluateRecipe(needsSalt,
                Collections.singletonList(item("Salt", 0, "g")), true).isStrictMatch());
    }

    // ---------------------------------------------------------------- almost there list

    @Test
    public void almostThereList_isSeparateFromStrictMatches() {
        Recipe missingTwo = recipe("Missing two", ing("Saffron", 1, "g"), ing("Truffle", 1, "g"));
        List<Recipe> recipes = Arrays.asList(toastie, scrambledEggs, missingTwo);
        List<PantryItem> pantry = toastiePantry(); // toastie complete, eggs recipe missing several

        List<RecipeMatchResult> strict = StrictRecipeMatcher.getStrictMatches(recipes, pantry, true);
        List<RecipeMatchResult> almost = StrictRecipeMatcher.getAlmostThereMatches(recipes, pantry, true);

        assertEquals(1, strict.size());
        assertEquals("Toastie", strict.get(0).getRecipe().getTitle());
        for (RecipeMatchResult result : almost) {
            assertFalse("An 'almost there' recipe must never be a strict match", result.isStrictMatch());
            assertEquals(1, result.getMissingOrInsufficientCount());
        }
    }

    // ---------------------------------------------------------------- helpers

    private List<PantryItem> toastiePantry() {
        return Arrays.asList(item("Bread", 2, "slice"), item("Cheddar", 60, "g"), item("Butter", 15, "g"));
    }

    private static Recipe recipe(String title, RecipeIngredient... ingredients) {
        Recipe recipe = new Recipe(title.hashCode(), title, "Test", 10, 1, "Steps");
        for (RecipeIngredient ingredient : ingredients) {
            recipe.addIngredient(ingredient);
        }
        return recipe;
    }

    private static RecipeIngredient ing(String name, double qty, String unit) {
        return new RecipeIngredient(name, null, qty, unit);
    }

    private static PantryItem item(String name, double qty, String unit) {
        return new PantryItem(name, null, qty, unit, FUTURE);
    }
}
