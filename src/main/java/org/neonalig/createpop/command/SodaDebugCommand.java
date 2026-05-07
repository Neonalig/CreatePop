package org.neonalig.createpop.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.soda.SodaEffectReducer;
import org.neonalig.createpop.soda.SodaFluidStackHelper;
import org.neonalig.createpop.soda.SodaTextHelper;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SodaDebugCommand {
    private SodaDebugCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("createpop")
                        .then(Commands.literal("soda_debug")
                                .requires(source -> source.hasPermission(0))
                                .executes(SodaDebugCommand::run))
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.translatable("createpop.command.soda_debug.players_only"));
            return 0;
        }

        Inspection inspection = inspectHeldSoda(player);
        if (inspection == null) {
            source.sendFailure(Component.translatable("createpop.command.soda_debug.no_soda"));
            return 0;
        }

        SodaData data = inspection.data();
        sendLine(player, Component.translatable("createpop.command.soda_debug.header", inspection.handName(), inspection.itemName()));
        sendLine(player, Component.translatable("createpop.command.soda_debug.color", SodaTextHelper.formatColorHex(data.color())));
        sendLine(player, Component.translatable("createpop.command.soda_debug.instability", String.format(Locale.ROOT, "%.2f", data.instability())));
        if (inspection.amount() > 0) {
            sendLine(player, Component.translatable("createpop.command.soda_debug.amount", inspection.amount()));
        }

        List<MobEffectInstance> effects = SodaEffectReducer.copyEffects(data.effects());
        effects.sort(Comparator.comparing(SodaEffectReducer::effectId));
        if (effects.isEmpty()) {
            sendLine(player, Component.translatable("createpop.command.soda_debug.no_effects"));
            return 1;
        }

        sendLine(player, Component.translatable("createpop.command.soda_debug.effects"));
        for (MobEffectInstance effect : effects) {
            sendLine(player, Component.literal(" - ").append(SodaTextHelper.formatEffect(effect)));
        }
        return 1;
    }

    private static void sendLine(ServerPlayer player, Component line) {
        player.sendSystemMessage(line);
    }

    private static Inspection inspectHeldSoda(ServerPlayer player) {
        Inspection main = inspectStack(player.getMainHandItem(), Component.translatable("createpop.command.soda_debug.hand.main"));
        if (main != null) {
            return main;
        }
        return inspectStack(player.getOffhandItem(), Component.translatable("createpop.command.soda_debug.hand.off"));
    }

    private static Inspection inspectStack(ItemStack stack, Component handName) {
        if (stack.isEmpty()) {
            return null;
        }

        SodaData direct = SodaFluidStackHelper.getSodaData(stack);
        if (!direct.equals(SodaData.EMPTY)) {
            return new Inspection(handName, stack.getHoverName(), direct, 0);
        }

        var fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler == null) {
            return null;
        }

        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            FluidStack fluid = fluidHandler.getFluidInTank(tank);
            if (SodaFluidStackHelper.isSoda(fluid)) {
                return new Inspection(handName, stack.getHoverName(), SodaFluidStackHelper.getSodaData(fluid), fluid.getAmount());
            }
        }
        return null;
    }

    private record Inspection(Component handName, Component itemName, SodaData data, int amount) {
    }
}

