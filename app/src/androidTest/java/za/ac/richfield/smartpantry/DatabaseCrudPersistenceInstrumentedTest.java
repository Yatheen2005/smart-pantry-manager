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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Android Instrumented Test verifying SQLite CRUD operations, 18-recipe seeding,
 * and persistence across database connection closure and reopening.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseCrudPersistenceInstrumentedTest {

    private Context context;
    private PantryDatabaseHelper dbHelper;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = new PantryDatabaseHelper(context);
        dbHelper.clearAllPantryItems();
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void testPreloadedRecipesSeededOnFirstRun() {
        List<Recipe> recipes = dbHelper.getAllRecipesWithIngredients();
        assertTrue("Database must contain at least 15-20 preloaded recipes", recipes.size() >= 18);
        for (Recipe r : recipes) {
            assertFalseOrNonEmpty(r.getTitle());
            assertTrue("Each seeded recipe must have required ingredients", !r.getIngredients().isEmpty());
        }
    }

    @Test
    public void testFullCrudCycleAndSessionPersistence() {
        // 1. CREATE
        PantryItem item = new PantryItem("Fresh Tomatoes", "tomato", 5.0, "pcs", "2026-11-01");
        long id = dbHelper.insertPantryItem(item);
        assertTrue(id > 0);

        // 2. Close helper and reopen a new instance to verify disk persistence
        dbHelper.close();
        PantryDatabaseHelper reopenedDb = new PantryDatabaseHelper(context);

        // 3. READ
        PantryItem fetched = reopenedDb.getPantryItemById(id);
        assertNotNull(fetched);
        assertEquals("Fresh Tomatoes", fetched.getName());
        assertEquals("tomato", fetched.getNormalizedName());
        assertEquals(5.0, fetched.getQuantity(), 1e-4);

        // 4. UPDATE
        fetched.setQuantity(12.0);
        int rowsUpdated = reopenedDb.updatePantryItem(fetched);
        assertEquals(1, rowsUpdated);
        assertEquals(12.0, reopenedDb.getPantryItemById(id).getQuantity(), 1e-4);

        // 5. DELETE
        int rowsDeleted = reopenedDb.deletePantryItem(id);
        assertEquals(1, rowsDeleted);
        assertNull(reopenedDb.getPantryItemById(id));
        reopenedDb.close();
    }

    private void assertFalseOrNonEmpty(String value) {
        assertNotNull(value);
        assertTrue(!value.trim().isEmpty());
    }
}
