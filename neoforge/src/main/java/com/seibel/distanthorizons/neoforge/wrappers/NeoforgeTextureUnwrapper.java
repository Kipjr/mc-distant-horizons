package com.seibel.distanthorizons.neoforge.wrappers;

#if MC_VER < MC_1_21_9
public class NeoforgeTextureUnwrapper
{ /* not needed for older MC versions */ }
#else

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;

public class NeoforgeTextureUnwrapper
{
	/**
	 * if Neoforge texture validation is enabled the GlTexture object will be wrapped with a
	 * Neoforge specific ValidationGpuTexture object.
	 * This helper allows us to get the underlying OpenGL texture ID
	 * regardless of what Neoforge returns.
	 */
	public static int getGlTextureIdFromGpuTexture(GpuTexture gpuTexture) throws ClassCastException
	{
		GlTexture glTexture;
		
		if (gpuTexture instanceof ValidationGpuTexture)
		{
			ValidationGpuTexture validationTexture = (ValidationGpuTexture) gpuTexture;
			glTexture = (GlTexture)validationTexture.getRealTexture();
		}
		else
		{
			glTexture = (GlTexture) gpuTexture;
		}
		
		int id = glTexture.glId();
		return id;
	}
	
}
#endif
