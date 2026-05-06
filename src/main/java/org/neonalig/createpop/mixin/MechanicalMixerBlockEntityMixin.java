package org.neonalig.createpop.mixin;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import org.neonalig.createpop.compat.create.DynamicSodaMixing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(MechanicalMixerBlockEntity.class)
public abstract class MechanicalMixerBlockEntityMixin {
    @Shadow
    protected abstract Optional<BasinBlockEntity> getBasin();

    @Inject(method = "getMatchingRecipes", at = @At("RETURN"))
    private void createpop$appendDynamicSodaRecipe(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        Optional<BasinBlockEntity> basin = getBasin();
        if (basin.isEmpty()) {
            return;
        }

        DynamicSodaMixing.findRecipe(basin.get()).ifPresent(recipe -> cir.getReturnValue().add(recipe));
    }
}

