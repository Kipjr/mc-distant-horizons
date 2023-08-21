package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

#if MC_1_16_5 || MC_1_17_1
import net.minecraft.core.Registry;
#elif MC_1_18_2 || MC_1_19_2
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
#else
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.EmptyBlockGetter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
#endif

public class BlockStateWrapper implements IBlockStateWrapper
{
	/** example "minecraft:plains" */
	public static final String RESOURCE_LOCATION_SEPARATOR = ":";
	/** example "minecraft:water_STATE_{level:0}" */
	public static final String STATE_STRING_SEPARATOR = "_STATE_";
	
	
	// must be defined before AIR, otherwise a null pointer will be thrown
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    public static final ConcurrentHashMap<BlockState, BlockStateWrapper> cache = new ConcurrentHashMap<>();
	public static final BlockStateWrapper AIR = fromBlockState(BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("minecraft:air")).defaultBlockState(), null, false);
	public static final String[] RENDERER_IGNORED_BLOCKS_RESOURCE_LOCATIONS = {"minecraft:air", "minecraft:barrier", "minecraft:structure_void", "minecraft:light"};
	public static final HashMap<BlockState, String> RENDERER_IGNORED_BLOCKS_INTERNAL = getRendererIgnoredBlocksInternal(RENDERER_IGNORED_BLOCKS_RESOURCE_LOCATIONS);
	public static final HashMap<String, ? extends IBlockStateWrapper> RENDERER_IGNORED_BLOCKS = getRendererIgnoredBlocks(RENDERER_IGNORED_BLOCKS_INTERNAL);
	
	/**
	 * Cached so it can be quickly used as a semi-stable hashing method. <br>
	 * This may also fix the issue where we can serialize and save after a level has been shut down.
	 */
	private String serializationResult = null;
	
	
	//==============//
	// constructors //
	//==============//
	
    public static BlockStateWrapper fromBlockState(BlockState blockState, @NotNull ILevelWrapper levelWrapper)
	{
		return fromBlockState(blockState, levelWrapper, true);
    }
	
	private static BlockStateWrapper fromBlockState(BlockState blockState, @Nullable ILevelWrapper levelWrapper, boolean nullCheck)
	{
		if (Objects.requireNonNull(blockState).isAir() && AIR != null)
			return AIR;
		
		return cache.computeIfAbsent(blockState, blockState1 -> new BlockStateWrapper(blockState1, levelWrapper, nullCheck));
	}
	
	/**
	 * Only meant for use in the {@code RENDERER_IGNORED_BLOCKS_INTERNAL} list. Do not use elsewhere, since the {@code levelWrapper} parameter of each {@code IBlockStateWrapper} will be {@code null}, causing issues.
	 * @param resourceLocations The resource location(s) of the block(s) that should be ignored by the renderer, may only contain {@code [a-z0-9/._-)} characters.
	 * @return The default blockstate(s) of the block(s), paired with the serialized resource location(s) of the block(s), which should be passed into the {@code RENDERER_IGNORED_BLOCKS_INTERNAL} map and the {@code getRendererIgnoredBlocks} method.
	 */
	@SuppressWarnings("SameParameterValue")
	private static @NotNull HashMap<BlockState, String> getRendererIgnoredBlocksInternal(String @NotNull ... resourceLocations)
	{
		HashMap<BlockState, String> blockStates = new HashMap<>();
		
		for (String resourceLocation : resourceLocations)
		{
			ResourceLocation fetchedResourceLocation = Objects.requireNonNull(ResourceLocation.tryParse(resourceLocation), String.format("Supplied a resource location that couldn't be parsed by Minecraft: %s", resourceLocation));
			var splitResourceLocation = resourceLocation.split(":");
			
			if (splitResourceLocation.length == 0) {
				LOGGER.warn("A resource location that should be ignored by the renderer was in an invalid format: {}", resourceLocation);
				continue;
			}
			
			blockStates.put(BuiltInRegistries.BLOCK.get(fetchedResourceLocation).defaultBlockState(), splitResourceLocation[1].toUpperCase(Locale.ROOT));
		}
		
		return blockStates;
	}
	
	/**
	 * Only meant for use in the {@code RENDERER_IGNORED_BLOCKS} list. Do not use elsewhere, since the {@code levelWrapper} parameter of each {@code IBlockStateWrapper} will be {@code null}, causing issues.
	 * @param rendererIgnoredBlocks A map containing the blockstate(s) of the block(s), paired with the resource location(s) of the block(s) that should be ignored by the renderer.
	 * @return The blockstate wrapper(s) of the blockstate(s), which should be passed into the {@code RENDERER_IGNORED_BLOCKS} list.
	 */
	@SuppressWarnings("SameParameterValue")
	private static @NotNull HashMap<String, ? extends IBlockStateWrapper> getRendererIgnoredBlocks(@NotNull Map<BlockState, String> rendererIgnoredBlocks)
	{
		HashMap<String, BlockStateWrapper> blockStateWrappers = new HashMap<>();
		
		for (Map.Entry<BlockState, String> blockStateResourceLocations : rendererIgnoredBlocks.entrySet())
		{
			blockStateWrappers.put(blockStateResourceLocations.getValue(), fromBlockState(blockStateResourceLocations.getKey(), null, false));
		}
		
		return blockStateWrappers;
	}
	
    public final BlockState blockState;
	@CheckForNull
	public final ILevelWrapper levelWrapper;
	
    BlockStateWrapper(BlockState blockState, @Nullable ILevelWrapper levelWrapper)
    {
        this(blockState, levelWrapper, true);
    }
	
	private BlockStateWrapper(BlockState blockState, @Nullable ILevelWrapper levelWrapper, boolean nullCheck) {
		this.blockState = blockState;
		
		if (nullCheck)
		{
			this.levelWrapper = RENDERER_IGNORED_BLOCKS_INTERNAL.containsKey(blockState)
					? null
					: Objects.requireNonNull(levelWrapper);
		}
		else
		{
			this.levelWrapper = levelWrapper;
		}
		
		LOGGER.trace("Created BlockStateWrapper for [{}]", blockState);
	}
	
	
	
	//=========//
	// methods //
	//=========//
	
	@Override
	public int getOpacity()
	{
		// this method isn't perfect, but works well enough for our use case
		if (this.isAir() || !this.blockState.canOcclude())
		{
			// completely transparent
			return 0;
		}
		else
		{
			// completely opaque
			return 16;
		}
	}
	
	@Override
	public int getLightEmission() { return (this.blockState != null) ? this.blockState.getLightEmission() : 0; }
	
	@Override
    public String serialize()
	{
		// the result can be quickly used as a semi-stable hashing method, so it's going to be cached
		if (this.serializationResult != null)
			return this.serializationResult;
		
		if (RENDERER_IGNORED_BLOCKS_INTERNAL.containsKey(this.blockState))
			return this.serializationResult = RENDERER_IGNORED_BLOCKS_INTERNAL.get(this.blockState);
		
		Objects.requireNonNull(levelWrapper);
		RegistryAccess registryAccess = ((Level) levelWrapper.getWrappedMcObject()).registryAccess();
		
		ResourceLocation resourceLocation;
		#if MC_1_16_5 || MC_1_17_1
		resourceLocation = Registry.BLOCK.getKey(this.blockState.getBlock());
		#elif MC_1_18_2 || MC_1_19_2
		resourceLocation = registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY).getKey(this.blockState.getBlock());
		#else
		resourceLocation = registryAccess.registryOrThrow(Registries.BLOCK).getKey(this.blockState.getBlock());
		#endif
		Objects.requireNonNull(resourceLocation);
		
		this.serializationResult = resourceLocation.getNamespace() + RESOURCE_LOCATION_SEPARATOR + resourceLocation.getPath()
				+ STATE_STRING_SEPARATOR + serializeBlockStateProperties(this.blockState);

		return this.serializationResult;
	}

	@Override
	@Nullable
	public ILevelWrapper getLevelWrapper() {
		return levelWrapper;
	}

	public static IBlockStateWrapper deserialize(String resourceStateString, ILevelWrapper levelWrapper) throws IOException
	{
		if (resourceStateString.isEmpty())
			throw new IOException("resourceStateString is empty");
		
		if (RENDERER_IGNORED_BLOCKS_INTERNAL.containsValue(resourceStateString))
			return RENDERER_IGNORED_BLOCKS.get(resourceStateString);
		
		// Parse the BlockState
		int stateSeparatorIndex = resourceStateString.indexOf(STATE_STRING_SEPARATOR);
		if (stateSeparatorIndex == -1)
		{
			throw new IOException("Unable to parse BlockState out of string: [" + resourceStateString + "].");
		}
		String blockStatePropertiesString = resourceStateString.substring(stateSeparatorIndex + STATE_STRING_SEPARATOR.length());
		resourceStateString = resourceStateString.substring(0, stateSeparatorIndex);
		
		// parse the resource location
		int resourceSeparatorIndex = resourceStateString.indexOf(RESOURCE_LOCATION_SEPARATOR);
		if (resourceSeparatorIndex == -1)
		{
			throw new IOException("Unable to parse Resource Location out of string: [" + resourceStateString + "].");
		}
		ResourceLocation resourceLocation = new ResourceLocation(resourceStateString.substring(0, resourceSeparatorIndex), resourceStateString.substring(resourceSeparatorIndex + 1));
		
		
		
		// attempt to get the BlockState from all possible BlockStates
		try
		{
			Block block;
			#if MC_1_16_5 || MC_1_17_1
			block = Registry.BLOCK.get(resourceLocation);
			#elif MC_1_18_2 || MC_1_19_2
			net.minecraft.core.RegistryAccess registryAccess = ((Level)levelWrapper.getWrappedMcObject()).registryAccess();
			block = registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY).get(resourceLocation);
			#else
			net.minecraft.core.RegistryAccess registryAccess = ((Level)levelWrapper.getWrappedMcObject()).registryAccess();
			block = registryAccess.registryOrThrow(Registries.BLOCK).get(resourceLocation);
			#endif
			
			
			
			BlockState foundState = null;
			List<BlockState> possibleStateList = block.getStateDefinition().getPossibleStates();
			for (BlockState possibleState : possibleStateList)
			{
				String possibleStatePropertiesString = serializeBlockStateProperties(possibleState);
				if (possibleStatePropertiesString.equals(blockStatePropertiesString))
				{
					foundState = possibleState;
					break;
				}
			}
			
			Objects.requireNonNull(foundState);
			return new BlockStateWrapper(foundState, levelWrapper);
		}
		catch (Exception e)
		{
			throw new IOException("Failed to deserialize the string [" + resourceStateString + "] into a BlockStateWrapper: " + e.getMessage(), e);
		}
	}
	
	/** used to compare and save BlockStates based on their properties */
	private static String serializeBlockStateProperties(BlockState blockState)
	{
		// get the property list for this block (doesn't contain this block state's values, just the names and possible values)
		java.util.Collection<net.minecraft.world.level.block.state.properties.Property<?>> blockPropertyCollection = blockState.getProperties();
		
		// alphabetically sort the list, so they are always in the same order
		List<net.minecraft.world.level.block.state.properties.Property<?>> sortedBlockPropteryList = new ArrayList<>(blockPropertyCollection);
		sortedBlockPropteryList.sort((a, b) -> a.getName().compareTo(b.getName()));
		
		
		StringBuilder stringBuilder = new StringBuilder();
		for (net.minecraft.world.level.block.state.properties.Property<?> property : sortedBlockPropteryList)
		{
			String propertyName = property.getName();
			
			String value = "NULL";
			if (blockState.hasProperty(property))
			{
				value = blockState.getValue(property).toString();
			}
			
			stringBuilder.append("{");
			stringBuilder.append(propertyName).append(RESOURCE_LOCATION_SEPARATOR).append(value);
			stringBuilder.append("}");
		}
		
		return stringBuilder.toString();
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if (obj == null || this.getClass() != obj.getClass())
		{
			return false;
		}
		
        BlockStateWrapper that = (BlockStateWrapper) obj;
	    // the serialized value is used, so we can test the contents instead of the references
        return Objects.equals(this.serialize(), that.serialize());
    }

    @Override
    public int hashCode() {
		return Objects.hash(this.serialize());
	}
	
	@Override
	public Object getWrappedMcObject() { return this.blockState; }
	
	@Override
	public boolean isAir() { return this.isAir(this.blockState); }
	public boolean isAir(BlockState blockState) { return blockState == null || blockState.isAir(); }
	
	@Override
	public boolean isSolid()
	{
        #if PRE_MC_1_20_1
		return this.blockState.getMaterial().isSolid();
        #else
		return !this.blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
        #endif
	}
	
	@Override
	public boolean isLiquid()
	{
		if (this.isAir())
		{
			return false;
		}
		
        #if PRE_MC_1_20_1
		return this.blockState.getMaterial().isLiquid();
        #else
		return !this.blockState.getFluidState().isEmpty();
        #endif
	}
	
	@Override
	public String toString() {
		return this.serialize();
	}

}
