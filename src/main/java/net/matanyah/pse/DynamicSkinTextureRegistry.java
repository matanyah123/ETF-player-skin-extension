package net.matanyah.pse;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class DynamicSkinTextureRegistry {
	private static final List<String> SCAN_PATHS = List.of(
			"optifine/random/entity",
			"etf/random/entity"
	);

	private static volatile Set<Identifier> dynamicTextures = Set.of();

	private DynamicSkinTextureRegistry() {}

	public static boolean isDynamic(Identifier texture) {
		return dynamicTextures.contains(texture);
	}

	public static void reload(ResourceManager resourceManager) {
		Set<Identifier> found = new HashSet<>();

		for (String basePath : SCAN_PATHS) {
			resourceManager.listResources(basePath, id -> id.getPath().endsWith(".properties"))
					.forEach((id, resource) -> {
						try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
							Properties props = new Properties();
							props.load(reader);

							for (int i = 1; i <= 64; i++) {
								boolean isDynamic = "dynamic".equals(props.getProperty("skins." + i));
								boolean isPlayer = "true".equals(props.getProperty("player." + i));
								if (isDynamic && isPlayer) {
									String propPath = id.getPath();
									String texPath = propPath.substring(0, propPath.length() - ".properties".length()) + ".png";
									found.add(Identifier.fromNamespaceAndPath(id.getNamespace(), texPath));
									PlayerSkinExtensionETF.LOGGER.info("Registered dynamic skin texture: {}:{}", id.getNamespace(), texPath);
								}
							}
						} catch (IOException ignored) {}
					});
		}

		dynamicTextures = Set.copyOf(found);
		PlayerSkinExtensionETF.LOGGER.info("Dynamic skin textures loaded: {}", dynamicTextures.size());
	}
}
