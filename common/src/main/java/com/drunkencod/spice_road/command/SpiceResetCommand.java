package com.drunkencod.spice_road.command;

import java.util.List;
import java.util.function.ToIntFunction;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import com.drunkencod.spice_road.grinder.CellDiscoveries;
import com.drunkencod.spice_road.grinder.GrinderView;
import com.drunkencod.spice_road.grinder.SpiceGrinderMenu;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.stats.ModStats;
import com.drunkencod.spice_road.stats.SpiceFindings;
import com.drunkencod.spice_road.stats.SpiceFindingsSync;

/**
 * {@code /spice_road reset found_spices} and {@code /spice_road reset unlocked_cells}:
 * wipe a player's progress, together with the statistic counting it. Anyone may
 * reset themselves (unless the server config turns that off); naming another
 * player takes operator permission.
 * <p>
 * Every reset is two-step: the bare command only says what would be erased
 * and offers a clickable confirmation, which runs the same command with a
 * trailing {@code confirm}.
 */
final class SpiceResetCommand {

    private static final String CONFIRM = "confirm";
    private static final String PLAYER = "player";

    private static final Reset FOUND_SPICES = new Reset("found_spices", ModStats.SPICES_FOUND,
            player -> SpiceFindings.of(player.server).count(player.getUUID()),
            player -> {
                int removed = SpiceFindings.of(player.server).clear(player.getUUID());
                Services.NETWORK.sendToPlayer(player, SpiceFindingsSync.of(player));
                return removed;
            });

    private static final Reset UNLOCKED_CELLS = new Reset("unlocked_cells", ModStats.CELLS_UNLOCKED,
            player -> CellDiscoveries.of(player.server).count(player.getUUID()),
            player -> {
                int removed = CellDiscoveries.of(player.server).clear(player.getUUID());
                if (player.containerMenu instanceof SpiceGrinderMenu menu)
                    menu.syncView(GrinderView.Event.NONE);
                return removed;
            });

    /**
     * One kind of progress that can be reset.
     *
     * @param name  The literal that selects it.
     * @param stat  The statistic counting it, zeroed along with it.
     * @param count How much of it a player has, for the preview.
     * @param clear Forgets it all and returns how much that was.
     */
    private record Reset(String name, ResourceLocation stat, ToIntFunction<ServerPlayer> count,
            ToIntFunction<ServerPlayer> clear) {

        String key(String suffix) {
            return "commands.spice_road.reset." + name + "." + suffix;
        }
    }

    private SpiceResetCommand() {
    }

    /** @return The {@code reset} node, to be added below the {@code /spice_road} root. */
    static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> reset = Commands.literal("reset")
                .requires(source -> source.hasPermission(2) || Services.CONFIG.isCommandPlayerResetAllowed());
        for (Reset kind : List.of(FOUND_SPICES, UNLOCKED_CELLS)) {
            reset.then(Commands.literal(kind.name())
                    .executes(context -> run(context.getSource(), kind, context.getSource().getPlayerOrException(),
                            false))
                    .then(Commands.literal(CONFIRM)
                            .executes(context -> run(context.getSource(), kind,
                                    context.getSource().getPlayerOrException(), true)))
                    .then(Commands.argument(PLAYER, EntityArgument.player())
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> run(context.getSource(), kind,
                                    EntityArgument.getPlayer(context, PLAYER), false))
                            .then(Commands.literal(CONFIRM)
                                    .executes(context -> run(context.getSource(), kind,
                                            EntityArgument.getPlayer(context, PLAYER), true)))));
        }
        return reset;
    }

    /**
     * Previews or carries out a reset.
     *
     * @param confirmed Whether to carry it out, instead of only describing it.
     * @return How much was forgotten; {@code 0} for a preview.
     */
    private static int run(CommandSourceStack source, Reset kind, ServerPlayer target, boolean confirmed) {
        Component name = target.getDisplayName();
        int count = kind.count().applyAsInt(target);
        if (count == 0 && ModStats.get(target, kind.stat()) == 0) {
            source.sendSuccess(() -> Component.translatable(kind.key("nothing"), name), false);
            return 0;
        }

        if (!confirmed) {
            String command = "/spice_road reset " + kind.name()
                    + (source.getEntity() == target ? "" : " " + target.getGameProfile().getName()) + " " + CONFIRM;
            Component button = Component.translatable("commands.spice_road.reset.confirm")
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("commands.spice_road.reset.confirm.tooltip"))));
            source.sendSuccess(() -> Component.translatable(kind.key("preview"), count, name).append(" ")
                    .append(button), false);
            return 0;
        }

        int removed = kind.clear().applyAsInt(target);
        ModStats.reset(target, kind.stat());
        source.sendSuccess(() -> Component.translatable(kind.key("success"), removed, name), true);
        return removed;
    }
}
