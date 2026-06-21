package net.matanyah.pse;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerSkinTokenTest {
	private static final String UUID = "12345678-1234-5678-9abc-123456789abc";

	@Test
	void selectsTokenByOneBasedSlot() {
		String name = "$Matanyah$ and $Mogswamp$";

		assertEquals(Optional.of("Matanyah"), PlayerSkinToken.findPlayerReference(name, 1));
		assertEquals(Optional.of("Mogswamp"), PlayerSkinToken.findPlayerReference(name, 2));
		assertEquals(Optional.empty(), PlayerSkinToken.findPlayerReference(name, 3));
		assertEquals(Optional.empty(), PlayerSkinToken.findPlayerReference(name, 0));
	}

	@Test
	void uuidTokensParticipateInSlotSelection() {
		String name = "$Matanyah$ $" + UUID + "$";

		assertEquals(Optional.of("Matanyah"), PlayerSkinToken.findPlayerReference(name, 1));
		assertEquals(Optional.of(UUID), PlayerSkinToken.findPlayerReference(name, 2));
	}

	@Test
	void replacesUuidTokensWithResolvedNamesForDisplay() {
		String name = "base $" + UUID + "$ -cape";

		assertEquals(
				"base Mogswamp",
				PlayerSkinToken.formatDisplayName(name, ignored -> Optional.of("Mogswamp"))
		);
		assertEquals(
				"base",
				PlayerSkinToken.formatDisplayName(name, ignored -> Optional.empty())
		);
	}

	@Test
	void ignoresMalformedTokensWhenCountingSlots() {
		String name = "$ab$ $Valid_Name$ $ThisUsernameIsFarTooLong$ $SecondUser$";

		assertEquals(Optional.of("Valid_Name"), PlayerSkinToken.findPlayerReference(name, 1));
		assertEquals(Optional.of("SecondUser"), PlayerSkinToken.findPlayerReference(name, 2));
	}
}
