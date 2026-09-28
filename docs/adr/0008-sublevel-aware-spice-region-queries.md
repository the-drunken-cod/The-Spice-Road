# Sublevel-aware Spice Region queries via a jarJar'd Sable Companion shim

Spice Region resolution (growth gating, F3 debug info, and the `/locate`/Spice Map search origin) reads a raw world position, which is wrong for a Spice Plant stored inside a Sable sub-level (e.g. an airship): the block's stored coordinates are an arbitrary point in a parked "plot" grid, not its real location. We resolve this by projecting every position through `SableCompanion.INSTANCE.projectOutOfSubLevel` before any region math, at the same handful of call sites `canBeCultivatedAt` already centralizes.

Unlike ADR 0005's rejection of a Create-specific dependency, Sable Companion isn't added as a `compileOnly` + `isModLoaded`-gated mod dependency: it's a jarJar'd/included shim library, published specifically to be bundled directly into a consuming mod. Its own default implementation is a pure identity passthrough when Sable itself isn't installed, so calling it unconditionally carries no behavior change and no extra runtime check for players without Sable - the "avoid mod dependencies" concern doesn't apply the same way here, since we ship the safe-by-default code ourselves rather than depending on Sable's presence at runtime.

## Considered Options

- No sublevel support (status quo): rejected, this is the reported bug.
- `compileOnly` + `IPlatformHelper.isModLoaded` gate, matching the Farmer's Delight/Cooking for Blockheads compat pattern: rejected. Sable Companion isn't a full mod API to probe for at runtime - it's designed to be embedded directly, and doing so is simpler and matches its own documented usage.
- Hard runtime dependency on Sable: rejected, would force every player to install a mod unrelated to spices just to plant crops normally.

## Consequences

- `common/build.gradle` takes a plain dependency on `sable-companion-common`; `neoforge/build.gradle` and `fabric/build.gradle` each add their own `jarJar`/`include` declaration so the real shim jar ships inside our mod on both loaders.
- The three call sites (`Spice#canBeCultivatedAt`, the F3 debug source, `RegionHeartSearch`'s search origin) are the only places that need to change; `SpiceRegionResolver` stays pure Java with no Minecraft or Sable types.
