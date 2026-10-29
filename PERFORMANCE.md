# Performance design and limits

## Rendering

Reactor effects, Energy Cores and energy crystals use a dedicated immediate
buffer. Their flushes therefore do not drain the shared Iris scene buffer.
The client regression instruments scene-buffer writes and flushes while invoking
the actual renderers, alongside geometry and energy-transfer checks.

This addresses a measured call-path problem, but no controlled full-modpack FPS
improvement is claimed. Shader packs, other renderers and moving-body physics
can still dominate frame time. Tests using Sodium and Iris had shaders disabled.

## Reactor explosions (0.1.6)

- Preparation uses resumable annulus scans, iterative vertical rays, cached
  terrain and empty-space skipping. It overlaps DE's safety countdown.
- Preparation uses a cooperative 2 ms slice and 6 ms per-level/tick ceiling.
- Sublevel removal shares a 12 ms per-level/tick allowance across blast work,
  with an 8,192-entry ceiling. These are soft limits: chunk operations, block
  callbacks and final mass flushes cannot be preempted.
- Selected bodies receive temporary physics holds during removal. Per-block
  mass bookkeeping and normal block callbacks remain; merged/native mass
  updates are batched at removal-slice boundaries.
- DE's terrain removal remains a separate process with its own costs.
- Logs report elapsed ticks, time-limit and entry-limit stops, largest batch,
  total synchronous work and longest slice. A higher entry ceiling will not
  help if the time allowance is already limiting each slice.

The isolated 2,047-entry fixture completed in 14 ticks with a longest slice of
13.3575 ms. This small test is not a full-pack throughput guarantee. Unexplored
terrain may require expensive chunk generation. Pending explosions are not
persisted across restarts; temporary tickets and holds have cleanup paths.

## Routine checks

Loading tickets are not rebuilt when their required sets remain unchanged.
Owner cleanup runs once per second. Shell checks occur on multiblock validation,
not every idle tick. See [VALIDATION.md](VALIDATION.md) for test scope and limits.
