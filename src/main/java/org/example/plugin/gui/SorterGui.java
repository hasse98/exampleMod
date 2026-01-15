package org.example.plugin.gui;

import org.example.plugin.util.CategoryUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Custom GUI for the Sorter mod. Displays tabs for each category and lists
 * items inside the selected category. Includes search functionality.
 */
public class SorterGui extends InteractiveCustomUIPage<SorterGui.SorterGuiData> {

    private String searchQuery;
    private String selectedCategory;
    private Map<String, Map<String, Integer>> categorisedItems;
    private final Map<String, String> visibleItems = new HashMap<>();

    /**
     * Construct a new Sorter GUI.
     *
     * @param playerRef        reference to the player opening the page
     * @param lifetime         whether the page can be dismissed
     * @param defaultSearch    initial search query
     * @param defaultCategory  the initially selected category
     * @param categorisedItems mapping of category → (item id → quantity)
     */
    public SorterGui(@Nonnull PlayerRef playerRef,
                     @Nonnull CustomPageLifetime lifetime,
                     String defaultSearch,
                     String defaultCategory,
                     Map<String, Map<String, Integer>> categorisedItems) {
        super(playerRef, lifetime, SorterGuiData.CODEC);
        this.searchQuery = defaultSearch.trim().toLowerCase(Locale.ROOT);
        this.selectedCategory = defaultCategory;
        this.categorisedItems = categorisedItems;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder,
                      @Nonnull Store<EntityStore> store) {
        // Load the UI template for the sorter GUI
        uiCommandBuilder.append("Pages/Buuz135_SorterGui.ui");
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        // Bind search changes
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
                EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        // Build category tabs and item list
        buildTabsAndItems(ref, uiCommandBuilder, uiEventBuilder, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull SorterGuiData data) {
        super.handleDataEvent(ref, store, data);
        boolean update = false;
        if (data.selectedCategory != null) {
            this.selectedCategory = data.selectedCategory;
            update = true;
        }
        // Handle retrieving items: remove items from the player's inventory and re‑categorise
        if (data.retrieveItem != null) {
            String[] parts = data.retrieveItem.split(":");
            String itemId = parts[0];
            boolean fullStack = parts.length > 1 && "full".equals(parts[1]);
            removeItemsFromPlayer(ref, store, itemId, fullStack ? Integer.MAX_VALUE : 1);
            // Rebuild the categorisation after removal
            Player player = store.getComponent(ref, Player.getComponentType());
            this.categorisedItems = CategoryUtils.categoriseInventory(player.getInventory().getStorage());
            update = true;
        }
        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim().toLowerCase(Locale.ROOT);
            update = true;
        }
        if (update) {
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            buildTabsAndItems(ref, commandBuilder, eventBuilder, store);
            sendUpdate(commandBuilder, eventBuilder, false);
        }
    }

    /**
     * Remove a specified quantity of an item from the player's inventory. The
     * player's container does not have a single call to remove by ID, so we
     * iterate over all slots and subtract the requested amount until satisfied.
     *
     * @param ref     reference to the player's proxy entity
     * @param store   entity store to access components
     * @param itemId  the ID of the item to remove
     * @param amount  the number of items to remove (use Integer.MAX_VALUE for a full stack)
     */
    private void removeItemsFromPlayer(Ref<EntityStore> ref, Store<EntityStore> store, String itemId, int amount) {
        Player player = store.getComponent(ref, Player.getComponentType());
        var container = player.getInventory().getStorage();
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            if (amount <= 0) break;
            var stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty()) continue;
            if (!stack.getItem().getId().equals(itemId)) continue;
            int remove = Math.min(stack.getQuantity(), amount);
            // Create a copy with the quantity to remove and request removal
            var toRemove = stack.withQuantity(remove);
            var transaction = container.removeItemStack(toRemove);
            if (transaction.succeeded() && transaction.getQuery() != null) {
                amount -= transaction.getQuery().getQuantity();
            }
        }
    }

    /**
     * Build both the category tabs and the item list for the currently
     * selected category and search query.
     */
    private void buildTabsAndItems(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull UICommandBuilder commandBuilder,
                                   @Nonnull UIEventBuilder eventBuilder,
                                   @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        // Rebuild the tabs. Each tab binds an event when clicked.
        commandBuilder.clear("#CategoryTabs");
        int tabIndex = 0;
        for (String category : categorisedItems.keySet()) {
            commandBuilder.append("#CategoryTabs", "Group { LayoutMode: Left; Anchor: (Top: 0); }");
            // Use translation keys if available, otherwise fallback to raw category name
            Message display = Message.raw(category);
            commandBuilder.append("#CategoryTabs[" + tabIndex + "]", "Pages/Buuz135_SorterTab.ui");
            commandBuilder.set("#CategoryTabs[" + tabIndex + "] #TabLabel.TextSpans", display);
            // Highlight the currently selected tab
            boolean selected = category.equals(this.selectedCategory);
            commandBuilder.set("#CategoryTabs[" + tabIndex + "] #TabHighlight.Visible", selected);
            // Bind click to change category
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#CategoryTabs[" + tabIndex + "]", EventData.of("SelectCategory", category));
            tabIndex++;
        }
        // Filter items in the selected category by search query
        Map<String, Integer> itemsInCategory = categorisedItems.getOrDefault(selectedCategory, new HashMap<>());
        visibleItems.clear();
        if (searchQuery.isEmpty()) {
            itemsInCategory.forEach((id, qty) -> visibleItems.put(id, id));
        } else {
            for (String id : itemsInCategory.keySet()) {
                String lower = getItemName(componentAccessor, id).toLowerCase(Locale.ROOT);
                if (lower.contains(searchQuery)) {
                    visibleItems.put(id, id);
                }
            }
        }
        buildItemButtons(ref, commandBuilder, eventBuilder, componentAccessor, itemsInCategory);
    }

    /**
     * Build UI buttons for each visible item. Similar to the WhereThisAt mod,
     * the buttons show the item icon, quantity and provide a tooltip. Right
     * clicking retrieves a single item; left clicking retrieves a full stack.
     */
    private void buildItemButtons(@Nonnull Ref<EntityStore> ref,
                                  @Nonnull UICommandBuilder commandBuilder,
                                  @Nonnull UIEventBuilder eventBuilder,
                                  @Nonnull ComponentAccessor<EntityStore> componentAccessor,
                                  Map<String, Integer> itemsInCategory) {
        commandBuilder.clear("#ItemCards");
        Player playerComponent = componentAccessor.getComponent(ref, Player.getComponentType());
        int rowIndex = 0;
        int cardsInRow = 0;
        for (String id : visibleItems.keySet()) {
            int amount = itemsInCategory.getOrDefault(id, 0);
            // Start a new row if necessary
            if (cardsInRow == 0) {
                commandBuilder.append("#ItemCards", "Group { LayoutMode: Left; Anchor: (Top: 0); }");
            }
            commandBuilder.append("#ItemCards[" + rowIndex + "]", "Pages/Buuz135_SorterItemCard.ui");
            // Set item icon and quantity
            commandBuilder.set("#ItemCards[" + rowIndex + "] #ItemIcon.ItemId", id);
            commandBuilder.set("#ItemCards[" + rowIndex + "] #ItemQuantity.Text", Integer.toString(amount));
            // Tooltip shows translated name and quantity
            Message tooltip = Message.raw(getItemName(componentAccessor, id) + "\nAmount: " + amount);
            commandBuilder.set("#ItemCards[" + rowIndex + "] #ItemIcon.TooltipTextSpans", tooltip);
            // Bind retrieval events; for now retrieval simply removes items from the player's
            // inventory and re‑categorises. Left click retrieves a full stack, right click one.
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#ItemCards[" + rowIndex + "] #RetrieveButton",
                    EventData.of("RetrieveItem", id + ":full"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.RightClicking,
                    "#ItemCards[" + rowIndex + "] #RetrieveButton",
                    EventData.of("RetrieveItem", id + ":single"));
            cardsInRow++;
            if (cardsInRow >= 8) {
                cardsInRow = 0;
                rowIndex++;
            }
        }
    }

    /**
     * Resolve a human‑readable name for an item ID. In a real mod, this would
     * query the I18n module using the item's translation key. Because the
     * Item type isn't directly available in this scope, we fallback to the
     * item ID itself.
     */
    private String getItemName(ComponentAccessor<EntityStore> accessor, String id) {
        // When access to Item definitions becomes available, use:
        // Item item = Main.ITEMS.get(id);
        // return I18nModule.get().getMessage(playerRef.getLanguage(), item.getTranslationKey());
        return id;
    }

    /**
     * Data class used to serialise events from the client. Includes keys for
     * selecting a category, retrieving an item and updating the search.
     */
    public static class SorterGuiData {
        static final String KEY_SELECT_CATEGORY = "SelectCategory";
        static final String KEY_RETRIEVE_ITEM = "RetrieveItem";
        static final String KEY_SEARCH_QUERY = "@SearchQuery";
        public static final BuilderCodec<SorterGuiData> CODEC = BuilderCodec.<SorterGuiData>builder(SorterGuiData.class, SorterGuiData::new)
                .addField(new KeyedCodec<>(KEY_SELECT_CATEGORY, Codec.STRING), (data, s) -> data.selectedCategory = s, data -> data.selectedCategory)
                .addField(new KeyedCodec<>(KEY_RETRIEVE_ITEM, Codec.STRING), (data, s) -> data.retrieveItem = s, data -> data.retrieveItem)
                .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (data, s) -> data.searchQuery = s, data -> data.searchQuery)
                .build();

        private String selectedCategory;
        private String retrieveItem;
        private String searchQuery;
    }
}