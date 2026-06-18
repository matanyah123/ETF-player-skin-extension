package net.matanyah.pse;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlayerAssetTextureCache {
	private static final Identifier LOADING_TEXTURE = Identifier.fromNamespaceAndPath(PlayerSkinExtensionETF.MOD_ID, "textures/loading.png");
	private static final Pattern MOJANG_PROFILE_RESPONSE = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"\\s*,\\s*\"name\"\\s*:\\s*\"([^\"]+)\"");
	private static final Map<String, AssetRequest> REQUESTS = new ConcurrentHashMap<>();

	private PlayerAssetTextureCache() {}

	public static Identifier getTexture(String username, PlayerAssetType assetType, Identifier fallbackTexture) {
		AssetRequest request = REQUESTS.computeIfAbsent(cacheKey(username), ignored -> startRequest(username));
		return switch (request.state) {
			case LOADING -> assetType == PlayerAssetType.SKIN ? LOADING_TEXTURE : fallbackTexture;
			case READY -> resolveReadyTexture(request.playerSkin, assetType, fallbackTexture);
			case FAILED -> assetType == PlayerAssetType.SKIN ? DefaultPlayerSkin.getDefaultTexture() : fallbackTexture;
		};
	}

	public static Optional<PlayerModelType> getModelType(String username) {
		AssetRequest request = REQUESTS.computeIfAbsent(cacheKey(username), ignored -> startRequest(username));
		if (request.state != RequestState.READY || request.playerSkin == null) {
			return Optional.empty();
		}
		return Optional.of(request.playerSkin.model());
	}

	private static Identifier resolveReadyTexture(PlayerSkin playerSkin, PlayerAssetType assetType, Identifier fallbackTexture) {
		if (playerSkin == null) {
			return assetType == PlayerAssetType.SKIN ? DefaultPlayerSkin.getDefaultTexture() : fallbackTexture;
		}

		return Optional.ofNullable(assetType.texture(playerSkin))
				.map(net.minecraft.core.ClientAsset.Texture::texturePath)
				.orElse(assetType == PlayerAssetType.SKIN ? DefaultPlayerSkin.getDefaultTexture() : fallbackTexture);
	}

	private static AssetRequest startRequest(String username) {
		AssetRequest request = new AssetRequest();
		Minecraft minecraft = Minecraft.getInstance();

		CompletableFuture
				.supplyAsync(() -> resolveProfile(minecraft, username))
				.thenCompose(profile -> getPlayerSkin(minecraft, profile))
				.thenAccept(playerSkin -> {
					request.playerSkin = playerSkin;
					request.state = RequestState.READY;
				})
				.exceptionally(error -> {
					request.state = RequestState.FAILED;
					PlayerSkinExtensionETF.LOGGER.warn("Could not load Minecraft assets for '{}'", username, error);
					return null;
				});

		return request;
	}

	private static GameProfile resolveProfile(Minecraft minecraft, String username) {
		Optional<com.mojang.authlib.yggdrasil.response.NameAndId> cachedProfile = minecraft.services().profileRepository().findProfileByName(username);
		GameProfile profile = cachedProfile
				.map(value -> new GameProfile(value.id(), value.name()))
				.orElseGet(() -> fetchProfileFromMojang(username));

		try {
			com.mojang.authlib.yggdrasil.ProfileResult fullProfile = minecraft.services().sessionService().fetchProfile(profile.id(), true);
			if (fullProfile != null) {
				return fullProfile.profile();
			}
		} catch (Exception error) {
			PlayerSkinExtensionETF.LOGGER.debug("Could not hydrate Minecraft profile for '{}'", username, error);
		}

		return profile;
	}

	private static GameProfile fetchProfileFromMojang(String username) {
		try {
			byte[] response = URI.create("https://api.mojang.com/users/profiles/minecraft/" + username).toURL()
					.openConnection()
					.getInputStream()
					.readAllBytes();
			String json = new String(response, StandardCharsets.UTF_8);
			return parseMojangProfile(json);
		} catch (IOException error) {
			throw new IllegalArgumentException("No Minecraft profile found for " + username, error);
		}
	}

	private static GameProfile parseMojangProfile(String json) {
		Matcher matcher = MOJANG_PROFILE_RESPONSE.matcher(json);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Mojang profile response did not include a UUID and name");
		}

		return new GameProfile(parseUndashedUuid(matcher.group(1)), matcher.group(2));
	}

	private static UUID parseUndashedUuid(String value) {
		return UUID.fromString(value.substring(0, 8)
				+ "-" + value.substring(8, 12)
				+ "-" + value.substring(12, 16)
				+ "-" + value.substring(16, 20)
				+ "-" + value.substring(20));
	}

	private static CompletableFuture<PlayerSkin> getPlayerSkin(Minecraft minecraft, GameProfile gameProfile) {
		return minecraft.getSkinManager()
				.get(gameProfile)
				.thenApply(skin -> skin.orElseGet(() -> DefaultPlayerSkin.get(gameProfile)));
	}

	private static String cacheKey(String username) {
		return username.toLowerCase(Locale.ROOT);
	}

	private enum RequestState {
		LOADING,
		READY,
		FAILED
	}

	private static final class AssetRequest {
		private volatile RequestState state = RequestState.LOADING;
		private volatile PlayerSkin playerSkin;
	}
}
