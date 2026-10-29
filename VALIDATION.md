# Validation

## Recorded release checks

Gameplay release 0.1.6 passed 26 server checks: the 24-test main suite, a separate
live-physics removal test, and an interrupted-shutdown test. All three isolated
servers saved and exited. These were disposable worlds, not existing user saves.

Coverage includes placement and assembly transforms, reactor/Energy Core
connectivity, real energy transfer, loading readiness, blast selection and
resistance, movement/replanning, queued removal, holds and ticket cleanup.
The optimized vertical trace was compared with the version-matched upstream
method across 64 cases, including selected blocks, update sets, lava and RNG
progression; resistance agrees within floating-point tolerance (1e-10).

The removal test checks overlapping hold ownership, preservation of an existing
staff-style constraint, per-block mass bookkeeping, batched native updates,
bulk damage, COM changes, restoration of falling, cancellation and body removal.
Its 2,047-entry fixture finished in 14 ticks; maximum held-point drift was
0.00126986 blocks. This is not a controlled full-modpack benchmark.

Earlier client checks invoked all 51 registered block models/renderers on terrain
and a sublevel and compared directional placement across six views and two body
orientations. Selected effects, crystal links and real crystal/pylon/core energy
transfer were checked. Sodium 0.8.13, Iris 1.8.14-beta.1 and ImmediatelyFast 1.6.15
were tested with shader packs disabled. Shared-buffer isolation regressions
passed for reactor effects, Energy Cores and all nine energy-crystal variants.

The licensing revision built successfully. All 59 runtime classes have identical
`javap -c -p` output to gameplay release 0.1.6. Attribution comments change debug
line numbers, not executable instructions. Packaging includes the license texts,
notices and mixed-license metadata. Licensing/publication checks do not imply a
new gameplay run.

## Reproduction

Supply the dependencies listed in [DISTRIBUTION.md](DISTRIBUTION.md), then run:

```powershell
.\gradlew.bat runGameTestServer runBlastRemovalSmoke runPendingShutdownSmoke build
```

Tests create disposable `run*` directories. Start with fresh test worlds when
comparing fixtures. Client checks are `runClientSmoke` and
`runClientSmoke -PsodiumSmoke`. The Sodium check requires Sodium 0.8.13, Iris
1.8.14-beta.1 and ImmediatelyFast 1.6.15 in `test-libs`. Run
`tools/Prepare-SodiumSmoke.ps1` to prepare Sodium for the development harness;
its combined JAR is test-only and must not be installed in a modpack.

## Limits

- No live two-player remote session is certified by these checks.
- Invoking all block renderers is not exhaustive gameplay validation of all
  blocks, portals, spawners, fusion stages or guardian events.
- The Celestial Manipulator's legacy renderer has not been ported here.
- Explosion damage on rotated bodies uses a voxel approximation and considers
  loaded intersecting sublevels; it does not search unloaded bodies on disk.
- No matched full-pack FPS gain or shader-pack-enabled compatibility is claimed.
- Local logs, screenshots, profiling captures and test saves are not distributed.
