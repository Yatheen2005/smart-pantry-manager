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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.InputValidator;
import za.ac.richfield.smartpantry.util.UnitConverter;

/**
 * Screen 2: Add / Edit Ingredient.
 *
 * - Started WITHOUT EXTRA_PANTRY_ITEM_ID: "Add" mode, Save performs an INSERT.
 * - Started WITH EXTRA_PANTRY_ITEM_ID: "Edit" mode, the record is loaded from SQLite and Save
 *   performs an UPDATE; a Delete button is also shown.
 *
 * All input is checked by InputValidator before anything is written to the database.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_PANTRY_ITEM_ID = "extra_pantry_item_id";
    private static final long NO_ID = -1;

    private PantryDatabaseHelper dbHelper;
    private long editingItemId = NO_ID;

    private EditText etIngredientName;
    private EditText etQuantity;
    private Spinner spinnerUnit;
    private EditText etExpiryDate;
    private TextView tvValidationError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        NavigationHelper.applyBottomSystemBarPadding(findViewById(R.id.addEditRoot));

        dbHelper = PantryDatabaseHelper.getInstance(this);

        TextView tvFormHeading = findViewById(R.id.tvFormHeading);
        etIngredientName = findViewById(R.id.etIngredientName);
        etQuantity = findViewById(R.id.etQuantity);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        tvValidationError = findViewById(R.id.tvValidationError);
        Button btnPickDate = findViewById(R.id.btnPickDate);
        Button btnSaveIngredient = findViewById(R.id.btnSaveIngredient);
        Button btnDeleteIngredient = findViewById(R.id.btnDeleteIngredient);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, UnitConverter.SUPPORTED_DISPLAY_UNITS);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);

        editingItemId = getIntent().getLongExtra(EXTRA_PANTRY_ITEM_ID, NO_ID);
        boolean isEditMode = editingItemId != NO_ID;

        int titleRes = isEditMode ? R.string.title_edit_ingredient : R.string.title_add_ingredient;
        setTitle(titleRes);
        tvFormHeading.setText(titleRes);
        btnDeleteIngredient.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        // Only load from the database on first creation. After a rotation, Android restores
        // what the user has typed, and reloading would overwrite their unsaved changes.
        if (isEditMode && savedInstanceState == null) {
            populateExistingItem(editingItemId);
        }

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnSaveIngredient.setOnClickListener(v -> savePantryItem());
        btnDeleteIngredient.setOnClickListener(v -> confirmDelete());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // return to the screen underneath (the pantry list)
        return true;
    }

    private void populateExistingItem(long id) {
        PantryItem existing = dbHelper.getPantryItemById(id);
        if (existing == null) {
            Toast.makeText(this, R.string.item_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        etIngredientName.setText(existing.getName());
        etQuantity.setText(PantryItem.formatQuantityValue(existing.getQuantity()));
        if (existing.hasExpiryDate()) {
            etExpiryDate.setText(existing.getExpiryDate());
        }
        String unit = UnitConverter.normalizeUnit(existing.getUnit());
        for (int i = 0; i < UnitConverter.SUPPORTED_DISPLAY_UNITS.length; i++) {
            if (UnitConverter.SUPPORTED_DISPLAY_UNITS[i].equals(unit)) {
                spinnerUnit.setSelection(i);
                break;
            }
        }
    }

    /** Opens a calendar, starting on the date already entered (or today). */
    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        String current = etExpiryDate.getText().toString().trim();
        if (!current.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            try {
                cal.setTime(sdf.parse(current));
            } catch (ParseException ignored) {
                // Invalid text: just start the picker on today's date
            }
        }
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> etExpiryDate.setText(
                        String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void savePantryItem() {
        String name = etIngredientName.getText().toString().trim();
        String rawQty = etQuantity.getText().toString();
        String unit = spinnerUnit.getSelectedItem() != null ? spinnerUnit.getSelectedItem().toString() : "";
        String rawExpiry = etExpiryDate.getText().toString().trim();

        InputValidator.ValidationResult validation =
                InputValidator.validatePantryInput(name, rawQty, unit, rawExpiry);
        if (!validation.isValid()) {
            showValidationError(validation);
            return;
        }
        tvValidationError.setVisibility(View.GONE);

        PantryItem item = new PantryItem(name, null, InputValidator.parseQuantity(rawQty), unit,
                rawExpiry.isEmpty() ? null : rawExpiry);

        if (editingItemId != NO_ID) {
            item.setId(editingItemId);
            if (dbHelper.updatePantryItem(item) == 1) {
                Toast.makeText(this, getString(R.string.item_updated, name), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.item_not_found, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Adding: if the same ingredient already exists in a convertible unit, offer to merge
        PantryItem duplicate = findCompatibleDuplicate(item);
        if (duplicate != null) {
            askMergeOrKeepSeparate(item, duplicate);
        } else {
            insertAndFinish(item);
        }
    }

    private void showValidationError(InputValidator.ValidationResult validation) {
        tvValidationError.setVisibility(View.VISIBLE);
        tvValidationError.setText(validation.getErrorMessage());
        String field = validation.getField();
        if (InputValidator.FIELD_NAME.equals(field)) {
            etIngredientName.setError(validation.getErrorMessage());
            etIngredientName.requestFocus();
        } else if (InputValidator.FIELD_QUANTITY.equals(field)) {
            etQuantity.setError(validation.getErrorMessage());
            etQuantity.requestFocus();
        } else if (InputValidator.FIELD_EXPIRY.equals(field)) {
            etExpiryDate.setError(validation.getErrorMessage());
            etExpiryDate.requestFocus();
        }
    }

    private PantryItem findCompatibleDuplicate(PantryItem newItem) {
        List<PantryItem> sameName = dbHelper.findPantryItemsByName(newItem.getName());
        for (PantryItem existing : sameName) {
            if (UnitConverter.areUnitsCompatible(existing.getUnit(), newItem.getUnit())) {
                return existing;
            }
        }
        return null;
    }

    private void askMergeOrKeepSeparate(PantryItem newItem, PantryItem existing) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.duplicate_title)
                .setMessage(getString(R.string.duplicate_message,
                        existing.getName(), existing.getFormattedQuantity(), newItem.getFormattedQuantity()))
                .setPositiveButton(R.string.btn_merge, (dialog, which) -> mergeAndFinish(newItem, existing))
                .setNeutralButton(R.string.btn_keep_separate, (dialog, which) -> insertAndFinish(newItem))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /**
     * Adds the new quantity (converted to the existing row's unit) to the existing row.
     * The earlier expiry date is kept, so the merged entry is never shown as fresher than it is.
     */
    private void mergeAndFinish(PantryItem newItem, PantryItem existing) {
        double addedQty = UnitConverter.convert(newItem.getQuantity(), newItem.getUnit(), existing.getUnit());
        existing.setQuantity(existing.getQuantity() + addedQty);
        existing.setExpiryDate(earlierDate(existing.getExpiryDate(), newItem.getExpiryDate()));
        dbHelper.updatePantryItem(existing);
        Toast.makeText(this, getString(R.string.item_merged, existing.getName(), existing.getFormattedQuantity()),
                Toast.LENGTH_SHORT).show();
        finish();
    }

    private void insertAndFinish(PantryItem item) {
        if (dbHelper.insertPantryItem(item) == -1) {
            Toast.makeText(this, R.string.item_save_failed, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, getString(R.string.item_added, item.getName()), Toast.LENGTH_SHORT).show();
        finish();
    }

    /** YYYY-MM-DD strings sort in date order, so a plain string comparison is enough. */
    private static String earlierDate(String a, String b) {
        if (a == null || a.isEmpty()) {
            return b;
        }
        if (b == null || b.isEmpty()) {
            return a;
        }
        return a.compareTo(b) <= 0 ? a : b;
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_item_title)
                .setMessage(R.string.delete_this_item_message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    dbHelper.deletePantryItem(editingItemId);
                    Toast.makeText(this, getString(R.string.item_deleted,
                            etIngredientName.getText().toString().trim()), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
