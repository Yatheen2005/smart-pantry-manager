package za.ac.richfield.smartpantry;

import org.junit.Test;

import za.ac.richfield.smartpantry.util.IngredientNormalizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IngredientNormalizerTest {

    @Test
    public void singularAndPluralForms_normalizeToTheSameName() {
        assertEquals("tomato", IngredientNormalizer.normalize("Tomatoes"));
        assertEquals("tomato", IngredientNormalizer.normalize("tomato"));
        assertEquals("potato", IngredientNormalizer.normalize("Potatoes"));
        assertEquals("egg", IngredientNormalizer.normalize("Eggs"));
        assertEquals("onion", IngredientNormalizer.normalize("Onions"));
        assertEquals("berry", IngredientNormalizer.normalize("Berries"));
        assertEquals("loaf", IngredientNormalizer.normalize("Loaves"));
        assertEquals("peach", IngredientNormalizer.normalize("Peaches"));
        assertEquals("carrot", IngredientNormalizer.normalize("Carrots"));
        assertEquals("olive", IngredientNormalizer.normalize("Olives"));
    }

    @Test
    public void casingWhitespaceAndPunctuation_areIgnored() {
        assertEquals("tomato", IngredientNormalizer.normalize("  TOMATOES!  "));
        assertEquals("cheddar cheese", IngredientNormalizer.normalize("Cheddar   cheese"));
        assertEquals("", IngredientNormalizer.normalize("   "));
        assertEquals("", IngredientNormalizer.normalize(null));
    }

    @Test
    public void descriptiveWords_areRemovedInAnyOrder() {
        assertEquals("tomato", IngredientNormalizer.normalize("Fresh Tomatoes"));
        assertEquals("egg", IngredientNormalizer.normalize("Large Eggs"));
        assertEquals("egg", IngredientNormalizer.normalize("Large Fresh Eggs"));
        assertEquals("egg", IngredientNormalizer.normalize("Fresh Large Eggs"));
        assertEquals("milk", IngredientNormalizer.normalize("Full Cream Milk"));
        assertEquals("salt", IngredientNormalizer.normalize("Sea Salt"));
        assertEquals("onion", IngredientNormalizer.normalize("Red Onions"));
    }

    @Test
    public void synonyms_mapToOneCanonicalName() {
        assertEquals("beef mince", IngredientNormalizer.normalize("Ground Beef"));
        assertEquals("maize meal", IngredientNormalizer.normalize("Mealie Meal"));
        assertEquals("maize meal", IngredientNormalizer.normalize("Mielie-meal"));
        assertEquals("flour", IngredientNormalizer.normalize("Cake Flour"));
        assertEquals("cooking oil", IngredientNormalizer.normalize("Sunflower Oil"));
        assertEquals("garlic", IngredientNormalizer.normalize("Garlic Cloves"));
        assertEquals("bread", IngredientNormalizer.normalize("Bread Slices"));
        assertEquals("cheddar cheese", IngredientNormalizer.normalize("Cheddar"));
        assertEquals("pasta", IngredientNormalizer.normalize("Spaghetti"));
    }

    @Test
    public void colourSynonymIsResolvedBeforeDescriptorStripping() {
        // "red" is a descriptor, but "red pepper" is a bell pepper, not black pepper
        assertEquals("bell pepper", IngredientNormalizer.normalize("Red Peppers"));
        assertEquals("bell pepper", IngredientNormalizer.normalize("Green Pepper"));
        assertEquals("black pepper", IngredientNormalizer.normalize("Black Pepper"));
    }

    @Test
    public void wordsEndingInSThatAreNotPlural_areUnchanged() {
        assertEquals("couscous", IngredientNormalizer.normalize("Couscous"));
        assertEquals("hummus", IngredientNormalizer.normalize("Hummus"));
        assertEquals("molasses", IngredientNormalizer.normalize("Molasses"));
    }

    @Test
    public void normalizeIsIdempotent() {
        String[] names = {"Fresh Tomatoes", "Garlic Cloves", "Red Peppers", "Mealie Meal", "Cheddar"};
        for (String name : names) {
            String once = IngredientNormalizer.normalize(name);
            assertEquals(once, IngredientNormalizer.normalize(once));
        }
    }

    @Test
    public void differentIngredients_doNotMatch() {
        assertFalse(IngredientNormalizer.isSameIngredient("Egg", "Eggplant"));
        assertFalse(IngredientNormalizer.isSameIngredient("Cheese", "Cheddar Cheese"));
        assertFalse(IngredientNormalizer.isSameIngredient("Chicken", "Chicken Breast"));
        assertTrue(IngredientNormalizer.isSameIngredient("Tomato", "tomatoes"));
    }
}
