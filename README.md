# Player Skin Extension for ETF

**pse-etf** is a Fabric client-side mod for Minecraft 26.1 that lets resource pack makers display real player assets dynamically on CEM model parts — including skins, capes, and elytra textures — by embedding a player name token in the entity's custom name.

![Example banner](https://cdn.modrinth.com/data/QK0j571t/images/e0f4feeb8253025c7848b29255e138c759059e94.png)

---

## How it works

Rename any entity to include a player name wrapped in dollar signs:

```
$Username$
```

For example: `Nightpack$Notch$` or just `$Steve$`.

The mod reads the token, fetches that player's profile from Mojang, and replaces the designated placeholder texture on the CEM model with the selected player asset at render time — all with no commands or data packs required.

ETF/OptiFine-style random texture rules still decide which texture variant is selected. This mod only replaces the contents of the selected texture when that selected variant explicitly opts into dynamic player asset resolution.

The name tag displayed in-game automatically strips the token, showing only the remainder (e.g. `Nightpack` for `Nightpack$Notch$`).

---

## Resource pack setup

The mod only replaces textures that are **explicitly opted in** via a properties file. This prevents it from accidentally affecting other entities.

### 1. CEM model

In your `.jem` file, set the `"texture"` field on the submodel that should show the player asset:

```json
{
  "id": "Player",
  "texture": "optifine/random/entity/armorstand/player.png",
  "textureSize": [64, 64],
  "submodels": [ ... ]
}
```

The texture file does not need to be named `player.png`. Any basename works. The file can be a real fallback texture, a transparent texture, or just a placeholder identifier used by your pack.

### 2. Properties file

Create a `.properties` file in the same folder as the texture placeholder, with the same base name:

**Path:** `assets/minecraft/optifine/random/entity/armorstand/player.properties`

```properties
skins.1=dynamic
player_asset.1=skin
```

| Key | Value | Required? | Meaning |
|-----|-------|-----------|---------|
| `skins.1` or `textures.1` | `dynamic` | Yes | Opts this slot into dynamic player asset lookup |
| `player_asset.1` | `skin` / `cape` / `elytra` | Yes* | Chooses which player asset to inject into this slot |
| `name_source.1` | `token` / `nbt:<field>` / `static:<username>` / `self` | No | Chooses where the username comes from; defaults to `token` |
| `name_slot.1` | Positive integer | No | Chooses the 1-based token when `name_source` is `token`; defaults to `1` |

Dynamic replacement requires `skins.X=dynamic` (or `textures.X=dynamic`) plus an asset selector. The preferred selector is `player_asset.X`; legacy `player.X=true` can be used instead and selects `skin`. `name_source.X` and `name_slot.X` are optional, defaulting to `token` and `1` respectively.

Legacy compatibility:

```properties
skins.1=dynamic
player.1=true
```

`player.1=true` is still supported and behaves exactly like `player_asset.1=skin`.

### Player name sources

By default, a dynamic slot uses the first `$Username$` token in the entity's custom name. The source can be changed independently for each rule:

```properties
# Default behavior
name_source.1=token

# Read a username from a top-level string field in the entity's client-visible NBT
name_source.1=nbt:Owner

# Always load one player's assets
name_source.1=static:Notch

# Load the local player's assets
name_source.1=self
```

The resolved value must be a valid Minecraft username (3-16 letters, digits, or underscores). If the configured source is unavailable or invalid, the selected fallback texture is left unchanged. NBT fields must be present in the entity data available to the client; server-only NBT cannot be read by a client-side resource pack mod.

ETF rule matching is unchanged. Keys such as `name.X`, `nbt.X`, and `biomes.X` still determine whether the rule matches. `name_source.X` is consulted only after ETF selects a dynamic rule.

### Multiple player tokens

Use `name_slot.X` to select a token by its 1-based position in the raw entity name:

```text
$Matanyah$ $Mogswamp$
```

```properties
skins.1=dynamic
player_asset.1=skin
name_slot.1=1

skins.2=dynamic
player_asset.2=skin
name_slot.2=2
```

Rule 1 resolves `Matanyah`; rule 2 resolves `Mogswamp`. When omitted, `name_slot.X` defaults to `1`. It is ignored by `nbt`, `static`, and `self` sources.

### 3. Optional ETF variant rules

You can combine dynamic player asset lookup with normal ETF random/name rules in the same texture properties file.

Dynamic skin with a clear override:

```properties
skins.1=dynamic
player_asset.1=skin

skins.2=2
name.2=ipattern:*-clear*
```

With matching files like:

```text
player.png
player.properties
player2.png
```

or:

```text
LimbsRegular.png
LimbsRegular.properties
LimbsRegular2.png
```

Behavior:

- `$Matanyah$` uses Matanyah's fetched player skin.
- `$Matanyah$ -clear` uses `player2.png` or `LimbsRegular2.png`.
- `NormalName -clear` uses `player2.png` or `LimbsRegular2.png`.
- `NormalName` uses the base selected texture with no dynamic injection.

Dynamic cape with a hidden flag:

```properties
skins.1=1
name.1=ipattern:*

skins.2=dynamic
player_asset.2=cape
name.2=ipattern:*-cape*
```

Behavior:

- `$Matanyah$ builder` uses the base fallback cape texture.
- `$Matanyah$ builder -cape` injects Matanyah's real cape texture if that player has one.
- If the player has no cape, the selected fallback texture is kept.

The same pattern works with `player_asset.X=elytra`.

### Directory structure example

```
assets/
└── minecraft/
    └── optifine/
        ├── cem/
        │   └── armor_stand.jem          ← CEM model referencing player.png
        └── random/
            └── entity/
                └── armorstand/
                    ├── player.png       ← placeholder (can be any 1×1 image or omitted)
                    └── player.properties← opts in to dynamic player asset replacement
```

`etf/random/entity/` paths are also supported.

---

## Entity name token format

| Pattern | Result |
|---------|--------|
| `$Steve$` | Displays Steve's skin; name tag hidden |
| `Nightpack$Notch$` | Displays Notch's skin; name tag shows `Nightpack` |
| `My Stand $Alex$` | Displays Alex's skin; name tag shows `My Stand` |
| `$Steve$ builder -slim -cape` | Displays Steve's selected player assets; name tag shows `Steve builder` |

- Username must be **3–16 characters**, letters, digits, or underscores (`A-Za-z0-9_`)
- The token must be surrounded by `$` on both sides
- Case-sensitive for skin lookup (matches Mojang's username exactly)

### Hidden flags

Any complete word that starts with `-` is treated as a hidden flag.

- Hidden flags are removed from the displayed name tag.
- Hidden flags remain available internally for future mod features.
- Multiple flags are supported.
- Flags are normalized case-insensitively for internal matching.

Examples:

```text
$Matanyah$ swims -slim
```

Displays as `Matanyah swims`, with detected flags `["slim"]`.

```text
$Matanyah$ -swims -slim
```

Displays as `Matanyah`, with detected flags `["swims", "slim"]`.

---

## Combining with ETF random entity textures

You can pair this with ETF's CEM model selection so the custom model only activates when the name contains a token:

```properties
# Enable dynamic player asset replacement
skins.1=dynamic
player_asset.1=skin
```

> **Note:** !!! For pack creators, add a skin file as a fallback/placeholder. The mod needs that for it to work. !!!

In your `armor_stand.properties` (CEM model selector), use `name.X=ipattern:*$*` to trigger the matching CEM model whenever the entity name contains a `$` token:

```properties
models.1=1
name.1=ipattern:*$*
```

---

## Requirements

| Dependency | Version | Required? |
|------------|---------|-----------|
| Minecraft | 26.1 | ✅ |
| Fabric Loader | ≥ 0.19.3 | ✅ |
| Fabric API | Any | ✅ |
| Entity Model Features (EMF) | Any | ✅ for CEM skins |
| Entity Texture Features (ETF) | Any | ❌ optional |

> **Note:** EMF is required for the CEM model texture replacement to work. The mod loads without it, but skin replacement on CEM submodels will not function.

---

## FAQ

**Does this work on servers?**  
The mod is client-side only. The entity just needs a custom name with the `$Username$` token — no server-side mod required.

**What if the skin is still loading?**  
A loading placeholder is shown until the skin is fetched from Mojang. Player assets are cached for the session.

**What if the username doesn't exist?**  
The default Steve/Alex skin is shown as a fallback for skin slots.

**What happens if a player has no cape or elytra texture?**  
The selected ETF/CEM fallback texture stays in place for `cape` and `elytra` slots.

**Does it replace the entity's normal texture?**  
No. ETF/EMF still decide which texture variant is selected. This mod only swaps in a player asset when the selected variant is registered with `skins.X=dynamic` plus `player_asset.X=...` or legacy `player.X=true`.

**Is dynamic player asset support hardcoded to `player.png`?**  
No. Any texture basename can opt in, as long as the `.properties` file sits next to it and marks the desired variant as dynamic.

**Does `-cape` automatically render a vanilla cape layer?**  
No. Hidden flags stay available for your pack and for future mod features, but this mod currently only swaps the texture content of the selected CEM/ETF texture slot.

**Can I use this for entities other than armor stands?**  
Yes — any entity type that has a CEM model and a properly configured properties file will work.

**Is this compatible with Iris/Sodium?**  
Yes, the mod does not touch shaders or chunk rendering.

---

## License

[MIT](LICENSE) — Do whatever you want with it, just keep the license and copyright notice.
