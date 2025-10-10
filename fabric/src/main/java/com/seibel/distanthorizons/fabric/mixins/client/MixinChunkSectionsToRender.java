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

#if MC_VER < MC_1_21_10
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
	
	
	// needs to fire at HEAD otherwise it will be canceled by Sodium
	@Inject(at = @At("HEAD"), method = "renderGroup")
	private void renderDeferredLayer(ChunkSectionLayerGroup chunkSectionLayerGroup, CallbackInfo ci)
	{
		ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapperIfDifferent(ClientApi.RENDER_STATE.clientLevelWrapper, Minecraft.getInstance().levelRenderer.level);
		
		
		if (chunkSectionLayerGroup == ChunkSectionLayerGroup.OPAQUE)
		{
			ClientApi.INSTANCE.renderFadeOpaque();
		}
		else if (chunkSectionLayerGroup == ChunkSectionLayerGroup.TRANSLUCENT)
		{
			ClientApi.INSTANCE.renderDeferredLodsForShaders();
		}
	}
	
	// canceled by sodium, but there isn't a better way to handle it right now
	// https://github.com/CaffeineMC/sodium/blob/dev/common/src/main/java/net/caffeinemc/mods/sodium/mixin/core/render/world/ChunkSectionsToRenderMixin.java
	@Inject(at = @At("RETURN"), method = "renderGroup")
	private void renderOpaqueFade(ChunkSectionLayerGroup chunkSectionLayerGroup, CallbackInfo ci)
	{
		ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapperIfDifferent(ClientApi.RENDER_STATE.clientLevelWrapper, Minecraft.getInstance().levelRenderer.level);
		
		
		if (chunkSectionLayerGroup == ChunkSectionLayerGroup.OPAQUE)
		{
			ClientApi.INSTANCE.renderFadeOpaque();
		}
	}
	
	
}

#endif

