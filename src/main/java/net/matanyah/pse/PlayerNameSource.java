package net.matanyah.pse;

import java.util.Locale;

public record PlayerNameSource(Type type, String value) {
	public static final PlayerNameSource TOKEN = new PlayerNameSource(Type.TOKEN, "");

	public static PlayerNameSource fromPropertyValue(String value) {
		if (value == null || value.isBlank()) {
			return TOKEN;
		}

		String trimmed = value.trim();
		String normalized = trimmed.toLowerCase(Locale.ROOT);
		if ("token".equals(normalized)) {
			return TOKEN;
		}
		if ("self".equals(normalized)) {
			return new PlayerNameSource(Type.SELF, "");
		}
		if (normalized.startsWith("nbt:")) {
			String field = trimmed.substring(4).trim();
			return field.isEmpty() ? TOKEN : new PlayerNameSource(Type.NBT, field);
		}
		if (normalized.startsWith("static:")) {
			String username = trimmed.substring(7).trim();
			return username.isEmpty() ? TOKEN : new PlayerNameSource(Type.STATIC, username);
		}

		return TOKEN;
	}

	public String propertyValue() {
		return switch (type) {
			case TOKEN -> "token";
			case NBT -> "nbt:" + value;
			case STATIC -> "static:" + value;
			case SELF -> "self";
		};
	}

	public enum Type {
		TOKEN,
		NBT,
		STATIC,
		SELF
	}
}
