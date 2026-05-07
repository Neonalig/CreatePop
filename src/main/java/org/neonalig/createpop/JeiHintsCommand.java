package org.neonalig.createpop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.neonalig.createpop.soda.BrewingDiscoveryManager;

import java.util.Collection;

public final class JeiHintsCommand {
    private JeiHintsCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("createpop")
                        .then(Commands.literal("unlock_jei_hints")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(JeiHintsCommand::run))));
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        final Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(context, "targets");
        } catch (CommandSyntaxException exception) {
            context.getSource().sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }

        int totalAdded = 0;
        for (ServerPlayer player : targets) {
            totalAdded += BrewingDiscoveryManager.unlockAllJeiHintRecipes(player);
        }

        final int added = totalAdded;
        context.getSource().sendSuccess(
                () -> Component.translatable("createpop.command.unlock_jei_hints.success", targets.size(), added),
                true
        );
        return added;
    }
}

