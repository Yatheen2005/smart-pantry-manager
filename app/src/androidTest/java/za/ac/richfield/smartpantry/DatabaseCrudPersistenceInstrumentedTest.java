package za.ac.richfield.smartpantry;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Runs on a device/emulator against a real SQLite file. Uses a separate database file so the
 * app's real pantry is never touched.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseCrudPersistenceInstrumentedTest {

    private static final String TEST_DB = "test_smart_pantry.db";

    private Context context;
    private PantryDatabaseHelper dbHelper;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(TEST_DB);
        dbHelper = new PantryDatabaseHelper(context, TEST_DB);
    }

    @After
    public void tearDown() {
        dbHelper.close();
        context.deleteDatabase(TEST_DB);
    }

    @Test
    public void recipesAreSeededOnFirstCreation() {
        List<Recipe> recipes = dbHelper.getAllRecipesWithIngredients();
        assertEquals(18, recipes.size());
        assertEquals(18, dbHelper.getRecipeCount());
        for (Recipe recipe : recipes) {
            assertFalse(recipe.getTitle().trim().isEmpty());
            assertFalse(recipe.getTitle(), recipe.getIngredients().isEmpty());
        }
        Recipe first = dbHelper.getRecipeById(recipes.get(0).getId());
        assertNotNull(first);
        assertEquals(recipes.get(0).getIngredients().size(), first.getIngredients().size());
    }

    @Test
    public void fullCrudCycle_persistsAcrossClosingAndReopeningTheDatabase() {
        // CREATE
        long id = dbHelper.insertPantryItem(new PantryItem("Fresh Tomatoes", null, 5, "pcs", "2026-11-01"));
        assertTrue(id > 0);

        // Close the connection and open a brand-new helper on the same file
        dbHelper.close();
        dbHelper = new PantryDatabaseHelper(context, TEST_DB);

        // READ
        PantryItem fetched = dbHelper.getPantryItemById(id);
        assertNotNull(fetched);
        assertEquals("Fresh Tomatoes", fetched.getName());
        assertEquals("tomato", fetched.getNormalizedName());
        assertEquals(5.0, fetched.getQuantity(), 1e-9);
        assertEquals("2026-11-01", fetched.getExpiryDate());
        assertEquals(1, dbHelper.getAllPantryItems().size());

        // UPDATE (also re-normalizes the name)
        fetched.setName("Cherry Tomatoes");
        fetched.setQuantity(12);
        assertEquals(1, dbHelper.updatePantryItem(fetched));
        dbHelper.close();
        dbHelper = new PantryDatabaseHelper(context, TEST_DB);
        PantryItem updated = dbHelper.getPantryItemById(id);
        assertEquals(12.0, updated.getQuantity(), 1e-9);
        assertEquals("cherry tomato", updated.getNormalizedName());

        // DELETE
        assertEquals(1, dbHelper.deletePantryItem(id));
        assertNull(dbHelper.getPantryItemById(id));
        assertEquals(0, dbHelper.deletePantryItem(id));
    }

    @Test
    public void optionalExpiryDate_isStoredAsNull() {
        long id = dbHelper.insertPantryItem(new PantryItem("Salt", null, 200, "g", "  "));
        assertFalse(dbHelper.getPantryItemById(id).hasExpiryDate());
    }

    @Test
    public void findByName_usesNormalizedNames() {
        dbHelper.insertPantryItem(new PantryItem("Fresh Tomatoes", null, 2, "pcs", null));
        dbHelper.insertPantryItem(new PantryItem("Onion", null, 1, "pcs", null));
        assertEquals(1, dbHelper.findPantryItemsByName("tomato").size());
        assertEquals(0, dbHelper.findPantryItemsByName("Cherry").size());
    }

    @Test
    public void cooking_deductsIngredientsAndDeletesUsedUpRows() {
        dbHelper.loadSamplePantry();
        Recipe toastie = findRecipe("Golden Grilled Cheese Toastie"); // 2 slice bread, 60 g cheddar, 15 g butter

        assertTrue(dbHelper.consumeRecipeIngredients(toastie, true));

        assertEquals(6.0, quantityOf("Bread Slices"), 1e-9);    // 8 - 2
        assertEquals(140.0, quantityOf("Cheddar Cheese"), 1e-9); // 200 - 60
        assertEquals(235.0, quantityOf("Butter"), 1e-9);         // 250 - 15
    }

    @Test
    public void cooking_convertsUnitsWhenDeducting() {
        dbHelper.loadSamplePantry(); // milk is stored as 1 l
        Recipe pancakes = findRecipe("Fluffy Homemade Pancakes"); // needs 250 ml milk, 200 g flour

        assertTrue(dbHelper.consumeRecipeIngredients(pancakes, true));

        assertEquals(0.75, quantityOf("Full Cream Milk"), 1e-9); // 1 l - 250 ml
        assertEquals(0.8, quantityOf("Cake Flour"), 1e-9);       // 1 kg - 200 g
    }

    @Test
    public void cooking_isRefusedAndChangesNothingWhenRecipeIsNotAStrictMatch() {
        dbHelper.insertPantryItem(new PantryItem("Bread", null, 2, "slice", null));
        dbHelper.insertPantryItem(new PantryItem("Butter", null, 15, "g", null)); // no cheese
        Recipe toastie = findRecipe("Golden Grilled Cheese Toastie");

        assertFalse(dbHelper.consumeRecipeIngredients(toastie, true));
        assertEquals(2.0, quantityOf("Bread"), 1e-9);
        assertEquals(15.0, quantityOf("Butter"), 1e-9);
    }

    @Test
    public void cooking_usesUpRowsThatReachZero() {
        dbHelper.insertPantryItem(new PantryItem("Bread", null, 2, "slice", null));
        dbHelper.insertPantryItem(new PantryItem("Cheddar", null, 60, "g", null));
        dbHelper.insertPantryItem(new PantryItem("Butter", null, 100, "g", null));

        assertTrue(dbHelper.consumeRecipeIngredients(findRecipe("Golden Grilled Cheese Toastie"), true));

        List<PantryItem> left = dbHelper.getAllPantryItems();
        assertEquals(1, left.size());
        assertEquals("Butter", left.get(0).getName());
    }

    @Test
    public void databaseUpgrade_keepsTheUsersPantry() {
        dbHelper.insertPantryItem(new PantryItem("Eggs", null, 6, "pcs", null));

        dbHelper.onUpgrade(dbHelper.getWritableDatabase(), 1, 2);

        assertEquals(1, dbHelper.getAllPantryItems().size());
        assertEquals(18, dbHelper.getRecipeCount());
    }

    private Recipe findRecipe(String title) {
        for (Recipe recipe : dbHelper.getAllRecipesWithIngredients()) {
            if (recipe.getTitle().equals(title)) {
                return recipe;
            }
        }
        throw new AssertionError("Recipe not seeded: " + title);
    }

    private double quantityOf(String name) {
        for (PantryItem item : dbHelper.getAllPantryItems()) {
            if (item.getName().equals(name)) {
                return item.getQuantity();
            }
        }
        throw new AssertionError("Not in pantry: " + name);
    }
}
