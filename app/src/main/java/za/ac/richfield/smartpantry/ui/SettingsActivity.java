package za.ac.richfield.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.util.UserPreferencesManager;

/**
 * Screen 5: Settings Activity.
 * Allows the user to configure expiry-alert notifications, warning threshold days,
 * expired-item exclusion in strict matching, and demo database seeding/resetting.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final Integer[] THRESHOLD_OPTIONS = {2, 3, 5, 7, 14};

    private UserPreferencesManager prefsManager;
    private PantryDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);

        prefsManager = new UserPreferencesManager(this);
        dbHelper = PantryDatabaseHelper.getInstance(this);

        Switch switchExpiryAlerts = findViewById(R.id.switchExpiryAlerts);
        Spinner spinnerExpiryDays = findViewById(R.id.spinnerExpiryDays);
        Switch switchExcludeExpired = findViewById(R.id.switchExcludeExpired);
        Switch switchShowAlmostThere = findViewById(R.id.switchShowAlmostThere);
        Button btnSeedSampleData = findViewById(R.id.btnSeedSampleData);
        Button btnClearAllPantry = findViewById(R.id.btnClearAllPantry);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        ArrayAdapter<Integer> daysAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                THRESHOLD_OPTIONS
        );
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExpiryDays.setAdapter(daysAdapter);

        // Load saved preferences
        switchExpiryAlerts.setChecked(prefsManager.isExpiryAlertsEnabled());
        switchExcludeExpired.setChecked(prefsManager.isExcludeExpiredFromMatching());
        switchShowAlmostThere.setChecked(prefsManager.isShowAlmostThereSection());

        int currentThreshold = prefsManager.getExpiryThresholdDays();
        for (int i = 0; i < THRESHOLD_OPTIONS.length; i++) {
            if (THRESHOLD_OPTIONS[i] == currentThreshold) {
                spinnerExpiryDays.setSelection(i);
                break;
            }
        }

        // Listeners
        switchExpiryAlerts.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefsManager.setExpiryAlertsEnabled(isChecked));

        switchExcludeExpired.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefsManager.setExcludeExpiredFromMatching(isChecked));

        switchShowAlmostThere.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefsManager.setShowAlmostThereSection(isChecked));

        spinnerExpiryDays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefsManager.setExpiryThresholdDays(THRESHOLD_OPTIONS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnSeedSampleData.setOnClickListener(v -> {
            dbHelper.seedSamplePantryItems();
            Toast.makeText(this, "Sample pantry items loaded into SQLite!", Toast.LENGTH_SHORT).show();
        });

        btnClearAllPantry.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear All Pantry Items")
                    .setMessage("Are you sure you want to delete all ingredients from your pantry?")
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        dbHelper.clearAllPantryItems();
                        Toast.makeText(this, "All pantry items cleared.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_pantry) {
                startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_recipes) {
                startActivity(new Intent(SettingsActivity.this, SuggestedRecipesActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });
    }
}
