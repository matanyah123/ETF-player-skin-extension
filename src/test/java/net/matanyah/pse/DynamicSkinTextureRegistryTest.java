package net.matanyah.pse;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicSkinTextureRegistryTest {
	@Test
	void dynamicRulesUseBackwardCompatibleNameDefaults() {
		Properties properties = new Properties();
		properties.setProperty("skins.1", "dynamic");
		properties.setProperty("player_asset.1", "skin");

		DynamicSkinTextureRegistry.VariantFlags flags = DynamicSkinTextureRegistry.getVariantFlags(properties, 1);

		assertTrue(flags.dynamic());
		assertEquals(PlayerAssetType.SKIN, flags.assetType());
		assertEquals(PlayerNameSource.TOKEN, flags.nameSource());
		assertEquals(1, flags.nameSlot());
	}

	@Test
	void readsSourceAndSlotFromTheSameRuleNumber() {
		Properties properties = new Properties();
		properties.setProperty("textures.2", "dynamic");
		properties.setProperty("player_asset.2", "cape");
		properties.setProperty("name_source.2", "nbt:Owner");
		properties.setProperty("name_slot.2", "2");

		DynamicSkinTextureRegistry.VariantFlags flags = DynamicSkinTextureRegistry.getVariantFlags(properties, 2);

		assertTrue(flags.dynamic());
		assertEquals(PlayerAssetType.CAPE, flags.assetType());
		assertEquals(new PlayerNameSource(PlayerNameSource.Type.NBT, "Owner"), flags.nameSource());
		assertEquals(2, flags.nameSlot());
	}

	@Test
	void invalidSlotsFallBackToFirstToken() {
		Properties properties = new Properties();
		properties.setProperty("name_slot.1", "not-a-number");
		properties.setProperty("name_slot.2", "0");

		assertEquals(1, DynamicSkinTextureRegistry.getVariantFlags(properties, 1).nameSlot());
		assertEquals(1, DynamicSkinTextureRegistry.getVariantFlags(properties, 2).nameSlot());
	}
}
