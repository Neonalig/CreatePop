package org.neonalig.createpop.soda;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

import javax.annotation.Nullable;
import org.neonalig.createpop.advancement.CreatePopAdvancementGrants;
import org.neonalig.createpop.advancement.ModTriggers;
import org.neonalig.createpop.component.SodaData;

public class SodaBucketItem extends BucketItem {
    public SodaBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, BlockHitResult hitResult, ItemStack container) {
        SodaData data = SodaFluidStackHelper.getSodaData(container);
        if (!level.isClientSide && player != null) {
            BrewingDiscoveryManager.learn(player, data, java.util.List.of(
                    "Carbonated Water",
                    "Potion/Soda reactants",
                    "Soda bucket sample"
            ));
        }
        boolean placed = super.emptyContents(player, level, pos, hitResult, container);
        if (placed && !level.isClientSide && !data.equals(SodaData.EMPTY) && level instanceof ServerLevel serverLevel) {
            playDestabilizeEffects(serverLevel, pos);
            if (player instanceof ServerPlayer serverPlayer) {
                CreatePopAdvancementGrants.grantPouredSodaBucket(serverPlayer);
                ModTriggers.POURED_SODA_BUCKET.trigger(serverPlayer);
            }
        }
        return placed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        SodaTextHelper.appendSodaTooltip(tooltip, SodaFluidStackHelper.getSodaData(stack));
    }

    private static void playDestabilizeEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        level.playSound(null, x, y, z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.3F + level.random.nextFloat() * 0.6F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 8, 0.25D, 0.2D, 0.25D, 0.01D);
    }
}


