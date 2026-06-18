package net.matanyah.pse.mixin;

import net.matanyah.pse.PlayerSkinToken;
import net.matanyah.pse.PlayerSkinTokenState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements PlayerSkinTokenState {
	@Unique
	private PlayerSkinToken pse_etf$playerSkinToken;
	@Unique
	private Entity pse_etf$playerSkinEntity;
	@Unique
	private List<String> pse_etf$hiddenFlags = List.of();

	@Override
	public void pse_etf$setPlayerSkinToken(PlayerSkinToken token) {
		this.pse_etf$playerSkinToken = token;
	}

	@Override
	public PlayerSkinToken pse_etf$getPlayerSkinToken() {
		return pse_etf$playerSkinToken;
	}

	@Override
	public void pse_etf$setPlayerSkinEntity(Entity entity) {
		this.pse_etf$playerSkinEntity = entity;
	}

	@Override
	public Entity pse_etf$getPlayerSkinEntity() {
		return pse_etf$playerSkinEntity;
	}

	@Override
	public void pse_etf$setHiddenFlags(List<String> flags) {
		this.pse_etf$hiddenFlags = List.copyOf(flags);
	}

	@Override
	public List<String> pse_etf$getHiddenFlags() {
		return pse_etf$hiddenFlags;
	}
}
