package org.neonalig.createpop.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

import java.util.List;

public class BrewersGuideItem extends WrittenBookItem {
    public BrewersGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, createGuideContent());

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundOpenBookPacket(hand));
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static WrittenBookContent createGuideContent() {
        List<Filterable<Component>> pages = List.of(
                page(Component.literal("Brewer's Guide\n\n")
                        .append(Component.literal("1) Carbonate water in a Create mixer using water + diamond.\n"))
                        .append(Component.literal("2) Bottle or bucket the carbonated water for transport."))),
                page(Component.literal("Making Soda\n\n")
                        .append(Component.literal("Mix carbonated water with a beneficial tier-1 potion to create your first soda base.\n\n"))
                        .append(Component.literal("Soda can also be recolored with dyes."))),
                page(Component.literal("Mixing Tips\n\n")
                        .append(Component.literal("- Mix different sodas to combine effects.\n"))
                        .append(Component.literal("- Dilution reduces duration when one input is much smaller than the other.\n"))
                        .append(Component.literal("- Matching effects stack duration."))),
                page(Component.literal("Instability\n\n")
                        .append(Component.literal("Each mix adds instability. Very unstable mixes can gain negative side effects.\n\n"))
                        .append(Component.literal("Stabilise with: stripped acacia log (heated), magma cream (heated), or amethyst shard (superheated).")))
        );

        return new WrittenBookContent(
                Filterable.passThrough("Brewer's Guide"),
                "Create Pop",
                0,
                pages,
                true
        );
    }

    private static Filterable<Component> page(Component text) {
        return Filterable.passThrough(text);
    }
}

