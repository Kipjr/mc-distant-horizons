package com.seibel.distanthorizons.common;

import com.mojang.brigadier.CommandDispatcher;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDhInitEvent;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftDedicatedServerWrapper;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.ConfigBase;
import com.seibel.distanthorizons.core.config.eventHandlers.presets.ThreadPresetConfigEventHandler;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.jar.ModJarInfo;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IModAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base for all mod loader initializers 
 * and handles most setup. 
 */
public abstract class AbstractModInitializer
{
	protected static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	
	private CommandDispatcher<CommandSourceStack> commandDispatcher;
	
	
	
	//==================//
	// abstract methods //
	//==================//
	
	protected abstract void createInitialBindings();
	protected abstract IEventProxy createClientProxy();
	protected abstract IEventProxy createServerProxy(boolean isDedicated);
	protected abstract void initializeModCompat();
	
	protected abstract void subscribeRegisterCommandsEvent(Consumer<CommandDispatcher<CommandSourceStack>> eventHandler);
	
	protected abstract void subscribeClientStartedEvent(Runnable eventHandler);
	protected abstract void subscribeServerStartingEvent(Consumer<MinecraftServer> eventHandler);
	protected abstract void runDelayedSetup();
	
	
	
	//===================//
	// initialize events //
	//===================//
	
	public void onInitializeClient()
	{
		DependencySetup.createClientBindings();
		
		LOGGER.info("Initializing " + ModInfo.READABLE_NAME);
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeDhInitEvent.class, null);
		
		this.startup();
		this.printModInfo(true);
		
		this.createClientProxy().registerEvents();
		this.createServerProxy(false).registerEvents();
		
		this.initializeModCompat();
		
		LOGGER.info(ModInfo.READABLE_NAME + " Initialized");
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterDhInitEvent.class, null);
		
		// Client uses config for auto-updater, so it's initialized here instead of post-init stage
		this.initConfig();
		
		this.subscribeClientStartedEvent(this::postInit);
	}
	
	public void onInitializeServer()
	{
		DependencySetup.createServerBindings();
		
		LOGGER.info("Initializing " + ModInfo.READABLE_NAME);
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeDhInitEvent.class, null);
		
		this.startup();
		this.printModInfo(false);
		
		// This prevents returning uninitialized Config values,
		// resulting from a circular reference mid-initialization in a static class
		// noinspection ResultOfMethodCallIgnored
		ThreadPresetConfigEventHandler.INSTANCE.toString();
		
		this.createServerProxy(true).registerEvents();
		
		LOGGER.info(ModInfo.READABLE_NAME + " Initialized");
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterDhInitEvent.class, null);
		
		this.subscribeRegisterCommandsEvent(dispatcher -> { this.commandDispatcher = dispatcher; });
		
		this.subscribeServerStartingEvent(server -> 
		{
			MinecraftDedicatedServerWrapper.INSTANCE.dedicatedServer = (DedicatedServer)server;
			
			this.initConfig();
			this.postInit();
			this.initCommands();
			
			LOGGER.info("Dedicated server initialized at " + server.getServerDirectory());
		});
	}
	
	
	
	//===========================//
	// inner initializer methods //
	//===========================//
	
	private void startup()
	{
		DependencySetup.createSharedBindings();
		SharedApi.init();
		this.createInitialBindings();
	}
	
	private void printModInfo(boolean printGitInfo)
	{
		LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);
		
		if (printGitInfo)
		{
			// Useful for dev builds
			LOGGER.info("DH Branch: " + ModJarInfo.Git_Branch);
			LOGGER.info("DH Commit: " + ModJarInfo.Git_Commit);
			LOGGER.info("DH Jar Build Source: " + ModJarInfo.Build_Source);
		}
	}
	
	protected <T extends IModAccessor> void tryCreateModCompatAccessor(String modId, Class<? super T> accessorClass, Supplier<T> accessorConstructor)
	{
		IModChecker modChecker = SingletonInjector.INSTANCE.get(IModChecker.class);
		if (modChecker.isModLoaded(modId))
		{
			//noinspection unchecked
			ModAccessorInjector.INSTANCE.bind((Class<? extends IModAccessor>) accessorClass, accessorConstructor.get());
		}
	}
	
	private void initConfig()
	{
		ConfigBase.INSTANCE = new ConfigBase(ModInfo.ID, ModInfo.NAME, Config.class, 2);
		Config.completeDelayedSetup();
	}
	
	private void postInit()
	{
		LOGGER.info("Post-Initializing Mod");
		this.runDelayedSetup();
		LOGGER.info("Mod Post-Initialized");
	}
	
	private void initCommands() { /* currently unimplemented */ }
	
	
	
	//================//
	// helper classes //
	//================//
	
	public interface IEventProxy
	{
		void registerEvents();
	}
	
}
