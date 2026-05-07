<div align="center">
  <img src="Resources/logo.png" alt="Create: Pop Logo" width="128" />
  <h1>Create: Pop</h1>
</div>

Create: Pop is a NeoForge addon for [Create](https://modrinth.com/mod/create).

Brew custom sodas in your Create mixer by carbonating water and reacting it with potions, dyes, and stabilisers. Every recipe is procedurally unique: effects combine, stack, and mutate through a reaction chain that rewards experimentation and careful record-keeping. The instability system ensures that reckless remixing has consequences - but the right stabiliser at the right moment can turn a dangerous batch into something remarkable.

## Features

* **Soda brewing via Create mixers:** Carbonate water with a diamond, then mix it with any potion to create a soda base. Sodas carry real potion effects and can be bottled, bucketed, or piped through your Create contraptions.
* **Procedural effect reactions:** Mixing two different sodas triggers a deterministic reaction phase that can fuse, split, or catalyse effects into entirely new combinations.
* **Instability system:** Every remix adds instability. Push too far and the batch backfires, injecting a random negative effect. Stabilise with acacia, magma cream, or amethyst to keep production clean.
* **Discovery and naming:** First-time recipes prompt you to name the result. Names persist across the world and sync to all players who encounter the same soda.
* **Brewer's Notebook:** Record learned recipes, ingredients, effects, and personal tasting notes. Combine or duplicate notebooks to share recipes with other players.
* **Brewer's Guide:** An in-game reference covering carbonation, mixing rules, instability, and the full discovery loop.
* **JEI integration:** Soda reaction hints are visible in JEI once discovered, with an optional cheat/debug mode to reveal all possibilities.
* **Dye recolouring:** Add a dye to the mixer to shift a soda's colour without changing its effects.

## Dependency

<div align="center">
  <table>
    <tr>
      <td align="center" width="50%">
        <a href="https://github.com/neoforged/NeoForge">
          <img src="https://raw.githubusercontent.com/neoforged/NeoForge/26.1.x/docs/assets/installer_profile_icon.png" alt="NeoForge Icon" width="64" />
          <br /><b>NeoForge</b>
        </a>
        <br />
        <img src="https://img.shields.io/github/v/tag/neoforged/NeoForge?label=Latest%20Tag&color=1f6feb" alt="NeoForge latest tag" />
        <br />
        <img src="https://img.shields.io/github/v/tag/neoforged/NeoForge?filter=21.1.*&label=Target%20Line%2021.1.x&color=2ea043" alt="NeoForge target line" />
      </td>
      <td align="center" width="50%">
        <a href="https://github.com/Creators-of-Create/Create">
          <img src="https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/.idea/icon.png" alt="Create Icon" width="64" />
          <br /><b>Create</b>
        </a>
        <br />
        <img src="https://img.shields.io/github/v/tag/Creators-of-Create/Create?label=Latest%20Tag&color=1f6feb" alt="Create latest tag" />
        <br />
        <img src="https://img.shields.io/github/v/tag/Creators-of-Create/Create?filter=mc1.21.1*&label=Target%20Line%20mc1.21.1&color=2ea043" alt="Create target line" />
      </td>
    </tr>
  </table>
</div>

---

## Technical Overview

### Version and Target

* **Mod id:** `createpop`
* **Current version:** ![Create: Pop Current Tag](https://img.shields.io/github/v/tag/Neonalig/CreatePop?label=Current%20Version&sort=semver&color=1f6feb)
* **Target Minecraft:** ![Minecraft 1.21.1](https://img.shields.io/badge/Target%20Minecraft-1.21.1-2ea043)
* **Target NeoForge:** ![NeoForge 21.1.x](https://img.shields.io/badge/Target%20NeoForge-21.1.x-2ea043)
* **Java toolchain:** ![Java 21](https://img.shields.io/badge/Java%20Toolchain-21-ea580c)

### Brewing Chain

1. Add water to a Create mixer basin.
2. Add a **diamond** to carbonate the water (heat not required).
3. Mix carbonated water with any **potion fluid** (or a potion item in the basin) to create a soda base. Each base carries a starting instability of ~0.24.
4. Mix two **different sodas** to combine and react their effects.
5. Add a **dye item** to recolour the soda without altering its effects.
6. Add a **stabiliser item** with the required heat level to reduce instability before a backfire occurs.
Every mix in steps 4-6 consumes exactly **250 mB** of each fluid input. Stabilisation requires **1000 mB**.

### Instability Mechanics

* Base creation adds `basePotionInstabilityGain` (default: 0.24) instability.
* Each remix adds `mixFlatInstabilityGain` (default: 0.12) plus `mixReactionInstabilityGain` (default: 0.45) per positive reaction resolved.
* When instability exceeds `instabilityThreshold` (default: 0.70), a random negative effect is injected and instability resets to `safeInstabilityAfterBackfire` (default: 0.45).
* **Acacia stabilisation** (heated mixer): reduces instability by `acaciaLogInstabilityReduction` (default: 0.05).
* **Magma cream stabilisation** (heated mixer): reduces instability by `magmaCreamInstabilityReduction` (default: 0.45).
* **Amethyst purification** (superheated mixer): sets instability to exactly 0.0 and inverts every negative effect to its positive counterpart.

### Reaction System

When two sodas are mixed, a deterministic per-world-seed reaction phase runs on the combined effect list:
* Each unique effect pair is tested against `reactionAffinityThreshold` (default: 0.45).
* Reactions may **fuse** (both effects consumed, all time goes to one new effect), **fission** (total time split between two effects), or **catalyse** (one effect kept at its own duration, the other converts to a new effect).
* Each triggered reaction increases instability by `mixReactionInstabilityGain`.

### Discovery System

* Picking up a soda for the first time triggers the learning flow for the player.
* Right-clicking a vessel while holding a Brewer's Notebook scans it for soda data.
* New discoveries prompt a naming dialog on the client; names are stored server-side and broadcast to all affected stacks.
* Notebooks carry their own recipe maps and can be merged, duplicated, or combined via crafting recipes.

### UI and Commands

* **Brewer's Guide** (`right-click`): In-game reference covering all major mechanics.
* **Brewer's Notebook** (`right-click in air`): Opens the notebook screen for browsing discovered recipes, reading ingredients and effects, and writing tasting notes.
* **Soda Name Prompt**: Appears automatically on first recipe discovery.
* `/createpop soda_debug`: Prints internal soda data (effects, colour, instability, fluid amount) to chat. Available to all players.

### Project Structure (Key Paths)

* [org.neonalig.createpop/soda/](https://github.com/Neonalig/CreatePop/tree/main/CreatePop/src/main/java/org/neonalig/createpop/soda/) - soda data model, fluid handlers, effect reducer, discovery manager
* [org.neonalig.createpop/compat/create/](https://github.com/Neonalig/CreatePop/tree/main/CreatePop/src/main/java/org/neonalig/createpop/compat/create/) - dynamic mixing recipe injection
* [org.neonalig.createpop/compat/jei/](https://github.com/Neonalig/CreatePop/tree/main/CreatePop/src/main/java/org/neonalig/createpop/compat/jei/) - JEI plugin
* [org.neonalig.createpop/client/](https://github.com/Neonalig/CreatePop/tree/main/CreatePop/src/main/java/org/neonalig/createpop/client/) - screen classes and client lifecycle
* [org.neonalig.createpop/item/](https://github.com/Neonalig/CreatePop/tree/main/CreatePop/src/main/java/org/neonalig/createpop/item/) - Brewer's Guide and Notebook items
* [org.neonalig.createpop/network/](https://github.com/Neonalig/CreatePop/tree/main/CreatePop/src/main/java/org/neonalig/createpop/network/) - server-to-client payloads
* [src/main/resources/data/createpop/](https://github.com/Neonalig/CreatePop/tree/main/CreatePop/src/main/resources/data/createpop/) - advancements, recipes, tags, soda name word lists
* [src/main/resources/assets/createpop/](https://github.com/Neonalig/CreatePop/tree/main/CreatePop/src/main/resources/assets/createpop/) - lang, models, blockstates

## Build and Run

### Prerequisites

* JDK 21
* Internet access for first dependency resolution

### Gradle Tasks

```powershell
# Build Jar (output in build/libs/)
.\gradlew.bat jar
# Run Dev Client
.\gradlew.bat runClient
# Run Dev Server
.\gradlew.bat runServer
# Run Data Generation
.\gradlew.bat runData
```

## Configuration

Common config (`config/createpop-common.toml`) includes:
* Instability values (base gain, reaction gain, flat gain, threshold, reset value)
* Stabilisation reduction amounts per stabiliser type
* Reaction affinity threshold
Client config (`config/createpop-client.toml`) includes:
* `forceUnlockAllJeiSodaRecipes` - debug option to show all JEI hints regardless of discovery state

## Troubleshooting

* **JEI shows no soda recipes:** Discover sodas in-game first, or enable `forceUnlockAllJeiSodaRecipes` in the client config.
* **Instability backfiring unexpectedly:** Use a stabiliser between major remix steps. Check current instability with `/createpop soda_debug`.
* **Notebook not saving recipes:** Right-click in air (not targeting a block) while holding the notebook, then use the save button in the screen.
* **Wrong fluid being consumed in mixing:** Ensure the mixer basin has at least 250 mB of each expected fluid input and the correct heat level.

## Notes for Contributors

* Keep `DynamicSodaMixing.findRecipe()` lightweight - it runs on the server thread per basin tick.
* Keep `SodaEffectReducer.mix()` deterministic given the same world seed and inputs.
* Avoid cross-thread mutation in the discovery manager.
* New item content should include recipes, advancements, lang entries, textures, and models.

## License

Licensed under the MIT License. See [LICENSE](LICENSE) for details.
