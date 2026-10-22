package za.ac.richfield.smartpantry.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes ingredient names so that trivial real-world variations such as
 * singular/plural differences ("tomato" vs "tomatoes", "egg" vs "eggs",
 * "potato" vs "potatoes", "berry" vs "berries"), letter casing, extra whitespace,
 * and common regional culinary synonyms resolve to a single canonical token.
 */
public final class IngredientNormalizer {

    private static final Map<String, String> IRREGULAR_PLURALS;
    private static final Map<String, String> SYNONYM_MAP;
    private static final Set<String> INVARIANT_S_WORDS;
    private static final String[] DESCRIPTOR_PREFIXES = {
        "fresh ", "large ", "medium ", "small ", "raw ", "whole ", "organic ", "ripe ", "dried ",
        "full cream ", "low fat ", "skim ", "skimmed ", "salted ", "unsalted ", "fine ", "table ", "sea ",
        "white ", "brown ", "red ", "sliced "
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
        invariants.add("oats");
        INVARIANT_S_WORDS = Collections.unmodifiableSet(invariants);

        Map<String, String> synonyms = new HashMap<>();
        synonyms.put("scallion", "spring onion");
        synonyms.put("green onion", "spring onion");
        synonyms.put("capsicum", "bell pepper");
        synonyms.put("sweet pepper", "bell pepper");
        synonyms.put("green pepper", "bell pepper");
        synonyms.put("red pepper", "bell pepper");
        synonyms.put("cilantro", "coriander");
        synonyms.put("mealie meal", "maize meal");
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
        synonyms.put("white sugar", "sugar");
        synonyms.put("granulated sugar", "sugar");
        synonyms.put("cheddar", "cheddar cheese");
        synonyms.put("garlic clove", "garlic");
        synonyms.put("clove garlic", "garlic");
        synonyms.put("bread slice", "bread");
        synonyms.put("slice bread", "bread");
        synonyms.put("loaf bread", "bread");
        synonyms.put("full cream milk", "milk");
        synonyms.put("low fat milk", "milk");
        SYNONYM_MAP = Collections.unmodifiableMap(synonyms);
    }

    private IngredientNormalizer() {
        // Utility class
    }

    /**
     * Converts any raw user or recipe ingredient name into its canonical singular lowercase form.
     *
     * @param rawName Input ingredient name (e.g., "  Fresh Tomatoes ", "Eggs", "Potatoes")
     * @return Normalized canonical name (e.g., "tomato", "egg", "potato")
     */
    public static String normalize(String rawName) {
        if (rawName == null) {
            return "";
        }

        // 1. Lowercase, strip punctuation except hyphens/spaces, and collapse whitespace
        String cleaned = rawName.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9\s-]", " ")
                .replaceAll("\s+", " ")
                .trim();

        if (cleaned.isEmpty()) {
            return "";
        }

        // 2. Strip optional non-essential prefixes ("fresh tomatoes" -> "tomatoes")
        for (String prefix : DESCRIPTOR_PREFIXES) {
            if (cleaned.startsWith(prefix) && cleaned.length() > prefix.length()) {
                cleaned = cleaned.substring(prefix.length()).trim();
            }
        }

        // 3. Check direct synonym mapping before singularizing
        if (SYNONYM_MAP.containsKey(cleaned)) {
            cleaned = SYNONYM_MAP.get(cleaned);
        }

        // 4. Singularize each word or the trailing head noun
        String[] tokens = cleaned.split(" ");
        int lastIndex = tokens.length - 1;
        tokens[lastIndex] = singularizeWord(tokens[lastIndex]);
        String singularized = String.join(" ", tokens);

        // 5. Re-check synonym mapping after singularization ("scallions" -> "scallion" -> "spring onion")
        if (SYNONYM_MAP.containsKey(singularized)) {
            singularized = SYNONYM_MAP.get(singularized);
        }

        return singularized;
    }

    private static String singularizeWord(String word) {
        if (word.length() <= 2 || INVARIANT_S_WORDS.contains(word)) {
            return word;
        }
        if (IRREGULAR_PLURALS.containsKey(word)) {
            return IRREGULAR_PLURALS.get(word);
        }
        // Words ending in "-ies" preceded by a consonant (e.g., "berries" -> "berry")
        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        // Words ending in "-oes" (e.g., "mangoes" -> "mango", "heroes" -> "hero")
        if (word.endsWith("oes") && word.length() > 4) {
            return word.substring(0, word.length() - 2);
        }
        // Words ending in "-ches", "-shes", "-xes", "-sses" (e.g., "peaches" -> "peach", "boxes" -> "box")
        if (word.endsWith("ches") || word.endsWith("shes") || word.endsWith("xes") || word.endsWith("sses")) {
            return word.substring(0, word.length() - 2);
        }
        // Note: Irregular -ves plurals (loaves, leaves, halves, knives) are handled via IRREGULAR_PLURALS
        // so regular words like 'cloves' and 'olives' fall through to standard '-s' removal.
        // Standard plural ending in "-s" (but not "-ss" or "-us")
        if (word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    /**
     * Checks whether two ingredient names refer to the same canonical ingredient.
     */
    public static boolean isSameIngredient(String nameA, String nameB) {
        String normA = normalize(nameA);
        String normB = normalize(nameB);
        return !normA.isEmpty() && normA.equals(normB);
    }
}
