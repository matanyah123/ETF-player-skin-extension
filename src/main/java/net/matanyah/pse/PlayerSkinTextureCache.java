package net.matanyah.pse;

import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

@Deprecated(forRemoval = false)
public final class PlayerSkinTextureCache {
	private PlayerSkinTextureCache() {}

	public static Identifier getTexture(String username) {
		return PlayerAssetTextureCache.getTexture(username, PlayerAssetType.SKIN, DefaultPlayerSkin.getDefaultTexture());
	}
}
