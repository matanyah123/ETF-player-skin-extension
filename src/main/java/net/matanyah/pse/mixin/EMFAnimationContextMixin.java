package net.matanyah.pse.mixin;

import net.matanyah.pse.PlayerSkinRenderContext;
import net.matanyah.pse.PlayerSkinTextureCache;
import net.matanyah.pse.PlayerSkinToken;
import net.matanyah.pse.PlayerSkinTokenState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
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
		if (!PlayerSkinRenderContext.isDynamicSkinTexture(texture)) return;

		EntityRenderState entityState = getEntityRenderState();
		if (!(entityState instanceof PlayerSkinTokenState tokenState)) return;

		PlayerSkinToken token = tokenState.pse_etf$getPlayerSkinToken();
		if (token == null) return;

		cir.setReturnValue(RenderTypes.entityCutout(PlayerSkinTextureCache.getTexture(token.username())));
	}
}
