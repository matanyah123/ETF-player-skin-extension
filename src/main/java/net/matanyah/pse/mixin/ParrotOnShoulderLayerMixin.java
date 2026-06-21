package net.matanyah.pse.mixin;

import com.mojang.authlib.GameProfile;
import net.matanyah.pse.PlayerIdentity;
import net.matanyah.pse.PlayerSkinRenderContext;
import net.matanyah.pse.PlayerSkinTokenState;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ParrotOnShoulderLayer.class)
public abstract class ParrotOnShoulderLayerMixin {
	@ModifyArg(
			method = "submitOnShoulder",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
			),
			index = 1
	)
	private Object pse_etf$attachShoulderOwner(Object renderState) {
		Entity entity = PlayerSkinRenderContext.currentEntity();
		if (entity instanceof Player player && renderState instanceof PlayerSkinTokenState tokenState) {
			GameProfile profile = player.getGameProfile();
			tokenState.pse_etf$setPlayerSkinEntity(player);
			tokenState.pse_etf$setShoulderOwner(new PlayerIdentity(profile.name(), profile.id()));
		}
		return renderState;
	}
}
