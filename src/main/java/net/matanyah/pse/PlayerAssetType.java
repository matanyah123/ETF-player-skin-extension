package net.matanyah.pse;

import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Locale;

public enum PlayerAssetType {
	SKIN {
		@Override
		public ClientAsset.Texture texture(PlayerSkin skin) {
			return skin.body();
		}
	},
	CAPE {
		@Override
		public ClientAsset.Texture texture(PlayerSkin skin) {
			return skin.cape();
		}
	},
	ELYTRA {
		@Override
		public ClientAsset.Texture texture(PlayerSkin skin) {
			return skin.elytra();
		}
	};

	public abstract ClientAsset.Texture texture(PlayerSkin skin);

	public static PlayerAssetType fromPropertyValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
