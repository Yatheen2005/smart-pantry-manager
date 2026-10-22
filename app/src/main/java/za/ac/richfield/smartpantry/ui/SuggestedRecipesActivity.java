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
import java.util.Locale;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.adapter.RecipeAdapter;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;
import za.ac.richfield.smartpantry.util.StrictRecipeMatcher;
import za.ac.richfield.smartpantry.util.UserPreferencesManager;

/**
 * Screen 3: Suggested Recipes Activity.
 * Runs StrictRecipeMatcher against the user's current SQLite pantry and displays ONLY
 * 100% matched recipes in the main list, with explicit empty-state feedback when zero
 * recipes match (Section 2.2 & Section 2.3).
 */
public class SuggestedRecipesActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeClickListener {

    private PantryDatabaseHelper dbHelper;
    private UserPreferencesManager prefsManager;

    private TextView tvStrictMatchCount;
    private LinearLayout layoutNoMatchesFeedback;
    private TextView tvNoMatchesMessage;
    private RecyclerView rvStrictRecipes;
    private LinearLayout layoutAlmostThereSection;
    private RecyclerView rvAlmostThereRecipes;

    private RecipeAdapter strictAdapter;
    private RecipeAdapter almostThereAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);
        setTitle(R.string.title_suggested_recipes);

        dbHelper = PantryDatabaseHelper.getInstance(this);
        prefsManager = new UserPreferencesManager(this);

        tvStrictMatchCount = findViewById(R.id.tvStrictMatchCount);
        layoutNoMatchesFeedback = findViewById(R.id.layoutNoMatchesFeedback);
        tvNoMatchesMessage = findViewById(R.id.tvNoMatchesMessage);
        rvStrictRecipes = findViewById(R.id.rvStrictRecipes);
        layoutAlmostThereSection = findViewById(R.id.layoutAlmostThereSection);
        rvAlmostThereRecipes = findViewById(R.id.rvAlmostThereRecipes);
        Button btnAddMoreIngredients = findViewById(R.id.btnAddMoreIngredients);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        strictAdapter = new RecipeAdapter(this);
        rvStrictRecipes.setLayoutManager(new LinearLayoutManager(this));
        rvStrictRecipes.setAdapter(strictAdapter);

        almostThereAdapter = new RecipeAdapter(this);
        rvAlmostThereRecipes.setLayoutManager(new LinearLayoutManager(this));
        rvAlmostThereRecipes.setAdapter(almostThereAdapter);

        btnAddMoreIngredients.setOnClickListener(v -> {
            startActivity(new Intent(SuggestedRecipesActivity.this, AddEditIngredientActivity.class));
        });

        bottomNav.setSelectedItemId(R.id.nav_recipes);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_pantry) {
                startActivity(new Intent(SuggestedRecipesActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_recipes) {
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(SuggestedRecipesActivity.this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        evaluateAndDisplayRecipes();
    }

    private void evaluateAndDisplayRecipes() {
        List<PantryItem> pantryItems = dbHelper.getAllPantryItems();
        List<Recipe> allRecipes = dbHelper.getAllRecipesWithIngredients();
        boolean excludeExpired = prefsManager.isExcludeExpiredFromMatching();

        List<RecipeMatchResult> strictMatches = StrictRecipeMatcher.getStrictMatches(
                allRecipes, pantryItems, excludeExpired
        );
        List<RecipeMatchResult> almostThereMatches = StrictRecipeMatcher.getAlmostThereMatches(
                allRecipes, pantryItems, excludeExpired
        );

        tvStrictMatchCount.setText(String.format(
                Locale.US,
                "Strict Recipe Matches: %d of %d Recipes Ready Now",
                strictMatches.size(),
                allRecipes.size()
        ));

        strictAdapter.updateData(strictMatches);

        if (strictMatches.isEmpty()) {
            rvStrictRecipes.setVisibility(View.GONE);
            layoutNoMatchesFeedback.setVisibility(View.VISIBLE);
            if (pantryItems.isEmpty()) {
                tvNoMatchesMessage.setText(
                        "Your pantry is empty — no recipes match yet. Add ingredients you have at home to see what you can cook!"
                );
            } else {
                tvNoMatchesMessage.setText(R.string.no_recipes_match_message);
            }
        } else {
            rvStrictRecipes.setVisibility(View.VISIBLE);
            layoutNoMatchesFeedback.setVisibility(View.GONE);
        }

        if (prefsManager.isShowAlmostThereSection() && !almostThereMatches.isEmpty()) {
            layoutAlmostThereSection.setVisibility(View.VISIBLE);
            almostThereAdapter.updateData(almostThereMatches);
        } else {
            layoutAlmostThereSection.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRecipeClick(RecipeMatchResult matchResult) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, matchResult.getRecipe().getId());
        startActivity(intent);
    }
}
