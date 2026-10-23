# Smart Pantry Manager (Mobile App Development 700)

A native **Java** Android app that helps a household cut food waste. The user records the
ingredients they already have at home (their *pantry*), and the app suggests **only** the recipes
they can cook right now with those ingredients, in the required quantities, without a shopping trip.

No Kotlin, no Google Maps, no location/GPS and no network access are used.

## Features

| Requirement | Where it is implemented |
|---|---|
| Add / view / edit / delete pantry items (name, quantity, unit, optional expiry date) | `AddEditIngredientActivity`, `MainActivity`, `PantryDatabaseHelper` |
| RecyclerView + custom Adapter bound to the database | `MainActivity` + `PantryAdapter`; `SuggestedRecipesActivity` + `RecipeAdapter` |
| 18 recipes stored in the database, seeded on first run | `RecipeSeeder` (called from `PantryDatabaseHelper.onCreate`) |
| Suggested Recipes using the strict-matching rule | `SuggestedRecipesActivity` + `StrictRecipeMatcher` |
| Recipe detail (ingredients + method) | `RecipeDetailActivity` |
| Settings screen | `SettingsActivity` + `UserPreferencesManager` (SharedPreferences) |
| Navigation element | Bottom navigation bar (`NavigationHelper`) and toolbar Up arrow |
| Intents that pass data | Pantry item id → edit screen; recipe id → detail screen |
| Input validation | `InputValidator` (name, quantity, unit, real calendar date) |
| Zero-match and empty-pantry feedback | `SuggestedRecipesActivity`, `MainActivity` |
| Optional "Almost There" list (missing exactly 1 ingredient), clearly separated | `StrictRecipeMatcher.getAlmostThereMatches` |

Extras: duplicate detection on add (merge into the existing entry or keep a separate batch),
expiry badges and alerts, and **Mark as Cooked**, which deducts the used quantities in one database
transaction, using the ingredients that expire soonest first.

## Strict matching: how it decides

A recipe is suggested **if and only if every required ingredient** is in the pantry in **at least
the required quantity** (`StrictRecipeMatcher.evaluateRecipe`). For each required ingredient:

1. Both names are normalized by `IngredientNormalizer` (lower case, punctuation removed,
   singular form, descriptive words such as *fresh/large* removed, small synonym table).
   The **whole** canonical names are compared with `equals`, never with substring checks, so
   *Eggplant* does not count as *Egg* and *Cheese* does not count as *Cheddar Cheese*.
2. All pantry rows with that name are added together after converting to the recipe's unit
   (`UnitConverter`): g/kg/mg/oz/lb with each other, ml/l/tsp/tbsp/cup/fl oz with each other.
3. If the total is short, or the ingredient is absent, the recipe is **not** suggested.

Deliberate limitations, chosen to avoid false "you can cook this" results:

* **Mass and volume are not converted into each other.** 1 cup of flour is about 125 g but 1 cup of
  milk is about 250 g, so a general rule would be wrong. The detail screen explains the unit clash
  so the user can edit the item.
* **Different count units are not interchangeable** (`pcs` ≠ `clove` ≠ `slice` ≠ `tin`).
* A generic name does not satisfy a specific one (*Chicken* does not satisfy *Chicken Breast*).
* Water is assumed to be available and is not listed as an ingredient.
* The synonym list is small and hand-written; it is not a full natural-language solution.
* Expired items are ignored by default (Settings → *Ignore expired ingredients when matching*).

## Database choice: SQLite (`SQLiteOpenHelper`)

* **Works offline.** A pantry app is used in the kitchen; local storage needs no network or account.
* **Relational data.** A recipe has many ingredients, which maps directly onto a
  `recipes` → `recipe_ingredients` one-to-many relationship with a foreign key.
* **No external services.** No API keys, billing or server to maintain (unlike Firebase or PostgreSQL).
* **Matches the module.** `SQLiteOpenHelper`, `ContentValues`, `Cursor` and transactions are what
  the persistent-data chapter covers.

Tables: `pantry_items` (full CRUD), `recipes` (18 seeded rows) and `recipe_ingredients`
(foreign key to `recipes`). The database file lives in the app's private storage, so data
remains after the app is closed or the device restarts.

## Project structure

```
app/src/main/java/za/ac/richfield/smartpantry/
├── adapter/   PantryAdapter, RecipeAdapter            (RecyclerView adapters)
├── database/  PantryDatabaseHelper, RecipeSeeder, SamplePantryData
├── model/     PantryItem, Recipe, RecipeIngredient, IngredientCheck, RecipeMatchResult
├── ui/        MainActivity, AddEditIngredientActivity, SuggestedRecipesActivity,
│              RecipeDetailActivity, SettingsActivity, NavigationHelper
└── util/      StrictRecipeMatcher, IngredientNormalizer, UnitConverter,
               InputValidator, UserPreferencesManager
```

The matching, normalizing, unit and validation classes have no Android dependencies, so they are
unit tested on the JVM.

## Setup and run

Requirements: **Android Studio 2026.2 or newer** (the project uses Android Gradle Plugin 9.4.1 and
Gradle 9.8.2 through the included wrapper), **Android SDK Platform 37**, and an emulator or device
running Android 7.0 (API 24) or newer. The JDK bundled with Android Studio is used.

1. In Android Studio choose **File → Open** and select the `SmartPantryManager` folder (the one that
   contains `settings.gradle`, **not** `app/src`).
2. Wait for the Gradle sync to finish. If prompted, install SDK Platform 37.
3. Choose an emulator or device and press **Run 'app'** (Shift + F10).
4. To try matching quickly: on the empty pantry screen tap **Load Sample Pantry Ingredients**.
   This loads 13 items (one deliberately expired); 6 recipes are suggested and 9 are "Almost There".
   Adding **Sugar 100 g** makes *Classic French Toast* appear; deleting it removes it again.

Command line (Windows: `gradlew.bat`, macOS/Linux: `./gradlew`):

```bash
gradlew assembleDebug              # build the APK
gradlew testDebugUnitTest          # JVM unit tests (matching, units, names, validation, seed data)
gradlew connectedDebugAndroidTest  # on-device tests: SQLite CRUD/persistence + Espresso UI flows
```

> The Espresso UI tests drive the real screens and therefore **clear the pantry** on the device
> they run on. The SQLite tests use a separate test database file.
