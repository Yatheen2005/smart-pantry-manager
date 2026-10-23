package za.ac.richfield.smartpantry.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes ingredient names so that trivial real-world variations resolve to one canonical
 * token that can be compared with String.equals():
 * - letter casing, punctuation and extra whitespace ("  Tomatoes! " -> "tomato")
 * - singular/plural forms ("tomatoes" -> "tomato", "berries" -> "berry", "loaves" -> "loaf")
 * - descriptive words that do not change the ingredient ("fresh", "large", "full cream")
 * - a small table of common synonyms ("mealie meal" -> "maize meal", "spaghetti" -> "pasta")
 *
 * Matching is done on the WHOLE canonical name, never with substring checks, so "egg" does not
 * accidentally match "eggplant" and "cheese" does not match "cheddar cheese".
 */
public final class IngredientNormalizer {

    private static final Map<String, String> IRREGULAR_PLURALS;
    private static final Map<String, String> SYNONYM_MAP;
    private static final Set<String> INVARIANT_S_WORDS;

    /** Leading descriptive words that are removed; they describe the item, not what it is. */
    private static final String[] DESCRIPTOR_PREFIXES = {
        "fresh ", "large ", "medium ", "small ", "raw ", "whole ", "organic ", "ripe ", "dried ",
        "full cream ", "low fat ", "skim ", "skimmed ", "salted ", "unsalted ", "fine ", "table ", "sea ",
        "white ", "brown ", "red ", "sliced ", "free range "
    };

    static {
        Map<String, String> irregulars = new HashMap<>();
        irregulars.put("tomatoes", "tomato");
        irregulars.put("potatoes", "potato");
        irregulars.put("loaves", "loaf");
        irregulars.put("leaves", "leaf");
        irregulars.put("halves", "half");
        irregulars.put("knives", "knife");
        irregulars.put("chillies", "chilli");
        irregulars.put("chilies", "chilli");
        irregulars.put("chili", "chilli");
        irregulars.put("berries", "berry");
        irregulars.put("strawberries", "strawberry");
        irregulars.put("blueberries", "blueberry");
        irregulars.put("cherries", "cherry");
        irregulars.put("patties", "patty");
        IRREGULAR_PLURALS = Collections.unmodifiableMap(irregulars);

        Set<String> invariants = new HashSet<>();
        invariants.add("couscous");
        invariants.add("hummus");
        invariants.add("molasses");
        invariants.add("asparagus");
        invariants.add("citrus");
        INVARIANT_S_WORDS = Collections.unmodifiableSet(invariants);

        // Keys and values are already singular, lower-case and punctuation-free
        Map<String, String> synonyms = new HashMap<>();
        synonyms.put("scallion", "spring onion");
        synonyms.put("green onion", "spring onion");
        synonyms.put("capsicum", "bell pepper");
        synonyms.put("sweet pepper", "bell pepper");
        synonyms.put("green pepper", "bell pepper");
        synonyms.put("red pepper", "bell pepper");
        synonyms.put("yellow pepper", "bell pepper");
        synonyms.put("cilantro", "coriander");
        synonyms.put("mealie meal", "maize meal");
        synonyms.put("mielie meal", "maize meal");
        synonyms.put("mielie-meal", "maize meal");
        synonyms.put("mealie-meal", "maize meal");
        synonyms.put("pap", "maize meal");
        synonyms.put("ground beef", "beef mince");
        synonyms.put("minced beef", "beef mince");
        synonyms.put("mince", "beef mince");
        synonyms.put("sunflower oil", "cooking oil");
        synonyms.put("vegetable oil", "cooking oil");
        synonyms.put("canola oil", "cooking oil");
        synonyms.put("plain flour", "flour");
        synonyms.put("all-purpose flour", "flour");
        synonyms.put("all purpose flour", "flour");
        synonyms.put("cake flour", "flour");
        synonyms.put("wheat flour", "flour");
        synonyms.put("granulated sugar", "sugar");
        synonyms.put("castor sugar", "sugar");
        synonyms.put("cheddar", "cheddar cheese");
        synonyms.put("garlic clove", "garlic");
        synonyms.put("clove garlic", "garlic");
        synonyms.put("bread slice", "bread");
        synonyms.put("slice bread", "bread");
        synonyms.put("loaf bread", "bread");
        synonyms.put("spaghetti", "pasta");
        synonyms.put("penne", "pasta");
        synonyms.put("macaroni", "pasta");
        synonyms.put("fusilli", "pasta");
        synonyms.put("chicken fillet", "chicken breast");
        synonyms.put("chicken breast fillet", "chicken breast");
        SYNONYM_MAP = Collections.unmodifiableMap(synonyms);
    }

    private IngredientNormalizer() {
        // Utility class
    }

    /**
     * Converts any raw user or recipe ingredient name into its canonical singular lowercase form.
     *
     * @param rawName Input ingredient name (e.g. "  Fresh Tomatoes ", "Eggs", "Red Peppers")
     * @return Canonical name (e.g. "tomato", "egg", "bell pepper"), or "" for blank input
     */
    public static String normalize(String rawName) {
        if (rawName == null) {
            return "";
        }

        // 1. Lowercase, replace punctuation (except hyphens) with spaces, collapse whitespace
        String cleaned = rawName.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        // 2. Singularize the head noun (last word): "red peppers" -> "red pepper"
        cleaned = singularizeLastWord(cleaned);

        // 3. Synonyms are checked BEFORE descriptors are removed, so "red pepper" becomes
        //    "bell pepper" instead of being reduced to just "pepper"
        String synonym = SYNONYM_MAP.get(cleaned);
        if (synonym != null) {
            return synonym;
        }

        // 4. Repeatedly strip descriptive prefixes, in any order ("large fresh egg" -> "egg")
        boolean stripped;
        do {
            stripped = false;
            for (String prefix : DESCRIPTOR_PREFIXES) {
                if (cleaned.startsWith(prefix) && cleaned.length() > prefix.length()) {
                    cleaned = cleaned.substring(prefix.length()).trim();
                    stripped = true;
                }
            }
        } while (stripped);

        // 5. Final synonym lookup on the stripped name ("fresh garlic clove" -> "garlic")
        synonym = SYNONYM_MAP.get(cleaned);
        return synonym != null ? synonym : cleaned;
    }

    /**
     * Checks whether two ingredient names refer to the same canonical ingredient.
     */
    public static boolean isSameIngredient(String nameA, String nameB) {
        String normA = normalize(nameA);
        return !normA.isEmpty() && normA.equals(normalize(nameB));
    }

    private static String singularizeLastWord(String phrase) {
        int lastSpace = phrase.lastIndexOf(' ');
        String head = phrase.substring(lastSpace + 1);
        return phrase.substring(0, lastSpace + 1) + singularizeWord(head);
    }

    private static String singularizeWord(String word) {
        if (word.length() <= 2 || INVARIANT_S_WORDS.contains(word)) {
            return word;
        }
        String irregular = IRREGULAR_PLURALS.get(word);
        if (irregular != null) {
            return irregular;
        }
        // "-ies" -> "-y" (e.g. "berries" -> "berry")
        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        // "-oes" -> "-o" (e.g. "mangoes" -> "mango")
        if (word.endsWith("oes") && word.length() > 4) {
            return word.substring(0, word.length() - 2);
        }
        // "-ches", "-shes", "-xes", "-sses" drop "es" (e.g. "peaches" -> "peach")
        if (word.endsWith("ches") || word.endsWith("shes") || word.endsWith("xes") || word.endsWith("sses")) {
            return word.substring(0, word.length() - 2);
        }
        // Irregular "-ves" plurals (loaves, leaves) are in IRREGULAR_PLURALS, so regular words
        // such as "cloves" and "olives" fall through to plain "-s" removal below.
        if (word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }
}
