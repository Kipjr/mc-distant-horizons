package com.seibel.distanthorizons.fabric.mixins.client;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public class MixinLevel
{
	@SuppressWarnings("UnreachableCode")
	@Inject(method = "close", at = @At("HEAD"))
	private void unloadWorldEvent(CallbackInfo ci)
	{
		if ((Object) this instanceof ClientLevel)
		{
			ClientApi.INSTANCE.clientLevelLoadEvent(ClientLevelWrapper.getWrapper((ClientLevel) (Object) this));
		}
	}
	
}