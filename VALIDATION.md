# Validation

## Recorded release checks

### 0.1.7

The standalone `fairBudgetTest` suite uses deterministic elapsed times to check
fixed caller order, repeated requests, uninterruptible budget overruns, mixed
preparation/removal, cancellation, abandoned jobs and independent queues.

The main server suite now has 25 checks. Its new concurrent fixture starts eight
maximum-radius (350) blasts on separate sublevels, requires early progress from
every plan, then checks actual block removal and temporary-ticket/hold cleanup.
Existing explosion latency fixtures use separate named batches with their
original assertions and timeouts; they no longer compete with unrelated cold
terrain generation. Separate live-physics removal and pending-shutdown checks
bring the server total to 27.

The release passed all 27 server checks on both dependency endpoints:

| Component | Lower endpoint | Upper endpoint |
| --- | --- | --- |
| NeoForge | 21.1.228 | 21.1.265 |
| Sable | 2.0.4 | 2.0.6 |
| Draconic Evolution | 3.1.3.620 | 3.1.4.633 |
| Brandon's Core | 3.2.1.307 | 3.2.1.309 |
| CodeChickenLib | 4.6.1.524 | 4.6.1.529 |

Both endpoint client runs passed assembly synchronization, reactor/crystal
geometry and bounds, actual crystal-to-pylon-to-core energy transfer, all 51
block-catalogue terrain/sublevel render comparisons, and buffer isolation.
These runs used vanilla rendering with no shader packs, Sodium or Iris.
The current-pack combination (NeoForge 21.1.259, Sable 2.0.5, DE 3.1.4.633,
Brandon's Core 3.2.1.309 and CodeChickenLib 4.6.1.529) also passed all 27 server
checks with the new release.
An additional audit run restored the original 24-test overlapping arrangement
on the upper endpoint, followed by the dedicated concurrent fixture. All 25
checks passed without relaxing the original assertions or timeouts. The previous
release had failed six timing checks in that overlapping arrangement on Sable
2.0.6. This supports the scheduling fix; it is not a full-pack latency guarantee.

The wider dependency audit examined the released DE, Brandon's Core,
CodeChickenLib and Sable versions in README.md. Sable 2.0.3 and older lack APIs
used by this implementation. DE 3.1.3.618 was excluded because its own particle
registration crashes on dedicated servers; 3.1.3.620 fixes that startup problem.
Intermediate dependency combinations were API-checked, not all play-tested.

### Earlier release evidence

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
