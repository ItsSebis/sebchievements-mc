package dev.sebi.sebchievements.handlers.silly;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;

/** Framework smoke test: awarded only by {@code /adv test}, so there is nothing to subscribe to. */
public final class TestHandler implements AdvancementHandler {
    @Override
    public String id() {
        return AdvancementIds.TEST;
    }

    @Override
    public void register(HandlerContext ctx) {}
}
