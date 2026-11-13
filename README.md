# Draconic Sable Compatibility

An addon that makes Draconic Evolution reactors, Energy Cores and related blocks
work on Sable moving structures (sublevels).

## What it changes

- **Placement and rotation:** corrects reactor injector/stabilizer and Dislocator
  Pedestal orientation on sublevels. Assembly rotation also updates component
  directions, relative positions and crystal links.
- **Multiblock connectivity:** keeps reactors and Energy Cores connected to their
  own valid components across intentional air gaps, preventing unwanted splitting
  when support blocks are removed.
- **Energy transfer:** fixes pylon access to Energy Cores on sparse sublevels and
  preserves links through temporary loading gaps. Reactor operation waits for
  saved components that have not finished loading.
- **Chunk loading:** keeps active Energy Cores and shielded reactors loaded,
  including their sublevel or required terrain chunks, and releases those loading
  tickets when no longer needed.
- **Rendering:** fixes missing reactor cores, displaced crystal beams and effects,
  and Energy Core colour interference. Also corrects sublevel coordinates for
  particles, labels, detector animation and several other block effects.
- **Reactor explosions:** uses the reactor's world position to damage terrain and
  loaded nearby sublevels. Preparation runs during the safety countdown, with
  cached, incremental calculations and batched removal. Affected structures are
  temporarily held steady while their blocks are removed. Simultaneous blasts
  take turns within the shared work budget to prevent starvation.
- **Performance:** isolates reactor, Energy Core and crystal rendering buffers to
  avoid repeatedly flushing the shared scene, and reduces repeated loading and
  explosion work.

## Installation

Requires **Minecraft 1.21.1**, **Java 21**, **NeoForge 21.1.228 or newer**,
**Draconic Evolution 3.1.3.620–3.1.4.633**, **BrandonsCore 3.2.1.307–3.2.1.309**
and **Sable 2.0.4–2.0.6**. Keep DE's required CodeChickenLib dependency;
versions **4.6.1.524, 4.6.1.526 and 4.6.1.529** were checked.

The NeoForge dependency has no upper version limit. Minecraft remains 1.21.1;
see [validation](VALIDATION.md) for the loader versions actually tested.

Place the compatibility JAR in `mods` on **the server and every client**, replacing
any older copy, then restart. Keep the original mods and their required dependencies.

Before uninstalling, shut down and cool reactors, deactivate Energy Cores, and
save the world so the addon's loading tickets are released.

## Things to know

- Assemble a core and its components together. Already separate sublevels are
  not automatically joined.
- Explosions affect loaded intersecting sublevels; they do not search unloaded
  structures on disk. Large blasts can still be expensive, and unfinished
  explosion processes are not saved across restarts.
- Compatibility is targeted, not complete coverage of every DE feature. See
  [validation and known limits](VALIDATION.md) for what has actually been tested,
  including multiplayer and rendering limits.

## Building and licensing

See [build instructions](DISTRIBUTION.md), [tests](VALIDATION.md), and
[performance details](PERFORMANCE.md).
Addon authors can use the [loading radius API](LOADING_API.md) to extend ordinary-world coverage without mixins.

Original contributions are **MIT licensed**. DE-derived portions retain
**Draconic Evolution's custom license**. See [LICENSING.md](LICENSING.md) and
[NOTICE](NOTICE) for the full scope and attribution. This is an independent addon.
