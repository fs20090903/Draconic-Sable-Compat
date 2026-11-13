# Core loading radius API (0.1.8+)

Addons can request a larger minimum **ordinary-world** loading radius through
NeoForge InterModComms. This does not require a compile-time dependency or mixin.

During `InterModEnqueueEvent`, send to `draconic_sable_compat` using the method
`minimum_core_loading_radius_v1`. The supplier must return a
`Map<ResourceLocation, Integer>`: registered block-entity type IDs mapped to
chunk radii (inclusive, from 0 to 32).

```java
InterModComms.sendTo("your_mod", "draconic_sable_compat", "minimum_core_loading_radius_v1",
    () -> Map.of(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(MY_CORE_TYPE.get()), 2));
```

Messages are consumed once during `InterModProcessEvent` and copied into an
immutable lookup table. Do not send world objects or mutate the supplied map.
Unknown type IDs and malformed entries are ignored with a warning. Multiple
requests for one type use the largest radius, regardless of registration order.

The effective radius cannot reduce the existing defaults: 1 (3x3 chunks) for
reactors, 2 (5x5) for Energy Cores. Only active supported cores receive tickets;
registering another block type does not make it a chunk loader. This is a
startup-only contract, not a live configuration or scripting-reload hook.

Ticket persistence, ownership and release remain managed by Compat. Moving
Sable structures still use whole-sublevel tickets; this radius does not cause
ordinary-world chunks at their storage coordinates to load. Resolution is one
map lookup per active ordinary-world core update; unchanged bindings reuse
their existing tickets.

Consumers should declare Compat as optional with minimum version 0.1.8. Absence
is supported, but older installed versions do not understand this contract.

Chaos Expansion 0.2.84+ uses this API. Update both jars together when both mods
are installed: older Expansion builds inject a numeric constant into the old
loader, so Compat also rejects those older builds instead of silently shrinking
their coverage. Compat still works on its own without Expansion installed.
