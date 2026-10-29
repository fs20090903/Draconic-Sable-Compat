# Draconic Sable Compatibility

Targeted compatibility for Minecraft 1.21.1, NeoForge 21.1.259, Draconic Evolution 3.1.4.633, BrandonsCore 3.2.1.309 and Sable 2.0.5. This addon targets these specific versions; it does not claim complete support for every Draconic Evolution feature on moving structures.

## Installation

Copy `build/libs/draconic-sable-compat-1.21.1-neoforge-0.1.6-licensing.1.jar` into the instance's `mods` directory, replacing the older compatibility jar. Install the same jar on the hosting instance/server **and every client**. Restart Minecraft/server after installation. Keep the existing Draconic Evolution, BrandonsCore, CodeChickenLib and Sable jars; this does not replace them.

Test on a copy of your world before using a hot reactor. The included automated checks use disposable worlds; see [VALIDATION.md](VALIDATION.md).

Before uninstalling, cool/shut down reactors and deactivate Energy Cores so their mod-owned persistent loading tickets are released, then save the world.

## Licensing and distribution

Original contributions are MIT licensed. Draconic Evolution-derived portions retain
DE's custom license; this is a mixed-license addon, not an MIT relicensing of DE.
See [LICENSING.md](LICENSING.md), [NOTICE](NOTICE), and [DISTRIBUTION.md](DISTRIBUTION.md).
Source repository: https://github.com/fs20090903/Draconic-Sable-Compat. The upstream terms require source visibility.

### 0.1.6-licensing.1

Licensing and packaging revision of 0.1.6: source attribution, bundled license texts,
mixed-license mod metadata and an explicitly scoped source archive. No gameplay
changes. Build the runtime JAR and source ZIP with `gradlew.bat build sourceDistribution`.

## Changes

### 0.1.6 removal allowance and diagnostics

- Raises the sublevel removal allowance to 12 ms per slice / per level tick, shared across simultaneous blast work, with an 8,192-entry ceiling. Calculation still uses its previous 2 ms slice / 6 ms ceiling. The budget remains cooperative: individual callbacks and the final mass flush can exceed it. Terrain removal remains the separate upstream DE process.
- Reports removal slice count, time-limit and block-limit stops, largest batch, total synchronous work time and longest slice. These distinguish a block-count bottleneck from time spent in block callbacks. Cancelled plans log that removal ended, rather than claiming completion.
- Holds, mass batching, damage selection, power and countdown behavior are unchanged. A higher allowance can increase individual tick work; it is not a guaranteed proportional speedup.
### 0.1.5 temporary explosion holds and faster sublevel removal

- At detonation, only bodies with selected sublevel damage receive transient world anchors using Sable's staff-lock constraint mechanism. Existing user locks and connections are left intact. This holds their pose; it does not globally pause Sable or disable collision geometry.
- Per-body force producers and queued force application pause during the hold. Physics mass/collider updates continue. Overlapping holds share a reference count; the last release waits two ticks for final updates, then restores the body's pre-hold linear/angular velocity. Temporary anchors and loading tickets are never saved as user locks and are released on cancellation, body removal, or normal server shutdown. A hard restart prunes orphan temporary tickets.
- Sublevel removal increases from 256 blocks / 2 ms per slice to 4,096 blocks / 8 ms. The time budget is cooperative and shares an 8 ms ceiling per level/tick with other blast work; individual block callbacks and final mass flushes can overrun it. Terrain removal still follows DE's separate native process.
- Sable still updates each removed block's mass bookkeeping, collision geometry, invalid-mass checks and block callbacks. Merged/native mass updates are coalesced at each removal-slice boundary, avoiding repeated uploads after individual blocks. No raw palette edits bypass machine cleanup or neighbor callbacks.
- Removal logs now include elapsed game ticks, batch cap and time allowance. These changes do not increase explosion radius, power, or the set of selected blocks.
### 0.1.4 reactor preparation and detonation

- Preparation runs during DE's existing safety countdown, rather than adding its duration before the countdown. The normal safety delay is retained. If preparation is still unfinished at expiry, the core remains present until the real detonation; the countdown is held at zero instead of restarting or deleting the core early.
- Revised plans reset their actual completion state. Movement no longer discards plans throughout the entire safety countdown: one near-end refresh and a final validity check replace that repeated work. A final request commits one world-space detonation location so continuing motion cannot make preparation chase the aircraft indefinitely.
- Iterative vertical rays replace recursive air traversal and large boxed air-visited sets. Empty sections and air above WORLD_SURFACE are skipped while retaining their resistance sum. Multiple rings can finish in one cooperative time slice. Block resistance, fluid/lava behavior, vertical limits, and overlapping sublevel damage remain part of the calculation.
- Terrain chunks are cached and held for preparation/removal. Replanning transfers the terrain cache instead of discarding loaded work. Tickets release after terrain removal, cancellation, or normal server shutdown. Revised body snapshots still re-evaluate moved bodies against the new world-space blast; this is not unconditional reuse of old damage decisions merely because a body remains within the maximum radius.
- The calculation retains the 2 ms per-call/6 ms per-level cooperative budget. Native Minecraft chunk loading/generation can exceed a slice and remains a limiting factor in ungenerated terrain. An experimental asynchronous loader was rejected after a shutdown regression and is not included.
- Targeted progress and removal counters remain in the log. No new network packets, save format, configuration edits, or renderer changes are introduced by this release.

- Reactor injector/stabilizer placement uses player position and view direction in the sublevel's coordinates, including above/below placement thresholds. Explicit copied `facing` block-entity data is respected. Dislocator Pedestal placement similarly uses local yaw.
- Reactors and Energy Cores have virtual mechanical connections to their own valid components across the intentional air gaps. Sable's connectivity gathering and splitting retain these connections. Removing a reactor's intrusive support block no longer separates the reactor core. This does not weld two already independent sublevels together.
- Sable assembly rotation updates DE's hidden directions, relative component offsets, stabilizer axes and crystal link offsets. Moving a hot assembled system does not invoke normal component-breaking callbacks.
- Pylons look up the actual loaded core chunk instead of requiring a surrounding area of nonexistent sparse sublevel chunks. Tests exercise actual FE/OP insertion/extraction and crystal-to-pylon-to-core transfer.
- Links survive temporary unavailability without retaining stale block-entity objects. Reactor simulation/energy access waits for missing saved components during incomplete loading; an actually missing block in a loaded chunk retains normal failure behaviour.
- Active Energy Cores and reactors with active shielding own persistent force-loading tickets. On a sublevel this keeps the body loaded. On terrain it holds a 5×5 chunk area for an Energy Core and a 3×3 area for a reactor. Each core owns its own ticket; one core stopping does not unload another. Removed/moved/inactive owners release their tickets.
- Reactor core rendering runs through the normal block-entity renderer on both terrain and sublevels, with the old transparent pass suppressed for this renderer to avoid duplication. This addresses the missing terrain core with Sodium as well as the missing sublevel core.
- Crystal beam endpoints, wireless receiver particles, crystal effects and their culling bounds use the correct rendered positions. Crystal distance-dependent appearance uses the local viewer position.
- Energy Core colour uniforms are flushed per core to prevent another buffered core from overwriting them. Saved tier, custom colours and legacy-render choices are preserved.
- Additional coordinate corrections cover Energy Core particles, detector head animation, fusion-crafting billboards, Dislocator Pedestal labels, grinder blade targets and Chaos Crystal viewing angles.

## Findings and limits

**Tier-8 colour/surface difference:** the user reported this with shader packs disabled. Identical configured tier-8 cores now show matching colour/surface behaviour on terrain and a tilted sublevel in the isolated tests, including actual Sodium + Iris with shaders disabled. The original reported difference has not been conclusively reproduced. An inspected save had custom purple/cyan colours enabled, whereas a default tier-8 core is orange; that is a possible comparison difference, not a confirmed diagnosis. No colour settings were reset.

**Large reactor meltdowns (0.1.1):** the blast uses the reactor's world position and considers terrain plus loaded intersecting sublevels, including separate nearby structures. DE's pre-detonation preparation remains in place. A stationary reactor reuses its completed plan; moving reactors refresh plans during the countdown. Movement just before detonation can require additional preparation, which continues in bounded slices rather than running one large synchronous scan. The impact position and body poses are locked when detonation is requested. Body blocks are sampled into world cells by their centres; this is a voxel approximation for rotated/scaled geometry. It does not search unloaded sublevel files on disk.

**Performance (0.1.6):** preparation uses cooperative 2 ms slices and a 6 ms per-level/tick ceiling. Sublevel removal uses a 12 ms shared per-level/tick allowance and an 8,192-entry ceiling. Ring scans visit accepted annulus columns in order and can resume within a ring. These are soft limits: an individual Minecraft chunk load or block callback cannot be interrupted. DE's existing terrain removal, lighting, packets, lava placement and delayed entity damage still have their own costs, so a large meltdown is not guaranteed to be lag-free. Pending explosion processes are not persisted by DE; orphan temporary body tickets are cleaned after reload.

Routine compatibility work avoids rebuilding unchanged loading-ticket sets, duplicate readiness chunk lookups and DE connectivity allocations for ordinary blocks. Owner cleanup runs once per second. Shell readiness scans occur on structure validation, not every idle tick.

**Rendering performance (0.1.3):** reactor effects, Energy Cores and energy crystals use a dedicated immediate buffer. DE's per-beam/core/shield/crystal flushes no longer drain Iris's shared scene buffer. This also corrects 0.1.1's typed Energy Core flush, which Iris ignored. Geometry inspection consumers are preserved. See [PERFORMANCE.md](PERFORMANCE.md) for the rendering changes and explosion budgets. No measured full-pack FPS gain is claimed from the unmatched captures.

**Other visual states:** all 51 registered blocks were inventoried and their placed models/renderers invoked on terrain and a sublevel; directional placement states were compared across six views and two body orientations. Inventory displays and selected animated targets were exercised. This is not exhaustive gameplay testing of all 51 blocks. In particular, the Celestial Manipulator's current modern renderer callback is empty in DE itself, even on terrain; its legacy effect renderer has not been ported here. Portal transport, spawner gameplay, all fusion recipe stages and guardian/Chaos Crystal events are not certified by the catalogue.

**Multiplayer:** the dedicated-server test suite starts successfully; functional changes use the server's existing authoritative state and DE synchronization. An integrated server/client confirms assembly state and real energy transfer. A live two-player remote-server session has not been tested.

**Assembly:** assemble the core and its components in one connected operation, or explicitly select the complete intended system. Already-separated structures are not automatically merged. This mod does not broadly strip or rewrite third-party schematic NBT.

## Build and tests

Java 21 and Gradle 9.2.1 are used. Local dependencies are in `libs` and the sibling `FlightRecorder_neoforge_1.21.1/libs` directory, as listed in `build.gradle`; none are bundled into the mod jar.

```powershell
.\gradlew.bat runGameTestServer build --offline
.\gradlew.bat runClientSmoke --offline
.\gradlew.bat runClientSmoke -PsodiumSmoke --offline
```

The last command additionally uses the installed Sodium 0.8.13, Iris 1.8.14-beta.1 and ImmediatelyFast 1.6.15 versions from `test-libs`. Sodium's outer service jar and nested implementation are combined **only for the development harness**, because ModDev's direct runtime dependency handling otherwise selects the outer metadata without loading the implementation. `tools/Prepare-SodiumSmoke.ps1` reproduces this test-only packaging. Do not install that test jar in a modpack.

See [VALIDATION.md](VALIDATION.md) for test coverage and limits. Local evidence, screenshots and test worlds are not distributed; reproduction tests create disposable `run*` folders.
