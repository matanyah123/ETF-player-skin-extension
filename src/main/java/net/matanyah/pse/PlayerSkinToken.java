package net.matanyah.pse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PlayerSkinToken(String username, String remainder, String displayRemainder, List<String> flags) {
	private static final Pattern TOKEN_PATTERN = Pattern.compile("\\$([A-Za-z0-9_]{3,16})\\$");
	private static final Pattern FLAG_PATTERN = Pattern.compile("(?<!\\S)-([^\\s]+)");
	private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

	public PlayerSkinToken {
		remainder = normalizeSpacing(remainder);
		displayRemainder = normalizeSpacing(displayRemainder);
		flags = List.copyOf(flags);
	}

	public static Optional<PlayerSkinToken> find(String name) {
		Matcher matcher = TOKEN_PATTERN.matcher(name);
		if (!matcher.find()) {
			return Optional.empty();
		}

		String remainder = name.substring(0, matcher.start()) + name.substring(matcher.end());
		HiddenFlagParseResult hiddenFlags = parseHiddenFlags(remainder);
		return Optional.of(new PlayerSkinToken(
				matcher.group(1),
				remainder,
				hiddenFlags.displayText(),
				hiddenFlags.flags()
		));
	}

	public static List<String> findHiddenFlags(String name) {
		return parseHiddenFlags(name).flags();
	}

	public static String stripHiddenFlags(String name) {
		return parseHiddenFlags(name).displayText();
	}

	public String displayName() {
		if (displayRemainder.isBlank()) {
			return username;
		}
		return username + " " + displayRemainder;
	}

	public boolean hasFlag(String flag) {
		return flags.contains(flag.toLowerCase(Locale.ROOT));
	}

	private static HiddenFlagParseResult parseHiddenFlags(String input) {
		ArrayList<String> flags = new ArrayList<>();
		Matcher matcher = FLAG_PATTERN.matcher(input);
		while (matcher.find()) {
			String flag = matcher.group(1);
			if (!flag.isBlank()) {
				flags.add(flag.toLowerCase(Locale.ROOT));
			}
		}

		String displayText = normalizeSpacing(matcher.replaceAll(" "));
		return new HiddenFlagParseResult(displayText, flags);
	}

	private static String normalizeSpacing(String value) {
		return MULTI_SPACE_PATTERN.matcher(value.trim()).replaceAll(" ");
	}

	private record HiddenFlagParseResult(String displayText, List<String> flags) {}
}
