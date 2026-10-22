package za.ac.richfield.smartpantry;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;
import za.ac.richfield.smartpantry.util.IngredientNormalizer;
import za.ac.richfield.smartpantry.util.InputValidator;
import za.ac.richfield.smartpantry.util.StrictRecipeMatcher;
import za.ac.richfield.smartpantry.util.UnitConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Automated JUnit 4 test suite verifying the Strict-Matching Rule (Section 2.3),
 * singular/plural normalization, unit conversions, empty-pantry handling, and input validation.
 */
public class StrictRecipeMatcherTest {

    private Recipe fiveIngredientRecipe;
    private Recipe toastieRecipe;

    @Before
    public void setUp() {
        // 5-ingredient Scrambled Eggs on Toast recipe
        fiveIngredientRecipe = new Recipe(1, "Classic Scrambled Eggs on Toast", "Breakfast", 10, 1, "Steps...");
        fiveIngredientRecipe.addIngredient(new RecipeIngredient("Eggs", "egg", 2, "pcs"));
        fiveIngredientRecipe.addIngredient(new RecipeIngredient("Milk", "milk", 30, "ml"));
        fiveIngredientRecipe.addIngredient(new RecipeIngredient("Butter", "butter", 15, "g"));
        fiveIngredientRecipe.addIngredient(new RecipeIngredient("Bread", "bread", 2, "slice"));
        fiveIngredientRecipe.addIngredient(new RecipeIngredient("Salt", "salt", 2, "g"));

        // 3-ingredient Grilled Cheese Toastie
        toastieRecipe = new Recipe(2, "Golden Grilled Cheese Toastie", "Lunch", 10, 1, "Steps...");
        toastieRecipe.addIngredient(new RecipeIngredient("Bread", "bread", 2, "slice"));
        toastieRecipe.addIngredient(new RecipeIngredient("Cheddar Cheese", "cheddar cheese", 60, "g"));
        toastieRecipe.addIngredient(new RecipeIngredient("Butter", "butter", 15, "g"));
    }

    @Test
    public void testStrictMatch_AllFiveIngredientsSufficient_ReturnsTrue() {
        List<PantryItem> pantry = Arrays.asList(
                new PantryItem("Large Eggs", "egg", 6, "pcs", null),
                new PantryItem("Milk", "milk", 1, "l", null),        // 1 L = 1000 ml >= 30 ml
                new PantryItem("Butter", "butter", 0.25, "kg", null), // 0.25 kg = 250 g >= 15 g
                new PantryItem("Bread Slices", "bread", 4, "slice", null),
                new PantryItem("Salt", "salt", 50, "g", null)
        );

        RecipeMatchResult result = StrictRecipeMatcher.evaluateRecipe(fiveIngredientRecipe, pantry, true);
        assertTrue("Recipe must strictly match when all 5 ingredients are sufficient", result.isStrictMatch());
        assertEquals(5, result.getMatchedIngredientsCount());
        assertEquals(0, result.getMissingOrInsufficientCount());
    }

    @Test
    public void testStrictMatch_FourOfFiveIngredientsPresent_MustBeExcluded() {
        // Missing "Salt" (user has 4 out of 5 required ingredients)
        List<PantryItem> pantry = Arrays.asList(
                new PantryItem("Eggs", "egg", 6, "pcs", null),
                new PantryItem("Milk", "milk", 250, "ml", null),
                new PantryItem("Butter", "butter", 100, "g", null),
                new PantryItem("Bread", "bread", 4, "slice", null)
        );

        List<RecipeMatchResult> strictSuggestions = StrictRecipeMatcher.getStrictMatches(
                Collections.singletonList(fiveIngredientRecipe), pantry, true
        );
        assertTrue("4-of-5 partial match MUST NOT appear in strict suggestions", strictSuggestions.isEmpty());

        RecipeMatchResult evaluation = StrictRecipeMatcher.evaluateRecipe(fiveIngredientRecipe, pantry, true);
        assertFalse(evaluation.isStrictMatch());
        assertTrue("Recipe missing exactly 1 ingredient should qualify for Almost There", evaluation.isAlmostThere());
    }

    @Test
    public void testStrictMatch_AllIngredientsPresentButOneInsufficientQuantity_MustBeExcluded() {
        // Needs 60 g Cheddar Cheese, but user only has 40 g
        List<PantryItem> pantry = Arrays.asList(
                new PantryItem("Bread", "bread", 2, "slice", null),
                new PantryItem("Cheddar Cheese", "cheddar cheese", 40, "g", null),
                new PantryItem("Butter", "butter", 50, "g", null)
        );

        RecipeMatchResult result = StrictRecipeMatcher.evaluateRecipe(toastieRecipe, pantry, true);
        assertFalse("Insufficient quantity must be excluded from strict matches", result.isStrictMatch());
        assertEquals(1, result.getMissingOrInsufficientCount());
    }

    @Test
    public void testEmptyPantry_ReturnsZeroMatches() {
        List<Recipe> recipes = Arrays.asList(fiveIngredientRecipe, toastieRecipe);
        List<RecipeMatchResult> matches = StrictRecipeMatcher.getStrictMatches(
                recipes, new ArrayList<>(), true
        );
        assertEquals(0, matches.size());
    }

    @Test
    public void testIngredientNormalizer_SingularPluralAndSynonyms() {
        assertEquals("tomato", IngredientNormalizer.normalize("Tomatoes"));
        assertEquals("tomato", IngredientNormalizer.normalize("  Fresh Tomato "));
        assertEquals("potato", IngredientNormalizer.normalize("Potatoes"));
        assertEquals("berry", IngredientNormalizer.normalize("Berries"));
        assertEquals("loaf", IngredientNormalizer.normalize("Loaves"));
        assertEquals("egg", IngredientNormalizer.normalize("Large Eggs"));
        assertEquals("beef mince", IngredientNormalizer.normalize("Ground Beef"));
        assertEquals("maize meal", IngredientNormalizer.normalize("Mealie Meal"));
    }

    @Test
    public void testUnitConverter_MassAndVolumeConversions() {
        assertEquals(1000.0, UnitConverter.convert(1.0, "kg", "g"), 1e-4);
        assertEquals(0.5, UnitConverter.convert(500.0, "ml", "l"), 1e-4);
        assertEquals(250.0, UnitConverter.convert(1.0, "cup", "ml"), 1e-4);
        assertEquals(30.0, UnitConverter.convert(2.0, "tbsp", "ml"), 1e-4);
        assertTrue(UnitConverter.hasSufficientQuantity(0.5, "kg", 250, "g"));
        assertFalse(UnitConverter.hasSufficientQuantity(0.1, "l", 250, "ml"));
    }

    @Test
    public void testInputValidator_RejectsInvalidAndAcceptsValidInput() {
        assertFalse(InputValidator.validatePantryInput("", "2", "pcs", null).isValid());
        assertFalse(InputValidator.validatePantryInput("Eggs", "-3", "pcs", null).isValid());
        assertFalse(InputValidator.validatePantryInput("Eggs", "abc", "pcs", null).isValid());
        assertFalse(InputValidator.validatePantryInput("Eggs", "2", "pcs", "2026-02-30").isValid());
        assertTrue(InputValidator.validatePantryInput("Tomatoes", "4", "pcs", "2026-10-25").isValid());
    }
}
