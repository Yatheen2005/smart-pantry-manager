package za.ac.richfield.smartpantry.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.IngredientNormalizer;
import za.ac.richfield.smartpantry.util.InputValidator;
import za.ac.richfield.smartpantry.util.UnitConverter;

/**
 * Screen 2: Add / Edit Pantry Ingredient Activity.
 * Handles creating new pantry records and updating or deleting existing records
 * with form validation (InputValidator) and DatePickerDialog support.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_PANTRY_ITEM_ID = "extra_pantry_item_id";

    private PantryDatabaseHelper dbHelper;
    private long editingItemId = -1;

    private TextView tvFormHeading;
    private EditText etIngredientName;
    private EditText etQuantity;
    private Spinner spinnerUnit;
    private EditText etExpiryDate;
    private TextView tvValidationError;
    private Button btnDeleteIngredient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        dbHelper = PantryDatabaseHelper.getInstance(this);

        tvFormHeading = findViewById(R.id.tvFormHeading);
        etIngredientName = findViewById(R.id.etIngredientName);
        etQuantity = findViewById(R.id.etQuantity);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        tvValidationError = findViewById(R.id.tvValidationError);
        Button btnPickDate = findViewById(R.id.btnPickDate);
        Button btnSaveIngredient = findViewById(R.id.btnSaveIngredient);
        btnDeleteIngredient = findViewById(R.id.btnDeleteIngredient);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                UnitConverter.SUPPORTED_DISPLAY_UNITS
        );
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_PANTRY_ITEM_ID)) {
            editingItemId = getIntent().getLongExtra(EXTRA_PANTRY_ITEM_ID, -1);
        }

        if (editingItemId != -1) {
            setTitle(R.string.title_edit_ingredient);
            tvFormHeading.setText(R.string.title_edit_ingredient);
            btnDeleteIngredient.setVisibility(View.VISIBLE);
            populateExistingItem(editingItemId);
        } else {
            setTitle(R.string.title_add_ingredient);
            tvFormHeading.setText(R.string.title_add_ingredient);
            btnDeleteIngredient.setVisibility(View.GONE);
        }

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnSaveIngredient.setOnClickListener(v -> savePantryItem());
        btnDeleteIngredient.setOnClickListener(v -> confirmDelete());
    }

    private void populateExistingItem(long id) {
        PantryItem existing = dbHelper.getPantryItemById(id);
        if (existing == null) {
            Toast.makeText(this, "Ingredient not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        etIngredientName.setText(existing.getName());
        etQuantity.setText(PantryItem.formatQuantityValue(existing.getQuantity()));
        if (existing.hasExpiryDate()) {
            etExpiryDate.setText(existing.getExpiryDate());
        }
        String normUnit = UnitConverter.normalizeUnit(existing.getUnit());
        for (int i = 0; i < UnitConverter.SUPPORTED_DISPLAY_UNITS.length; i++) {
            if (UnitConverter.SUPPORTED_DISPLAY_UNITS[i].equalsIgnoreCase(normUnit)) {
                spinnerUnit.setSelection(i);
                break;
            }
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    etExpiryDate.setText(formatted);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void savePantryItem() {
        String rawName = etIngredientName.getText().toString();
        String rawQty = etQuantity.getText().toString();
        String selectedUnit = spinnerUnit.getSelectedItem() != null
                ? spinnerUnit.getSelectedItem().toString() : "";
        String rawExpiry = etExpiryDate.getText().toString();

        InputValidator.ValidationResult validation = InputValidator.validatePantryInput(
                rawName, rawQty, selectedUnit, rawExpiry
        );

        if (!validation.isValid()) {
            tvValidationError.setVisibility(View.VISIBLE);
            tvValidationError.setText(validation.getErrorMessage());
            if ("name".equals(validation.getField())) {
                etIngredientName.setError(validation.getErrorMessage());
            } else if ("quantity".equals(validation.getField())) {
                etQuantity.setError(validation.getErrorMessage());
            } else if ("expiry".equals(validation.getField())) {
                etExpiryDate.setError(validation.getErrorMessage());
            }
            return;
        }

        tvValidationError.setVisibility(View.GONE);
        double quantity = Double.parseDouble(rawQty.trim());
        String normName = IngredientNormalizer.normalize(rawName);
        String normUnit = UnitConverter.normalizeUnit(selectedUnit);
        String cleanExpiry = rawExpiry.trim().isEmpty() ? null : rawExpiry.trim();

        if (editingItemId == -1) {
            PantryItem newItem = new PantryItem(rawName.trim(), normName, quantity, normUnit, cleanExpiry);
            dbHelper.insertPantryItem(newItem);
            Toast.makeText(this, "Added " + newItem.getName() + " to pantry!", Toast.LENGTH_SHORT).show();
        } else {
            PantryItem updated = new PantryItem(
                    editingItemId, rawName.trim(), normName, quantity, normUnit,
                    cleanExpiry, System.currentTimeMillis()
            );
            dbHelper.updatePantryItem(updated);
            Toast.makeText(this, "Updated " + updated.getName() + "!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Pantry Item")
                .setMessage("Are you sure you want to delete this ingredient from your pantry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deletePantryItem(editingItemId);
                    Toast.makeText(this, "Ingredient deleted.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
