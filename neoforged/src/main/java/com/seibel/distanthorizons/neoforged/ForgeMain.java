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

package com.seibel.distanthorizons.neoforged;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDhInitEvent;
import com.seibel.distanthorizons.common.LodCommonMain;
import com.seibel.distanthorizons.common.forge.LodForgeMethodCaller;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.common.wrappers.gui.GetConfigScreen;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.distanthorizons.core.jar.ModJarInfo;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractOptifineAccessor;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.distanthorizons.neoforged.wrappers.ForgeDependencySetup;

import com.seibel.distanthorizons.neoforged.wrappers.modAccessor.OptifineAccessor;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.*;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;

import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Initialize and setup the Mod. <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 *
 * @author coolGi
 * @author Ran
 * @author James Seibel
 * @version 8-15-2022
 */
@Mod(ModInfo.ID)
public class ForgeMain implements LodForgeMethodCaller
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	public static ForgeClientProxy client_proxy = null;
	public static ForgeServerProxy server_proxy = null;
	
	public ForgeMain()
	{
		DependencySetup.createClientBindings();

//		initDedicated(null);
//		initDedicated(null);
		// Register the mod initializer (Actual event registration is done in the different proxies)
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initClient);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initDedicated);
	}
	
	private void initClient(final FMLClientSetupEvent event)
	{
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeDhInitEvent.class, null);
		
		LOGGER.info("Initializing Mod");
		LodCommonMain.startup(this);
		ForgeDependencySetup.createInitialBindings();
		LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);
		
		// Print git info (Useful for dev builds)
		LOGGER.info("DH Branch: " + ModJarInfo.Git_Branch);
		LOGGER.info("DH Commit: " + ModJarInfo.Git_Commit);
		LOGGER.info("DH Jar Build Source: " + ModJarInfo.Build_Source);
		
		client_proxy = new ForgeClientProxy();
		NeoForge.EVENT_BUS.register(client_proxy);
		server_proxy = new ForgeServerProxy(false);
		NeoForge.EVENT_BUS.register(server_proxy);
		
		if (AbstractOptifineAccessor.optifinePresent())
		{
			ModAccessorInjector.INSTANCE.bind(IOptifineAccessor.class, new OptifineAccessor());
		}

		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> GetConfigScreen.getScreen(parent)));
		
		ForgeClientProxy.setupNetworkingListeners(event);
		
		LOGGER.info(ModInfo.READABLE_NAME + " Initialized");
		
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterDhInitEvent.class, null);
		
		// Init config
		// The reason im initialising in this rather than the post init process is cus im using this for the auto updater
		LodCommonMain.initConfig();
	}
	
	private void initDedicated(final FMLDedicatedServerSetupEvent event)
	{
//		DependencySetup.createServerBindings();
//		initCommon();

//		server_proxy = new ForgeServerProxy(true);
//		MinecraftForge.EVENT_BUS.register(server_proxy);
//
		postInitCommon();
	}
	
	private void postInitCommon()
	{
		LOGGER.info("Post-Initializing Mod");
		ForgeDependencySetup.runDelayedSetup();
		
		LOGGER.info("Mod Post-Initialized");
	}
	
	private final ModelData modelData = ModelData.EMPTY;
	
	@Override
	public List<BakedQuad> getQuads(MinecraftClientWrapper mc, Block block, BlockState blockState, Direction direction, RandomSource random)
	{
		return mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random, modelData, RenderType.solid() );
	}
	
	@Override //TODO: Check this if its still needed
	public int colorResolverGetColor(ColorResolver resolver, Biome biome, double x, double z)
	{
		return resolver.getColor(biome, x, z);
	}
	
}
