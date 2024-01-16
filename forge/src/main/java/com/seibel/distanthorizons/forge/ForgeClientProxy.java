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

package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.client.multiplayer.ClientLevel;
#if MC_VER < MC_1_19_2
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
#else
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
#endif

#if MC_VER >= MC_1_18_2
import net.minecraftforge.client.event.RenderLevelStageEvent;
#endif
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.level.chunk.ChunkAccess;

import net.minecraftforge.common.MinecraftForge;
#if MC_VER >= MC_1_20_2
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
#elif MC_VER >= MC_1_18_2
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
#elif MC_VER >= MC_1_17_1
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
#else // < 1.17.1
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
#endif
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL32;

import java.util.function.Predicate;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of the mod.
 *
 * @author James_Seibel
 * @version 2023-7-27
 */
public class ForgeClientProxy implements AbstractModInitializer.IEventProxy
{
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();

	private static SimpleChannel multiversePluginChannel;
	
	
	#if MC_VER < MC_1_19_2
	private static LevelAccessor GetEventLevel(WorldEvent e) { return e.getWorld(); }
	#else
	private static LevelAccessor GetEventLevel(LevelEvent e) { return e.getLevel(); }
	#endif
	
	
	
	@Override
	public void registerEvents()
	{
		MinecraftForge.EVENT_BUS.register(this);
		this.setupNetworkingListeners();
	}
	
	
	
	//=============//
	// tick events //
	//=============//
	
	@SubscribeEvent
	public void clientTickEvent(TickEvent.ClientTickEvent event)
	{
		if (event.phase == TickEvent.Phase.START)
		{
			ClientApi.INSTANCE.clientTickEvent();
		}
	}
	
	
	
	//==============//
	// world events //
	//==============//
	
	@SubscribeEvent
	#if MC_VER < MC_1_19_2
	public void clientLevelLoadEvent(WorldEvent.Load event)
	#else
	public void clientLevelLoadEvent(LevelEvent.Load event)
	#endif
	{
		LOGGER.info("level load");
		
		#if MC_VER < MC_1_19_2
		LevelAccessor level = event.getWorld();
		#else
		LevelAccessor level = event.getLevel();
		#endif
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
	#if MC_VER < MC_1_19_2
	public void clientLevelUnloadEvent(WorldEvent.Unload event)
	#else
	public void clientLevelUnloadEvent(LevelEvent.Load event)
	#endif
	{
		LOGGER.info("level unload");
		
		#if MC_VER < MC_1_19_2
		LevelAccessor level = event.getWorld();
		#else
		LevelAccessor level = event.getLevel();
		#endif
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
		LOGGER.trace("interact or block place event at blockPos: " + event.getPos());
		
		#if MC_VER < MC_1_19_2
		LevelAccessor level = event.getWorld();
		#else
		LevelAccessor level = event.getLevel();
		#endif
		
		ChunkAccess chunk = level.getChunk(event.getPos());
		this.onBlockChangeEvent(level, chunk);
	}
	@SubscribeEvent
	public void leftClickBlockEvent(PlayerInteractEvent.LeftClickBlock event)
	{
		LOGGER.trace("break or block attack at blockPos: " + event.getPos());
		
		#if MC_VER < MC_1_19_2
		LevelAccessor level = event.getWorld();
		#else
		LevelAccessor level = event.getLevel();
		#endif
		
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
	public void registerKeyBindings(#if MC_VER < MC_1_19_2 InputEvent.KeyInputEvent #else InputEvent.Key #endif event)
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
	
	public void setupNetworkingListeners()
	{
		#if MC_VER >= MC_1_20_2
		Channel.VersionTest versionTest = (status, version)
				-> status != Channel.VersionTest.Status.PRESENT || version == ModInfo.MULTIVERSE_PLUGIN_PROTOCOL_VERSION;
		
		multiversePluginChannel = ChannelBuilder.named(new ResourceLocation(ModInfo.NETWORKING_RESOURCE_NAMESPACE, ModInfo.MULTIVERSE_PLUGIN_NAMESPACE))
				.networkProtocolVersion(ModInfo.MULTIVERSE_PLUGIN_PROTOCOL_VERSION)
				.serverAcceptedVersions(versionTest)
				.clientAcceptedVersions(versionTest)
				.simpleChannel();
		
		multiversePluginChannel.messageBuilder(ByteBuf.class, 0)
				.decoder(FriendlyByteBuf::asReadOnly)
				.consumerNetworkThread((nettyByteBuf, contextRef) ->
				{
					ClientApi.INSTANCE.serverMessageReceived(nettyByteBuf);
					contextRef.setPacketHandled(true);
				})
				.add();
		#else // < 1.20.2
		Predicate<String> versionTest = versionString ->
		{
			if (versionString.equals(NetworkRegistry.ABSENT #if MC_VER >= MC_1_19_4 .version() #endif) || versionString.equals(NetworkRegistry.ACCEPTVANILLA))
			{
				// allow using networking on vanilla servers or if DH isn't installed on the server
				return true;
			}
			
			try
			{
				int version = Integer.parseInt(versionString);
				return ModInfo.MULTIVERSE_PLUGIN_PROTOCOL_VERSION == version;
			}
			catch (NumberFormatException ignored)
			{
				return false;
			}
		};
		
		multiversePluginChannel = NetworkRegistry.newSimpleChannel(
				new ResourceLocation(ModInfo.NETWORKING_RESOURCE_NAMESPACE, ModInfo.MULTIVERSE_PLUGIN_NAMESPACE),
				// network protocol version
				() -> ModInfo.MULTIVERSE_PLUGIN_PROTOCOL_VERSION +"",
				// client accepted versions
				versionTest,
				// server accepted versions
				versionTest
		);
		
		multiversePluginChannel.registerMessage(0/*should be incremented for each simple channel we listen to*/, ByteBuf.class,
				// encoder
				(pack, friendlyByteBuf) -> { },
				// decoder
				FriendlyByteBuf::asReadOnly,
				// message consumer
				(nettyByteBuf, contextRef) ->
				{
					ClientApi.INSTANCE.serverMessageReceived(nettyByteBuf);
					contextRef.get().setPacketHandled(true);
				}
		);
		#endif
	}
	
	
	//===========//
	// rendering //
	//===========//
	
	@SubscribeEvent
	#if MC_VER >= MC_1_18_2
	public void afterLevelRenderEvent(RenderLevelStageEvent event)
	#else
	public void afterLevelRenderEvent(TickEvent.RenderTickEvent event)
	#endif
	{
		#if MC_VER >= MC_1_20_1
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL)
		#elif MC_VER >= MC_1_18_2
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS)
		#else
		// FIXME: Is this the correct location for 1.16 & 1.17???
		// I couldnt find anything for rendering after the level, so is rendering after overlays ok?
		if (event.type.equals(TickEvent.RenderTickEvent.Type.WORLD))
		#endif
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
	
}
