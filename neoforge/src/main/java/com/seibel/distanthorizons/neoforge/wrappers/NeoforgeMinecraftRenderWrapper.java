package com.seibel.distanthorizons.neoforge.wrappers;

import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

#if MC_VER < MC_1_21_9
#else
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;
#endif

import java.lang.invoke.MethodHandles;

public class NeoforgeMinecraftRenderWrapper extends MinecraftRenderWrapper
{
	public static final NeoforgeMinecraftRenderWrapper INSTANCE = new NeoforgeMinecraftRenderWrapper();
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	private static final Minecraft MC = Minecraft.getInstance();
	
	
	
	@Override
	public int getDepthTextureId()
	{
		#if MC_VER < MC_1_21_9
		// no special handling required,
		// both neo/fabric uses the same back end objects
		return super.getDepthTextureId();
		#else
		try
		{
			GpuTexture gpuTexture = this.getRenderTarget().getDepthTexture();
			int id = NeoforgeTextureUnwrapper.getGlTextureIdFromGpuTexture(gpuTexture);
			return id;
		}
		catch (Exception e)
		{
			// only log this error once per session
			if (!this.depthTextureCastFailLogged)
			{
				this.depthTextureCastFailLogged = true;
				LOGGER.error("Unable to cast render Target depth texture to GlTexture. MC or a rendering mod may have changed the object type.", e);
			}
			return 0;
		}
		#endif
	}
	
	@Override
	public int getColorTextureId()
	{
		#if MC_VER < MC_1_21_9
		// no special handling required,
		// both neo/fabric uses the same back end objects
		return super.getColorTextureId();
		#else
		try
		{
			GpuTexture gpuTexture = this.getRenderTarget().getColorTexture();
			int id = NeoforgeTextureUnwrapper.getGlTextureIdFromGpuTexture(gpuTexture);
			return id;
		}
		catch (Exception e)
		{
			// only log this error once per session
			if (!this.colorTextureCastFailLogged)
			{
				this.colorTextureCastFailLogged = true;
				LOGGER.error("Unable to cast render Target color texture to ValidationGpuTexture or GlTexture. MC, Neoforge, or a rendering mod may have changed the object type.", e);
			}
			return 0;
		}
		#endif
	}
	
	
	
}
