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

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

#if MC_VER < MC_1_17_1
import net.minecraft.world.level.material.FluidState;
#elif MC_VER < MC_1_21_3
import net.minecraft.world.level.material.FogType;
#else
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.renderer.FogParameters;
import org.joml.Vector4f;
#endif

@Mixin(FogRenderer.class)
public class MixinFogRenderer
{
	// Using this instead of Float.MAX_VALUE because Sodium don't like it.
	@Unique
	private static final float A_REALLY_REALLY_BIG_VALUE = 420694206942069.F;
	@Unique
	private static final float A_EVEN_LARGER_VALUE = 42069420694206942069.F;
	
	
	
	#if MC_VER < MC_1_19_2
	@Inject(at = @At("RETURN"), method = "setupFog")
	private static void disableSetupFog(Camera camera, FogMode fogMode, float f, boolean bl, CallbackInfo callback)
	#elif MC_VER < MC_1_21_3
	@Inject(at = @At("RETURN"), method = "setupFog")
	private static void disableSetupFog(Camera camera, FogMode fogMode, float f, boolean bl, float g, CallbackInfo callback)
	#else
	@Inject(at = @At("RETURN"), method = "setupFog", cancellable = true)
	private static void disableSetupFog(Camera camera, FogMode fogMode, Vector4f vector4f, float f, boolean bl, float g, CallbackInfoReturnable<FogParameters> callback)
	#endif
	{
		boolean cameraNotInFluid = cameraNotInFluid(camera);
		
		Entity entity = camera.getEntity();
		boolean isSpecialFog = (entity instanceof LivingEntity) && ((LivingEntity) entity).hasEffect(MobEffects.BLINDNESS);
		if (!isSpecialFog && cameraNotInFluid && fogMode == FogMode.FOG_TERRAIN
				&& !SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class).isFogStateSpecial()
				&& !Config.Client.Advanced.Graphics.Fog.enableVanillaFog.get())
		{
			#if MC_VER < MC_1_17_1
			RenderSystem.fogStart(A_REALLY_REALLY_BIG_VALUE);
			RenderSystem.fogEnd(A_EVEN_LARGER_VALUE);
			#elif MC_VER < MC_1_21_3
			RenderSystem.setShaderFogStart(A_REALLY_REALLY_BIG_VALUE);
			RenderSystem.setShaderFogEnd(A_EVEN_LARGER_VALUE);
			#else
			callback.setReturnValue(FogParameters.NO_FOG);
			#endif
		}
	}
	
	@Unique
	private static boolean cameraNotInFluid(Camera camera)
	{
		#if MC_VER < MC_1_17_1
		FluidState fluidState = camera.getFluidInCamera();
		boolean cameraNotInFluid = fluidState.isEmpty();
		#else
		FogType fogTypes = camera.getFluidInCamera();
		boolean cameraNotInFluid = fogTypes == FogType.NONE;
		#endif
		
		return cameraNotInFluid;
	}
	
}
