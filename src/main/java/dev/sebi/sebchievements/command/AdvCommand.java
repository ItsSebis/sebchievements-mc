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
import dev.sebi.sebchievements.services.Services;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** {@code /adv progress} for everyone; the rest needs {@code smpadv.admin}. */
public final class AdvCommand {
    private static final String ADMIN = "smpadv.admin";

    private final Plugin plugin;
    private final Granter granter;
    private final PlayerDataStore store;
    private final Services services;
    private final List<AdvancementHandler> handlers;

    public AdvCommand(Plugin plugin, Granter granter, PlayerDataStore store, Services services,
                      List<AdvancementHandler> handlers) {
        this.plugin = plugin;
        this.granter = granter;
        this.store = store;
        this.services = services;
        this.handlers = handlers;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("adv")
                .then(Commands.literal("progress").executes(ctx -> progress(ctx.getSource().getSender())))
                .then(admin("test").executes(ctx -> test(ctx.getSource().getSender())))
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
        int shown = 0;
        for (AdvancementHandler h : handlers) {
            if (!(h instanceof ProgressReporting r)) continue;
            Advancement adv = Bukkit.getAdvancement(AdvancementIds.key(h.id()));
            if (adv == null || adv.getDisplay() == null) continue;
            String title = PlainTextComponentSerializer.plainText().serialize(adv.getDisplay().title());
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

    private int test(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return 0;
        }
        if (!granter.forceGrant(p, AdvancementIds.TEST)) p.sendMessage("Already earned (use /adv revoke to retest).");
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
