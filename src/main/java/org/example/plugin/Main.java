package org.example.plugin;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.example.plugin.interaction.Example_SorterInteraction;

import javax.annotation.Nonnull;

public class Main extends JavaPlugin {

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCodecRegistry(Interaction.CODEC).register(
                "Example_SorterInteraction",
                Example_SorterInteraction.class,
                Example_SorterInteraction.CODEC
        );
    }
}
