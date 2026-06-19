package net.matanyah.pse;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.Optional;

public final class PlayerNameResolver {
	private PlayerNameResolver() {}

	public static Optional<String> resolve(Entity entity, PlayerNameSource source, int slot) {
		if (entity == null) {
			return Optional.empty();
		}

		Optional<String> username = switch (source.type()) {
			case TOKEN -> resolveToken(entity, slot);
			case NBT -> resolveNbt(entity, source.value());
			case STATIC -> Optional.of(source.value());
			case SELF -> Optional.ofNullable(Minecraft.getInstance().getUser())
					.map(user -> user.getName());
		};
		return username.map(String::trim).filter(PlayerSkinToken::isValidUsername);
	}

	private static Optional<String> resolveToken(Entity entity, int slot) {
		Component customName = entity instanceof RawCustomNameAccess access
				? access.pse_etf$getRawCustomName()
				: entity.getCustomName();
		return customName == null
				? Optional.empty()
				: PlayerSkinToken.findUsername(customName.getString(), slot);
	}

	private static Optional<String> resolveNbt(Entity entity, String field) {
		try {
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
			entity.saveWithoutId(output);
			CompoundTag entityTag = output.buildResult();
			Tag value = entityTag.get(field);
			return value == null ? Optional.empty() : value.asString();
		} catch (RuntimeException error) {
			PlayerSkinExtensionETF.LOGGER.debug("Could not read player name from entity NBT field '{}'", field, error);
			return Optional.empty();
		}
	}
}
