package net.matanyah.pse;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public final class PlayerSkinRenderContext {
	private static final ThreadLocal<Context> CURRENT_CONTEXT = new ThreadLocal<>();

	private PlayerSkinRenderContext() {}

	public static void set(Entity entity) {
		CURRENT_CONTEXT.set(new Context(entity));
	}

	public static void clear() {
		CURRENT_CONTEXT.remove();
	}

	public static Entity currentEntity() {
		Context context = CURRENT_CONTEXT.get();
		return context == null ? null : context.entity();
	}

	public static Identifier replaceCemPlayerTexture(Identifier texture) {
		Context context = CURRENT_CONTEXT.get();
		ETFTextureVariantResolver.Resolution resolution = resolveSelectedTexture(texture);
		Identifier selectedTexture = resolution.selectedTexture();
		// ETF remains the source of truth for variant selection; we only swap in the
		// fetched player asset when the selected variant explicitly opted into it.
		PlayerIdentity identity = context == null ? null : PlayerNameResolver.resolve(
				context.entity(),
				resolution.flags().nameSource(),
				resolution.flags().nameSlot()
		).orElse(null);
		boolean inject = resolution.flags().dynamic()
				&& resolution.flags().assetType() != null
				&& identity != null;
		if (context != null && context.entity() != null) {
			ETFTextureVariantResolver.logResolution(context.entity(), texture, resolution, inject);
		}
		if (inject) {
			return PlayerAssetTextureCache.getTexture(identity, resolution.flags().assetType(), selectedTexture);
		}
		return selectedTexture;
	}

	private static ETFTextureVariantResolver.Resolution resolveSelectedTexture(Identifier texture) {
		Context context = CURRENT_CONTEXT.get();
		if (context == null || context.entity() == null) {
			return new ETFTextureVariantResolver.Resolution(texture, texture, 1, false, DynamicSkinTextureRegistry.VariantFlags.NONE);
		}
		return ETFTextureVariantResolver.resolveDetailed(texture, context.entity());
	}

	private record Context(Entity entity) {}
}
