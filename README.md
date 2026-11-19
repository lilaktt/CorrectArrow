# CorrectArrow

Arrows stay exactly where they hit. Vanilla scatters stuck arrows over the player at random and shows none on mobs;
with CorrectArrow every arrow sits on the body part its flight path really entered and moves with that part's animation:
a head, a swinging leg, a modded creature's spinning wheel.

- Works on players, every vanilla mob and modded creatures whose renderer uses a `ModelPart` model (most Fabric mods).
- Creatures without such a model (for example GeckoLib ones) still show their arrows where they hit the hitbox,
  turning with the body.
- Keeps the arrow's look: normal, tipped, spectral and modded arrows.
- Arrows leave the body when vanilla pulls them out; players who come close later see them in the same spots.
- Needed on the server and the client. A client without the mod on a modded server, or a modded client on a vanilla
  server, simply keeps vanilla behaviour.

## Versions

Fabric, every Minecraft release from 1.20 to 26.3, one jar per group:

| Jar | Minecraft |
| --- | --- |
| `+mc1.20-1.20.1` | 1.20, 1.20.1 |
| `+mc1.20.2` | 1.20.2 |
| `+mc1.20.3-1.20.4` | 1.20.3, 1.20.4 |
| `+mc1.20.5-1.20.6` | 1.20.5, 1.20.6 |
| `+mc1.21-1.21.1` | 1.21, 1.21.1 |
| `+mc1.21.2-1.21.3` | 1.21.2, 1.21.3 |
| `+mc1.21.4` | 1.21.4 |
| `+mc1.21.5` | 1.21.5 |
| `+mc1.21.6-1.21.8` | 1.21.6, 1.21.7, 1.21.8 |
| `+mc1.21.9-1.21.10` | 1.21.9, 1.21.10 |
| `+mc1.21.11` | 1.21.11 |
| `+mc26.1-26.1.2` | 26.1, 26.1.1, 26.1.2 |
| `+mc26.2` | 26.2 |
| `+mc26.3` | 26.3 |

## Build and test

All targets build from the same sources in `src/`. Lines between `//#if MC >= 1.21.9` / `//#elseif` / `//#else` /
`//#endif` are kept or blanked per target, and a few names that only moved between versions are swapped
(`gradle/mod.gradle` has the version table, the preprocessor and the swaps). Each target is a module in `versions/`.

```
./gradlew :mc-1.21.9:build                         # one target; the jar is in versions/1.21.9/build/libs
./gradlew :mc-1.21.9:runClientGameTest             # game test in a development client and a dedicated server
./gradlew :mc-1.21.9:runClientGameTest -Pmc=1.21.10    # the same target against another version it covers
./gradlew :mc-1.21.9:prodClientGameTest            # the jar players install, in a production launch
./gradlew :mc-1.21.9:runClientGameTest -PtestOnly=humanoid,modded,nomodel,sync,self,bow,dedicated
```

The game test fires arrows along known paths and checks that each one is drawn on the model surface (within 0.03
blocks), on the right part, following it; screenshots land in `versions/<target>/build/run/.../screenshots`. Fabric
API has no client game tests before 1.21.4, so there the test mod brings a small harness of its own (no dedicated
server part).

## How it works

The server sends the arrow's flight ray (relative to the target's body) the moment vanilla counts the arrow as stuck.
The client follows that ray into the posed model, finds the first cube it enters, and stores the part chain and the
point in that part's frame, so each frame the arrow is placed by the live pose. Fresh hits are resolved in the pose from
just before vanilla's hurt flail, which starts a moment before the hit itself reaches the client.
