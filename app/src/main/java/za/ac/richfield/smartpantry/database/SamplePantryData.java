package za.ac.richfield.smartpantry.database;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import za.ac.richfield.smartpantry.model.PantryItem;

/**
 * Builds the optional demo pantry used by the "Load Sample Pantry Ingredients" buttons.
 *
 * Expiry dates are calculated relative to today so the demo data never goes stale
 * (hard-coded dates would eventually all be in the past and stop matching recipes).
 * The items deliberately use mixed names and units ("Large Eggs", "Garlic Cloves",
 * 1 l of milk, 1 kg of flour) to demonstrate normalization and unit conversion.
 *
 * With this pantry exactly 6 recipes are strict matches and 9 are "Almost There".
 */
public final class SamplePantryData {

    private SamplePantryData() {
        // Utility class
    }

    public static List<PantryItem> build(Calendar today) {
        List<PantryItem> items = new ArrayList<>();
        items.add(item("Large Eggs", 6, "pcs", today, 7));
        items.add(item("Bread Slices", 8, "slice", today, 2));      // expiring soon
        items.add(item("Butter", 250, "g", today, 30));
        items.add(item("Cheddar Cheese", 200, "g", today, 10));
        items.add(item("Fresh Tomatoes", 4, "pcs", today, 4));
        items.add(item("Onions", 3, "pcs", today, 14));
        items.add(item("Garlic Cloves", 6, "clove", today, 20));
        items.add(item("Full Cream Milk", 1, "l", today, 5));
        items.add(item("Cake Flour", 1, "kg", today, 90));
        items.add(item("Cooking Oil", 500, "ml", today, 180));
        items.add(item("Salt", 200, "g", null, 0));
        items.add(item("Black Pepper", 50, "g", null, 0));
        items.add(item("Plain Yoghurt", 500, "g", today, -2));      // already expired
        return items;
    }

    private static PantryItem item(String name, double quantity, String unit,
                                   Calendar today, int daysUntilExpiry) {
        String expiry = null;
        if (today != null) {
            Calendar date = (Calendar) today.clone();
            date.add(Calendar.DAY_OF_YEAR, daysUntilExpiry);
            expiry = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime());
        }
        return new PantryItem(name, null, quantity, unit, expiry);
    }
}
