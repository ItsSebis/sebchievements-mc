package dev.sebi.sebchievements;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.io.IOException;
import java.net.URISyntaxException;

public final class SebchievementsBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            try {
                event.registrar().discoverPack(getClass().getResource("/custom_datapack").toURI(), "bundled");
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException("Could not register bundled datapack", e);
            }
        });
    }
}
