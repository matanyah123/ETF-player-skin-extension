package net.matanyah.pse.mixin;

import net.matanyah.pse.PlayerSkinRenderContext;
import net.matanyah.pse.PlayerSkinToken;
import net.matanyah.pse.PlayerSkinTokenState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void pse_etf$extractPlayerSkinToken(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
		PlayerSkinTokenState tokenState = (PlayerSkinTokenState) state;
		tokenState.pse_etf$setPlayerSkinToken(null);

		Component customName = entity.getCustomName();
		if (customName == null) {
			return;
		}

		Optional<PlayerSkinToken> token = PlayerSkinToken.find(customName.getString());
		token.ifPresent(value -> {
			tokenState.pse_etf$setPlayerSkinToken(value);
			if (state.nameTag != null) {
				state.nameTag = Component.literal(getDisplayedName(value));
			}
		});
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
	private void pse_etf$beginPlayerSkinRender(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
		PlayerSkinRenderContext.set(((PlayerSkinTokenState) state).pse_etf$getPlayerSkinToken());
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
	private void pse_etf$endPlayerSkinRender(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
		PlayerSkinRenderContext.clear();
	}

	private static String getDisplayedName(PlayerSkinToken token) {
		if (token.remainder().isBlank()) {
			return token.username();
		}
		return token.username() + " " + token.remainder();
	}
}
