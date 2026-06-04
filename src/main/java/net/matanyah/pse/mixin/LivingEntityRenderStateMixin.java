package net.matanyah.pse.mixin;

import net.matanyah.pse.PlayerSkinToken;
import net.matanyah.pse.PlayerSkinTokenState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements PlayerSkinTokenState {
	@Unique
	private PlayerSkinToken pse_etf$playerSkinToken;

	@Override
	public void pse_etf$setPlayerSkinToken(PlayerSkinToken token) {
		this.pse_etf$playerSkinToken = token;
	}

	@Override
	public PlayerSkinToken pse_etf$getPlayerSkinToken() {
		return pse_etf$playerSkinToken;
	}
}
