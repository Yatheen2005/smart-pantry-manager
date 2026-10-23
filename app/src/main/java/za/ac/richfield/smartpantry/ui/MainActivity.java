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

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.adapter.PantryAdapter;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.UserPreferencesManager;

/**
 * Screen 1: Pantry List (launcher Activity).
 *
 * Shows every row of the SQLite pantry_items table in a RecyclerView (PantryAdapter), shows an
 * empty state when there are no items, and navigates to the other screens with explicit Intents.
 *
 * Lifecycle: views are set up once in onCreate(); the list is re-read from the database in
 * onResume(), so it is always current after returning from the Add/Edit or Recipe screens.
 */
public class MainActivity extends AppCompatActivity implements PantryAdapter.OnPantryItemActionListener {

    private PantryDatabaseHelper dbHelper;
    private UserPreferencesManager prefsManager;
    private PantryAdapter pantryAdapter;

    private TextView tvPantrySummary;
    private TextView tvExpiryAlertBanner;
    private RecyclerView rvPantryItems;
    private LinearLayout layoutEmptyPantry;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.title_pantry_list);
        NavigationHelper.applyTopSystemBarPadding(findViewById(R.id.pantryRoot));

        dbHelper = PantryDatabaseHelper.getInstance(this);
        prefsManager = new UserPreferencesManager(this);

        tvPantrySummary = findViewById(R.id.tvPantrySummary);
        tvExpiryAlertBanner = findViewById(R.id.tvExpiryAlertBanner);
        rvPantryItems = findViewById(R.id.rvPantryItems);
        layoutEmptyPantry = findViewById(R.id.layoutEmptyPantry);
        Button btnQuickSuggest = findViewById(R.id.btnQuickSuggest);
        Button btnLoadDemoPantry = findViewById(R.id.btnLoadDemoPantry);
        FloatingActionButton fabAddIngredient = findViewById(R.id.fabAddIngredient);
        bottomNav = findViewById(R.id.bottomNavigation);

        pantryAdapter = new PantryAdapter(this);
        rvPantryItems.setLayoutManager(new LinearLayoutManager(this));
        rvPantryItems.setAdapter(pantryAdapter);

        // Explicit Intent with no extras = "add new ingredient" mode
        fabAddIngredient.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditIngredientActivity.class)));

        btnQuickSuggest.setOnClickListener(v ->
                startActivity(new Intent(this, SuggestedRecipesActivity.class)));

        btnLoadDemoPantry.setOnClickListener(v -> {
            int count = dbHelper.loadSamplePantry();
            refreshPantryList();
            Toast.makeText(this, getResources().getQuantityString(
                    R.plurals.sample_pantry_loaded, count, count), Toast.LENGTH_SHORT).show();
        });

        NavigationHelper.setUpBottomNavigation(this, bottomNav, R.id.nav_pantry);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_pantry);
        refreshPantryList();
    }

    /** Reads the pantry from SQLite and updates the list, summary and empty state. */
    private void refreshPantryList() {
        List<PantryItem> items = dbHelper.getAllPantryItems();
        boolean alertsEnabled = prefsManager.isExpiryAlertsEnabled();
        int warningDays = prefsManager.getExpiryThresholdDays();

        pantryAdapter.updateData(items, alertsEnabled, warningDays);
        tvPantrySummary.setText(getResources().getQuantityString(
                R.plurals.pantry_summary, items.size(), items.size()));

        boolean isEmpty = items.isEmpty();
        rvPantryItems.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmptyPantry.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

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
            tvExpiryAlertBanner.setText(getString(
                    R.string.expiry_alert_banner, expiringCount, warningDays, expiredCount));
        } else {
            tvExpiryAlertBanner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEditItem(PantryItem item) {
        // Pass the database id so the edit screen loads the current record itself
        Intent intent = new Intent(this, AddEditIngredientActivity.class);
        intent.putExtra(AddEditIngredientActivity.EXTRA_PANTRY_ITEM_ID, item.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteItem(PantryItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_item_title)
                .setMessage(getString(R.string.delete_item_message, item.getName(), item.getFormattedQuantity()))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    dbHelper.deletePantryItem(item.getId());
                    refreshPantryList();
                    Toast.makeText(this, getString(R.string.item_deleted, item.getName()),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
