package za.ac.richfield.smartpantry.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.IngredientCheck;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;
import za.ac.richfield.smartpantry.util.StrictRecipeMatcher;
import za.ac.richfield.smartpantry.util.UserPreferencesManager;

/**
 * Screen 4: Recipe Detail.
 *
 * Receives a recipe id through the Intent (EXTRA_RECIPE_ID), loads the recipe from SQLite and
 * shows every required ingredient with a live pantry check (✓ / ✗), the method, and - only
 * when the recipe is a strict match - a button that deducts the used ingredients.
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        NavigationHelper.applyBottomSystemBarPadding(findViewById(R.id.recipeDetailRoot));

        dbHelper = PantryDatabaseHelper.getInstance(this);
        prefsManager = new UserPreferencesManager(this);

        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailMeta = findViewById(R.id.tvDetailMeta);
        tvDetailMatchBanner = findViewById(R.id.tvDetailMatchBanner);
        tvDetailIngredientsList = findViewById(R.id.tvDetailIngredientsList);
        tvDetailInstructions = findViewById(R.id.tvDetailInstructions);
        btnCookAndDeduct = findViewById(R.id.btnCookAndDeduct);

        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1);
        currentRecipe = recipeId == -1 ? null : dbHelper.getRecipeById(recipeId);
        if (currentRecipe == null) {
            Toast.makeText(this, R.string.recipe_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnCookAndDeduct.setOnClickListener(v -> confirmCook());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentRecipe != null) {
            showRecipe();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** Re-checks the recipe against the current pantry and fills in every view. */
    private void showRecipe() {
        RecipeMatchResult match = StrictRecipeMatcher.evaluateRecipe(
                currentRecipe, dbHelper.getAllPantryItems(), prefsManager.isExcludeExpiredFromMatching());

        tvDetailTitle.setText(currentRecipe.getTitle());
        tvDetailMeta.setText(getString(R.string.detail_meta, currentRecipe.getCategory(),
                currentRecipe.getPrepTimeMinutes(), currentRecipe.getServings()));

        if (match.isStrictMatch()) {
            tvDetailMatchBanner.setText(R.string.detail_strict_match);
            tvDetailMatchBanner.setBackgroundResource(R.drawable.bg_badge_green);
            tvDetailMatchBanner.setTextColor(ContextCompat.getColor(this, R.color.primary_green_dark));
            btnCookAndDeduct.setVisibility(View.VISIBLE);
        } else {
            tvDetailMatchBanner.setText(getString(R.string.detail_not_available,
                    match.getMissingOrInsufficientCount(), match.getTotalIngredientsCount()));
            tvDetailMatchBanner.setBackgroundResource(R.drawable.bg_badge_amber);
            tvDetailMatchBanner.setTextColor(ContextCompat.getColor(this, R.color.accent_amber_text));
            btnCookAndDeduct.setVisibility(View.GONE);
        }

        // One line per ingredient, in the recipe's own order
        StringBuilder lines = new StringBuilder();
        for (IngredientCheck check : match.getChecks()) {
            if (lines.length() > 0) {
                lines.append('\n');
            }
            int format = check.isSatisfied() ? R.string.check_available : R.string.check_unavailable;
            lines.append(getString(format, check.describe()));
        }
        tvDetailIngredientsList.setText(lines.toString());
        tvDetailInstructions.setText(currentRecipe.getInstructions());
    }

    private void confirmCook() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.cook_dialog_title, currentRecipe.getTitle()))
                .setMessage(R.string.cook_dialog_message)
                .setPositiveButton(R.string.btn_cook_confirm, (dialog, which) -> {
                    boolean cooked = dbHelper.consumeRecipeIngredients(
                            currentRecipe, prefsManager.isExcludeExpiredFromMatching());
                    if (cooked) {
                        Toast.makeText(this, R.string.cooked_toast, Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.cook_failed, Toast.LENGTH_LONG).show();
                        showRecipe();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
