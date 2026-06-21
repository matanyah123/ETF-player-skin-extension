package net.matanyah.pse;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public record PlayerIdentity(String reference, UUID uuid) {
	private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");
	private static final Pattern DASHED_UUID_PATTERN = Pattern.compile(
			"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
	);
	private static final Pattern UNDASHED_UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{32}");

	public static Optional<PlayerIdentity> parse(String value) {
		if (value == null) {
			return Optional.empty();
		}

		String trimmed = value.trim();
		if (USERNAME_PATTERN.matcher(trimmed).matches()) {
			return Optional.of(new PlayerIdentity(trimmed, null));
		}

		return parseUuid(trimmed).map(uuid -> new PlayerIdentity(uuid.toString(), uuid));
	}

	public static PlayerIdentity of(UUID uuid) {
		return new PlayerIdentity(uuid.toString(), uuid);
	}

	public boolean isUuid() {
		return uuid != null;
	}

	public String cacheKey() {
		return isUuid() ? "uuid:" + uuid : "name:" + reference.toLowerCase(Locale.ROOT);
	}

	private static Optional<UUID> parseUuid(String value) {
		try {
			if (DASHED_UUID_PATTERN.matcher(value).matches()) {
				return Optional.of(UUID.fromString(value));
			}
			if (UNDASHED_UUID_PATTERN.matcher(value).matches()) {
				return Optional.of(UUID.fromString(
						value.substring(0, 8) + "-"
								+ value.substring(8, 12) + "-"
								+ value.substring(12, 16) + "-"
								+ value.substring(16, 20) + "-"
								+ value.substring(20)
				));
			}
		} catch (IllegalArgumentException ignored) {}
		return Optional.empty();
	}
}
