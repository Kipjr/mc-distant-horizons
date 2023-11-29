package com.seibel.distanthorizons.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.seibel.distanthorizons.common.LodCommonMain;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftDedicatedServerWrapper;
import com.seibel.distanthorizons.core.config.ConfigBase;
import com.seibel.distanthorizons.core.config.eventHandlers.presets.ThreadPresetConfigEventHandler;
import com.seibel.distanthorizons.core.config.types.AbstractConfigType;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.objects.Pair;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Environment(EnvType.SERVER)
public class FabricDedicatedServerMain implements DedicatedServerModInitializer
{
	private static final Logger LOGGER = LogManager.getLogger(FabricDedicatedServerMain.class.getSimpleName());
	private static final ResourceLocation INITIAL_PHASE = ResourceLocation.tryParse("distanthorizons:dedicated_server_initial");
	
	public static FabricServerProxy server_proxy;
	public boolean hasPostSetupDone = false;
	
	private CommandDispatcher<CommandSourceStack> commandDispatcher;
	
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
		
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			this.commandDispatcher = dispatcher;
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
			this.initCommands();
			
			LOGGER.info("Dedicated server initialized at " + server.getServerDirectory());
		});
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void initCommands()
	{
		LiteralArgumentBuilder<CommandSourceStack> builder = literal("dhconfig")
				.requires(source -> source.hasPermission(4));
		
		for (AbstractConfigType<?, ?> type : ConfigBase.INSTANCE.entries)
		{
			if (!(type instanceof ConfigEntry configEntry)) continue;
			if (configEntry.getServersideShortName() == null) continue;
			
			Function<
					Function<CommandContext<CommandSourceStack>, Object>,
					Command<CommandSourceStack>
			> makeConfigUpdater = getter -> c -> {
				var value = getter.apply(c);
				c.getSource().sendSuccess(() -> Component.literal("Changed the value of "+configEntry.getServersideShortName()+" to "+value), true);
				configEntry.set(value);
				return 1;
			};
			
			var subcommand = literal(configEntry.getServersideShortName())
					.executes(c -> {
						c.getSource().sendSuccess(() -> Component.literal("Current value of "+configEntry.getServersideShortName()+" is {}"+configEntry.get()), true);
						return 1;
					});
			
			if (Enum.class.isAssignableFrom(configEntry.getType()))
			{
				for (var choice : configEntry.getType().getEnumConstants())
				{
					subcommand.then(
							literal(choice.toString())
									.executes(makeConfigUpdater.apply(c -> choice))
					);
				}
			}
			else
			{
				for (var pair : new HashMap<
						Class<?>, 
						Pair<
								Supplier<ArgumentType<?>>, 
								BiFunction<CommandContext<?>, String, ?>>
						>() {{
					put(Integer.class, new Pair<>(() -> integer((int) configEntry.getMin(), (int) configEntry.getMax()), IntegerArgumentType::getInteger));
					put(Boolean.class, new Pair<>(BoolArgumentType::bool, BoolArgumentType::getBool));
				}}.entrySet())
				{
					if (!pair.getKey().isAssignableFrom(configEntry.getType())) continue;
					
					subcommand.then(argument("value", pair.getValue().first.get())
							.executes(makeConfigUpdater.apply(c -> pair.getValue().second.apply(c, "value"))));
				}
			}
			
			builder.then(subcommand);
		}
		
		commandDispatcher.register(builder);
	}
	
}
