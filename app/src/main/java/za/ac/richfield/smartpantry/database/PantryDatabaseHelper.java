package za.ac.richfield.smartpantry.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;
import za.ac.richfield.smartpantry.util.IngredientNormalizer;
import za.ac.richfield.smartpantry.util.StrictRecipeMatcher;
import za.ac.richfield.smartpantry.util.UnitConverter;

/**
 * Local SQLite persistence layer built with Android's SQLiteOpenHelper (Section 3.2).
 *
 * Tables:
 * 1. pantry_items       - the user's pantry (full Create, Read, Update, Delete)
 * 2. recipes            - 18 recipes seeded once, when the database file is first created
 * 3. recipe_ingredients - required ingredients per recipe (one-to-many, foreign key to recipes)
 *
 * The database is a file in the app's private storage, so data survives the app being closed,
 * swiped away or the device restarting.
 */
public class PantryDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "smart_pantry.db";
    public static final int DATABASE_VERSION = 1;

    // Table 1: pantry_items
    public static final String TABLE_PANTRY = "pantry_items";
    public static final String COL_PANTRY_ID = "_id";
    public static final String COL_PANTRY_NAME = "name";
    public static final String COL_PANTRY_NORM_NAME = "normalized_name";
    public static final String COL_PANTRY_QUANTITY = "quantity";
    public static final String COL_PANTRY_UNIT = "unit";
    public static final String COL_PANTRY_EXPIRY = "expiry_date";
    public static final String COL_PANTRY_UPDATED = "updated_at";

    // Table 2: recipes
    public static final String TABLE_RECIPES = "recipes";
    public static final String COL_RECIPE_ID = "_id";
    public static final String COL_RECIPE_TITLE = "title";
    public static final String COL_RECIPE_CATEGORY = "category";
    public static final String COL_RECIPE_PREP_TIME = "prep_time_minutes";
    public static final String COL_RECIPE_SERVINGS = "servings";
    public static final String COL_RECIPE_INSTRUCTIONS = "instructions";

    // Table 3: recipe_ingredients
    public static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";
    public static final String COL_RI_ID = "_id";
    public static final String COL_RI_RECIPE_ID = "recipe_id";
    public static final String COL_RI_NAME = "ingredient_name";
    public static final String COL_RI_NORM_NAME = "normalized_name";
    public static final String COL_RI_QUANTITY = "required_quantity";
    public static final String COL_RI_UNIT = "required_unit";

    private static final String SQL_CREATE_PANTRY_TABLE =
            "CREATE TABLE " + TABLE_PANTRY + " ("
                    + COL_PANTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_PANTRY_NAME + " TEXT NOT NULL, "
                    + COL_PANTRY_NORM_NAME + " TEXT NOT NULL, "
                    + COL_PANTRY_QUANTITY + " REAL NOT NULL CHECK(" + COL_PANTRY_QUANTITY + " >= 0), "
                    + COL_PANTRY_UNIT + " TEXT NOT NULL, "
                    + COL_PANTRY_EXPIRY + " TEXT, "
                    + COL_PANTRY_UPDATED + " INTEGER NOT NULL"
                    + ");";

    private static final String SQL_CREATE_RECIPES_TABLE =
            "CREATE TABLE " + TABLE_RECIPES + " ("
                    + COL_RECIPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_RECIPE_TITLE + " TEXT NOT NULL UNIQUE, "
                    + COL_RECIPE_CATEGORY + " TEXT NOT NULL, "
                    + COL_RECIPE_PREP_TIME + " INTEGER NOT NULL, "
                    + COL_RECIPE_SERVINGS + " INTEGER NOT NULL, "
                    + COL_RECIPE_INSTRUCTIONS + " TEXT NOT NULL"
                    + ");";

    private static final String SQL_CREATE_RECIPE_INGREDIENTS_TABLE =
            "CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " ("
                    + COL_RI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_RI_RECIPE_ID + " INTEGER NOT NULL, "
                    + COL_RI_NAME + " TEXT NOT NULL, "
                    + COL_RI_NORM_NAME + " TEXT NOT NULL, "
                    + COL_RI_QUANTITY + " REAL NOT NULL CHECK(" + COL_RI_QUANTITY + " > 0), "
                    + COL_RI_UNIT + " TEXT NOT NULL, "
                    + "FOREIGN KEY(" + COL_RI_RECIPE_ID + ") REFERENCES "
                    + TABLE_RECIPES + "(" + COL_RECIPE_ID + ") ON DELETE CASCADE"
                    + ");";

    private static PantryDatabaseHelper instance;

    /**
     * Single shared helper for the whole app, so every Activity uses the same open connection.
     * The application context is used to avoid leaking an Activity.
     */
    public static synchronized PantryDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PantryDatabaseHelper(context.getApplicationContext(), DATABASE_NAME);
        }
        return instance;
    }

    /**
     * @param databaseName file name of the database; tests pass their own name so they never
     *                     touch the user's real pantry.
     */
    @VisibleForTesting
    public PantryDatabaseHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PANTRY_TABLE);
        db.execSQL(SQL_CREATE_RECIPES_TABLE);
        db.execSQL(SQL_CREATE_RECIPE_INGREDIENTS_TABLE);
        db.execSQL("CREATE INDEX idx_pantry_norm_name ON " + TABLE_PANTRY + "(" + COL_PANTRY_NORM_NAME + ");");
        db.execSQL("CREATE INDEX idx_ri_recipe_id ON " + TABLE_RECIPE_INGREDIENTS + "(" + COL_RI_RECIPE_ID + ");");

        // Seed the pre-loaded recipes once, when the database file is first created
        RecipeSeeder.seedRecipes(db);
    }

    /**
     * Called when DATABASE_VERSION is increased. Recipe tables are app-owned reference data, so
     * they are rebuilt; the user's pantry_items table is kept so no user data is lost.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL(SQL_CREATE_RECIPES_TABLE);
        db.execSQL(SQL_CREATE_RECIPE_INGREDIENTS_TABLE);
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ri_recipe_id ON "
                + TABLE_RECIPE_INGREDIENTS + "(" + COL_RI_RECIPE_ID + ");");
        RecipeSeeder.seedRecipes(db);
    }

    // =========================================================================
    // PANTRY CRUD OPERATIONS
    // =========================================================================

    /**
     * CREATE: inserts a new pantry item. The canonical name and unit are stored alongside the
     * name the user typed.
     *
     * @return the new row id, or -1 if the insert failed
     */
    public long insertPantryItem(PantryItem item) {
        ContentValues values = toContentValues(item);
        long id = getWritableDatabase().insert(TABLE_PANTRY, null, values);
        item.setId(id);
        item.setNormalizedName(values.getAsString(COL_PANTRY_NORM_NAME));
        item.setUnit(values.getAsString(COL_PANTRY_UNIT));
        return id;
    }

    /**
     * READ (all): every pantry item, ordered alphabetically by name.
     */
    public List<PantryItem> getAllPantryItems() {
        return queryPantry(null, null);
    }

    /**
     * READ (one): a single pantry item by id, or null if it does not exist.
     */
    public PantryItem getPantryItemById(long id) {
        List<PantryItem> result = queryPantry(COL_PANTRY_ID + " = ?", new String[]{String.valueOf(id)});
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * READ (duplicates): items whose canonical name equals the canonical form of rawName,
     * e.g. "Tomatoes" finds an existing "Fresh Tomato" row.
     */
    public List<PantryItem> findPantryItemsByName(String rawName) {
        String normalized = IngredientNormalizer.normalize(rawName);
        return queryPantry(COL_PANTRY_NORM_NAME + " = ?", new String[]{normalized});
    }

    /**
     * UPDATE: updates an existing pantry item by id.
     *
     * @return number of rows changed (1 if successful, 0 if the id does not exist)
     */
    public int updatePantryItem(PantryItem item) {
        ContentValues values = toContentValues(item);
        item.setNormalizedName(values.getAsString(COL_PANTRY_NORM_NAME));
        item.setUnit(values.getAsString(COL_PANTRY_UNIT));
        return getWritableDatabase().update(TABLE_PANTRY, values,
                COL_PANTRY_ID + " = ?", new String[]{String.valueOf(item.getId())});
    }

    /**
     * DELETE: deletes a pantry item by id.
     *
     * @return number of rows deleted (1 if successful, 0 if the id does not exist)
     */
    public int deletePantryItem(long id) {
        return getWritableDatabase().delete(TABLE_PANTRY,
                COL_PANTRY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /** Deletes every pantry item (Settings > Clear All). */
    public void clearAllPantryItems() {
        getWritableDatabase().delete(TABLE_PANTRY, null, null);
    }

    /**
     * Replaces the whole pantry with the demo items in one transaction.
     *
     * @return number of items inserted
     */
    public int loadSamplePantry() {
        List<PantryItem> samples = SamplePantryData.build(Calendar.getInstance());
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_PANTRY, null, null);
            for (PantryItem item : samples) {
                insertPantryItem(item);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return samples.size();
    }

    /**
     * "Mark as cooked": subtracts each required ingredient from the pantry.
     *
     * The recipe is re-checked first, using the same rules as the Suggested Recipes screen, so
     * nothing is deducted unless the user really has everything. Stock that expires soonest is
     * used first, rows that reach zero are deleted, and all changes happen in one transaction
     * (either every ingredient is deducted or none are).
     *
     * @return true if the ingredients were deducted, false if the recipe is not a strict match
     */
    public boolean consumeRecipeIngredients(Recipe recipe, boolean excludeExpired) {
        if (recipe == null || recipe.getIngredients().isEmpty()) {
            return false;
        }
        List<PantryItem> pantry = getAllPantryItems();
        if (!StrictRecipeMatcher.evaluateRecipe(recipe, pantry, excludeExpired).isStrictMatch()) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (RecipeIngredient required : recipe.getIngredients()) {
                String requiredUnit = required.getRequiredUnit();
                double remaining = required.getRequiredQuantity();

                List<PantryItem> usable =
                        StrictRecipeMatcher.findUsablePantryItems(required, pantry, excludeExpired);
                Collections.sort(usable, (a, b) ->
                        Long.compare(a.getDaysUntilExpiry(), b.getDaysUntilExpiry()));

                for (PantryItem item : usable) {
                    if (remaining <= 1e-6) {
                        break;
                    }
                    double availableInRequiredUnit =
                            UnitConverter.convert(item.getQuantity(), item.getUnit(), requiredUnit);
                    if (availableInRequiredUnit <= remaining + 1e-6) {
                        remaining -= availableInRequiredUnit;
                        item.setQuantity(0);
                        deletePantryItem(item.getId());
                    } else {
                        double leftOver = availableInRequiredUnit - remaining;
                        remaining = 0;
                        item.setQuantity(UnitConverter.convert(leftOver, requiredUnit, item.getUnit()));
                        updatePantryItem(item);
                    }
                }
                if (remaining > 1e-6) {
                    return false; // should not happen after the strict check; transaction is rolled back
                }
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // RECIPE QUERIES
    // =========================================================================

    public int getRecipeCount() {
        return (int) DatabaseUtils.queryNumEntries(getReadableDatabase(), TABLE_RECIPES);
    }

    /**
     * Retrieves all pre-loaded recipes along with their required ingredients.
     */
    public List<Recipe> getAllRecipesWithIngredients() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_RECIPES, null, null, null, null, null,
                COL_RECIPE_ID + " ASC")) {
            while (cursor.moveToNext()) {
                Recipe recipe = mapCursorToRecipe(cursor);
                recipe.setIngredients(getIngredientsForRecipe(db, recipe.getId()));
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    /**
     * Retrieves a single recipe and its ingredients by id, or null if it does not exist.
     */
    public Recipe getRecipeById(long recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_RECIPES, null, COL_RECIPE_ID + " = ?",
                new String[]{String.valueOf(recipeId)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                Recipe recipe = mapCursorToRecipe(cursor);
                recipe.setIngredients(getIngredientsForRecipe(db, recipe.getId()));
                return recipe;
            }
        }
        return null;
    }

    private List<RecipeIngredient> getIngredientsForRecipe(SQLiteDatabase db, long recipeId) {
        List<RecipeIngredient> list = new ArrayList<>();
        try (Cursor c = db.query(TABLE_RECIPE_INGREDIENTS, null, COL_RI_RECIPE_ID + " = ?",
                new String[]{String.valueOf(recipeId)}, null, null, COL_RI_ID + " ASC")) {
            while (c.moveToNext()) {
                list.add(new RecipeIngredient(
                        c.getLong(c.getColumnIndexOrThrow(COL_RI_ID)),
                        c.getLong(c.getColumnIndexOrThrow(COL_RI_RECIPE_ID)),
                        c.getString(c.getColumnIndexOrThrow(COL_RI_NAME)),
                        c.getString(c.getColumnIndexOrThrow(COL_RI_NORM_NAME)),
                        c.getDouble(c.getColumnIndexOrThrow(COL_RI_QUANTITY)),
                        c.getString(c.getColumnIndexOrThrow(COL_RI_UNIT))
                ));
            }
        }
        return list;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private List<PantryItem> queryPantry(String selection, String[] selectionArgs) {
        List<PantryItem> items = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE_PANTRY, null, selection, selectionArgs,
                null, null, COL_PANTRY_NAME + " COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                items.add(mapCursorToPantryItem(cursor));
            }
        }
        return items;
    }

    private static ContentValues toContentValues(PantryItem item) {
        ContentValues values = new ContentValues();
        values.put(COL_PANTRY_NAME, item.getName().trim());
        values.put(COL_PANTRY_NORM_NAME, IngredientNormalizer.normalize(item.getName()));
        values.put(COL_PANTRY_QUANTITY, item.getQuantity());
        values.put(COL_PANTRY_UNIT, UnitConverter.normalizeUnit(item.getUnit()));
        values.put(COL_PANTRY_EXPIRY, item.hasExpiryDate() ? item.getExpiryDate().trim() : null);
        values.put(COL_PANTRY_UPDATED, System.currentTimeMillis());
        return values;
    }

    private static PantryItem mapCursorToPantryItem(Cursor cursor) {
        return new PantryItem(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_PANTRY_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PANTRY_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PANTRY_NORM_NAME)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PANTRY_QUANTITY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PANTRY_UNIT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PANTRY_EXPIRY)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_PANTRY_UPDATED))
        );
    }

    private static Recipe mapCursorToRecipe(Cursor cursor) {
        return new Recipe(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_RECIPE_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_CATEGORY)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECIPE_PREP_TIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECIPE_SERVINGS)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_INSTRUCTIONS))
        );
    }
}
