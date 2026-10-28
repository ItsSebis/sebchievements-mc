package dev.sebi.sebchievements.core;

import org.bukkit.entity.Player;

import java.util.List;

/** One handler per {@code mode: plugin} advancement. Add it to {@link Handlers#ALL}. */
public interface AdvancementHandler {
    /** A constant from {@code AdvancementIds}. */
    String id();

    void register(HandlerContext ctx);

    /** Extra lines for {@code /adv debug}. */
    default List<String> debug(Player player) {
        return List.of();
    }
}
