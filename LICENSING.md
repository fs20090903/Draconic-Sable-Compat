# Licensing and attribution

Draconic Sable Compatibility is an independent addon. It requires the original
mods; it does not replace them and is not an official Draconic Evolution or
Sable release. The distribution has mixed licensing, not a choice between
alternative licenses for the same upstream code.

## Original contributions

Original contributions by this project's contributors are offered under the
MIT License in `LICENSES/MIT.txt`. This grant covers only rights held by those
contributors. It does not relicense any upstream code, grant rights in third-party
assets, or waive upstream conditions applicable to this derived project.

## Draconic Evolution-derived material

Upstream: Draconic Evolution, by brandon3055 and its contributors.
The upstream license notice reads: Copyright (c) 2013-2023 brandon3055.

- Repository: https://github.com/Draconic-Inc/Draconic-Evolution
- Code reference used for compatibility: Minecraft 1.21.1, DE 3.1.4.633.
- License: `LICENSES/LicenseRef-Draconic-Evolution.txt`, the upstream custom
  "Don't Be a Jerk" license, preserved verbatim.
- License source: https://github.com/Draconic-Inc/Draconic-Evolution/blob/03e49b40c27b29c75317d5a0b7defbf33826b87c/LICENSE.md
- Retrieved from the upstream `1.21` branch on 2026-10-30. The linked commit is
  the license file's last-change commit, not a claim identifying the exact
  source commit of the 3.1.4.633 binary.

The following paths are relative to `src/main/java/dev/draconicsable/` unless
otherwise stated. Their original additions are MIT; their DE-derived portions
remain subject to DE's custom terms. Headers use
`MIT AND LicenseRef-Draconic-Evolution` to indicate both apply within these
mixed files, not that either license alone licenses the whole file.

| File | Upstream basis and modifications |
| --- | --- |
| `explosion/BlastRings.java` | `ProcessExplosion.updateCalculation`: annulus order, blast power, vertical reach, noise and resistance formulas; adapted to resumable bounded slices and an injected trace implementation. |
| `explosion/BlastTrace.java` | `ProcessExplosion.trace`: destruction thresholds, resistance, fluid and lava handling; adapted to iterative traversal, cached terrain, empty-space skipping and sublevel resistance. |
| `explosion/WorldBlast.java` | `ProcessExplosion.trace` destruction/resistance rules in `trace`; adapted to world-space sublevel voxels. Snapshotting, holds, tickets and queued removal are compatibility additions. |
| `MultiblockLinks.java` | Reactor component discovery rules from `TileReactorCore` / `TileReactorComponent`; adapted to loaded-chunk checks and same-sublevel connectivity. |
| `src/gametest/java/dev/draconicsable/tests/ExplosionTests.java` (project-relative) | Reference annulus scan from `ProcessExplosion.updateCalculation` used for regression comparison; the remaining test harness exercises the compatibility implementation. |

This is an attribution inventory, not a numerical certification of the custom
license's functionally-equivalent-statements restriction. The original license
text controls. In particular, the addon permission coexists with its visible
source repository requirement, restrictions on redistribution and equivalent
code, and prohibition on claiming ownership of upstream work. MIT labels on
original files do not remove those project-level obligations.

The custom license's statement about contribution to the upstream repository
describes that upstream repository. This project does not purport to transfer
its contributors' rights to the upstream author by reproducing the notice.

## Other dependencies and build tooling

Sable, Sable Companion, Create, Ponder, NeoForge, Brandon's Core, CodeChickenLib,
Minecraft and other development/runtime dependencies retain their respective
licenses and ownership. They are external dependencies, not sublicensed or
bundled by this project's license. No DE art or sound assets are included.

The Gradle wrapper files distributed with the source retain their existing
copyright notices and Apache License 2.0 terms; see `LICENSES/Apache-2.0.txt`.
They are not included in the runtime mod JAR.

In the runtime JAR, this document, `LICENSE` and `NOTICE` are under `META-INF/`,
and the license texts are under `META-INF/licenses/`. In the source archive,
they are at the project root and under `LICENSES/`.

See `DISTRIBUTION.md` for source publication and packaging instructions.
