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

#if MC_VER < MC_1_21_9
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public class MixinChunkSectionsToRender
{ /* rendering before was handled via Fabric API events */ }
#else

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public class MixinChunkSectionsToRender
{
	
	
	// needs to fire at HEAD with a lower than normal order (less than 1000)
	// otherwise it will be canceled by Sodium
	@Inject(at = @At("HEAD"), method = "renderGroup", order = 800)
	private void renderDeferredLayer(ChunkSectionLayerGroup chunkSectionLayerGroup, CallbackInfo ci)
	{
		ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapperIfDifferent(ClientApi.RENDER_STATE.clientLevelWrapper, Minecraft.getInstance().levelRenderer.level);
		
		
		if (chunkSectionLayerGroup == ChunkSectionLayerGroup.TRANSLUCENT)
		{
			ClientApi.INSTANCE.renderFadeTransparent();
			ClientApi.INSTANCE.renderDeferredLodsForShaders();
		}
		else if (chunkSectionLayerGroup == ChunkSectionLayerGroup.TRIPWIRE)
		{
			ClientApi.INSTANCE.renderFadeOpaque();
		}
	}
	
	
	
}

#endif

