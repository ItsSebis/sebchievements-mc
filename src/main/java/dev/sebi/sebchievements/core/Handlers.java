package dev.sebi.sebchievements.core;

import dev.sebi.sebchievements.handlers.social.BetrayalHandler;
import dev.sebi.sebchievements.handlers.social.BodyguardHandler;
import dev.sebi.sebchievements.handlers.social.DinnerPartyHandler;
import dev.sebi.sebchievements.handlers.social.GroupProjectHandler;
import dev.sebi.sebchievements.handlers.social.MedicHandler;
import dev.sebi.sebchievements.handlers.social.NeighborlyHandler;
import dev.sebi.sebchievements.handlers.social.RevengeHandler;
import dev.sebi.sebchievements.handlers.social.RoommatesHandler;
import dev.sebi.sebchievements.handlers.social.TwinningHandler;
import dev.sebi.sebchievements.handlers.social.WitnessHandler;
import dev.sebi.sebchievements.handlers.challenges.ActualSniperHandler;
import dev.sebi.sebchievements.handlers.challenges.FoodCriticHandler;
import dev.sebi.sebchievements.handlers.challenges.FreeDiverHandler;
import dev.sebi.sebchievements.handlers.challenges.FreshStartHandler;
import dev.sebi.sebchievements.handlers.challenges.FrozenHumanHandler;
import dev.sebi.sebchievements.handlers.challenges.IronWillHandler;
import dev.sebi.sebchievements.handlers.challenges.JockeyJamboreeHandler;
import dev.sebi.sebchievements.handlers.challenges.MovingMountainsHandler;
import dev.sebi.sebchievements.handlers.challenges.NakedAndAfraidHandler;
import dev.sebi.sebchievements.handlers.challenges.ObsidianObsessionHandler;
import dev.sebi.sebchievements.handlers.challenges.SkullCollectorHandler;
import dev.sebi.sebchievements.handlers.challenges.OneManArmyHandler;
import dev.sebi.sebchievements.handlers.challenges.PacifistHandler;
import dev.sebi.sebchievements.handlers.challenges.TotemChainHandler;
import dev.sebi.sebchievements.handlers.challenges.UntouchableHandler;
import dev.sebi.sebchievements.handlers.challenges.WardenBullyHandler;
import dev.sebi.sebchievements.handlers.challenges.WhittledDownHandler;
import dev.sebi.sebchievements.handlers.challenges.WitherStormHandler;
import dev.sebi.sebchievements.handlers.challenges.YeetTheGoatHandler;
import dev.sebi.sebchievements.handlers.exploration.CavemanHandler;
import dev.sebi.sebchievements.handlers.exploration.DimensionHopperHandler;
import dev.sebi.sebchievements.handlers.exploration.LongWayHomeHandler;
import dev.sebi.sebchievements.handlers.exploration.MarathonHandler;
import dev.sebi.sebchievements.handlers.exploration.RoadsWereAMistakeHandler;
import dev.sebi.sebchievements.handlers.exploration.SkyCamperHandler;
import dev.sebi.sebchievements.handlers.exploration.TopToBottomHandler;
import dev.sebi.sebchievements.handlers.silly.CreeperWhispererHandler;
import dev.sebi.sebchievements.handlers.silly.FashionDisasterHandler;
import dev.sebi.sebchievements.handlers.silly.GravityCheckHandler;
import dev.sebi.sebchievements.handlers.silly.HoarderHandler;
import dev.sebi.sebchievements.handlers.silly.ItsFineHandler;
import dev.sebi.sebchievements.handlers.silly.OneMansTrashHandler;
import dev.sebi.sebchievements.handlers.silly.ProfessionalNapperHandler;
import dev.sebi.sebchievements.handlers.silly.SelfOwnHandler;
import dev.sebi.sebchievements.handlers.silly.StaringContestHandler;

import java.util.List;

/** The one explicit list of handlers. Adding a handler = one class + one line here. */
public final class Handlers {
    public static final List<AdvancementHandler> ALL = List.of(
            // silly
            new ProfessionalNapperHandler(),
            new ItsFineHandler(),
            new HoarderHandler(),
            new FashionDisasterHandler(),
            new CreeperWhispererHandler(),
            new SelfOwnHandler(),
            new GravityCheckHandler(),
            new StaringContestHandler(),
            new OneMansTrashHandler(),
            // social
            new NeighborlyHandler(),
            new RoommatesHandler(),
            new BetrayalHandler(),
            new GroupProjectHandler(),
            new BodyguardHandler(),
            new MedicHandler(),
            new RevengeHandler(),
            new WitnessHandler(),
            new TwinningHandler(),
            new DinnerPartyHandler(),
            // exploration
            new RoadsWereAMistakeHandler(),
            new LongWayHomeHandler(),
            new TopToBottomHandler(),
            new DimensionHopperHandler(),
            new SkyCamperHandler(),
            new CavemanHandler(),
            new MarathonHandler(),
            // challenges
            new NakedAndAfraidHandler(),
            new WhittledDownHandler(),
            new UntouchableHandler(),
            new PacifistHandler(),
            new FoodCriticHandler(),
            new TotemChainHandler(),
            new ActualSniperHandler(),
            new OneManArmyHandler(),
            new WardenBullyHandler(),
            new FreshStartHandler(),
            new WitherStormHandler(),
            new IronWillHandler(),
            new FrozenHumanHandler(),
            new FreeDiverHandler(),
            new JockeyJamboreeHandler(),
            new YeetTheGoatHandler(),
            new ObsidianObsessionHandler(),
            new MovingMountainsHandler(),
            new SkullCollectorHandler()
    );

    private Handlers() {}
}
