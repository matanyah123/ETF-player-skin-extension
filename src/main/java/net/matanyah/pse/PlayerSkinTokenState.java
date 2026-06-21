package net.matanyah.pse;

import net.minecraft.world.entity.Entity;

import java.util.List;

public interface PlayerSkinTokenState {
	void pse_etf$setPlayerSkinToken(PlayerSkinToken token);

	PlayerSkinToken pse_etf$getPlayerSkinToken();

	void pse_etf$setPlayerSkinEntity(Entity entity);

	Entity pse_etf$getPlayerSkinEntity();

	void pse_etf$setShoulderOwner(PlayerIdentity owner);

	PlayerIdentity pse_etf$getShoulderOwner();

	void pse_etf$setHiddenFlags(List<String> flags);

	List<String> pse_etf$getHiddenFlags();
}
