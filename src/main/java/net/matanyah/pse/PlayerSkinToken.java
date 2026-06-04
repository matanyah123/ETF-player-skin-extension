package net.matanyah.pse;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PlayerSkinToken(String username, String remainder) {
	private static final Pattern TOKEN_PATTERN = Pattern.compile("\\$([A-Za-z0-9_]{3,16})\\$");

	public static Optional<PlayerSkinToken> find(String name) {
		Matcher matcher = TOKEN_PATTERN.matcher(name);
		if (!matcher.find()) {
			return Optional.empty();
		}

		String remainder = (name.substring(0, matcher.start()) + name.substring(matcher.end())).trim();
		return Optional.of(new PlayerSkinToken(matcher.group(1), remainder));
	}
}
