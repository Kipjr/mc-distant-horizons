/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.fabric.mixins.client;

import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.client.renderer.LightTexture;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

#if MC_VER < MC_1_21_3
import com.mojang.blaze3d.platform.NativeImage;
#else
import com.mojang.blaze3d.pipeline.TextureTarget;
#endif

@Mixin(LightTexture.class)
public class MixinLightTexture
{
	@Shadow 
	@Final 
	#if MC_VER < MC_1_21_3
	private NativeImage lightPixels;
	#else
	private TextureTarget target;
	#endif
	
	@Inject(method = "updateLightTexture(F)V", at = @At("RETURN"))
	public void updateLightTexture(float partialTicks, CallbackInfo ci)
	{
		IMinecraftClientWrapper mc = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
		if (mc == null || mc.getWrappedClientLevel() == null)
		{
			return;
		}
		
		
		IClientLevelWrapper clientLevel = mc.getWrappedClientLevel();
		
		#if MC_VER < MC_1_21_3
		MinecraftRenderWrapper.INSTANCE.updateLightmap(this.lightPixels, clientLevel);
		#else
		MinecraftRenderWrapper.INSTANCE.setLightmapId(this.target.getColorTextureId(), clientLevel);
		#endif
	}
	
}
