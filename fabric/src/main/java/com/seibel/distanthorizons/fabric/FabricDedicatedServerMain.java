package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.LodCommonMain;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftDedicatedServerWrapper;
import com.seibel.distanthorizons.core.config.eventHandlers.presets.ThreadPresetConfigEventHandler;
import com.seibel.distanthorizons.core.util.LodUtil;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

#if MC_VER >= MC_1_19_2
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
#else // < 1.19.2
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
#endif

@Environment(EnvType.SERVER)
public class FabricDedicatedServerMain implements DedicatedServerModInitializer
{
	private static final Logger LOGGER = LogManager.getLogger(FabricDedicatedServerMain.class.getSimpleName());
	private static final ResourceLocation INITIAL_PHASE = ResourceLocation.tryParse("distanthorizons:dedicated_server_initial");
	
	public static FabricServerProxy server_proxy;
	public boolean hasPostSetupDone = false;
	
	
	@Override
	public void onInitializeServer()
	{
		DependencySetup.createServerBindings();
		FabricMain.init();
		
		// FIXME this prevents returning uninitialized Config values
		//  resulting from a circular reference mid-initialization in a static class
		//noinspection ResultOfMethodCallIgnored
		ThreadPresetConfigEventHandler.INSTANCE.toString();
		
		server_proxy = new FabricServerProxy(true);
		server_proxy.registerEvents();
		
		
		#if MC_VER >= MC_1_19_2
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
		#else // < 1.19.2
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
		#endif
			LodCommonMain.commandDispatcher = dispatcher;
		});
		
		ServerLifecycleEvents.SERVER_STARTING.addPhaseOrdering(INITIAL_PHASE, Event.DEFAULT_PHASE);
		ServerLifecycleEvents.SERVER_STARTING.register(INITIAL_PHASE, (server) ->
		{
			if (this.hasPostSetupDone)
			{
				return;
			}
			
			this.hasPostSetupDone = true;
			LodUtil.assertTrue(server instanceof DedicatedServer);
			
			MinecraftDedicatedServerWrapper.INSTANCE.dedicatedServer = (DedicatedServer) server;
			LodCommonMain.initConfig();
			FabricMain.postInit();
			LodCommonMain.initCommands();
			
			LOGGER.info("Dedicated server initialized at " + server.getServerDirectory());
		});
	}
	
}
