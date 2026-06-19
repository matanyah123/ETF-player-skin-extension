package net.matanyah.pse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerNameSourceTest {
	@Test
	void defaultsToToken() {
		assertEquals(PlayerNameSource.TOKEN, PlayerNameSource.fromPropertyValue(null));
		assertEquals(PlayerNameSource.TOKEN, PlayerNameSource.fromPropertyValue(""));
		assertEquals(PlayerNameSource.TOKEN, PlayerNameSource.fromPropertyValue("unknown"));
	}

	@Test
	void parsesSupportedSources() {
		assertEquals(PlayerNameSource.TOKEN, PlayerNameSource.fromPropertyValue(" TOKEN "));
		assertEquals(new PlayerNameSource(PlayerNameSource.Type.NBT, "Owner"), PlayerNameSource.fromPropertyValue("nbt:Owner"));
		assertEquals(new PlayerNameSource(PlayerNameSource.Type.STATIC, "Notch"), PlayerNameSource.fromPropertyValue("static:Notch"));
		assertEquals(new PlayerNameSource(PlayerNameSource.Type.SELF, ""), PlayerNameSource.fromPropertyValue("self"));
	}

	@Test
	void emptyParameterizedSourcesFallBackToToken() {
		assertEquals(PlayerNameSource.TOKEN, PlayerNameSource.fromPropertyValue("nbt:   "));
		assertEquals(PlayerNameSource.TOKEN, PlayerNameSource.fromPropertyValue("static:   "));
	}
}
