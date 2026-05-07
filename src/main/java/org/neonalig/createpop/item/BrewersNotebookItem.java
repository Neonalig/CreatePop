package org.neonalig.createpop.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.neonalig.createpop.component.BrewersNotebookData;
import org.neonalig.createpop.network.OpenBrewersNotebookPayload;
import org.neonalig.createpop.soda.BrewingDiscoveryManager;

import java.util.List;

public class BrewersNotebookItem extends WrittenBookItem {
    public BrewersNotebookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player.isShiftKeyDown()) {
            if (isTargetingBlock(player)) {
                return InteractionResultHolder.pass(stack);
            }

            int added = BrewingDiscoveryManager.writePlayerRecipesToNotebook(player, stack);
            if (added > 0) {
                player.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.saved_added", added), true);
                level.playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("entity.villager.work_cartographer")), SoundSource.PLAYERS, 0.8F, 1.0F);
            } else {
                player.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.saved_none"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (!level.isClientSide) {
            int learned = BrewingDiscoveryManager.learnFromNotebook(player, stack);
            if (learned > 0) {
                player.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.learned", learned), true);
            }

            BrewersNotebookData data = BrewingDiscoveryManager.notebookData(stack);
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenBrewersNotebookPayload(hand == InteractionHand.MAIN_HAND, data));
                serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return BrewingDiscoveryManager.notebookHasEntries(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.createpop.brewers_notebook.entry_count", BrewingDiscoveryManager.notebookEntryCount(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.createpop.brewers_notebook.tooltip_hint", Component.keybind("key.sneak")).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean isTargetingBlock(Player player) {
        HitResult hit = player.pick(5.0D, 0.0F, false);
        return hit.getType() == HitResult.Type.BLOCK;
    }
}
