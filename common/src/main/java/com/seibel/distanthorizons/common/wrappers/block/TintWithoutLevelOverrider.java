/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
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

package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.core.util.ColorUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

#if MC_VER >= MC_1_18_2
import net.minecraft.core.Holder;
#endif

public class TintWithoutLevelOverrider implements BlockAndTintGetter
{
	/** 
	 * This will only ever be null if there was an issue with {@link IClientLevelWrapper#getPlainsBiomeWrapper()}
	 * but {@link Nullable} is there just in case. 
	 */
	@Nullable
	#if MC_VER >= MC_1_18_2
	public final Holder<Biome> biome;
	#else
	public final Biome biome;
	#endif
	
	/**
	 * Constructs the TintWithoutLevelOverrider, storing the provided Biome Holder for late-binding access.
	 *
	 * <p>Previously, this class might have immediately unwrapped the Holder like this:</p>
	 * <pre>{@code
	 * // Inside constructor (OLD WAY - PROBLEMATIC):
	 * Holder<Biome> biomeHolder = getTheHolderFromSomewhere();
	 * this.biome = biomeHolder.value(); // <-- PROBLEM HERE
	 * }</pre>
	 *
	 * <p>This approach is problematic because the {@link net.minecraft.core.Holder} system,
	 * particularly {@code Holder.Reference}, is designed for <strong>late binding</strong>. Here's why storing
	 * the Holder itself is now necessary:</p>
	 * <ol>
	 *   <li>A {@code Holder.Reference<Biome>} might be created initially just with a
	 *       {@link net.minecraft.resources.ResourceKey} (like {@code minecraft:plains}), but its actual
	 *       {@link net.minecraft.core.Holder#value() value()} (the {@code Biome} object itself) might be {@code null}
	 *       at construction time.</li>
	 *   <li>Later, during game loading, registry population, or potentially due to modifications by other mods
	 *       (e.g., Polytone), the system calls internal binding methods (like {@code bindValue(Biome)})
	 *       on the {@code Holder} instance. This sets or <strong>updates</strong> the internal reference to the
	 *       actual {@code Biome} object.</li>
	 *   <li>Crucially, the binding process might assign a completely <strong>new</strong> {@code Biome} object
	 *       instance to the {@code Holder} reference, replacing any previous one.</li>
	 * </ol>
	 *
	 * <p>If we unwrapped the {@code Holder} using {@code .value()} within the constructor (the old way),
	 * our class's internal {@code biome} field would permanently store a reference to whatever {@code Biome}
	 * object the {@code Holder} pointed to *at that exact moment*. It would have no link back to the
	 * {@code Holder} and would be unaware if the {@code Holder} was later updated to point to a different
	 * (or the initially missing) {@code Biome} object. This would lead to using stale or even {@code null} data.</p>
	 *
	 * <p>By storing the {@code Holder<Biome>} itself, this class can call {@link net.minecraft.core.Holder#value()}
	 * whenever the biome information is needed, ensuring it always retrieves the most current {@code Biome}
	 * instance associated with the holder at that time.</p>
	 */
	public TintWithoutLevelOverrider(BiomeWrapper biomeWrapper, IClientLevelWrapper clientLevelWrapper)
	{
		#if MC_VER >= MC_1_18_2 Holder<Biome> #else Biome #endif biome = biomeWrapper.biome;
		if (biome == null) // We are looking at the empty biome wrapper
		{
			BiomeWrapper plainsBiomeWrapper = ((BiomeWrapper) clientLevelWrapper.getPlainsBiomeWrapper());
			if (plainsBiomeWrapper != null)
			{
				biome = plainsBiomeWrapper.biome;
			}
		}
		
		this.biome = biome;
	}
	
	
	
	@Override
	public int getBlockTint(@NotNull BlockPos blockPos, @NotNull ColorResolver colorResolver)
	{
		if (this.biome == null)
		{
			// hopefully unneeded debug color
			return ColorUtil.CYAN;
		}
		return colorResolver.getColor(unwrap(biome), blockPos.getX(), blockPos.getZ());
	}
	
	private static Biome unwrap(#if MC_VER >= MC_1_18_2 Holder<Biome> #else Biome #endif biome)
	{
		#if MC_VER >= MC_1_18_2
		return biome.value();
		#else
		return biome;
		#endif
	}
	
	
	
	//================//
	// unused methods //
	//================//
	
	@Override
	public float getShade(@NotNull Direction direction, boolean shade)
	{
		throw new UnsupportedOperationException("ERROR: getShade() called on TintWithoutLevelOverrider. Object is for tinting only.");
	}
	@Override
	public @NotNull LevelLightEngine getLightEngine()
	{
		throw new UnsupportedOperationException("ERROR: getLightEngine() called on TintWithoutLevelOverrider. Object is for tinting only.");
	}
	@Nullable
	@Override
	public BlockEntity getBlockEntity(@NotNull BlockPos pos)
	{
		throw new UnsupportedOperationException("ERROR: getBlockEntity() called on TintWithoutLevelOverrider. Object is for tinting only.");
	}
	
	@Override
	public @NotNull BlockState getBlockState(@NotNull BlockPos pos)
	{
		throw new UnsupportedOperationException("ERROR: getBlockState() called on TintWithoutLevelOverrider. Object is for tinting only.");
	}
	@Override
	public @NotNull FluidState getFluidState(@NotNull BlockPos pos)
	{
		throw new UnsupportedOperationException("ERROR: getFluidState() called on TintWithoutLevelOverrider. Object is for tinting only.");
	}
	
	
	
	//==============//
	// post MC 1.17 //
	//==============//
	
	#if MC_VER >= MC_1_17_1
	
	@Override
	public int getHeight()
	{ throw new UnsupportedOperationException("ERROR: getHeight() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	
	#if MC_VER < MC_1_21_3
	@Override
	public int getMinBuildHeight() 
	{ throw new UnsupportedOperationException("ERROR: getMinBuildHeight() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	#else
	@Override
	public int getMinY()
	{ throw new UnsupportedOperationException("ERROR: getMinY() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	#endif
	
	#endif
	
}
