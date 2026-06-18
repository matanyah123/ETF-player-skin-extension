package net.matanyah.pse.mixin;

import net.matanyah.pse.ETFTextureVariantResolver;
import net.matanyah.pse.PlayerAssetTextureCache;
import net.matanyah.pse.PlayerSkinToken;
import net.matanyah.pse.PlayerSkinTokenState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "traben.entity_model_features.models.animation.EMFAnimationEntityContext", remap = false)
public abstract class EMFAnimationContextMixin {
	// Shadow EMF's static method that returns the entity render state at render time
	@Shadow(remap = false)
	public static EntityRenderState getEntityRenderState() { return null; }

	@Inject(
			method = "getLayerFromRecentFactoryOrETFOverrideOrTranslucent",
			at = @At("HEAD"),
			cancellable = true,
			remap = false
	)
	private static void pse_etf$interceptCemTexture(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
		EntityRenderState entityState = getEntityRenderState();
		if (!(entityState instanceof PlayerSkinTokenState tokenState)) return;

		PlayerSkinToken token = tokenState.pse_etf$getPlayerSkinToken();
		if (token == null) return;

		Entity entity = tokenState.pse_etf$getPlayerSkinEntity();
		if (entity == null) return;

		// EMF can ask for a render layer outside the thread-local window used by the
		// regular model hooks, so resolve the ETF-selected variant directly from the entity.
		ETFTextureVariantResolver.Resolution resolution = ETFTextureVariantResolver.resolveDetailed(texture, entity);
		Identifier selectedTexture = resolution.selectedTexture();
		boolean inject = resolution.flags().dynamic() && resolution.flags().assetType() != null;
		ETFTextureVariantResolver.logResolution(entity, texture, resolution, inject);
		if (!inject) return;

		cir.setReturnValue(RenderTypes.entityCutout(
				PlayerAssetTextureCache.getTexture(token.username(), resolution.flags().assetType(), selectedTexture)
		));
	}
}
