package net.matanyah.pse;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerIdentityTest {
	private static final UUID UUID_VALUE = UUID.fromString("12345678-1234-5678-9abc-123456789abc");

	@Test
	void parsesUsernamesAndBothUuidFormats() {
		PlayerIdentity username = PlayerIdentity.parse("Matanyah").orElseThrow();
		PlayerIdentity dashedUuid = PlayerIdentity.parse(UUID_VALUE.toString()).orElseThrow();
		PlayerIdentity undashedUuid = PlayerIdentity.parse("12345678123456789abc123456789abc").orElseThrow();

		assertFalse(username.isUuid());
		assertEquals("Matanyah", username.reference());
		assertEquals(UUID_VALUE, dashedUuid.uuid());
		assertEquals(dashedUuid, undashedUuid);
	}

	@Test
	void rejectsValuesThatAreNeitherUsernamesNorUuids() {
		assertTrue(PlayerIdentity.parse("ab").isEmpty());
		assertTrue(PlayerIdentity.parse("not a player").isEmpty());
		assertTrue(PlayerIdentity.parse("12345678-1234-invalid").isEmpty());
	}
}
