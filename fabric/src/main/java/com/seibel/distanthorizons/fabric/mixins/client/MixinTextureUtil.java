package com.seibel.distanthorizons.fabric.mixins.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.seibel.distanthorizons.core.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sets Minecraft's LOD Bias (looks similar to mipmaps)
 *
 * @author coolGi
 */
@Mixin(TextureUtil.class)
public class MixinTextureUtil
{
	@Redirect(method = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(Lcom/mojang/blaze3d/platform/NativeImage$InternalGlFormat;IIII)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texParameter(IIF)V", #if MC_VER == MC_1_16_5 remap = true #else remap = false #endif))
	private static void setLodBias(int target, int pname, float param)
	{
		float biasValue = Config.Client.Advanced.Graphics.Quality.lodBias.get().floatValue();
		if (biasValue != 0)
		{
			// The target is GL11.GL_TEXTURE_2D
			// And the pname is GL14.GL_TEXTURE_LOD_BIAS
			GlStateManager._texParameter(target, pname, biasValue);
		}
	}
	
}
