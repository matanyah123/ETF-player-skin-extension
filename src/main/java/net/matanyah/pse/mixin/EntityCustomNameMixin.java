package net.matanyah.pse.mixin;

import net.matanyah.pse.PlayerSkinToken;
import net.matanyah.pse.RawCustomNameAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityCustomNameMixin implements RawCustomNameAccess {
	@Shadow
	@Final
	private static EntityDataAccessor<Optional<Component>> field_6027;

	@Shadow
	@Final
	private SynchedEntityData field_6011;

	@Override
	public Component pse_etf$getRawCustomName() {
		return field_6011.get(field_6027).orElse(null);
	}

	@Inject(method = "method_5797", at = @At("RETURN"), cancellable = true)
	private void pse_etf$stripPlayerSkinToken(CallbackInfoReturnable<Component> cir) {
		Component customName = cir.getReturnValue();
		if (customName == null) {
			return;
		}

		PlayerSkinToken.find(customName.getString())
				.ifPresent(token -> {
					String visibleName = token.remainder().isBlank() ? "$" : token.remainder() + " $";
					cir.setReturnValue(Component.literal(visibleName));
				});
	}
}
