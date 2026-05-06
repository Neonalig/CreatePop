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
import net.minecraft.world.effect.MobEffectInstance;
import org.neonalig.createpop.component.SodaData;

public class SodaBottleItem extends Item {
    public SodaBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        Player player = livingEntity instanceof Player p ? p : null;

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }

        if (!level.isClientSide) {
            SodaData data = SodaFluidStackHelper.getSodaData(stack);
            for (MobEffectInstance effect : data.effects()) {
                livingEntity.addEffect(new MobEffectInstance(effect));
            }
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
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        return ItemUtils.startUsingInstantly(level, player, usedHand);
    }
}

