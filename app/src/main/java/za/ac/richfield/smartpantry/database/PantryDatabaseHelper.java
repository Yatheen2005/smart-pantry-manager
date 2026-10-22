package za.ac.richfield.smartpantry.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;
import za.ac.richfield.smartpantry.util.IngredientNormalizer;
import za.ac.richfield.smartpantry.util.UnitConverter;

/**
 * Local SQLite persistence layer built with Android's SQLiteOpenHelper (Section 3.2).
 *
 * Manages three normalized relational tables:
 * 1. pantry_items       - User's current pantry inventory (full Create, Read, Update, Delete)
 * 2. recipes            - Pre-loaded collection of 18 recipes seeded on first database creation
 * 3. recipe_ingredients - Required ingredients per recipe (1-to-many foreign key relationship)
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

    public static synchronized PantryDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PantryDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public PantryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

        // Seed the 18 pre-loaded recipes on first database creation
        RecipeSeeder.seedRecipes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANTRY);
        onCreate(db);
    }

    // =========================================================================
    // PANTRY CRUD OPERATIONS
    // =========================================================================

    /**
     * CREATE: Inserts a new PantryItem into the database.
     * Automatically populates normalized_name and canonical unit before saving.
     */
    public long insertPantryItem(PantryItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String normName = IngredientNormalizer.normalize(item.getName());
        String normUnit = UnitConverter.normalizeUnit(item.getUnit());

        values.put(COL_PANTRY_NAME, item.getName().trim());
        values.put(COL_PANTRY_NORM_NAME, normName);
        values.put(COL_PANTRY_QUANTITY, item.getQuantity());
        values.put(COL_PANTRY_UNIT, normUnit);
        values.put(COL_PANTRY_EXPIRY, item.hasExpiryDate() ? item.getExpiryDate().trim() : null);
        values.put(COL_PANTRY_UPDATED, System.currentTimeMillis());

        long id = db.insert(TABLE_PANTRY, null, values);
        item.setId(id);
        item.setNormalizedName(normName);
        item.setUnit(normUnit);
        return id;
    }

    /**
     * READ (All): Retrieves all pantry items ordered alphabetically by ingredient name.
     */
    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_PANTRY,
                null,
                null,
                null,
                null,
                null,
                COL_PANTRY_NAME + " COLLATE NOCASE ASC"
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    items.add(mapCursorToPantryItem(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return items;
    }

    /**
     * READ (Single by ID): Retrieves a single PantryItem by primary key _id.
     */
    public PantryItem getPantryItemById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_PANTRY,
                null,
                COL_PANTRY_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return mapCursorToPantryItem(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * UPDATE: Updates an existing PantryItem record by primary key _id.
     *
     * @return Number of rows affected (1 if successful, 0 otherwise).
     */
    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String normName = IngredientNormalizer.normalize(item.getName());
        String normUnit = UnitConverter.normalizeUnit(item.getUnit());

        values.put(COL_PANTRY_NAME, item.getName().trim());
        values.put(COL_PANTRY_NORM_NAME, normName);
        values.put(COL_PANTRY_QUANTITY, item.getQuantity());
        values.put(COL_PANTRY_UNIT, normUnit);
        values.put(COL_PANTRY_EXPIRY, item.hasExpiryDate() ? item.getExpiryDate().trim() : null);
        values.put(COL_PANTRY_UPDATED, System.currentTimeMillis());

        item.setNormalizedName(normName);
        item.setUnit(normUnit);

        return db.update(
                TABLE_PANTRY,
                values,
                COL_PANTRY_ID + " = ?",
                new String[]{String.valueOf(item.getId())}
        );
    }

    /**
     * DELETE: Deletes a PantryItem record by primary key _id.
     *
     * @return Number of rows deleted (1 if found and deleted, 0 otherwise).
     */
    public int deletePantryItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(
                TABLE_PANTRY,
                COL_PANTRY_ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    /**
     * Clears all user pantry items (used in Settings reset or testing).
     */
    public void clearAllPantryItems() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PANTRY, null, null);
    }

    /**
     * Deducts the required ingredients of a cooked recipe from the user's pantry.
     * Removes any pantry item whose remaining quantity reaches 0.
     */
    public void consumeRecipeIngredients(Recipe recipe) {
        if (recipe == null || recipe.getIngredients() == null) {
            return;
        }
        List<PantryItem> currentPantry = getAllPantryItems();
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (RecipeIngredient req : recipe.getIngredients()) {
                String reqNorm = IngredientNormalizer.normalize(req.getIngredientName());
                String reqUnit = UnitConverter.normalizeUnit(req.getRequiredUnit());
                double remainingToDeduct = req.getRequiredQuantity();

                for (PantryItem item : currentPantry) {
                    if (remainingToDeduct <= 1e-6) {
                        break;
                    }
                    if (reqNorm.equals(item.getNormalizedName())
                            && UnitConverter.areUnitsCompatible(item.getUnit(), reqUnit)) {
                        double itemAvailableInReqUnit = UnitConverter.convert(
                                item.getQuantity(), item.getUnit(), reqUnit);
                        if (itemAvailableInReqUnit <= remainingToDeduct + 1e-6) {
                            remainingToDeduct -= itemAvailableInReqUnit;
                            item.setQuantity(0);
                            deletePantryItem(item.getId());
                        } else {
                            double newQtyInReqUnit = itemAvailableInReqUnit - remainingToDeduct;
                            double newQtyInItemUnit = UnitConverter.convert(
                                    newQtyInReqUnit, reqUnit, item.getUnit());
                            remainingToDeduct = 0;
                            item.setQuantity(newQtyInItemUnit);
                            updatePantryItem(item);
                        }
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // RECIPE QUERIES & DEMO SEEDING
    // =========================================================================

    /**
     * Retrieves all pre-loaded recipes along with their required ingredients.
     */
    public List<Recipe> getAllRecipesWithIngredients() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Ensure recipes are seeded if table was somehow emptied
        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_RECIPES, null);
        if (countCursor != null) {
            try {
                if (countCursor.moveToFirst() && countCursor.getInt(0) == 0) {
                    RecipeSeeder.seedRecipes(db);
                }
            } finally {
                countCursor.close();
            }
        }

        Cursor cursor = db.query(
                TABLE_RECIPES,
                null,
                null,
                null,
                null,
                null,
                COL_RECIPE_ID + " ASC"
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    Recipe recipe = mapCursorToRecipe(cursor);
                    recipe.setIngredients(getIngredientsForRecipe(db, recipe.getId()));
                    recipes.add(recipe);
                }
            } finally {
                cursor.close();
            }
        }
        return recipes;
    }

    /**
     * Retrieves a single Recipe and its ingredients by ID.
     */
    public Recipe getRecipeById(long recipeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_RECIPES,
                null,
                COL_RECIPE_ID + " = ?",
                new String[]{String.valueOf(recipeId)},
                null,
                null,
                null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    Recipe recipe = mapCursorToRecipe(cursor);
                    recipe.setIngredients(getIngredientsForRecipe(db, recipe.getId()));
                    return recipe;
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private List<RecipeIngredient> getIngredientsForRecipe(SQLiteDatabase db, long recipeId) {
        List<RecipeIngredient> list = new ArrayList<>();
        Cursor c = db.query(
                TABLE_RECIPE_INGREDIENTS,
                null,
                COL_RI_RECIPE_ID + " = ?",
                new String[]{String.valueOf(recipeId)},
                null,
                null,
                COL_RI_ID + " ASC"
        );
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    RecipeIngredient ri = new RecipeIngredient(
                            c.getLong(c.getColumnIndexOrThrow(COL_RI_ID)),
                            c.getLong(c.getColumnIndexOrThrow(COL_RI_RECIPE_ID)),
                            c.getString(c.getColumnIndexOrThrow(COL_RI_NAME)),
                            c.getString(c.getColumnIndexOrThrow(COL_RI_NORM_NAME)),
                            c.getDouble(c.getColumnIndexOrThrow(COL_RI_QUANTITY)),
                            c.getString(c.getColumnIndexOrThrow(COL_RI_UNIT))
                    );
                    list.add(ri);
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    /**
     * Seeds realistic sample pantry items so a marker or user can immediately test strict
     * matching, unit conversions, and singular/plural normalization with one tap.
     */
    public void seedSamplePantryItems() {
        clearAllPantryItems();
        insertPantryItem(new PantryItem("Large Eggs", "egg", 6, "pcs", "2026-10-24"));
        insertPantryItem(new PantryItem("Bread Slices", "bread", 8, "slice", "2026-10-19"));
        insertPantryItem(new PantryItem("Butter", "butter", 250, "g", "2026-11-15"));
        insertPantryItem(new PantryItem("Cheddar Cheese", "cheddar cheese", 200, "g", "2026-10-20"));
        insertPantryItem(new PantryItem("Fresh Tomatoes", "tomato", 4, "pcs", "2026-10-18"));
        insertPantryItem(new PantryItem("Onions", "onion", 3, "pcs", "2026-11-05"));
        insertPantryItem(new PantryItem("Garlic Cloves", "garlic", 6, "clove", "2026-11-10"));
        insertPantryItem(new PantryItem("Full Cream Milk", "milk", 1, "l", "2026-10-21"));
        insertPantryItem(new PantryItem("Cake Flour", "flour", 1, "kg", "2027-01-15"));
        insertPantryItem(new PantryItem("Cooking Oil", "cooking oil", 500, "ml", "2027-03-01"));
        insertPantryItem(new PantryItem("Salt", "salt", 200, "g", null));
        insertPantryItem(new PantryItem("Black Pepper", "black pepper", 50, "g", null));
    }

    private PantryItem mapCursorToPantryItem(Cursor cursor) {
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

    private Recipe mapCursorToRecipe(Cursor cursor) {
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
