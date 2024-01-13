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

import com.mojang.brigadier.CommandDispatcher;
import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.IEventProxy;
import com.seibel.distanthorizons.common.wrappers.gui.GetConfigScreen;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.neoforge.wrappers.NeoforgeDependencySetup;
import com.seibel.distanthorizons.neoforge.wrappers.modAccessor.OptifineAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.function.Consumer;

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
@SuppressWarnings("unused")
public class NeoforgeMain extends AbstractModInitializer
{
	public NeoforgeMain(IEventBus eventBus)
	{
		eventBus.addListener((FMLClientSetupEvent e) -> this.onInitializeClient());
		eventBus.addListener((FMLDedicatedServerSetupEvent e) -> this.onInitializeServer());
	}
	
	@Override
	protected void createInitialBindings()
	{
		NeoforgeDependencySetup.createInitialBindings();
	}
	
	@Override
	protected IEventProxy createClientProxy()
	{
		return new NeoforgeClientProxy();
	}
	
	@Override
	protected IEventProxy createServerProxy(boolean isDedicated)
	{
		return new NeoforgeServerProxy(isDedicated);
	}
	
	@Override
	protected void initializeModCompat()
	{
		this.tryCreateModCompatAccessor("optifine", IOptifineAccessor.class, OptifineAccessor::new);
		
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> GetConfigScreen.getScreen(parent)));
	}
	
	@Override
	protected void subscribeRegisterCommandsEvent(Consumer<CommandDispatcher<CommandSourceStack>> eventHandler)
	{
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> {
			eventHandler.accept(e.getDispatcher());
		});
	}
	
	@Override
	protected void subscribeClientStartedEvent(Runnable eventHandler)
	{
		// FIXME Why it's unused?
	}
	
	@Override
	protected void subscribeServerStartingEvent(Consumer<MinecraftServer> eventHandler)
	{
		NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> {
			eventHandler.accept(e.getServer());
		});
	}
	
	@Override
	protected void runDelayedSetup()
	{
		NeoforgeDependencySetup.runDelayedSetup();
	}
	
}
