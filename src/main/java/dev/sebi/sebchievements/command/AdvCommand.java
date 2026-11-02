package dev.sebi.sebchievements.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.Granter;
import dev.sebi.sebchievements.core.PlayerDataStore;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import dev.sebi.sebchievements.rewards.PerkMenu;
import dev.sebi.sebchievements.rewards.PerkService;
import dev.sebi.sebchievements.rewards.RewardMenu;
import dev.sebi.sebchievements.rewards.RewardService;
import dev.sebi.sebchievements.services.Services;
import dev.sebi.sebchievements.tracking.TrackService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** {@code /adv progress}, {@code track}, {@code untrack}, {@code rewards} and {@code perks} for everyone; the rest needs {@code smpadv.admin}. */
public final class AdvCommand {
    private static final String ADMIN = "smpadv.admin";

    private final Plugin plugin;
    private final Granter granter;
    private final PlayerDataStore store;
    private final Services services;
    private final List<AdvancementHandler> handlers;

    private final RewardService rewards;
    private final PerkService perks;
    private final TrackService track;

    public AdvCommand(Plugin plugin, Granter granter, PlayerDataStore store, Services services,
                      List<AdvancementHandler> handlers, RewardService rewards, PerkService perks,
                      TrackService track) {
        this.plugin = plugin;
        this.granter = granter;
        this.store = store;
        this.services = services;
        this.handlers = handlers;
        this.rewards = rewards;
        this.perks = perks;
        this.track = track;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("adv")
                .then(Commands.literal("progress").executes(ctx -> progress(ctx.getSource().getSender())))
                .then(Commands.literal("rewards").executes(ctx -> rewards(ctx.getSource().getSender(), false)))
                .then(Commands.literal("track").then(idArgument().executes(ctx -> track(ctx.getSource().getSender(),
                        StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("untrack").executes(ctx -> untrack(ctx.getSource().getSender(), null))
                        .then(idArgument().executes(ctx -> untrack(ctx.getSource().getSender(),
                                StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("perks").executes(ctx -> rewards(ctx.getSource().getSender(), true)))
                .then(admin("grant").then(playerAndId(ctx -> change(ctx, Op.GRANT))))
                .then(admin("revoke").then(playerAndId(ctx -> change(ctx, Op.REVOKE))))
                .then(admin("reset").then(playerAndId(ctx -> change(ctx, Op.RESET))))
                .then(admin("debug")
                        .then(idArgument().executes(ctx -> debug(ctx, null))
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(ctx -> debug(ctx, resolvePlayer(ctx))))))
                .build();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> admin(String name) {
        return Commands.literal(name).requires(src -> src.getSender().hasPermission(ADMIN));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> idArgument() {
        return Commands.argument("id", StringArgumentType.word()).suggests((ctx, builder) -> {
            AdvancementIds.ALL.stream().sorted().filter(i -> i.startsWith(builder.getRemainingLowerCase()))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, PlayerSelectorArgumentResolver> playerAndId(
            Command<CommandSourceStack> run) {
        return Commands.argument("player", ArgumentTypes.player()).then(idArgument().executes(run));
    }

    private static Player resolvePlayer(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        List<Player> found = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
        return found.isEmpty() ? null : found.get(0);
    }

    private int progress(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return 0;
        }
        // a header with the time, so several runs stacked in the chat can be told apart
        p.sendMessage(Component.text("--- Progress at " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS) + " ---",
                NamedTextColor.GOLD));
        int shown = 0;
        for (AdvancementHandler h : handlers) {
            if (!(h instanceof ProgressReporting r)) continue;
            Advancement adv = Bukkit.getAdvancement(AdvancementIds.key(h.id()));
            if (adv == null || adv.getDisplay() == null) continue;
            String title = TrackService.titleOf(adv);
            boolean done = p.getAdvancementProgress(adv).isDone();
            Optional<Progress> prog = r.progress(p);
            String text = done ? "Completed" : prog.map(Progress::format).orElse("not started");
            p.sendMessage(Component.text(title + ": ", NamedTextColor.YELLOW)
                    .append(Component.text(text, done ? NamedTextColor.GREEN : NamedTextColor.WHITE)));
            shown++;
        }
        if (shown == 0) p.sendMessage(Component.text("No timed or streak advancements yet.", NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private int track(CommandSender sender, String id) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return 0;
        }
        TrackService.Result result = track.track(p, id);
        String text = switch (result) {
            case TRACKED -> "Tracking " + id + " (/adv untrack " + id + " to stop).";
            case ALREADY -> "Already tracking " + id + ".";
            case UNKNOWN -> "Unknown advancement id: " + id;
            case DONE -> "You already completed " + id + ".";
            case NO_PROGRESS -> id + " has no progress to show.";
        };
        boolean ok = result == TrackService.Result.TRACKED || result == TrackService.Result.ALREADY;
        p.sendMessage(Component.text(text, ok ? NamedTextColor.GREEN : NamedTextColor.RED));
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private int untrack(CommandSender sender, String id) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return 0;
        }
        if (id == null) {
            p.sendMessage(Component.text("Stopped tracking " + track.untrackAll(p) + " advancement(s).", NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        }
        boolean removed = track.untrack(p, id);
        p.sendMessage(Component.text(removed ? "Stopped tracking " + id + "." : "You are not tracking " + id + ".",
                removed ? NamedTextColor.GREEN : NamedTextColor.RED));
        return removed ? Command.SINGLE_SUCCESS : 0;
    }

    private int rewards(CommandSender sender, boolean perkMenu) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return 0;
        }
        if (perkMenu) PerkMenu.open(rewards, perks, p);
        else RewardMenu.open(rewards, perks, p);
        return Command.SINGLE_SUCCESS;
    }

    private enum Op { GRANT, REVOKE, RESET }

    private int change(CommandContext<CommandSourceStack> ctx, Op op) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        Player target = resolvePlayer(ctx);
        String id = StringArgumentType.getString(ctx, "id");
        if (target == null) {
            sender.sendMessage(Component.text("No such player online.", NamedTextColor.RED));
            return 0;
        }
        if (!AdvancementIds.ALL.contains(id)) {
            sender.sendMessage(Component.text("Unknown advancement id: " + id, NamedTextColor.RED));
            return 0;
        }
        String result = switch (op) {
            case GRANT -> granter.forceGrant(target, id) ? "Granted" : "Nothing to grant for";
            case REVOKE -> granter.revoke(target, id) ? "Revoked" : "Nothing to revoke for";
            case RESET -> {
                services.resetState(target.getUniqueId(), id);
                for (String key : store.dump(target, id).keySet()) store.remove(target, key);
                yield "Reset tracked state of";
            }
        };
        sender.sendMessage(Component.text(result + " " + id + " (" + target.getName() + ")", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int debug(CommandContext<CommandSourceStack> ctx, Player explicit) {
        CommandSender sender = ctx.getSource().getSender();
        Player target = explicit != null ? explicit : sender instanceof Player p ? p : null;
        String id = StringArgumentType.getString(ctx, "id");
        if (target == null || !AdvancementIds.ALL.contains(id)) {
            sender.sendMessage(Component.text("Usage: /adv debug <id> [player] (valid id, online player)", NamedTextColor.RED));
            return 0;
        }
        sender.sendMessage(Component.text("State of " + id + " for " + target.getName() + ":", NamedTextColor.YELLOW));
        Map<String, String> dump = store.dump(target, id);
        if (dump.isEmpty()) sender.sendMessage(Component.text("  (no stored state)", NamedTextColor.GRAY));
        dump.forEach((k, v) -> sender.sendMessage("  " + k + " = " + v));
        for (AdvancementHandler h : handlers) {
            if (h.id().equals(id)) h.debug(target).forEach(line -> sender.sendMessage("  " + line));
        }
        return Command.SINGLE_SUCCESS;
    }
}
