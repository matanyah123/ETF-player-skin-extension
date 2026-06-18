package net.matanyah.pse.mixin;

import net.matanyah.pse.PlayerSkinRenderContext;
import net.minecraft.client.model.Model;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Model.class, priority = 900)
public abstract class ModelMixin {
	@ModifyVariable(method = "renderType(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), argsOnly = true)
	private Identifier pse_etf$replaceCemPlayerTexture(Identifier texture) {
		return PlayerSkinRenderContext.replaceCemPlayerTexture(texture);
	}
}
