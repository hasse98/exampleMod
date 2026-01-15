package org.example.plugin.interaction;

import org.example.plugin.gui.SorterGui;
import org.example.plugin.util.CategoryUtils;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Map;

/**
 * Handles player interactions with the Sorter block. When a player right‑clicks
 * the block, this interaction collects the contents of the player's inventory
 * and opens a custom GUI to display sorted categories. The codec name is
 * prefixed with the mod group and name to avoid collisions in the global
 * registry (Example_SorterInteraction).
 */
public class Example_SorterInteraction extends SimpleBlockInteraction {

    /**
     * Codec used by Hytale to serialise/deserialise this interaction. The
     * registration in {@link com.example.sorter.Main} ties the codec to a
     * JSON definition for the block interaction.
     */
    public static final BuilderCodec<Example_SorterInteraction> CODEC = BuilderCodec.builder(Example_SorterInteraction.class, Example_SorterInteraction::new).build();

    @Override
    protected void interactWithBlock(@NonNullDecl World world,
                                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                                     @NonNullDecl InteractionType interactionType,
                                     @NonNullDecl InteractionContext interactionContext,
                                     @NullableDecl ItemStack itemStack,
                                     @NonNullDecl Vector3i vector3i,
                                     @NonNullDecl CooldownHandler cooldownHandler) {
        var ref = interactionContext.getEntity();
        var store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());

        Map<String, Map<String, Integer>> categorised = CategoryUtils.categoriseInventory(player.getInventory().getStorage());

        player.getPageManager().openCustomPage(ref, store,
                new SorterGui(playerRefComponent, CustomPageLifetime.CanDismiss, "", "Food", categorised));
    }

    @Override
    protected void simulateInteractWithBlock(@NonNullDecl InteractionType interactionType,
                                             @NonNullDecl InteractionContext interactionContext,
                                             @NullableDecl ItemStack itemStack,
                                             @NonNullDecl World world,
                                             @NonNullDecl Vector3i vector3i) {
        // No simulation necessary
    }
}