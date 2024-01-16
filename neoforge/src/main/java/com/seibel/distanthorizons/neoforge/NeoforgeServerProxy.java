package com.seibel.distanthorizons.neoforge;

import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;


import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class NeoforgeServerProxy implements AbstractModInitializer.IEventProxy
{
	private static LevelAccessor GetEventLevel(LevelEvent e) { return e.getLevel(); }
	
	private final ServerApi serverApi = ServerApi.INSTANCE;
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private final boolean isDedicated;
	public static Supplier<Boolean> isGenerationThreadChecker = null;
	
	
	//=============//
	// constructor //
	//=============//
	
	public NeoforgeServerProxy(boolean isDedicated)
	{
		this.isDedicated = isDedicated;
		isGenerationThreadChecker = BatchGenerationEnvironment::isCurrentThreadDistantGeneratorThread;
	}
	
	@Override
	public void registerEvents()
	{
		NeoForge.EVENT_BUS.register(this);
	}
	
	
	
	
	//========//
	// events //
	//========//
	
	// ServerTickEvent (at end)
	@SubscribeEvent
	public void serverTickEvent(TickEvent.ServerTickEvent event)
	{
		if (event.phase == TickEvent.Phase.END)
		{
			this.serverApi.serverTickEvent();
		}
	}
	
	// ServerWorldLoadEvent
	@SubscribeEvent
	public void dedicatedWorldLoadEvent(ServerAboutToStartEvent event)
	{
		this.serverApi.serverLoadEvent(this.isDedicated);
	}
	
	// ServerWorldUnloadEvent
	@SubscribeEvent
	public void serverWorldUnloadEvent(ServerStoppingEvent event)
	{
		this.serverApi.serverUnloadEvent();
	}
	
	// ServerLevelLoadEvent
	@SubscribeEvent
	public void serverLevelLoadEvent(LevelEvent.Load event)
	{
		if (GetEventLevel(event) instanceof ServerLevel)
		{
			this.serverApi.serverLevelLoadEvent(this.getServerLevelWrapper((ServerLevel) GetEventLevel(event)));
		}
	}
	
	// ServerLevelUnloadEvent
	@SubscribeEvent
	public void serverLevelUnloadEvent(LevelEvent.Unload event)
	{
		if (GetEventLevel(event) instanceof ServerLevel)
		{
			this.serverApi.serverLevelUnloadEvent(this.getServerLevelWrapper((ServerLevel) GetEventLevel(event)));
		}
	}
	
	@SubscribeEvent
	public void serverChunkLoadEvent(ChunkEvent.Load event)
	{
		ILevelWrapper levelWrapper = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		
		IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetEventLevel(event), levelWrapper);
		this.serverApi.serverChunkLoadEvent(chunk, levelWrapper);
	}
	@SubscribeEvent
	public void serverChunkSaveEvent(ChunkEvent.Unload event)
	{
		ILevelWrapper levelWrapper = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		
		IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetEventLevel(event), levelWrapper);
		this.serverApi.serverChunkSaveEvent(chunk, levelWrapper);
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	private static ServerLevelWrapper getServerLevelWrapper(ServerLevel level) { return ServerLevelWrapper.getWrapper(level); }
	
	
}
