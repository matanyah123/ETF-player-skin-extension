package net.matanyah.pse;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class DynamicSkinTextureRegistry {
	private static final List<String> SCAN_PATHS = List.of(
			"optifine/random/entity",
			"etf/random/entity"
	);
	private static final Pattern TRAILING_NUMBERED_TEXTURE = Pattern.compile(".*\\D\\d+\\.[^.]+$");

	private static volatile Set<Identifier> dynamicTextures = Set.of();
	private static volatile Map<Identifier, Map<Integer, VariantFlags>> ruleFlagsByBaseTexture = Map.of();

	private DynamicSkinTextureRegistry() {}

	public static boolean isDynamic(Identifier texture) {
		return dynamicTextures.contains(texture);
	}

	public static VariantFlags getVariantFlags(Identifier baseTexture, int variantIndex) {
		Map<Integer, VariantFlags> flags = ruleFlagsByBaseTexture.get(baseTexture);
		if (flags == null) {
			return VariantFlags.NONE;
		}
		return flags.getOrDefault(variantIndex, VariantFlags.NONE);
	}

	public static Identifier getTextureForVariant(Identifier baseTexture, int variantIndex) {
		String texturePath = baseTexture.getPath();
		if (variantIndex < 2) {
			return baseTexture;
		}

		int extensionIndex = texturePath.lastIndexOf('.');
		if (extensionIndex < 0) {
			return Identifier.fromNamespaceAndPath(baseTexture.getNamespace(), texturePath + variantIndex);
		}

		String numberedTexturePath = TRAILING_NUMBERED_TEXTURE.matcher(texturePath).matches()
				? texturePath.substring(0, extensionIndex) + "." + variantIndex + texturePath.substring(extensionIndex)
				: texturePath.substring(0, extensionIndex) + variantIndex + texturePath.substring(extensionIndex);
		return Identifier.fromNamespaceAndPath(baseTexture.getNamespace(), numberedTexturePath);
	}

	public static void reload(ResourceManager resourceManager) {
		Set<Identifier> found = new HashSet<>();
		Map<Identifier, Map<Integer, VariantFlags>> foundFlags = new ConcurrentHashMap<>();

		for (String basePath : SCAN_PATHS) {
			resourceManager.listResources(basePath, id -> id.getPath().endsWith(".properties"))
					.forEach((id, resource) -> {
						try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
							Properties props = new Properties();
							props.load(reader);

							for (int i = 1; i <= 64; i++) {
								boolean isDynamic = "dynamic".equalsIgnoreCase(getSkinValue(props, i));
								PlayerAssetType assetType = getPlayerAssetType(props, i);
								boolean isPlayer = assetType != null;
								Identifier baseTexture = getBaseTexture(id);
								if (isDynamic || isPlayer) {
									foundFlags.computeIfAbsent(baseTexture, ignored -> new HashMap<>())
											.merge(i, new VariantFlags(isDynamic, isPlayer, assetType), VariantFlags::merge);
								}
								if (isDynamic && isPlayer) {
									Identifier dynamicTexture = i < 2 ? baseTexture : getTextureForVariant(baseTexture, i);
									found.add(dynamicTexture);
									PlayerSkinExtensionETF.LOGGER.info("Registered dynamic player asset texture: {} (rule {}, asset={})", dynamicTexture, i, assetType.name().toLowerCase());
								}
							}
						} catch (IOException ignored) {}
					});
		}

		dynamicTextures = Set.copyOf(found);
		ruleFlagsByBaseTexture = copyNestedMap(foundFlags);
		PlayerSkinExtensionETF.LOGGER.info("Dynamic player asset textures loaded: {}", dynamicTextures.size());
	}

	private static String getSkinValue(Properties props, int index) {
		String skins = props.getProperty("skins." + index);
		if (skins != null) {
			return skins.trim();
		}

		String textures = props.getProperty("textures." + index);
		return textures == null ? null : textures.trim();
	}

	private static PlayerAssetType getPlayerAssetType(Properties props, int index) {
		PlayerAssetType typedAsset = PlayerAssetType.fromPropertyValue(props.getProperty("player_asset." + index));
		if (typedAsset != null) {
			return typedAsset;
		}

		return Boolean.parseBoolean(props.getProperty("player." + index)) ? PlayerAssetType.SKIN : null;
	}

	private static Identifier getBaseTexture(Identifier propertiesId) {
		String propertiesPath = propertiesId.getPath();
		String texturePath = propertiesPath.substring(0, propertiesPath.length() - ".properties".length()) + ".png";
		return Identifier.fromNamespaceAndPath(propertiesId.getNamespace(), texturePath);
	}

	private static Map<Identifier, Map<Integer, VariantFlags>> copyNestedMap(Map<Identifier, Map<Integer, VariantFlags>> source) {
		Map<Identifier, Map<Integer, VariantFlags>> copy = new HashMap<>();
		source.forEach((texture, flags) -> copy.put(texture, Map.copyOf(flags)));
		return Map.copyOf(copy);
	}

	public record VariantFlags(boolean dynamic, boolean player, PlayerAssetType assetType) {
		public static final VariantFlags NONE = new VariantFlags(false, false, null);

		private VariantFlags merge(VariantFlags other) {
			PlayerAssetType mergedAssetType = assetType != null ? assetType : other.assetType;
			return new VariantFlags(dynamic || other.dynamic, player || other.player, mergedAssetType);
		}
	}
}
