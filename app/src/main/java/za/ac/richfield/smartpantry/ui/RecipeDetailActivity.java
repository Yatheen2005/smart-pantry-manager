package za.ac.richfield.smartpantry.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;
import za.ac.richfield.smartpantry.util.StrictRecipeMatcher;
import za.ac.richfield.smartpantry.util.UserPreferencesManager;

/**
 * Screen 4: Recipe Detail Activity.
 * Displays the selected recipe's full required ingredient list, real-time pantry sufficiency
 * verification, preparation method, and a button to deduct cooked ingredients from the pantry.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private PantryDatabaseHelper dbHelper;
    private UserPreferencesManager prefsManager;
    private Recipe currentRecipe;

    private TextView tvDetailTitle;
    private TextView tvDetailMeta;
    private TextView tvDetailMatchBanner;
    private TextView tvDetailIngredientsList;
    private TextView tvDetailInstructions;
    private Button btnCookAndDeduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);
        setTitle(R.string.title_recipe_detail);

        dbHelper = PantryDatabaseHelper.getInstance(this);
        prefsManager = new UserPreferencesManager(this);

        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailMeta = findViewById(R.id.tvDetailMeta);
        tvDetailMatchBanner = findViewById(R.id.tvDetailMatchBanner);
        tvDetailIngredientsList = findViewById(R.id.tvDetailIngredientsList);
        tvDetailInstructions = findViewById(R.id.tvDetailInstructions);
        btnCookAndDeduct = findViewById(R.id.btnCookAndDeduct);

        long recipeId = getIntent() != null ? getIntent().getLongExtra(EXTRA_RECIPE_ID, -1) : -1;
        if (recipeId == -1) {
            Toast.makeText(this, "Invalid recipe selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadRecipeDetails(recipeId);

        btnCookAndDeduct.setOnClickListener(v -> {
            if (currentRecipe == null) {
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Cook " + currentRecipe.getTitle())
                    .setMessage("Deduct the required quantities for this recipe from your pantry?")
                    .setPositiveButton("Cook & Deduct", (dialog, which) -> {
                        dbHelper.consumeRecipeIngredients(currentRecipe);
                        Toast.makeText(this,
                                "Enjoy your meal! Pantry quantities updated.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadRecipeDetails(long recipeId) {
        currentRecipe = dbHelper.getRecipeById(recipeId);
        if (currentRecipe == null) {
            Toast.makeText(this, "Recipe not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        List<PantryItem> pantryItems = dbHelper.getAllPantryItems();
        RecipeMatchResult matchResult = StrictRecipeMatcher.evaluateRecipe(
                currentRecipe, pantryItems, prefsManager.isExcludeExpiredFromMatching()
        );

        tvDetailTitle.setText(currentRecipe.getTitle());
        tvDetailMeta.setText(String.format(
                Locale.US,
                "%s • Prep Time: %d mins • Serves %d",
                currentRecipe.getCategory(),
                currentRecipe.getPrepTimeMinutes(),
                currentRecipe.getServings()
        ));

        if (matchResult.isStrictMatch()) {
            tvDetailMatchBanner.setText(
                    "Strict Match Verified: Every required ingredient is in your pantry in sufficient quantity!"
            );
            tvDetailMatchBanner.setBackgroundResource(R.drawable.bg_badge_green);
            btnCookAndDeduct.setVisibility(View.VISIBLE);
        } else {
            tvDetailMatchBanner.setText(String.format(
                    Locale.US,
                    "Missing or Insufficient: %d of %d required ingredients.",
                    matchResult.getMissingOrInsufficientCount(),
                    matchResult.getTotalIngredientsCount()
            ));
            tvDetailMatchBanner.setBackgroundResource(R.drawable.bg_badge_amber);
            btnCookAndDeduct.setVisibility(View.GONE);
        }

        StringBuilder ingBuilder = new StringBuilder();
        for (String matched : matchResult.getMatchedBreakdown()) {
            ingBuilder.append("✓ ").append(matched).append("
");
        }
        for (String missing : matchResult.getMissingBreakdown()) {
            ingBuilder.append("✗ ").append(missing).append("
");
        }
        tvDetailIngredientsList.setText(ingBuilder.toString().trim());
        tvDetailInstructions.setText(currentRecipe.getInstructions());
    }
}
