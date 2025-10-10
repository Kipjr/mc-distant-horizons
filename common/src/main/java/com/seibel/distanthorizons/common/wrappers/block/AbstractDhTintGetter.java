package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPosMutable;
import com.seibel.distanthorizons.core.util.ColorUtil;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

#if MC_VER >= MC_1_18_2
import net.minecraft.core.Holder;
#endif


public abstract class AbstractDhTintGetter implements BlockAndTintGetter
{
	protected final BiomeWrapper biomeWrapper;
	
	protected final int smoothingRadiusInBlocks;
	protected final FullDataSourceV2 fullDataSource;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public AbstractDhTintGetter(BiomeWrapper biomeWrapper, FullDataSourceV2 fullDataSource)
	{
		this.biomeWrapper = biomeWrapper;
		this.fullDataSource = fullDataSource;
		this.smoothingRadiusInBlocks = Config.Client.Advanced.Graphics.Quality.lodBiomeBlending.get();
	}
	
	
	
	//================//
	// shared methods //
	//================//
	
	@Override
	public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver)
	{
		// determine how wide this data source is so we can determine
		// if blending should be used
		byte dataSourceDetailLevel = DhSectionPos.getDetailLevel(this.fullDataSource.getPos());
		// convert from section detail level to absolute detail level
		dataSourceDetailLevel = (byte)(dataSourceDetailLevel - DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
		int dataSourceLodWidthInBlocks = DhSectionPos.getDetailLevelWidthInBlocks(dataSourceDetailLevel);
		
		// don't do any smoothing if smoothing is disabled or if the LOD
		// is to large for block-based smoothing to show up
		if (this.smoothingRadiusInBlocks == 0
			|| dataSourceLodWidthInBlocks > this.smoothingRadiusInBlocks)
		{
			return colorResolver.getColor(this.unwrapBiome(this.biomeWrapper.biome), blockPos.getX(), blockPos.getZ());
		}
		
		
		// use a rolling average to calculate the color
		int dataPointCount = 0;
		int rollingRed = 0;
		int rollingGreen = 0;
		int rollingBlue = 0;
		
		int xMin = blockPos.getX() - this.smoothingRadiusInBlocks;
		int xMax = blockPos.getX() + this.smoothingRadiusInBlocks;
		
		int zMin = blockPos.getZ() - this.smoothingRadiusInBlocks;
		int zMax = blockPos.getZ() + this.smoothingRadiusInBlocks;
		
		DhBlockPosMutable mutableBlockPos = new DhBlockPosMutable(0, blockPos.getY(), 0);
		for (int x = xMin; x < xMax; x++)
		{
			for (int z = zMin; z < zMax; z++)
			{
				mutableBlockPos.setX(x);
				mutableBlockPos.setZ(z);
				
				// this can return the same position/datapoint for larger LODs duplicating work,
				// however for small smoothing ranges that isn't a big deal and for large LODs
				// we ignore smoothing anyway
				long dataPoint = this.fullDataSource.getAtBlockPos(mutableBlockPos);
				if (dataPoint == FullDataPointUtil.EMPTY_DATA_POINT)
				{
					continue;
				}
				
				// get the color for this nearby position
				int id = FullDataPointUtil.getId(dataPoint);
				BiomeWrapper biomeWrapper = (BiomeWrapper) this.fullDataSource.mapping.getBiomeWrapper(id);
				int color = colorResolver.getColor(this.unwrapBiome(biomeWrapper.biome), mutableBlockPos.getX(), mutableBlockPos.getZ());
				
				
				// rolling average
				rollingRed += ColorUtil.getRed(color);
				rollingGreen += ColorUtil.getGreen(color);
				rollingBlue += ColorUtil.getBlue(color);
				
				dataPointCount++;
			}
		}
		
		
		// if no data was present (rarely possible)
		// just use the default center's color
		if (dataPointCount == 0)
		{
			return colorResolver.getColor(this.unwrapBiome(this.biomeWrapper.biome), blockPos.getX(), blockPos.getZ());
		}
		
		int colorInt = ColorUtil.argbToInt(
				255, // blending often ignores alpha, having it always 255 prevents multiplication issues later
				rollingRed / dataPointCount,
				rollingGreen / dataPointCount,
				rollingBlue / dataPointCount);
		return colorInt;
	}
	
	protected Biome unwrapBiome(#if MC_VER >= MC_1_18_2 Holder<Biome> #else Biome #endif biome)
	{
		#if MC_VER >= MC_1_18_2
		return biome.value();
		#else
		return biome;
		#endif
	}
	
	
	
}
