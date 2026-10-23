package za.ac.richfield.smartpantry;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import za.ac.richfield.smartpantry.database.PantryDatabaseHelper;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.ui.MainActivity;
import za.ac.richfield.smartpantry.util.UserPreferencesManager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

/**
 * End-to-end UI tests (Espresso) for navigation, validation, CRUD and strict matching.
 *
 * NOTE: these drive the real screens, which use the app's own database, so each test starts by
 * clearing the pantry on the device/emulator it runs on.
 */
@RunWith(AndroidJUnit4.class)
public class PantryUiFlowTest {

    private PantryDatabaseHelper db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = PantryDatabaseHelper.getInstance(context);
        db.clearAllPantryItems();
        UserPreferencesManager prefs = new UserPreferencesManager(context);
        prefs.setExcludeExpiredFromMatching(true);
        prefs.setShowAlmostThereSection(true);
        prefs.setExpiryAlertsEnabled(true);
    }

    @Test
    public void emptyPantry_showsEmptyStateAndZeroMatchMessage() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.empty_pantry_message)).check(matches(isDisplayed()));

            onView(withId(R.id.nav_recipes)).perform(click());
            onView(withId(R.id.tvNoMatchesMessage)).check(matches(withText(R.string.empty_pantry_no_matches)));
            onView(withId(R.id.tvStrictMatchCount))
                    .check(matches(withText("0 of 18 recipes can be cooked right now")));
        }
    }

    @Test
    public void addIngredient_showsValidationErrorsThenSavesToTheList() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAddIngredient)).perform(click());

            // Empty form -> name error
            onView(withId(R.id.btnSaveIngredient)).perform(scrollTo(), click());
            onView(withId(R.id.tvValidationError)).check(matches(withText("Ingredient name is required.")));

            // Zero quantity -> quantity error
            onView(withId(R.id.etIngredientName)).perform(replaceText("Tomatoes"));
            onView(withId(R.id.etQuantity)).perform(replaceText("0"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveIngredient)).perform(scrollTo(), click());
            onView(withId(R.id.tvValidationError)).check(matches(withText("Quantity must be greater than zero.")));

            // Impossible date -> expiry error
            onView(withId(R.id.etQuantity)).perform(replaceText("4"));
            onView(withId(R.id.etExpiryDate)).perform(replaceText("2026-02-30"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveIngredient)).perform(scrollTo(), click());
            onView(withId(R.id.tvValidationError))
                    .check(matches(withText("Invalid calendar date (check the month and day).")));

            // Valid -> saved and shown in the RecyclerView
            onView(withId(R.id.etExpiryDate)).perform(replaceText(""), closeSoftKeyboard());
            onView(withId(R.id.btnSaveIngredient)).perform(scrollTo(), click());
            onView(withId(R.id.rvPantryItems)).check(matches(hasDescendant(withText("Tomatoes"))));
            onView(withId(R.id.rvPantryItems)).check(matches(hasDescendant(withText("Available: 4 pcs"))));
        }
    }

    @Test
    public void editAndDeleteIngredient_updateTheList() {
        db.insertPantryItem(new PantryItem("Butter", null, 250, "g", null));
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            // UPDATE through the edit screen (opened with the item id in the Intent)
            onView(withContentDescription("Edit Butter")).perform(click());
            onView(withId(R.id.etIngredientName)).check(matches(withText("Butter")));
            onView(withId(R.id.etQuantity)).perform(replaceText("100"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveIngredient)).perform(scrollTo(), click());
            onView(withId(R.id.rvPantryItems)).check(matches(hasDescendant(withText("Available: 100 g"))));

            // DELETE with confirmation dialog
            onView(withContentDescription("Delete Butter")).perform(click());
            onView(withText(R.string.btn_delete)).inRoot(isDialog()).perform(click());
            onView(withText(R.string.empty_pantry_message)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void addingDuplicate_canBeMergedIntoExistingEntry() {
        db.insertPantryItem(new PantryItem("Eggs", null, 2, "pcs", null));
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAddIngredient)).perform(click());
            onView(withId(R.id.etIngredientName)).perform(replaceText("Egg"));
            onView(withId(R.id.etQuantity)).perform(replaceText("4"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveIngredient)).perform(scrollTo(), click());

            onView(withText(R.string.btn_merge)).inRoot(isDialog()).perform(click());
            onView(withId(R.id.rvPantryItems)).check(matches(hasDescendant(withText("Available: 6 pcs"))));
        }
    }

    @Test
    public void recipeAppearsOnlyWhenTheMissingIngredientIsAdded() {
        db.loadSamplePantry();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_recipes)).perform(click());
            onView(withId(R.id.tvStrictMatchCount))
                    .check(matches(withText("6 of 18 recipes can be cooked right now")));
            onView(withId(R.id.rvStrictRecipes))
                    .check(matches(not(hasDescendant(withText("Classic French Toast")))));
            onView(withId(R.id.rvAlmostThereRecipes))
                    .check(matches(hasDescendant(withText("Classic French Toast"))));

            // Add the one missing ingredient, then come back to the Recipes screen
            onView(withId(R.id.nav_pantry)).perform(click());
            db.insertPantryItem(new PantryItem("Sugar", null, 100, "g", null));
            onView(withId(R.id.nav_recipes)).perform(click());

            onView(withId(R.id.tvStrictMatchCount))
                    .check(matches(withText("7 of 18 recipes can be cooked right now")));
            onView(withId(R.id.rvStrictRecipes))
                    .check(matches(hasDescendant(withText("Classic French Toast"))));
        }
    }

    @Test
    public void tappingARecipe_opensDetailWithIngredientsAndCookButton() {
        db.loadSamplePantry();
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnQuickSuggest)).perform(click());
            onView(withText("Golden Grilled Cheese Toastie")).perform(scrollTo(), click());

            onView(withId(R.id.tvDetailTitle)).check(matches(withText("Golden Grilled Cheese Toastie")));
            onView(withId(R.id.tvDetailMatchBanner)).check(matches(withText(R.string.detail_strict_match)));
            onView(withId(R.id.btnCookAndDeduct)).perform(scrollTo()).check(matches(isDisplayed()));

            // Cook: ingredients are deducted and we return to the suggestions
            onView(withId(R.id.btnCookAndDeduct)).perform(click());
            onView(withText(R.string.btn_cook_confirm)).inRoot(isDialog()).perform(click());
            onView(withId(R.id.tvStrictMatchCount)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void bottomNavigation_movesBetweenTopLevelScreens() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_settings)).perform(click());
            onView(withId(R.id.switchExcludeExpired)).check(matches(isDisplayed()));

            onView(withId(R.id.nav_recipes)).perform(click());
            onView(withId(R.id.tvStrictMatchCount)).check(matches(isDisplayed()));

            // Back from a tab returns to the pantry, not to the previous tab
            pressBack();
            onView(withId(R.id.fabAddIngredient)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void pantryData_survivesActivityRecreation() {
        db.insertPantryItem(new PantryItem("Rice", null, 1, "kg", null));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.recreate(); // e.g. rotation: Activity destroyed and rebuilt
            onView(withId(R.id.rvPantryItems)).check(matches(hasDescendant(withText("Rice"))));
        }
    }
}
