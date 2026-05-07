package org.neonalig.createpop.soda;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.neonalig.createpop.advancement.CreatePopAdvancementGrants;

import javax.annotation.Nonnull;

public class CarbonatedWaterBottleItem extends Item {
    public CarbonatedWaterBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity livingEntity) {
        Player player = livingEntity instanceof Player p ? p : null;

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            CreatePopAdvancementGrants.grantBasicHydration(serverPlayer);
        }

        if (player != null && player.getAbilities().instabuild) {
            return stack;
        }

        stack.shrink(1);
        ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) {
            return glassBottle;
        }

        if (player != null) {
            player.getInventory().add(glassBottle);
        }

        return stack;
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity entity) {
        return 32;
    }

    @Override
    @Nonnull
    public UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand usedHand) {
        return ItemUtils.startUsingInstantly(level, player, usedHand);
    }
}

