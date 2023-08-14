/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

import com.seibel.distanthorizons.api.enums.config.ELoggerMode;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

#if POST_MC_1_17
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryOps;
#endif
#if POST_MC_1_19_2
#endif
#if MC_1_16_5 || MC_1_17_1
import net.minecraft.core.Registry;
#elif MC_1_18_2 || MC_1_19_2
#else
import net.minecraft.core.registries.Registries;
#endif

#if !PRE_MC_1_18_2
#endif


/** This class wraps the minecraft BlockPos.Mutable (and BlockPos) class */
public class BiomeWrapper implements IBiomeWrapper
{
	private static final Logger LOGGER = LogManager.getLogger();

	#if PRE_MC_1_18_2
	public static final ConcurrentMap<Biome, BiomeWrapper> biomeWrapperMap = new ConcurrentHashMap<>();
	public final Biome biome;
	#else
	public static final ConcurrentMap<Holder<Biome>, BiomeWrapper> biomeWrapperMap = new ConcurrentHashMap<>();
	public final Holder<Biome> biome;
    #endif

	private final ILevelWrapper levelWrapper;

	/**
	 * Cached so it can be quickly used as a semi-stable hashing method. <br>
	 * This may also fix the issue where we can serialize and save after a level has been shut down.
	 */
	private String serializationResult = null;

	//==============//
	// constructors //
	//==============//

	static public IBiomeWrapper getBiomeWrapper(#if PRE_MC_1_18_2 Biome #else Holder<Biome> #endif biome, ILevelWrapper levelWrapper) {
		return biomeWrapperMap.computeIfAbsent(biome, biomeHolder -> new BiomeWrapper(biomeHolder, levelWrapper));
	}

	private BiomeWrapper(#if PRE_MC_1_18_2 Biome #else Holder<Biome> #endif biome, ILevelWrapper levelWrapper) {
		this.biome = biome;
		this.levelWrapper = levelWrapper;
	}

	//=========//
	// methods //
	//=========//

	@Override
	public String getName() {
        #if PRE_MC_1_18_2
        return biome.toString();
        #else
		return this.biome.unwrapKey().orElse(Biomes.THE_VOID).registry().toString();
        #endif
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}

		BiomeWrapper that = (BiomeWrapper) obj;
		// the serialized value is used so we can test the contents instead of the references
		return Objects.equals(this.serialize(this.getLevelWrapper()), that.serialize(this.getLevelWrapper()));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.serialize(this.getLevelWrapper()));
	}

	@Override
	public String serialize(ILevelWrapper levelWrapper) {
		if (this.serializationResult == null) {
			// FIXME: Workaround for serverside support
			RegistryAccess registryAccess;
			try {
				registryAccess = ((Level) levelWrapper.getWrappedMcObject()).registryAccess();
			} catch (Exception ignored) {
				this.serializationResult = "";
				return this.serializationResult;
			}

			ResourceLocation resourceLocation;
			#if MC_1_16_5 || MC_1_17_1
			resourceLocation = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).getKey(this.biome);
			#elif MC_1_18_2 || MC_1_19_2
			resourceLocation = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).getKey(this.biome.value());
			#else
			resourceLocation = registryAccess.registryOrThrow(Registries.BIOME).getKey(this.biome.value());
			#endif

			if (resourceLocation == null) {
				String biomeName;
				#if MC_1_16_5 || MC_1_17_1
				biomeName = this.biome.toString();
				#else
				biomeName = this.biome.value().toString();
				#endif

				LOGGER.warn("unable to serialize: "+biomeName);
				// shouldn't normally happen, but just in case
				this.serializationResult = "";
			} else {
				this.serializationResult = resourceLocation.getNamespace() + ":" + resourceLocation.getPath();
			}
		}

		return this.serializationResult;
	}

	@Override
	public ILevelWrapper getLevelWrapper() {
		return levelWrapper;
	}

	public static IBiomeWrapper deserialize(String resourceLocationString, ILevelWrapper levelWrapper) throws IOException {
		if (resourceLocationString.trim().isEmpty() || resourceLocationString.equals("")) {
			if (Config.Client.Advanced.Logging.logWorldGenEvent.get() == ELoggerMode.LOG_WARNING_TO_CHAT_AND_FILE) {
				LOGGER.warn("null biome string deserialized");
			}

			// shouldn't normally happen, but just in case
			resourceLocationString = new ResourceLocation("minecraft", "the_void").toString(); // just "void" in MC 1.12 through 1.9 (inclusive)
		}

		// parse the resource location
		int separatorIndex = resourceLocationString.indexOf(":");
		if (separatorIndex == -1) {
			throw new IOException("Unable to parse resource location string: [" + resourceLocationString + "].");
		}
		ResourceLocation resourceLocation = new ResourceLocation(
				resourceLocationString.substring(0, separatorIndex), resourceLocationString.substring(separatorIndex + 1));

		try {
			net.minecraft.core.RegistryAccess registryAccess = ((Level) levelWrapper.getWrappedMcObject()).registryAccess();
			
			#if MC_1_16_5 || MC_1_17_1
			Biome biome = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).get(resourceLocation);
			#elif MC_1_18_2 || MC_1_19_2
			Biome unwrappedBiome = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).get(resourceLocation);
			Holder<Biome> biome = new Holder.Direct<>(unwrappedBiome);
			#else
			Biome unwrappedBiome = registryAccess.registryOrThrow(Registries.BIOME).get(resourceLocation);
			if (unwrappedBiome == null)
			{
				LOGGER.warn("null biome string deserialized from string: "+resourceLocationString);
			}
			Holder<Biome> biome = new Holder.Direct<>(unwrappedBiome);
			#endif

			return getBiomeWrapper(biome, levelWrapper);
		} catch (Exception e) {
			throw new IOException(
					"Failed to deserialize the string [" + resourceLocationString + "] into a BiomeWrapper: " + e.getMessage(), e);
		}
	}

	@Override
	public Object getWrappedMcObject() {
		return this.biome;
	}

	@Override
	public String toString() {
		return this.serialize(this.getLevelWrapper());
	}

}
