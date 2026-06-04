package net.matanyah.pse;

import net.minecraft.resources.Identifier;

public final class PlayerSkinRenderContext {
	private static final ThreadLocal<PlayerSkinToken> CURRENT_TOKEN = new ThreadLocal<>();

	private PlayerSkinRenderContext() {}

	public static void set(PlayerSkinToken token) {
		CURRENT_TOKEN.set(token);
	}

	public static void clear() {
		CURRENT_TOKEN.remove();
	}

	public static boolean isDynamicSkinTexture(Identifier texture) {
		return DynamicSkinTextureRegistry.isDynamic(texture);
	}

	public static Identifier replaceCemPlayerTexture(Identifier texture) {
		PlayerSkinToken token = CURRENT_TOKEN.get();
		if (token != null && DynamicSkinTextureRegistry.isDynamic(texture)) {
			return PlayerSkinTextureCache.getTexture(token.username());
		}
		return texture;
	}
}
