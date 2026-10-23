package za.ac.richfield.smartpantry.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import za.ac.richfield.smartpantry.R;

/**
 * Shared navigation code used by the three top-level screens (Pantry, Recipes, Settings).
 *
 * Back stack rule: MainActivity (Pantry) always stays at the bottom of the stack and at most
 * ONE other tab sits on top of it. So pressing Back from Recipes or Settings returns to the
 * Pantry, and switching tabs never piles up duplicate screens.
 */
public final class NavigationHelper {

    private NavigationHelper() {
        // Utility class
    }

    /**
     * Highlights the current tab and handles taps on the other tabs with explicit Intents.
     */
    public static void setUpBottomNavigation(Activity activity, BottomNavigationView bottomNav,
                                             int currentItemId) {
        bottomNav.setSelectedItemId(currentItemId);
        bottomNav.setOnItemSelectedListener(item -> {
            int targetId = item.getItemId();
            if (targetId == currentItemId) {
                return true;
            }
            boolean onPantryTab = currentItemId == R.id.nav_pantry;
            if (targetId == R.id.nav_pantry) {
                // Return to the existing MainActivity instead of creating a new one
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
            } else if (targetId == R.id.nav_recipes) {
                activity.startActivity(new Intent(activity, SuggestedRecipesActivity.class));
            } else if (targetId == R.id.nav_settings) {
                activity.startActivity(new Intent(activity, SettingsActivity.class));
            } else {
                return false;
            }
            if (!onPantryTab) {
                activity.finish(); // replace this tab rather than stacking on top of it
            }
            return true;
        });
    }

    /**
     * Keeps content clear of the navigation bar on Android 15+ (which draws apps edge-to-edge)
     * by adding the system-bar insets as padding to the given view.
     */
    public static void applyBottomSystemBarPadding(View view) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(left + bars.left, top, right + bars.right, bottom + bars.bottom);
            return windowInsets;
        });
    }

    /**
     * Keeps the top of a screen below the status bar and action bar on Android 15+ (edge-to-edge).
     * There, AppCompat no longer pushes the content down itself; instead it passes the status-bar
     * height plus the action-bar height as the top inset, which is added here as top padding.
     * On older versions that top inset is 0, so the layout is unchanged.
     */
    public static void applyTopSystemBarPadding(View view) {
        final int top = view.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), top + bars.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets; // pass on unchanged so the bottom navigation still gets its inset
        });
    }
}
