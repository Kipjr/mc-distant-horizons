package com.seibel.distanthorizons.fabric.testing;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.AbstractDhApiChunkWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiChunk;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;

public class TestWorldGenerator extends AbstractDhApiChunkWorldGenerator
{
	private final ServerLevel level;
	private final IDhApiLevelWrapper levelWrapper;
	
	public TestWorldGenerator(ServerLevel level)
	{
		this.level = level;
		this.levelWrapper = ServerLevelWrapper.getWrapper(level);
	}
	
	
	@Override
	public EDhApiWorldGeneratorReturnType getReturnType() { return EDhApiWorldGeneratorReturnType.API_CHUNKS; }
	
	@Override
	public boolean isBusy() { return false; }
	
	
	@Override
	public Object[] generateChunk(int chunkX, int chunkZ, EDhApiDistantGeneratorMode eDhApiDistantGeneratorMode)
	{
		ChunkAccess chunk = this.level.getChunk(chunkX, chunkZ);
		return new Object[] { chunk, this.level };
	}
	
	@Override
	public DhApiChunk generateApiChunk(int chunkPosX, int chunkPosZ, EDhApiDistantGeneratorMode generatorMode)
	{
		ChunkAccess chunk = this.level.getChunk(chunkPosX, chunkPosZ);
		
		int minBuildHeight = chunk.getMinBuildHeight();
		int maxBuildHeight = chunk.getMaxBuildHeight();
		
		DhApiChunk apiChunk = DhApiChunk.create(chunkPosX, chunkPosZ, minBuildHeight, maxBuildHeight);
		for (int x = 0; x < 16; x++)
		{
			for (int z = 0; z < 16; z++)
			{
				ArrayList<DhApiTerrainDataPoint> dataPoints = new ArrayList<>();
				
				IDhApiBlockStateWrapper block = null;
				IDhApiBiomeWrapper biome = null;
				
				for (int y = minBuildHeight; y < maxBuildHeight; y++)
				{
					block = DhApi.Delayed.wrapperFactory.getBlockStateWrapper(new Object[]{chunk.getBlockState(new BlockPos(x, y, z))}, this.levelWrapper);
					biome = DhApi.Delayed.wrapperFactory.getBiomeWrapper(new Object[]{chunk.getNoiseBiome(x, y, z)}, this.levelWrapper);
					dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, 15, y, y + 1, block, biome));
				}
				
				apiChunk.setDataPoints(x, z, dataPoints);
			}
		}
		return apiChunk;
	}
	
	@Override
	public void preGeneratorTaskStart() { /* do nothing */ }
	
	@Override
	public void close() { /* do nothing */ }
	
}
