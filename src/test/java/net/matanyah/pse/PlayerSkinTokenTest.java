package net.matanyah.pse;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerSkinTokenTest {
	@Test
	void selectsTokenByOneBasedSlot() {
		String name = "$Matanyah$ and $Mogswamp$";

		assertEquals(Optional.of("Matanyah"), PlayerSkinToken.findUsername(name, 1));
		assertEquals(Optional.of("Mogswamp"), PlayerSkinToken.findUsername(name, 2));
		assertEquals(Optional.empty(), PlayerSkinToken.findUsername(name, 3));
		assertEquals(Optional.empty(), PlayerSkinToken.findUsername(name, 0));
	}

	@Test
	void ignoresMalformedTokensWhenCountingSlots() {
		String name = "$ab$ $Valid_Name$ $ThisUsernameIsFarTooLong$ $SecondUser$";

		assertEquals(Optional.of("Valid_Name"), PlayerSkinToken.findUsername(name, 1));
		assertEquals(Optional.of("SecondUser"), PlayerSkinToken.findUsername(name, 2));
	}
}
