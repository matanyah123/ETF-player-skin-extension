package net.matanyah.pse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerSkinExtensionETF implements ClientModInitializer {
	public static final String MOD_ID = "pse-etf";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
				new SimpleSynchronousResourceReloadListener() {
					@Override
					public Identifier getFabricId() {
						return Identifier.fromNamespaceAndPath(MOD_ID, "dynamic_skin_textures");
					}

					@Override
					public void onResourceManagerReload(ResourceManager manager) {
						DynamicSkinTextureRegistry.reload(manager);
					}
				}
		);

		LOGGER.info("Player Skin Extension for ETF initialized");
	}
}
