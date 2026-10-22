package za.ac.richfield.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Locale;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.adapter.PantryAdapter;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.UserPreferencesManager;

/**
 * Screen 1: Pantry List Activity.
 * Displays all current ingredients in the user's SQLite pantry using a RecyclerView and
 * PantryAdapter, highlights expiring items, and navigates to Add/Edit, Suggested Recipes,
 * and Settings screens via explicit Intents.
 */
public class MainActivity extends AppCompatActivity implements PantryAdapter.OnPantryItemActionListener {

    private PantryDatabaseHelper dbHelper;
    private UserPreferencesManager prefsManager;
    private PantryAdapter pantryAdapter;

    private TextView tvPantrySummary;
    private TextView tvExpiryAlertBanner;
    private RecyclerView rvPantryItems;
    private LinearLayout layoutEmptyPantry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.title_pantry_list);

        dbHelper = PantryDatabaseHelper.getInstance(this);
        prefsManager = new UserPreferencesManager(this);

        tvPantrySummary = findViewById(R.id.tvPantrySummary);
        tvExpiryAlertBanner = findViewById(R.id.tvExpiryAlertBanner);
        rvPantryItems = findViewById(R.id.rvPantryItems);
        layoutEmptyPantry = findViewById(R.id.layoutEmptyPantry);
        Button btnQuickSuggest = findViewById(R.id.btnQuickSuggest);
        Button btnLoadDemoPantry = findViewById(R.id.btnLoadDemoPantry);
        FloatingActionButton fabAddIngredient = findViewById(R.id.fabAddIngredient);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        pantryAdapter = new PantryAdapter(this);
        rvPantryItems.setLayoutManager(new LinearLayoutManager(this));
        rvPantryItems.setAdapter(pantryAdapter);

        fabAddIngredient.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditIngredientActivity.class);
            startActivity(intent);
        });

        btnQuickSuggest.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SuggestedRecipesActivity.class);
            startActivity(intent);
        });

        btnLoadDemoPantry.setOnClickListener(v -> {
            dbHelper.seedSamplePantryItems();
            refreshPantryList();
            Toast.makeText(this, "Loaded 12 sample pantry ingredients!", Toast.LENGTH_SHORT).show();
        });

        bottomNav.setSelectedItemId(R.id.nav_pantry);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_pantry) {
                return true;
            } else if (itemId == R.id.nav_recipes) {
                startActivity(new Intent(MainActivity.this, SuggestedRecipesActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_pantry);
        refreshPantryList();
    }

    private void refreshPantryList() {
        List<PantryItem> items = dbHelper.getAllPantryItems();
        boolean alertsEnabled = prefsManager.isExpiryAlertsEnabled();
        int warningDays = prefsManager.getExpiryThresholdDays();

        pantryAdapter.updateData(items, alertsEnabled, warningDays);

        if (items.isEmpty()) {
            rvPantryItems.setVisibility(View.GONE);
            layoutEmptyPantry.setVisibility(View.VISIBLE);
            tvPantrySummary.setText("0 Ingredients in Pantry");
            tvExpiryAlertBanner.setVisibility(View.GONE);
        } else {
            rvPantryItems.setVisibility(View.VISIBLE);
            layoutEmptyPantry.setVisibility(View.GONE);
            tvPantrySummary.setText(String.format(
                    Locale.US, "%d Ingredient%s in Pantry",
                    items.size(), items.size() == 1 ? "" : "s"
            ));

            int expiringCount = 0;
            int expiredCount = 0;
            for (PantryItem item : items) {
                if (item.isExpired()) {
                    expiredCount++;
                } else if (item.isExpiringSoon(warningDays)) {
                    expiringCount++;
                }
            }

            if (alertsEnabled && (expiredCount > 0 || expiringCount > 0)) {
                tvExpiryAlertBanner.setVisibility(View.VISIBLE);
                tvExpiryAlertBanner.setText(String.format(
                        Locale.US,
                        "Expiry Alert: %d expiring within %d days, %d expired.",
                        expiringCount, warningDays, expiredCount
                ));
            } else {
                tvExpiryAlertBanner.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onEditItem(PantryItem item) {
        Intent intent = new Intent(MainActivity.this, AddEditIngredientActivity.class);
        intent.putExtra(AddEditIngredientActivity.EXTRA_PANTRY_ITEM_ID, item.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteItem(PantryItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Ingredient")
                .setMessage("Remove '" + item.getName() + "' (" + item.getFormattedQuantity() + ") from your pantry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deletePantryItem(item.getId());
                    refreshPantryList();
                    Toast.makeText(this, "Deleted " + item.getName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
