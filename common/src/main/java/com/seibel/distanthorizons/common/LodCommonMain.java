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

package com.seibel.distanthorizons.common;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.seibel.distanthorizons.common.forge.LodForgeMethodCaller;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.core.config.types.AbstractConfigType;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.core.util.objects.Pair;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.ConfigBase;
import net.minecraft.commands.CommandSourceStack;

#if MC_VER >= MC_1_19_2
import net.minecraft.network.chat.Component;
#else // < 1.19.2
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

/**
 * This is the common main class
 *
 * @author Ran
 */
public class LodCommonMain
{
	public static boolean forge = false;
	public static LodForgeMethodCaller forgeMethodCaller;
	public static CommandDispatcher<CommandSourceStack> commandDispatcher;
	
	
	public static void startup(LodForgeMethodCaller forgeMethodCaller)
	{
		if (forgeMethodCaller != null)
		{
			LodCommonMain.forge = true;
			LodCommonMain.forgeMethodCaller = forgeMethodCaller;
		}
		
		DependencySetup.createSharedBindings();
		SharedApi.init();
	}
	
	public static void initConfig()
	{
		ConfigBase.INSTANCE = new ConfigBase(ModInfo.ID, ModInfo.NAME, Config.class, 2);
		Config.completeDelayedSetup();
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void initCommands()
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
