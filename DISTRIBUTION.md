# Distributing this compatibility addon

## Publication

Use a Custom / mixed-license entry on the hosting page, with this description:

> Original contributions: MIT. Draconic Evolution-derived portions: Draconic
> Evolution's custom Don't Be a Jerk license. See LICENSING.md and the included
> license texts. This is an independent addon and requires the original mods.

Publish a visible source repository containing the matching release sources,
build files, notices and licenses, and link it on the binary download page.
Tag the source revision for each binary release. A locally generated source ZIP
does not itself fulfil the upstream visible-repository condition. The source repository for this addon is https://github.com/fs20090903/Draconic-Sable-Compat.

Preserve upstream notices and source-file attribution. Distribute the addon JAR,
not modified upstream JARs. The custom license's other restrictions still apply;
this documentation does not assert a verified percentage of functionally
equivalent statements or override any upstream condition.

## Build from the source archive

Use Java 21 and the included Windows Gradle wrapper (Gradle 9.2.1). Obtain the
following dependencies from their authors. Dependency JARs are intentionally not
included in the source archive. The current build uses this local directory layout:

| Directory relative to project | Required filenames |
| --- | --- |
| `libs/` | `Draconic-Evolution-1.21.1-3.1.4.633.jar`, `BrandonsCore-1.21.1-3.2.1.309.jar`, `CodeChickenLib-1.21.1-4.6.1.529.jar` |
| `../FlightRecorder_neoforge_1.21.1/libs/` | `sable-2.0.5.jar`, `sable-companion-1.6.0.jar`, `create-6.0.10.jar`, `ponder.jar` (Ponder 1.0.82 for Minecraft 1.21.1) |

The sibling folder name is a development layout convention; that recorder mod's
source or runtime installation is not required. Gradle resolves NeoForge
21.1.259 and its development tooling. First-time setup normally needs internet
access. Use `--offline` only after dependencies have been cached.

```powershell
.\gradlew.bat build sourceDistribution
```

Runtime JAR: `build/libs/`. Curated source ZIP: `build/distributions/`.
The source ZIP includes all project `src/`, build files, wrapper, README and
licensing documents. It excludes `audit/`, `evidence/`, `run*/`, dependency JARs,
logs, IDE state and build caches. Do not upload the entire working directory.

Existing server checks can be run in disposable worlds with:

```powershell
.\gradlew.bat runBlastRemovalSmoke runPendingShutdownSmoke runGameTestServer
```

Client/Sodium smoke tests require additional local test dependencies not included
in the source archive. `build` also runs the deterministic `fairBudgetTest` suite.
Build dependencies above remain pinned for reproducibility; the runtime accepts
the wider checked versions listed in README.md.
