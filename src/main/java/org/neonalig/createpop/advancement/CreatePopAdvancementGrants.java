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
    private static final ResourceLocation REQUIRED_READING = id("required_reading");
    private static final ResourceLocation BLANK_PAGE_PROBLEM = id("blank_page_problem");
    private static final ResourceLocation MARKETING_DEPARTMENT = id("marketing_department");
    private static final ResourceLocation BLOOP = id("bloop");
    private static final ResourceLocation BASIC_HYDRATION = id("basic_hydration");
    private static final ResourceLocation PROOF_OF_CONCEPT = id("proof_of_concept");
    private static final ResourceLocation SCIENCE_PART = id("science_part");
    private static final ResourceLocation GRAND_OPENING = id("grand_opening");
    private static final ResourceLocation OVERCROWDED = id("overcrowded");
    private static final ResourceLocation OOPSIE_FIZZIE = id("oopsie_fizzie");
    private static final ResourceLocation ZERO_DEFECTS = id("zero_defects");
    private static final ResourceLocation GUM_ARABIC = id("gum_arabic");
    private static final ResourceLocation THE_EMULSIFIER = id("the_emulsifier");
    private static final ResourceLocation VITRIFICATION_PROTOCOL = id("vitrification_protocol");

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
            award(player, REQUIRED_READING);
        }
        if (hasNotebook) {
            award(player, REQUIRED_READING);
            award(player, BLANK_PAGE_PROBLEM);
        }
        if (hasCarb) {
            award(player, BLOOP);
        }
        if (hasSoda) {
            award(player, PROOF_OF_CONCEPT);
        }
    }

    public static void grantBasicHydration(ServerPlayer player) {
        grantRoot(player);
        award(player, BLOOP);
        award(player, BASIC_HYDRATION);
    }

    public static void grantObtainedSoda(ServerPlayer player) {
        grantRoot(player);
        award(player, PROOF_OF_CONCEPT);
    }

    public static void grantDrankSoda(ServerPlayer player) {
        LOGGER.debug("[CreatePopAdv] drank soda event for {}", player.getGameProfile().getName());
        grantObtainedSoda(player);
        award(player, SCIENCE_PART);
    }

    public static void grantPouredSodaBucket(ServerPlayer player) {
        LOGGER.debug("[CreatePopAdv] poured soda bucket event for {}", player.getGameProfile().getName());
        grantObtainedSoda(player);
        award(player, GRAND_OPENING);
    }

    public static void grantNamedSoda(ServerPlayer player) {
        LOGGER.debug("[CreatePopAdv] named soda event for {}", player.getGameProfile().getName());
        grantRoot(player);
        award(player, REQUIRED_READING);
        award(player, BLANK_PAGE_PROBLEM);
        award(player, MARKETING_DEPARTMENT);
    }

    public static void grantCompoundSoda(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, OVERCROWDED);
    }

    public static void grantNegativeSoda(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, OOPSIE_FIZZIE);
    }

    public static void grantPerfectSoda(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, ZERO_DEFECTS);
    }

    public static void grantStabilisedWithAcacia(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, GUM_ARABIC);
    }

    public static void grantStabilisedWithMagmaCream(ServerPlayer player) {
        grantObtainedSoda(player);
        award(player, THE_EMULSIFIER);
    }

    public static void grantStabilisedWithAmethyst(ServerPlayer player) {
        grantPerfectSoda(player);
        award(player, VITRIFICATION_PROTOCOL);
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
                REQUIRED_READING,
                BLANK_PAGE_PROBLEM,
                MARKETING_DEPARTMENT,
                BLOOP,
                BASIC_HYDRATION,
                PROOF_OF_CONCEPT,
                SCIENCE_PART,
                GRAND_OPENING,
                OVERCROWDED,
                OOPSIE_FIZZIE,
                ZERO_DEFECTS,
                GUM_ARABIC,
                THE_EMULSIFIER,
                VITRIFICATION_PROTOCOL
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

