package net.matanyah.pse.mixin;

import net.matanyah.pse.PlayerSkinRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderTypes.class)
public abstract class RenderTypesMixin {
	@SuppressWarnings("UnresolvedMixinReference")
	@ModifyVariable(method = {
			"entitySolid",
			"entitySolidZOffsetForward",
			"entityCutoutCull",
			"entityCutout(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			"entityCutout(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			"entityCutoutZOffset(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			"entityCutoutZOffset(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			"entityTranslucentCullItemTarget",
			"entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			"entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			"entityTranslucentEmissive(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			"entityTranslucentEmissive(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			"entityShadow",
			"eyes"
	}, at = @At("HEAD"), argsOnly = true)
	private static Identifier pse_etf$replaceCemPlayerTexture(Identifier texture) {
		return PlayerSkinRenderContext.replaceCemPlayerTexture(texture);
	}
}
