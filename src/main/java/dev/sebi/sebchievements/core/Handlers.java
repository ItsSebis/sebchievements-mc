package dev.sebi.sebchievements.core;

import dev.sebi.sebchievements.handlers.silly.CreeperWhispererHandler;
import dev.sebi.sebchievements.handlers.silly.FashionDisasterHandler;
import dev.sebi.sebchievements.handlers.silly.GravityCheckHandler;
import dev.sebi.sebchievements.handlers.silly.HoarderHandler;
import dev.sebi.sebchievements.handlers.silly.ItsFineHandler;
import dev.sebi.sebchievements.handlers.silly.OneMansTrashHandler;
import dev.sebi.sebchievements.handlers.silly.ProfessionalNapperHandler;
import dev.sebi.sebchievements.handlers.silly.SelfOwnHandler;
import dev.sebi.sebchievements.handlers.silly.StaringContestHandler;
import dev.sebi.sebchievements.handlers.silly.TestHandler;

import java.util.List;

/** The one explicit list of handlers. Adding a handler = one class + one line here. */
public final class Handlers {
    public static final List<AdvancementHandler> ALL = List.of(
            new TestHandler(),
            // silly
            new ProfessionalNapperHandler(),
            new ItsFineHandler(),
            new HoarderHandler(),
            new FashionDisasterHandler(),
            new CreeperWhispererHandler(),
            new SelfOwnHandler(),
            new GravityCheckHandler(),
            new StaringContestHandler(),
            new OneMansTrashHandler()
    );

    private Handlers() {}
}
