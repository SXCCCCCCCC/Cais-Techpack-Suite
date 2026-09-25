# MA Addon (maaddon)

A Mystical Agriculture 1.20.1 (Forge, MA 7.0.x) addon that registers three
RESOURCE-type crops via MA's plugin API:

| Crop    | Tier   | Crafting material | Essence exchange (MA format, ratios from Industrial Agriculture) |
|---|---|---|---|
| salt       | ONE  | mekanism:salt (item) | 2x2 essence (4) -> mekanism:salt x16 |
| plastic    | TWO  | forge:plastic (tag)  | 2x2 essence (4) -> industrialforegoing:plastic x8 |
| pink_slime | FOUR | industrialforegoing:pink_slime (item) | 3x3 essence (9) -> industrialforegoing:pink_slime x4 |

Seeds, essences and crop blocks are created automatically by MA's crop
registry (registered under the `mysticalagriculture` namespace); resource
layout (models/blockstates/tags/seed recipes) mirrors
MysticalAgradditions-1.20 as the template. Recipe JSONs are hand-written
(dynamic seed recipe generators are disabled in the plugin config, same as
MA/Agradditions do).

Code is MIT licensed; textures are from Industrial Agriculture (LGPL v2.1),
see THIRD_PARTY_NOTICES.md.

## Build

```
JAVA_HOME="C:\Program Files\Java\jdk-17" ./gradlew build
```

Output: build/libs/maaddon-0.1.0.jar (archivesName = mod_id, version 0.1.0).

Compile dependency: libs/MysticalAgriculture-1.20.1-7.0.24.jar (from the
1.20.1-Tec pack), declared as `compileOnly fg.deobf(files(...))` like the
fix project standard.
