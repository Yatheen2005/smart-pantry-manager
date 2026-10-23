package za.ac.richfield.smartpantry.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.util.UserPreferencesManager;

/**
 * Screen 5: Settings.
 *
 * Preferences are saved immediately to SharedPreferences (UserPreferencesManager) and are read
 * by the Pantry and Suggested Recipes screens in their onResume(). Also offers demo-data tools.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final Integer[] THRESHOLD_OPTIONS = {1, 2, 3, 5, 7, 14};

    private UserPreferencesManager prefsManager;
    private PantryDatabaseHelper dbHelper;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);
        NavigationHelper.applyTopSystemBarPadding(findViewById(R.id.settingsRoot));

        prefsManager = new UserPreferencesManager(this);
        dbHelper = PantryDatabaseHelper.getInstance(this);

        SwitchMaterial switchExpiryAlerts = findViewById(R.id.switchExpiryAlerts);
        Spinner spinnerExpiryDays = findViewById(R.id.spinnerExpiryDays);
        SwitchMaterial switchExcludeExpired = findViewById(R.id.switchExcludeExpired);
        SwitchMaterial switchShowAlmostThere = findViewById(R.id.switchShowAlmostThere);
        Button btnSeedSampleData = findViewById(R.id.btnSeedSampleData);
        Button btnClearAllPantry = findViewById(R.id.btnClearAllPantry);
        bottomNav = findViewById(R.id.bottomNavigation);

        ArrayAdapter<Integer> daysAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, THRESHOLD_OPTIONS);
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExpiryDays.setAdapter(daysAdapter);

        // Show the saved values
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

        // Save each change straight away
        switchExpiryAlerts.setOnCheckedChangeListener((button, isChecked) ->
                prefsManager.setExpiryAlertsEnabled(isChecked));
        switchExcludeExpired.setOnCheckedChangeListener((button, isChecked) ->
                prefsManager.setExcludeExpiredFromMatching(isChecked));
        switchShowAlmostThere.setOnCheckedChangeListener((button, isChecked) ->
                prefsManager.setShowAlmostThereSection(isChecked));
        spinnerExpiryDays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefsManager.setExpiryThresholdDays(THRESHOLD_OPTIONS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do
            }
        });

        btnSeedSampleData.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.sample_replace_title)
                .setMessage(R.string.sample_replace_message)
                .setPositiveButton(R.string.btn_load, (dialog, which) -> {
                    int count = dbHelper.loadSamplePantry();
                    Toast.makeText(this, getResources().getQuantityString(
                            R.plurals.sample_pantry_loaded, count, count), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show());

        btnClearAllPantry.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.clear_pantry_title)
                .setMessage(R.string.clear_pantry_message)
                .setPositiveButton(R.string.btn_clear_all, (dialog, which) -> {
                    dbHelper.clearAllPantryItems();
                    Toast.makeText(this, R.string.pantry_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show());

        NavigationHelper.setUpBottomNavigation(this, bottomNav, R.id.nav_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_settings);
    }
}
