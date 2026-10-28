package dev.sebi.sebchievements.handlers.social;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;

/** Be within the radius of enough other players at once (spectators are excluded by ProximityService). */
public final class GroupProjectHandler implements AdvancementHandler {
    @Override
    public String id() {
        return AdvancementIds.GROUP_PROJECT;
    }

    @Override
    public void register(HandlerContext ctx) {
        double radius = ctx.config().getDouble("radius", 16);
        int others = ctx.config().getInt("others", 4);
        ctx.services().tick().register(ctx.id(), p -> {
            if (ctx.services().proximity().nearby(p, radius).size() >= others) ctx.grant(p);
        });
    }
}
