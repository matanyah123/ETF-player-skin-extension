package net.matanyah.pse;

import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.Optional;

public final class PlayerNameResolver {
	private PlayerNameResolver() {}

	public static Optional<PlayerIdentity> resolve(Entity entity, PlayerNameSource source, int slot) {
		return switch (source.type()) {
			case TOKEN -> entity == null ? Optional.empty() : resolveToken(entity, slot);
			case NBT -> entity == null ? Optional.empty() : resolveNbt(entity, source.value());
			case STATIC -> PlayerIdentity.parse(source.value());
			case SELF -> Optional.ofNullable(Minecraft.getInstance().getUser())
					.map(user -> new PlayerIdentity(user.getName(), user.getProfileId()));
		};
	}

	public static boolean usesNbtOwner(PlayerNameSource source) {
		if (source.type() != PlayerNameSource.Type.NBT) {
			return false;
		}
		String path = source.value();
		int separator = path.lastIndexOf('.');
		return "Owner".equals(separator < 0 ? path : path.substring(separator + 1));
	}

	private static Optional<PlayerIdentity> resolveToken(Entity entity, int slot) {
		Component customName = entity instanceof RawCustomNameAccess access
				? access.pse_etf$getRawCustomName()
				: entity.getCustomName();
		return customName == null
				? Optional.empty()
				: PlayerSkinToken.findPlayerReference(customName.getString(), slot).flatMap(PlayerIdentity::parse);
	}

	private static Optional<PlayerIdentity> resolveNbt(Entity entity, String field) {
		try {
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
			entity.saveWithoutId(output);
			return resolveNbtValue(output.buildResult(), field);
		} catch (RuntimeException error) {
			PlayerSkinExtensionETF.LOGGER.debug("Could not read player name from entity NBT field '{}'", field, error);
			return Optional.empty();
		}
	}

	static Optional<PlayerIdentity> resolveNbtValue(CompoundTag entityTag, String field) {
		Tag value = findTag(entityTag, field);
		if (value == null && field.indexOf('.') < 0) {
			value = entityTag.getCompound("data").map(data -> data.get(field)).orElse(null);
		}
		if (value == null) {
			return Optional.empty();
		}

		Optional<int[]> uuidArray = value.asIntArray();
		if (uuidArray.isPresent() && uuidArray.get().length == 4) {
			try {
				return Optional.of(PlayerIdentity.of(UUIDUtil.uuidFromIntArray(uuidArray.get())));
			} catch (IllegalArgumentException ignored) {}
		}
		return value.asString().flatMap(PlayerIdentity::parse);
	}

	private static Tag findTag(CompoundTag root, String path) {
		Tag current = root;
		for (String segment : path.split("\\.")) {
			if (segment.isEmpty() || !(current instanceof CompoundTag compound)) {
				return null;
			}
			current = compound.get(segment);
			if (current == null) {
				return null;
			}
		}
		return current;
	}
}
