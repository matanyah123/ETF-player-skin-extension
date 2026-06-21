package net.matanyah.pse;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerNameResolverTest {
	private static final UUID OWNER_UUID = UUID.fromString("12345678-1234-5678-9abc-123456789abc");

	@Test
	void readsParrotStyleUuidFields() {
		CompoundTag entityTag = new CompoundTag();
		entityTag.putIntArray("Owner", UUIDUtil.uuidToIntArray(OWNER_UUID));

		PlayerIdentity identity = PlayerNameResolver.resolveNbtValue(entityTag, "Owner").orElseThrow();

		assertEquals(OWNER_UUID, identity.uuid());
	}

	@Test
	void fallsBackToEntityCustomDataForSimpleFieldNames() {
		CompoundTag customData = new CompoundTag();
		customData.putString("Owner", "Mogswamp");
		CompoundTag entityTag = new CompoundTag();
		entityTag.put("data", customData);

		assertEquals(
				"Mogswamp",
				PlayerNameResolver.resolveNbtValue(entityTag, "Owner").orElseThrow().reference()
		);
		assertEquals(
				"Mogswamp",
				PlayerNameResolver.resolveNbtValue(entityTag, "data.Owner").orElseThrow().reference()
		);
	}

	@Test
	void rejectsNonPlayerNbtValues() {
		CompoundTag entityTag = new CompoundTag();
		entityTag.putString("Owner", "not a player name");

		assertTrue(PlayerNameResolver.resolveNbtValue(entityTag, "Owner").isEmpty());
	}

	@Test
	void recognizesOwnerPathsForShoulderRendering() {
		assertTrue(PlayerNameResolver.usesNbtOwner(
				new PlayerNameSource(PlayerNameSource.Type.NBT, "Owner")
		));
		assertTrue(PlayerNameResolver.usesNbtOwner(
				new PlayerNameSource(PlayerNameSource.Type.NBT, "data.Owner")
		));
		assertFalse(PlayerNameResolver.usesNbtOwner(
				new PlayerNameSource(PlayerNameSource.Type.NBT, "CustomName")
		));
		assertFalse(PlayerNameResolver.usesNbtOwner(PlayerNameSource.TOKEN));
	}
}
