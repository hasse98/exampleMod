package org.example.plugin.util;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helper methods for assigning items to categories. The Sorter mod needs to
 * bucket incoming items into categories such as Food, Ores, Potions and
 * Miscellaneous. This class uses simple string heuristics on the item ID
 * because the full item taxonomy isn't exposed at run‑time. Should the API
 * expand to expose item tags, this class could query those tags instead.
 */
public final class CategoryUtils {

    private CategoryUtils() { /* Utility class – do not instantiate. */ }

    /**
     * Iterate through the given item container and place each non‑empty slot
     * into a category based on its item ID. The resulting structure maps
     * category names to a map of item IDs and their total counts.
     *
     * @param storage the player's item storage to analyse
     * @return a nested map keyed by category then item ID
     */
    public static Map<String, Map<String, Integer>> categoriseInventory(ItemContainer storage) {
        Map<String, Map<String, Integer>> categories = new HashMap<>();

        // Prepare known categories. Additional categories can be added here
        // without changing the GUI logic.
        String[] knownCategories = {"Food", "Ores", "Potions", "Misc"};
        for (String cat : knownCategories) {
            categories.put(cat, new HashMap<>());
        }

        short capacity = storage.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = storage.getItemStack(slot);
            if (stack == null || stack.isEmpty()) continue;
            String id = stack.getItem().getId();
            int count = stack.getQuantity();
            String category = determineCategory(id);
            Map<String, Integer> items = categories.computeIfAbsent(category, k -> new HashMap<>());
            items.put(id, items.getOrDefault(id, 0) + count);
        }
        return categories;
    }

    /**
     * Determine a category for the given item ID. Heuristics are based on
     * simple substrings within the ID. If no rule matches, the item falls
     * back to the "Misc" category.
     *
     * @param id the item ID to categorise
     * @return the name of the appropriate category
     */
    public static String determineCategory(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.contains("apple") || lower.contains("meat") || lower.contains("bread") || lower.contains("fish") || lower.contains("cookie")) {
            return "Food";
        }
        if (lower.contains("ore") || lower.contains("ingot") || lower.contains("gem") || lower.contains("coal")) {
            return "Ores";
        }
        if (lower.contains("potion") || lower.contains("brew")) {
            return "Potions";
        }
        return "Misc";
    }
}