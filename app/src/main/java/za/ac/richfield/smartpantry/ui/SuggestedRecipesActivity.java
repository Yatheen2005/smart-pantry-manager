package za.ac.richfield.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.adapter.RecipeAdapter;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;
import za.ac.richfield.smartpantry.util.StrictRecipeMatcher;
import za.ac.richfield.smartpantry.util.UserPreferencesManager;

/**
 * Screen 3: Suggested Recipes.
 *
 * Runs StrictRecipeMatcher against the current pantry and lists ONLY recipes whose every
 * ingredient is available in the required quantity. Shows clear feedback when nothing matches.
 * The optional "Almost There" list (missing exactly one ingredient) is a separate section
 * below a divider and is never mixed into the main suggestions.
 */
public class SuggestedRecipesActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeClickListener {

    private PantryDatabaseHelper dbHelper;
    private UserPreferencesManager prefsManager;

    private TextView tvStrictMatchCount;
    private LinearLayout layoutNoMatchesFeedback;
    private TextView tvNoMatchesMessage;
    private RecyclerView rvStrictRecipes;
    private LinearLayout layoutAlmostThereSection;
    private BottomNavigationView bottomNav;

    private RecipeAdapter strictAdapter;
    private RecipeAdapter almostThereAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);
        setTitle(R.string.title_suggested_recipes);
        NavigationHelper.applyTopSystemBarPadding(findViewById(R.id.suggestedRecipesRoot));

        dbHelper = PantryDatabaseHelper.getInstance(this);
        prefsManager = new UserPreferencesManager(this);

        tvStrictMatchCount = findViewById(R.id.tvStrictMatchCount);
        layoutNoMatchesFeedback = findViewById(R.id.layoutNoMatchesFeedback);
        tvNoMatchesMessage = findViewById(R.id.tvNoMatchesMessage);
        rvStrictRecipes = findViewById(R.id.rvStrictRecipes);
        layoutAlmostThereSection = findViewById(R.id.layoutAlmostThereSection);
        RecyclerView rvAlmostThereRecipes = findViewById(R.id.rvAlmostThereRecipes);
        Button btnAddMoreIngredients = findViewById(R.id.btnAddMoreIngredients);
        bottomNav = findViewById(R.id.bottomNavigation);

        strictAdapter = new RecipeAdapter(this);
        rvStrictRecipes.setLayoutManager(new LinearLayoutManager(this));
        rvStrictRecipes.setAdapter(strictAdapter);

        almostThereAdapter = new RecipeAdapter(this);
        rvAlmostThereRecipes.setLayoutManager(new LinearLayoutManager(this));
        rvAlmostThereRecipes.setAdapter(almostThereAdapter);

        btnAddMoreIngredients.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditIngredientActivity.class)));

        NavigationHelper.setUpBottomNavigation(this, bottomNav, R.id.nav_recipes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_recipes);
        // Re-run matching every time the screen is shown: the pantry may have changed
        evaluateAndDisplayRecipes();
    }

    private void evaluateAndDisplayRecipes() {
        List<PantryItem> pantryItems = dbHelper.getAllPantryItems();
        List<Recipe> allRecipes = dbHelper.getAllRecipesWithIngredients();
        boolean excludeExpired = prefsManager.isExcludeExpiredFromMatching();

        List<RecipeMatchResult> strictMatches =
                StrictRecipeMatcher.getStrictMatches(allRecipes, pantryItems, excludeExpired);

        tvStrictMatchCount.setText(getString(R.string.strict_match_count, strictMatches.size(), allRecipes.size()));
        strictAdapter.updateData(strictMatches);

        if (strictMatches.isEmpty()) {
            rvStrictRecipes.setVisibility(View.GONE);
            layoutNoMatchesFeedback.setVisibility(View.VISIBLE);
            tvNoMatchesMessage.setText(pantryItems.isEmpty()
                    ? R.string.empty_pantry_no_matches
                    : R.string.no_recipes_match_message);
        } else {
            rvStrictRecipes.setVisibility(View.VISIBLE);
            layoutNoMatchesFeedback.setVisibility(View.GONE);
        }

        List<RecipeMatchResult> almostThere = prefsManager.isShowAlmostThereSection()
                ? StrictRecipeMatcher.getAlmostThereMatches(allRecipes, pantryItems, excludeExpired)
                : null;
        if (almostThere != null && !almostThere.isEmpty()) {
            layoutAlmostThereSection.setVisibility(View.VISIBLE);
            almostThereAdapter.updateData(almostThere);
        } else {
            layoutAlmostThereSection.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRecipeClick(RecipeMatchResult matchResult) {
        // Pass only the recipe id; the detail screen re-reads the recipe and pantry itself
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, matchResult.getRecipe().getId());
        startActivity(intent);
    }
}
