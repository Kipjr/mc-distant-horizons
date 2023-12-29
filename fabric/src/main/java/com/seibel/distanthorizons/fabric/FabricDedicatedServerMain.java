package com.seibel.distanthorizons.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

#if MC_VER >= MC_1_19_2
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;
#else // < 1.19.2
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.network.chat.TranslatableComponent;
#endif

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
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
		
		
		#if MC_VER > MC_1_19_2
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
		#else // < 1.19.2
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
		#endif
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
			if (!(type instanceof ConfigEntry)) continue;
			ConfigEntry configEntry = (ConfigEntry) type;
			if (configEntry.getServersideShortName() == null) continue;
			
			Function<
					Function<CommandContext<CommandSourceStack>, Object>,
					Command<CommandSourceStack>
			> makeConfigUpdater = getter -> c -> {
				Object value = getter.apply(c);
				#if MC_VER >= MC_1_20_1
				c.getSource().sendSuccess(() -> Component.literal("Changed the value of "+configEntry.getServersideShortName()+" to "+value), true);
				#elif MC_VER >= MC_1_19_2
				c.getSource().sendSuccess(Component.literal("Changed the value of "+configEntry.getServersideShortName()+" to "+value), true);
				#else // < 1.19.2
				c.getSource().sendSuccess(new TranslatableComponent("Changed the value of "+configEntry.getServersideShortName()+" to "+value), true);
				#endif
				configEntry.set(value);
				return 1;
			};
			
			LiteralArgumentBuilder<CommandSourceStack> subcommand = literal(configEntry.getServersideShortName())
					.executes(c -> {
						#if MC_VER >= MC_1_20_1
						c.getSource().sendSuccess(() -> Component.literal("Current value of "+configEntry.getServersideShortName()+" is "+configEntry.get()), true);
						#elif MC_VER >= MC_1_19_2
						c.getSource().sendSuccess(Component.literal("Current value of "+configEntry.getServersideShortName()+" is "+configEntry.get()), true);
						#else // < 1.19.2
						c.getSource().sendSuccess(new TranslatableComponent("Current value of "+configEntry.getServersideShortName()+" is "+configEntry.get()), true);
						#endif
						return 1;
					});
			
			if (Enum.class.isAssignableFrom(configEntry.getType()))
			{
				for (Object choice : configEntry.getType().getEnumConstants())
				{
					subcommand.then(
							literal(choice.toString())
									.executes(makeConfigUpdater.apply(c -> choice))
					);
				}
			}
			else
			{
				boolean setterAdded = false;
				
				for (java.util.Map.Entry<Class<?>, Pair<Supplier<ArgumentType<?>>, BiFunction<CommandContext<?>, String, ?>>> pair : new HashMap<
						Class<?>, 
						Pair<
								Supplier<ArgumentType<?>>, 
								BiFunction<CommandContext<?>, String, ?>>
						>() {{
					put(Integer.class, new Pair<>(() -> integer((int) configEntry.getMin(), (int) configEntry.getMax()), IntegerArgumentType::getInteger));
					put(Double.class, new Pair<>(() -> doubleArg((double) configEntry.getMin(), (double) configEntry.getMax()), DoubleArgumentType::getDouble));
					put(Boolean.class, new Pair<>(BoolArgumentType::bool, BoolArgumentType::getBool));
				}}.entrySet())
				{
					if (!pair.getKey().isAssignableFrom(configEntry.getType())) continue;
					
					subcommand.then(argument("value", pair.getValue().first.get())
							.executes(makeConfigUpdater.apply(c -> pair.getValue().second.apply(c, "value"))));
					
					setterAdded = true;
					break;
				}
				
				if (!setterAdded)
					throw new RuntimeException("Config type of "+type.getName()+" is not supported: "+configEntry.getType().getSimpleName());
			}
			
			builder.then(subcommand);
		}
		
		commandDispatcher.register(builder);
	}
	
}
