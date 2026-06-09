# Player Skin Extension for ETF

**pse-etf** is a Fabric client-side mod for Minecraft 26.1 that lets resource pack makers display real player skins dynamically on CEM model parts — such as mannequin armor stands — by embedding a player name token in the entity's custom name.

![Example banner](https://cdn.modrinth.com/data/QK0j571t/images/e0f4feeb8253025c7848b29255e138c759059e94.png)

---

## How it works

Rename any entity to include a player name wrapped in dollar signs:

```
$Username$
```

For example: `Nightpack$Notch$` or just `$Steve$`.

The mod reads the token, fetches that player's skin from Mojang, and replaces the designated placeholder texture on the CEM model with the real skin — all at render time, with no commands or data packs required.

The name tag displayed in-game automatically strips the token, showing only the remainder (e.g. `Nightpack` for `Nightpack$Notch$`).

---

## Resource pack setup

The mod only replaces textures that are **explicitly opted in** via a properties file. This prevents it from accidentally affecting other entities.

### 1. CEM model

In your `.jem` file, set the `"texture"` field on the submodel that should show the player skin:

```json
{
  "id": "Player",
  "texture": "optifine/random/entity/armorstand/player.png",
  "textureSize": [64, 64],
  "submodels": [ ... ]
}
```

The texture file (`player.png`) does not need to exist — it is just a placeholder identifier.

### 2. Properties file

Create a `.properties` file in the same folder as the texture placeholder, with the same base name:

**Path:** `assets/minecraft/optifine/random/entity/armorstand/player.properties`

```properties
skins.1=dynamic
player.1=true
```

| Key | Value | Meaning |
|-----|-------|---------|
| `skins.1` | `dynamic` | This skin slot uses dynamic player skin lookup |
| `player.1` | `true` | Enables player-name-based resolution for this slot |

Both keys are required. Without them the mod leaves the texture untouched.

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
                    └── player.properties← opts in to dynamic skin replacement
```

`etf/random/entity/` paths are also supported.

---

## Entity name token format

| Pattern | Result |
|---------|--------|
| `$Steve$` | Displays Steve's skin; name tag hidden |
| `Nightpack$Notch$` | Displays Notch's skin; name tag shows `Nightpack` |
| `My Stand $Alex$` | Displays Alex's skin; name tag shows `My Stand` |

- Username must be **3–16 characters**, letters, digits, or underscores (`A-Za-z0-9_`)
- The token must be surrounded by `$` on both sides
- Case-sensitive for skin lookup (matches Mojang's username exactly)

---

## Combining with ETF random entity textures

You can pair this with ETF's CEM model selection so the custom model only activates when the name contains a token:

```properties
# Enable dynamic player skin replacement
skins.1=dynamic
player.1=true
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
A loading placeholder is shown until the skin is fetched from Mojang. Skins are cached for the session.

**What if the username doesn't exist?**  
The default Steve/Alex skin is shown as a fallback.

**Does it replace the entity's normal texture?**  
No. Only textures registered via `skins.X=dynamic` + `player.X=true` in a properties file are affected. All other entity textures are left completely untouched.

**Can I use this for entities other than armor stands?**  
Yes — any entity type that has a CEM model and a properly configured properties file will work.

**Is this compatible with Iris/Sodium?**  
Yes, the mod does not touch shaders or chunk rendering.

---

## License

[MIT](LICENSE) — Do whatever you want with it, just keep the license and copyright notice.
