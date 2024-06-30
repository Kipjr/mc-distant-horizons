/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.neoforge;

import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.world.level.LevelAccessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.level.chunk.ChunkAccess;

//import net.neoforged.network.NetworkRegistry;
//import net.neoforged.network.simple.SimpleChannel;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.opengl.GL32;

#if MC_VER < MC_1_20_6
import net.neoforged.neoforge.event.TickEvent;
#else
import net.neoforged.neoforge.client.event.ClientTickEvent;
#endif


/**
 * This handles all events sent to the client,
 * and is the starting point for most of the mod.
 *
 * @author James_Seibel
 * @version 2023-7-27
 */
public class NeoforgeClientProxy implements AbstractModInitializer.IEventProxy
{
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();

//	private static SimpleChannel multiversePluginChannel;
	
	// Not the cleanest way of passing this to the LOD renderer, but it'll have to do for now
	public static Mat4f currentModelViewMatrix = new Mat4f();
	public static Mat4f currentProjectionMatrix = new Mat4f();
	
	
	
	
	@Override
	public void registerEvents()
	{
		NeoForge.EVENT_BUS.register(this);
		setupNetworkingListeners();
	}
	
	
	
	//=============//
	// tick events //
	//=============//
	
	#if MC_VER < MC_1_20_6
	@SubscribeEvent
	public void clientTickEvent(TickEvent.ClientTickEvent event)
	{
		if (event.phase == TickEvent.Phase.START)
		{
			ClientApi.INSTANCE.clientTickEvent();
		}
	}
	#else
	@SubscribeEvent
	public void clientTickEvent(ClientTickEvent.Pre event)
	{
		ClientApi.INSTANCE.clientTickEvent();
	}
	#endif
	
	
	
	//==============//
	// world events //
	//==============//
	
	@SubscribeEvent
	public void clientLevelLoadEvent(LevelEvent.Load event)
	{
		LOGGER.info("level load");
		
		LevelAccessor level = event.getLevel();
		if (!(level instanceof ClientLevel))
		{
			return;
		}
		
		ClientLevel clientLevel = (ClientLevel) level;
		IClientLevelWrapper clientLevelWrapper = ClientLevelWrapper.getWrapper(clientLevel);
		// TODO this causes a crash due to level being set to null somewhere
		ClientApi.INSTANCE.clientLevelLoadEvent(clientLevelWrapper);
	}
	@SubscribeEvent
	public void clientLevelUnloadEvent(LevelEvent.Unload event)
	{
		LOGGER.info("level unload");
		
		LevelAccessor level = event.getLevel();
		if (!(level instanceof ClientLevel))
		{
			return;
		}
		
		ClientLevel clientLevel = (ClientLevel) level;
		IClientLevelWrapper clientLevelWrapper = ClientLevelWrapper.getWrapper(clientLevel);
		ClientApi.INSTANCE.clientLevelUnloadEvent(clientLevelWrapper);
	}
	
	
	
	//==============//
	// chunk events //
	//==============//
	
	@SubscribeEvent
	public void rightClickBlockEvent(PlayerInteractEvent.RightClickBlock event)
	{
		if (SharedApi.isChunkAtBlockPosAlreadyUpdating(event.getPos().getX(), event.getPos().getZ()))
		{
			return;
		}
		
		//LOGGER.trace("interact or block place event at blockPos: " + event.getPos());
		
		LevelAccessor level = event.getLevel();
		
		ChunkAccess chunk = level.getChunk(event.getPos());
		this.onBlockChangeEvent(level, chunk);
	}
	@SubscribeEvent
	public void leftClickBlockEvent(PlayerInteractEvent.LeftClickBlock event)
	{
		if (SharedApi.isChunkAtBlockPosAlreadyUpdating(event.getPos().getX(), event.getPos().getZ()))
		{
			return;
		}
		
		//LOGGER.trace("break or block attack at blockPos: " + event.getPos());
		
		LevelAccessor level = event.getLevel();
		
		ChunkAccess chunk = level.getChunk(event.getPos());
		this.onBlockChangeEvent(level, chunk);
	}
	private void onBlockChangeEvent(LevelAccessor level, ChunkAccess chunk)
	{
		ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(level);
		SharedApi.INSTANCE.chunkBlockChangedEvent(new ChunkWrapper(chunk, level, wrappedLevel), wrappedLevel);
	}
	
	
	@SubscribeEvent
	public void clientChunkLoadEvent(ChunkEvent.Load event)
	{
		ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetEventLevel(event), wrappedLevel);
		SharedApi.INSTANCE.chunkLoadEvent(chunk, wrappedLevel);
	}
	@SubscribeEvent
	public void clientChunkUnloadEvent(ChunkEvent.Unload event)
	{
		ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetEventLevel(event), wrappedLevel);
		SharedApi.INSTANCE.chunkUnloadEvent(chunk, wrappedLevel);
	}
	
	
	
	//==============//
	// key bindings //
	//==============//
	
	@SubscribeEvent
	public void registerKeyBindings(InputEvent.Key event)
	{
		if (Minecraft.getInstance().player == null)
		{
			return;
		}
		if (event.getAction() != GLFW.GLFW_PRESS)
		{
			return;
		}
		
		ClientApi.INSTANCE.keyPressedEvent(event.getKey());
	}
	
	
	
	//============//
	// networking //
	//============//
	
	public static void setupNetworkingListeners()
	{
//		multiversePluginChannel = NetworkRegistry.newSimpleChannel(
//				new ResourceLocation(ModInfo.NETWORKING_RESOURCE_NAMESPACE, ModInfo.MULTIVERSE_PLUGIN_NAMESPACE),
//				// network protocol version
//				() -> ModInfo.MULTIVERSE_PLUGIN_PROTOCOL_VERSION +"",
//				// client accepted versions
//				ForgeClientProxy::isReceivedProtocolVersionAcceptable,
//				// server accepted versions
//				ForgeClientProxy::isReceivedProtocolVersionAcceptable
//		);
//		
//		multiversePluginChannel.registerMessage(0/*should be incremented for each simple channel we listen to*/, ByteBuf.class,
//				// encoder
//				(pack, friendlyByteBuf) -> { },
//				// decoder
//				(friendlyByteBuf) -> friendlyByteBuf.asByteBuf(),
//				// message consumer
//				(nettyByteBuf, contextRef) ->
//				{
//					ClientApi.INSTANCE.serverMessageReceived(nettyByteBuf);
//					contextRef.get().setPacketHandled(true);
//				}
//		);
	}
	
	public static boolean isReceivedProtocolVersionAcceptable(String versionString)
	{
		if (versionString.toLowerCase().contains("allowvanilla"))
		{
			// allow using networking on vanilla servers
			return true;
		}
		else if (versionString.toLowerCase().contains("absent"))
		{
			// allow using networking even if DH isn't installed on the server
			return true;
		}
		else
		{
			// DH is installed on the server, check if the version is valid to use
			try
			{
				int version = Integer.parseInt(versionString);
				return ModInfo.MULTIVERSE_PLUGIN_PROTOCOL_VERSION == version;
			}
			catch (NumberFormatException ignored)
			{
				return false;
			}
		}
	}
	
	
	
	//===========//
	// rendering //
	//===========//
	
	@SubscribeEvent
	public void beforeLevelRenderEvent(RenderLevelStageEvent event)
	{
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY)
		{
			currentModelViewMatrix = McObjectConverter.Convert(event.getModelViewMatrix());
			currentProjectionMatrix = McObjectConverter.Convert(event.getProjectionMatrix());
		}
	}
	
	@SubscribeEvent
	public void afterLevelRenderEvent(RenderLevelStageEvent event)
	{
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL)
		{
			try
			{
				// should generally only need to be set once per game session
				// allows DH to render directly to Optifine's level frame buffer,
				// allowing better shader support
				MinecraftRenderWrapper.INSTANCE.finalLevelFrameBufferId = GL32.glGetInteger(GL32.GL_FRAMEBUFFER_BINDING);
			}
			catch (Exception | Error e)
			{
				LOGGER.error("Unexpected error in afterLevelRenderEvent: "+e.getMessage(), e);
			}
		}
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	private static LevelAccessor GetEventLevel(LevelEvent e) { return e.getLevel(); }
	
	
}
