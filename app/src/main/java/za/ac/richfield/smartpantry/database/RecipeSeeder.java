package za.ac.richfield.smartpantry.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;
import za.ac.richfield.smartpantry.util.IngredientNormalizer;
import za.ac.richfield.smartpantry.util.UnitConverter;

/**
 * Seeds the SQLite database on first creation with 18 practical household and South African
 * leftover-friendly recipes (satisfying Section 2.2 requirement of 15-20 persisted recipes).
 */
public final class RecipeSeeder {

    private RecipeSeeder() {
        // Utility class
    }

    public static void seedRecipes(SQLiteDatabase db) {
        List<Recipe> defaultRecipes = buildPreloadedRecipes();
        db.beginTransaction();
        try {
            for (Recipe recipe : defaultRecipes) {
                ContentValues rValues = new ContentValues();
                rValues.put(PantryDatabaseHelper.COL_RECIPE_TITLE, recipe.getTitle());
                rValues.put(PantryDatabaseHelper.COL_RECIPE_CATEGORY, recipe.getCategory());
                rValues.put(PantryDatabaseHelper.COL_RECIPE_PREP_TIME, recipe.getPrepTimeMinutes());
                rValues.put(PantryDatabaseHelper.COL_RECIPE_SERVINGS, recipe.getServings());
                rValues.put(PantryDatabaseHelper.COL_RECIPE_INSTRUCTIONS, recipe.getInstructions());

                long recipeId = db.insertWithOnConflict(
                        PantryDatabaseHelper.TABLE_RECIPES,
                        null,
                        rValues,
                        SQLiteDatabase.CONFLICT_IGNORE
                );

                if (recipeId != -1) {
                    for (RecipeIngredient ing : recipe.getIngredients()) {
                        ContentValues iValues = new ContentValues();
                        iValues.put(PantryDatabaseHelper.COL_RI_RECIPE_ID, recipeId);
                        iValues.put(PantryDatabaseHelper.COL_RI_NAME, ing.getIngredientName());
                        iValues.put(PantryDatabaseHelper.COL_RI_NORM_NAME,
                                IngredientNormalizer.normalize(ing.getIngredientName()));
                        iValues.put(PantryDatabaseHelper.COL_RI_QUANTITY, ing.getRequiredQuantity());
                        iValues.put(PantryDatabaseHelper.COL_RI_UNIT,
                                UnitConverter.normalizeUnit(ing.getRequiredUnit()));
                        db.insert(PantryDatabaseHelper.TABLE_RECIPE_INGREDIENTS, null, iValues);
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static List<Recipe> buildPreloadedRecipes() {
        List<Recipe> list = new ArrayList<>();

        // 1. Classic Scrambled Eggs on Toast
        Recipe r1 = new Recipe(0, "Classic Scrambled Eggs on Toast", "Breakfast", 10, 1,
                "1. Crack the eggs into a bowl, add milk, salt, and black pepper, and whisk until combined.\n"
                + "2. Melt the butter in a non-stick pan over medium-low heat.\n"
                + "3. Pour in the egg mixture and gently stir with a spatula until soft curds form.\n"
                + "4. Toast the bread slices and serve the warm scrambled eggs on top.");
        addIng(r1, "Eggs", 2, "pcs");
        addIng(r1, "Milk", 30, "ml");
        addIng(r1, "Butter", 15, "g");
        addIng(r1, "Bread", 2, "slice");
        addIng(r1, "Salt", 2, "g");
        list.add(r1);

        // 2. Golden Grilled Cheese Toastie
        Recipe r2 = new Recipe(0, "Golden Grilled Cheese Toastie", "Quick Lunch", 10, 1,
                "1. Butter the outer sides of both bread slices evenly.\n"
                + "2. Grate or slice the cheddar cheese and layer it between the unbuttered sides of the bread.\n"
                + "3. Heat a skillet over medium heat and press the sandwich gently for 3 minutes per side until golden brown and the cheese has melted completely.");
        addIng(r2, "Bread", 2, "slice");
        addIng(r2, "Cheddar Cheese", 60, "g");
        addIng(r2, "Butter", 15, "g");
        list.add(r2);

        // 3. Tomato & Onion Braai Relish (Smoor)
        Recipe r3 = new Recipe(0, "Tomato & Onion Braai Relish (Smoor)", "South African Side", 20, 4,
                "1. Finely chop the onions, garlic cloves, and ripe tomatoes.\n"
                + "2. Heat the cooking oil in a saucepan over medium heat and saute the onions until translucent.\n"
                + "3. Add the garlic and cook for 1 minute until fragrant.\n"
                + "4. Stir in the chopped tomatoes, salt, and black pepper, then simmer uncovered for 12-15 minutes until thick and rich.");
        addIng(r3, "Tomatoes", 3, "pcs");
        addIng(r3, "Onions", 1, "pcs");
        addIng(r3, "Garlic", 2, "clove");
        addIng(r3, "Cooking Oil", 15, "ml");
        addIng(r3, "Salt", 5, "g");
        addIng(r3, "Black Pepper", 2, "g");
        list.add(r3);

        // 4. Fluffy Homemade Pancakes
        Recipe r4 = new Recipe(0, "Fluffy Homemade Pancakes", "Breakfast", 20, 4,
                "1. Whisk the flour and salt together in a mixing bowl.\n"
                + "2. In a separate jug, beat the eggs with the milk and melted butter.\n"
                + "3. Pour the wet mixture into the dry ingredients and whisk into a smooth batter.\n"
                + "4. Cook ladles of batter in a lightly oiled pan for 2 minutes per side until golden.");
        addIng(r4, "Flour", 200, "g");
        addIng(r4, "Milk", 250, "ml");
        addIng(r4, "Eggs", 2, "pcs");
        addIng(r4, "Butter", 30, "g");
        addIng(r4, "Salt", 2, "g");
        list.add(r4);

        // 5. Three-Egg Cheddar & Tomato Omelette
        Recipe r5 = new Recipe(0, "Cheddar & Tomato Omelette", "Breakfast", 12, 1,
                "1. Beat the eggs with salt and black pepper until frothy.\n"
                + "2. Dice the tomato and grate the cheddar cheese.\n"
                + "3. Melt butter in a frying pan, pour in the beaten eggs, and let the base set.\n"
                + "4. Scatter the diced tomato and grated cheddar over half the omelette, fold over, and cook for 1 more minute.");
        addIng(r5, "Eggs", 3, "pcs");
        addIng(r5, "Tomato", 1, "pcs");
        addIng(r5, "Cheddar Cheese", 40, "g");
        addIng(r5, "Butter", 15, "g");
        addIng(r5, "Salt", 2, "g");
        list.add(r5);

        // 6. Traditional Stiff Maize Meal Pap
        Recipe r6 = new Recipe(0, "Traditional Maize Meal Pap", "South African Staple", 25, 4,
                "1. Bring 750 ml of water and the salt to a boil in a heavy-bottomed pot.\n"
                + "2. Slowly pour in the maize meal while stirring briskly to prevent lumps.\n"
                + "3. Reduce heat to low, cover tightly with a lid, and steam for 20 minutes, stirring with a fork halfway through.\n"
                + "4. Stir in the butter before serving hot.");
        addIng(r6, "Maize Meal", 250, "g");
        addIng(r6, "Salt", 5, "g");
        addIng(r6, "Butter", 20, "g");
        list.add(r6);

        // 7. Spicy Chakalaka Baked Beans
        Recipe r7 = new Recipe(0, "Spicy Chakalaka Baked Beans", "South African Side", 25, 4,
                "1. Heat cooking oil in a pot and fry the chopped onion, garlic, and bell pepper until soft.\n"
                + "2. Stir in curry powder and grated carrots, cooking for 3 minutes.\n"
                + "3. Add chopped tomatoes and simmer for 8 minutes until saucy.\n"
                + "4. Fold in the baked beans, season with salt, and heat through for 5 minutes.");
        addIng(r7, "Onions", 1, "pcs");
        addIng(r7, "Garlic", 2, "clove");
        addIng(r7, "Bell Pepper", 1, "pcs");
        addIng(r7, "Carrots", 2, "pcs");
        addIng(r7, "Tomatoes", 2, "pcs");
        addIng(r7, "Baked Beans", 1, "tin");
        addIng(r7, "Curry Powder", 10, "g");
        addIng(r7, "Cooking Oil", 30, "ml");
        addIng(r7, "Salt", 5, "g");
        list.add(r7);

        // 8. Garlic & Herb Butter Pasta (Aglio e Olio Style)
        Recipe r8 = new Recipe(0, "Garlic Butter Skillet Pasta", "Dinner", 18, 2,
                "1. Boil the pasta in salted water until al dente, reserving 60 ml of pasta cooking water.\n"
                + "2. Thinly slice the garlic cloves and gently fry in butter and cooking oil over low heat until golden.\n"
                + "3. Toss the drained pasta and reserved water into the garlic butter pan and season with black pepper.");
        addIng(r8, "Pasta", 200, "g");
        addIng(r8, "Garlic", 4, "clove");
        addIng(r8, "Butter", 40, "g");
        addIng(r8, "Cooking Oil", 15, "ml");
        addIng(r8, "Salt", 5, "g");
        addIng(r8, "Black Pepper", 3, "g");
        list.add(r8);

        // 9. Creamy Tomato & Garlic Penne
        Recipe r9 = new Recipe(0, "Creamy Tomato & Garlic Pasta", "Dinner", 25, 3,
                "1. Cook the pasta in salted boiling water and drain.\n"
                + "2. Saute chopped onion and minced garlic in cooking oil until soft.\n"
                + "3. Add diced tomatoes and simmer for 10 minutes until broken down.\n"
                + "4. Stir in milk and grated cheddar cheese to create a velvety pink sauce, then toss with the pasta.");
        addIng(r9, "Pasta", 250, "g");
        addIng(r9, "Tomatoes", 3, "pcs");
        addIng(r9, "Onion", 1, "pcs");
        addIng(r9, "Garlic", 2, "clove");
        addIng(r9, "Milk", 100, "ml");
        addIng(r9, "Cheddar Cheese", 50, "g");
        addIng(r9, "Cooking Oil", 15, "ml");
        addIng(r9, "Salt", 5, "g");
        list.add(r9);

        // 10. Crispy Pan-Roasted Garlic Potatoes
        Recipe r10 = new Recipe(0, "Crispy Pan-Roasted Garlic Potatoes", "Side Dish", 30, 3,
                "1. Wash and dice the potatoes into 2 cm cubes.\n"
                + "2. Parboil in salted water for 7 minutes, then drain and steam dry.\n"
                + "3. Heat cooking oil and butter in a wide skillet and fry the potatoes for 15 minutes until crispy on all sides.\n"
                + "4. Toss with crushed garlic, salt, and black pepper for the final 2 minutes.");
        addIng(r10, "Potatoes", 500, "g");
        addIng(r10, "Garlic", 3, "clove");
        addIng(r10, "Butter", 25, "g");
        addIng(r10, "Cooking Oil", 30, "ml");
        addIng(r10, "Salt", 5, "g");
        addIng(r10, "Black Pepper", 2, "g");
        list.add(r10);

        // 11. Hearty Savoury Mince & Onion Skillet
        Recipe r11 = new Recipe(0, "Savoury Beef Mince Skillet", "Dinner", 30, 4,
                "1. Heat cooking oil in a heavy pan and brown the beef mince, breaking up any lumps.\n"
                + "2. Add finely chopped onions and crushed garlic, cooking for 5 minutes.\n"
                + "3. Stir in chopped tomatoes, salt, and black pepper, cover and simmer for 15 minutes until rich and tender.");
        addIng(r11, "Beef Mince", 400, "g");
        addIng(r11, "Onions", 1, "pcs");
        addIng(r11, "Garlic", 2, "clove");
        addIng(r11, "Tomatoes", 2, "pcs");
        addIng(r11, "Cooking Oil", 15, "ml");
        addIng(r11, "Salt", 5, "g");
        addIng(r11, "Black Pepper", 3, "g");
        list.add(r11);

        // 12. Cape Malay Style Curried Mince (Bobotie Base)
        Recipe r12 = new Recipe(0, "Curried Beef Mince & Egg Bake", "Dinner", 45, 4,
                "1. Saute chopped onions and garlic in cooking oil, then stir in curry powder until aromatic.\n"
                + "2. Brown the beef mince in the spiced onion mixture, season with salt, and transfer to a baking dish.\n"
                + "3. Whisk the eggs and milk together, pour evenly over the mince, and bake at 180 C for 25 minutes until golden and set.");
        addIng(r12, "Beef Mince", 500, "g");
        addIng(r12, "Onions", 2, "pcs");
        addIng(r12, "Garlic", 2, "clove");
        addIng(r12, "Curry Powder", 15, "g");
        addIng(r12, "Eggs", 2, "pcs");
        addIng(r12, "Milk", 150, "ml");
        addIng(r12, "Cooking Oil", 15, "ml");
        addIng(r12, "Salt", 5, "g");
        list.add(r12);

        // 13. Egg Fried Rice with Onion & Garlic
        Recipe r13 = new Recipe(0, "Leftover Egg Fried Rice", "Quick Dinner", 15, 2,
                "1. Cook the rice in salted water and let it cool completely.\n"
                + "2. Heat cooking oil in a wok or large frying pan and scramble the eggs quickly.\n"
                + "3. Add finely diced onion and garlic, stir-fry for 2 minutes, then toss in the rice, salt, and black pepper over high heat.");
        addIng(r13, "Rice", 200, "g");
        addIng(r13, "Eggs", 2, "pcs");
        addIng(r13, "Onion", 1, "pcs");
        addIng(r13, "Garlic", 2, "clove");
        addIng(r13, "Cooking Oil", 30, "ml");
        addIng(r13, "Salt", 5, "g");
        addIng(r13, "Black Pepper", 2, "g");
        list.add(r13);

        // 14. Classic French Toast
        Recipe r14 = new Recipe(0, "Classic French Toast", "Breakfast", 15, 2,
                "1. Whisk the eggs, milk, and sugar together in a shallow dish.\n"
                + "2. Dip each slice of bread into the custard mixture for 20 seconds per side.\n"
                + "3. Melt butter in a skillet over medium heat and fry the soaked bread until golden brown on both sides.");
        addIng(r14, "Bread", 4, "slice");
        addIng(r14, "Eggs", 2, "pcs");
        addIng(r14, "Milk", 100, "ml");
        addIng(r14, "Sugar", 15, "g");
        addIng(r14, "Butter", 20, "g");
        list.add(r14);

        // 15. Cheesy White Bechamel Sauce over Toast
        Recipe r15 = new Recipe(0, "Welsh Rarebit Style Cheese Sauce on Toast", "Quick Lunch", 15, 2,
                "1. Melt butter in a small saucepan and whisk in the flour to form a smooth roux; cook for 1 minute.\n"
                + "2. Gradually whisk in the milk and simmer until thickened.\n"
                + "3. Stir in grated cheddar cheese, salt, and black pepper until melted, then spoon generously over toasted bread slices.");
        addIng(r15, "Butter", 25, "g");
        addIng(r15, "Flour", 25, "g");
        addIng(r15, "Milk", 250, "ml");
        addIng(r15, "Cheddar Cheese", 80, "g");
        addIng(r15, "Bread", 2, "slice");
        addIng(r15, "Salt", 2, "g");
        addIng(r15, "Black Pepper", 2, "g");
        list.add(r15);

        // 16. Rustic Potato & Onion Frittata (Spanish Tortilla)
        Recipe r16 = new Recipe(0, "Rustic Potato & Onion Frittata", "Lunch / Dinner", 28, 3,
                "1. Thinly slice the potatoes and onions.\n"
                + "2. Gently cook the potatoes and onions in cooking oil over medium-low heat for 15 minutes until tender.\n"
                + "3. Beat the eggs with salt and black pepper, pour over the potatoes in the pan, and cook until set on both sides.");
        addIng(r16, "Potatoes", 350, "g");
        addIng(r16, "Onion", 1, "pcs");
        addIng(r16, "Eggs", 4, "pcs");
        addIng(r16, "Cooking Oil", 45, "ml");
        addIng(r16, "Salt", 5, "g");
        addIng(r16, "Black Pepper", 2, "g");
        list.add(r16);

        // 17. Pan-Seared Garlic & Lemon Chicken Breast
        Recipe r17 = new Recipe(0, "Garlic Butter Pan-Seared Chicken", "Dinner", 22, 2,
                "1. Season the chicken breasts evenly with salt and black pepper.\n"
                + "2. Heat cooking oil in a skillet and sear the chicken for 6 minutes per side until cooked through.\n"
                + "3. Add butter and crushed garlic cloves to the pan and baste the chicken for 2 minutes before resting.");
        addIng(r17, "Chicken Breast", 400, "g");
        addIng(r17, "Garlic", 3, "clove");
        addIng(r17, "Butter", 25, "g");
        addIng(r17, "Cooking Oil", 15, "ml");
        addIng(r17, "Salt", 5, "g");
        addIng(r17, "Black Pepper", 2, "g");
        list.add(r17);

        // 18. Creamy Stovetop Rice Pudding
        Recipe r18 = new Recipe(0, "Creamy Stovetop Rice Pudding", "Dessert", 30, 4,
                "1. Combine rice, milk, butter, sugar, and a pinch of salt in a heavy saucepan.\n"
                + "2. Bring to a gentle simmer over low heat, stirring frequently for 25 minutes until the rice is tender and creamy.");
        addIng(r18, "Rice", 150, "g");
        addIng(r18, "Milk", 600, "ml");
        addIng(r18, "Sugar", 50, "g");
        addIng(r18, "Butter", 20, "g");
        addIng(r18, "Salt", 2, "g");
        list.add(r18);

        return list;
    }

    private static void addIng(Recipe recipe, String name, double qty, String unit) {
        recipe.addIngredient(new RecipeIngredient(
                name,
                IngredientNormalizer.normalize(name),
                qty,
                UnitConverter.normalizeUnit(unit)
        ));
    }
}
