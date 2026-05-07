package org.neonalig.createpop.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.mojang.logging.LogUtils;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.registry.ModFluids;
import org.neonalig.createpop.registry.ModItems;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized helpers to grant Create: Pop advancements from server-side events.
 * This mirrors the robust direct-award approach used in Create: Polyphony.
 */
public final class CreatePopAdvancementGrants {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> MISSING_ADVANCEMENT_IDS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<UUID, String> LAST_INVENTORY_DEBUG_SIGNATURE = new ConcurrentHashMap<>();
    private static final Set<String> FIRST_RESOLVED = ConcurrentHashMap.newKeySet();
    private static final Set<String> FIRST_ALREADY_DONE = ConcurrentHashMap.newKeySet();
    private static final Set<String> FIRST_EMPTY_REMAINING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FIRST_ZERO_AWARDED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FIRST_DUMPED_PLAYER_STATE = ConcurrentHashMap.newKeySet();

    private static final ResourceLocation ROOT = id("root");
    private static final ResourceLocation GUIDE_OBTAINED = id("guide_obtained");
    private static final ResourceLocation NOTEBOOK_OBTAINED = id("notebook_obtained");
    private static final ResourceLocation SODA_NAMED = id("soda_named");
    private static final ResourceLocation CARBONATED_WATER_OBTAINED = id("carbonated_water_obtained");
    private static final ResourceLocation CARBONATED_WATER_CONSUMED = id("carbonated_water_consumed");
    private static final ResourceLocation SODA_OBTAINED = id("soda_obtained");
    private static final ResourceLocation SODA_CONSUMED = id("soda_consumed");
    private static final ResourceLocation SODA_POURED = id("soda_poured");
    private static final ResourceLocation COMPOUND_SODA_OBTAINED = id("compound_soda_obtained");
    private static final ResourceLocation NEGATIVE_SODA_OBTAINED = id("negative_soda_obtained");
    private static final ResourceLocation PERFECT_SODA_OBTAINED = id("perfect_soda_obtained");
    private static final ResourceLocation STABILISED_WITH_ACACIA = id("stabilised_with_acacia");
    private static final ResourceLocation STABILISED_WITH_MAGMA_CREAM = id("stabilised_with_magma_cream");
    private static final ResourceLocation STABILISED_WITH_AMETHYST = id("stabilised_with_amethyst");

    private CreatePopAdvancementGrants() {
    }

    public static void grantRoot(ServerPlayer player) {
        award(player, ROOT);
    }

    public static void grantInventoryAdvancements(ServerPlayer player) {
        if (FIRST_DUMPED_PLAYER_STATE.add(player.getUUID())) {
            dumpAdvancementState(player);
        }

        grantRoot(player);

        boolean hasGuide = hasItem(player, ModItems.BREWERS_GUIDE.get());
        boolean hasNotebook = hasItem(player, ModItems.BREWERS_NOTEBOOK.get());
        boolean hasCarb = hasItem(player, ModFluids.CARBONATED_WATER_BOTTLE.get()) || hasItem(player, ModFluids.CARBONATED_WATER_BUCKET.get());
        boolean hasSoda = hasItem(player, ModFluids.SODA_BOTTLE.get()) || hasItem(player, ModFluids.SODA_BUCKET.get());

        String signature = hasGuide + ":" + hasNotebook + ":" + hasCarb + ":" + hasSoda;
        String last = LAST_INVENTORY_DEBUG_SIGNATURE.put(player.getUUID(), signature);
        if (!signature.equals(last)) {
            LOGGER.debug("[CreatePopAdv] inventory snapshot {} -> guide={}, notebook={}, carb={}, soda={}",
                    player.getGameProfile().getName(), hasGuide, hasNotebook, hasCarb, hasSoda);
        }

        if (hasGuide) {
            award(player, GUIDE_OBTAINED);
        }
        if (hasNotebook) {
            award(player, GUIDE_OBTAINED);
            award(player, NOTEBOOK_OBTAINED);
        }
        if (hasCarb) {
            award(player, CARBONATED_WATER_OBTAINED);
        }
        if (hasSoda) {
            award(player, SODA_OBTAINED);
        }
    }

    public static void grantBasicHydration(ServerPlayer player) {
        grantRoot(player);
        award(player, CARBONATED_WATER_OBTAINED);
        award(player, CARBONATED_WATER_CONSUMED);
    }

    public static void grantObtainedSoda(ServerPlayer player) {
        grantRoot(player);
        award(player, SODA_OBTAINED);
    }

    public static void grantDrankSoda(ServerPlayer player) {
        LOGGER.debug("[CreatePopAdv] drank soda event for {}", player.getGameProfile().getName());
        grantObtainedSoda(player);
        award(player, SODA_CONSUMED);
    }

    public static void grantPouredSodaBucket(ServerPlayer player) {
        LOGGER.debug("[CreatePopAdv] poured soda bucket event for {}", player.getGameProfile().getName());
        grantObtainedSoda(player);
        award(player, SODA_POURED);
    }

    public static void grantNamedSoda(ServerPlayer player) {
        LOGGER.debug("[CreatePopAdv] named soda event for {}", player.getGameProfile().getName());
        grantRoot(player);
        award(player, GUIDE_OBTAINED);
        award(player, NOTEBOOK_OBTAINED);
        award(player, SODA_NAMED);
    }

    public static void grantCompoundSoda(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, COMPOUND_SODA_OBTAINED);
    }

    public static void grantNegativeSoda(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, NEGATIVE_SODA_OBTAINED);
    }

    public static void grantPerfectSoda(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, PERFECT_SODA_OBTAINED);
    }

    public static void grantStabilisedWithAcacia(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, STABILISED_WITH_ACACIA);
    }

    public static void grantStabilisedWithMagmaCream(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, STABILISED_WITH_MAGMA_CREAM);
    }

    public static void grantStabilisedWithAmethyst(ServerPlayer player) {
        grantPerfectSoda(player);
        award(player, STABILISED_WITH_AMETHYST);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, path);
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        return contains(player.getInventory().items, item) || contains(player.getInventory().offhand, item);
    }

    private static boolean contains(List<ItemStack> stacks, Item item) {
        for (ItemStack stack : stacks) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void award(ServerPlayer player, ResourceLocation id) {
        String playerAdvKey = player.getUUID() + "|" + id;

        if (player.getServer() == null) {
            LOGGER.warn("[CreatePopAdv] skipping award {} for {} because server is null", id, player.getGameProfile().getName());
            return;
        }

        AdvancementHolder advancement = player.getServer().getAdvancements().get(id);
        if (advancement == null) {
            if (MISSING_ADVANCEMENT_IDS.add(id)) {
                LOGGER.warn("[CreatePopAdv] advancement {} not found in server registry (check data/{}/advancement path and JSON ids)", id, CreatePop.MODID);
            }
            return;
        }

        if (FIRST_RESOLVED.add(playerAdvKey)) {
            LOGGER.debug("[CreatePopAdv] resolved {} for {}", id, player.getGameProfile().getName());
        }

        PlayerAdvancements playerAdvancements = player.getAdvancements();
        var progress = playerAdvancements.getOrStartProgress(advancement);
        if (progress.isDone()) {
            if (FIRST_ALREADY_DONE.add(playerAdvKey)) {
                LOGGER.debug("[CreatePopAdv] {} already done for {}", id, player.getGameProfile().getName());
            }
            return;
        }

        List<String> remaining = new ArrayList<>();
        for (String criterion : progress.getRemainingCriteria()) {
            remaining.add(criterion);
        }

        if (remaining.isEmpty()) {
            if (FIRST_EMPTY_REMAINING.add(playerAdvKey)) {
                LOGGER.debug("[CreatePopAdv] advancement {} had no remaining criteria for {}", id, player.getGameProfile().getName());
            }
            return;
        }

        int awarded = 0;
        for (String criterion : remaining) {
            if (playerAdvancements.award(advancement, criterion)) {
                awarded++;
            }
        }

        if (awarded > 0) {
            LOGGER.info("[CreatePopAdv] awarded {} criterion/criteria for {} to {}", awarded, id, player.getGameProfile().getName());
        } else if (FIRST_ZERO_AWARDED.add(playerAdvKey)) {
            LOGGER.warn("[CreatePopAdv] could not award any criteria for {} to {} (remaining={})", id, player.getGameProfile().getName(), remaining);
        }
    }

    private static void dumpAdvancementState(ServerPlayer player) {
        LOGGER.info("[CreatePopAdv] ----- advancement state dump for {} -----", player.getGameProfile().getName());
        List<ResourceLocation> ids = List.of(
                ROOT,
                GUIDE_OBTAINED,
                NOTEBOOK_OBTAINED,
                SODA_NAMED,
                CARBONATED_WATER_OBTAINED,
                CARBONATED_WATER_CONSUMED,
                SODA_OBTAINED,
                SODA_CONSUMED,
                SODA_POURED,
                COMPOUND_SODA_OBTAINED,
                NEGATIVE_SODA_OBTAINED,
                PERFECT_SODA_OBTAINED,
                STABILISED_WITH_ACACIA,
                STABILISED_WITH_MAGMA_CREAM,
                STABILISED_WITH_AMETHYST
        );

        for (ResourceLocation id : ids) {
            AdvancementHolder holder = player.getServer() == null ? null : player.getServer().getAdvancements().get(id);
            if (holder == null) {
                LOGGER.warn("[CreatePopAdv] dump {} -> MISSING", id);
                continue;
            }

            var adv = holder.value();
            boolean done = player.getAdvancements().getOrStartProgress(holder).isDone();
            LOGGER.info("[CreatePopAdv] dump {} -> present, done={}, display={}, root={}, parent={}",
                    id,
                    done,
                    adv.display().isPresent(),
                    adv.isRoot(),
                    adv.parent().map(ResourceLocation::toString).orElse("<none>"));
        }
        LOGGER.info("[CreatePopAdv] ----- end advancement state dump -----");
    }
}

