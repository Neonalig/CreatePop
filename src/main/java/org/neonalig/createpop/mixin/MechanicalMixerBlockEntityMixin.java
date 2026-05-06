package org.neonalig.createpop.mixin;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import org.neonalig.createpop.compat.create.DynamicSodaMixing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MechanicalMixerBlockEntity.class)
public abstract class MechanicalMixerBlockEntityMixin {
    @Inject(method = "getMatchingRecipes", at = @At("RETURN"))
    private void createpop$appendDynamicSodaRecipe(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }

        BlockEntity basinEntity = level.getBlockEntity(self.getBlockPos().below(2));
        if (!(basinEntity instanceof BasinBlockEntity basin)) {
            return;
        }

        DynamicSodaMixing.findRecipe(basin).ifPresent(recipe -> cir.getReturnValue().add(recipe));
    }
}

