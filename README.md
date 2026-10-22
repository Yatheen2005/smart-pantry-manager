# Smart Pantry Manager — Mobile App Development 700

A native Java Android application designed to help households cut food waste by tracking pantry ingredients and suggesting recipes using **strictly** the leftover ingredients already available at home in sufficient quantities.

## 1. Core Features & Assignment Alignment

- **Full SQLite Pantry CRUD (`PantryDatabaseHelper`)**: Add, view, update, and delete pantry items with ingredient name, numeric quantity, measurement unit (`g`, `kg`, `ml`, `l`, `tsp`, `tbsp`, `cup`, `pcs`, `clove`, `slice`, `tin`, `oz`, `lb`), and optional expiry date (`YYYY-MM-DD`).
- **Pre-Loaded Persistent Recipe Collection (`RecipeSeeder`)**: Automatically seeds **18 practical household and South African recipes** into the relational SQLite database on first run.
- **Strict-Matching Engine (`StrictRecipeMatcher`)**:
  - A recipe is suggested in the main list **if and only if every single required ingredient is present in the user's pantry in at least the required quantity**.
  - Recipes missing even 1 out of 5 ingredients (or having an insufficient quantity of any single ingredient) are strictly excluded from the main suggestions list.
  - Includes a clearly separated **"Almost There (Missing Only 1 Ingredient)"** bonus section below a divider.
- **Robust Real-World Normalization (`IngredientNormalizer` & `UnitConverter`)**:
  - Normalizes singular/plural forms (`"Tomatoes"` ↔ `"tomato"`, `"Potatoes"` ↔ `"potato"`, `"Berries"` ↔ `"berry"`, `"Loaves"` ↔ `"loaf"`, `"Large Eggs"` ↔ `"egg"`) and common culinary synonyms (`"Mealie Meal"` ↔ `"maize meal"`, `"Ground Beef"` ↔ `"beef mince"`).
  - Automatically converts compatible units (`kg` ↔ `g`, `l` ↔ `ml` ↔ `cup` ↔ `tbsp` ↔ `tsp`, `lb`/`oz` ↔ `g`).
- **Five Distinct Activities & Custom RecyclerViews**:
  1. `MainActivity` — Pantry inventory list (`RecyclerView` + `PantryAdapter`), expiry alerts banner, quick navigation.
  2. `AddEditIngredientActivity` — Data entry form with `InputValidator`, unit `Spinner`, and `DatePickerDialog`.
  3. `SuggestedRecipesActivity` — Strict recipe suggestions (`RecyclerView` + `RecipeAdapter`), zero-match feedback state, and separated "Almost There" bonus list.
  4. `RecipeDetailActivity` — Full ingredient checklist with pantry availability comparison, preparation method, and **"Mark as Cooked (Deduct Used Ingredients)"** action.
  5. `SettingsActivity` — Persistent user preferences (`SharedPreferences`) for expiry alert toggles, warning days threshold, and sample data loading.
- **Strict Scope Compliance**: Built 100% in Java (`minSdk 24`, `targetSdk 34`). Contains **zero** Kotlin code and **zero** Google Maps, GPS, or location-based SDKs.

## 2. Why SQLite (`SQLiteOpenHelper`) Was Chosen

We selected **SQLite via `SQLiteOpenHelper`** (Section 3.2 Option 1) for four architectural reasons:
1. **100% Offline Reliability**: Users check their kitchen cupboards and cook regardless of internet connectivity; local on-device storage guarantees zero network latency and zero cloud downtime.
2. **Relational Integrity for Strict Matching**: Recipes and their required ingredients form a classic `1:N` relational schema (`recipes` → `recipe_ingredients` with `FOREIGN KEY ... ON DELETE CASCADE`), which maps naturally to normalized SQLite tables.
3. **Zero External Paid Dependencies**: Unlike Firebase or hosted PostgreSQL tiers that require API keys, billing accounts, or external REST servers, SQLite is embedded directly into the Android OS runtime.
4. **Module Curriculum Alignment**: Directly demonstrates mastery of Android's native `SQLiteOpenHelper`, `SQLiteDatabase` transactions, `ContentValues`, and `Cursor` iteration covered in Mobile App Development 700.

## 3. Setup & Run Instructions

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or newer (Iguana / Jellyfish / Koala / Ladybug)
- **JDK 11 or JDK 17** (bundled with Android Studio)
- **Android SDK Platform 34** and an Android Emulator (API 24+) or physical Android device

### Running the Application
1. Open **Android Studio** and select **File → Open...**, then choose the `SmartPantryManager` folder.
2. Allow Gradle to sync dependencies (`appcompat`, `material`, `recyclerview`, `cardview`, `constraintlayout`).
3. Select an emulator (e.g., Pixel 6 API 34) or connected Android device and click **Run 'app'** (`Shift + F10`).
4. **Quick Marker Testing Tip**: On the empty Pantry screen (or in **Settings**), tap **"Load Sample Pantry Ingredients"** to immediately populate 12 realistic pantry items and observe strict recipe matching in action.

### Running Automated Tests
- **Unit Tests (JUnit 4)**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Android Instrumented Tests (SQLite CRUD & Persistence)**:
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```
