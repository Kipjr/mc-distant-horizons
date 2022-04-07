/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.common.wrappers.minecraft;

import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.common.wrappers.misc.LightMapWrapper;
import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.handlers.dependencyInjection.ModAccessorHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.util.LodUtil;

import com.seibel.lod.core.wrapperInterfaces.misc.ILightMapWrapper;
import net.minecraft.client.renderer.LightTexture;

import com.mojang.math.Vector3f;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.WrapperFactory;
import com.seibel.lod.common.wrappers.block.BlockPosWrapper;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
#if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
import net.minecraft.world.level.material.FogType;
#elif MC_VERSION_1_16_5
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
#endif
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL15;


/**
 * A singleton that contains everything
 * related to rendering in Minecraft.
 *
 * @author James Seibel
 * @version 12-12-2021
 */
public class MinecraftRenderWrapper implements IMinecraftRenderWrapper
{
	public static final MinecraftRenderWrapper INSTANCE = new MinecraftRenderWrapper();
	
	private static final Minecraft MC = Minecraft.getInstance();
	private static final GameRenderer GAME_RENDERER = MC.gameRenderer;
	private static final IWrapperFactory FACTORY = WrapperFactory.INSTANCE;
	
	@Override
	public Vec3f getLookAtVector()
	{
		Camera camera = GAME_RENDERER.getMainCamera();
		Vector3f cameraDir = camera.getLookVector();
		return new Vec3f(cameraDir.x(), cameraDir.y(), cameraDir.z());
	}
	
	@Override
	public AbstractBlockPosWrapper getCameraBlockPosition()
	{
		Camera camera = GAME_RENDERER.getMainCamera();
		BlockPos blockPos = camera.getBlockPosition();
		return new BlockPosWrapper(blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}
	
	@Override
	public boolean playerHasBlindnessEffect()
	{
		return MC.player.getActiveEffectsMap().get(MobEffects.BLINDNESS) != null;
	}
	
	@Override
	public Vec3d getCameraExactPosition()
	{
		Camera camera = GAME_RENDERER.getMainCamera();
		Vec3 projectedView = camera.getPosition();
		
		return new Vec3d(projectedView.x, projectedView.y, projectedView.z);
	}
	
	@Override
	public Mat4f getDefaultProjectionMatrix(float partialTicks)
	{
		#if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
		return McObjectConverter.Convert(GAME_RENDERER.getProjectionMatrix(GAME_RENDERER.getFov(GAME_RENDERER.getMainCamera(), partialTicks, true)));
		#elif MC_VERSION_1_16_5
		return McObjectConverter.Convert(GAME_RENDERER.getProjectionMatrix(GAME_RENDERER.getMainCamera(), partialTicks, true));
		#endif
	}
	
	@Override
	public double getGamma()
	{
		return MC.options.gamma;
	}
	
	@Override
	public Color getFogColor(float partialTicks) {
		#if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
		FogRenderer.setupColor(GAME_RENDERER.getMainCamera(), partialTicks, MC.level, 1, GAME_RENDERER.getDarkenWorldAmount(partialTicks));
		float[] colorValues = RenderSystem.getShaderFogColor();
		#elif MC_VERSION_1_16_5
		float[] colorValues = new float[4];
		GL15.glGetFloatv(GL15.GL_FOG_COLOR, colorValues);
		#endif
		return new Color(colorValues[0], colorValues[1], colorValues[2], colorValues[3]);
	}
	// getSpecialFogColor() is the same as getFogColor()
	
	@Override
	public Color getSkyColor() {
		if (MC.level.dimensionType().hasSkyLight()) {
			#if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
			Vec3 colorValues = MC.level.getSkyColor(MC.gameRenderer.getMainCamera().getPosition(), MC.getFrameTime());
			#elif MC_VERSION_1_16_5
			Vec3 colorValues = MC.level.getSkyColor(MC.gameRenderer.getMainCamera().getBlockPosition(), MC.getFrameTime());
			#endif
			return new Color((float) colorValues.x, (float) colorValues.y, (float) colorValues.z);
		} else
			return new Color(0, 0, 0);
	}
	
	@Override
	public double getFov(float partialTicks)
	{
		return GAME_RENDERER.getFov(GAME_RENDERER.getMainCamera(), partialTicks, true);
	}
	
	/** Measured in chunks */
	@Override
	public int getRenderDistance()
	{
		#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
        return MC.options.getEffectiveRenderDistance();
		#elif MC_VERSION_1_16_5 || MC_VERSION_1_17_1
		return MC.options.renderDistance;
		#endif
	}
	
	@Override
	public int getScreenWidth()
	{
		return MC.getWindow().getWidth();
	}
	@Override
	public int getScreenHeight()
	{
		return MC.getWindow().getHeight();
	}
	
    private RenderTarget getRenderTarget() {
        RenderTarget r = null; //MC.levelRenderer.getCloudsTarget();
        return r!=null ? r : MC.getMainRenderTarget();
    }

    @Override
    public int getTargetFrameBuffer() {
        return getRenderTarget().frameBufferId;
    }

    @Override
    public int getTargetFrameBufferViewportWidth() {
        return getRenderTarget().viewWidth;
    }

    @Override
    public int getTargetFrameBufferViewportHeight() {
        return getRenderTarget().viewHeight;
    }

	/**
	 * This method returns the ChunkPos of all chunks that Minecraft
	 * is going to render this frame. <br><br>
	 * <p>
	 */
	
    public boolean usingBackupGetVanillaRenderedChunks = false;
	@Override
	public HashSet<AbstractChunkPosWrapper> getVanillaRenderedChunks()
	{
		ISodiumAccessor sodium = ModAccessorHandler.get(ISodiumAccessor.class);
		if (sodium != null)
		{
			return sodium.getNormalRenderedChunks();
		}
		IOptifineAccessor optifine = ModAccessorHandler.get(IOptifineAccessor.class);
		if (optifine != null)
		{
			HashSet<AbstractChunkPosWrapper> pos = optifine.getNormalRenderedChunks();
			if (pos == null)
				pos = getMaximumRenderedChunks();
			return pos;
		}
		if (!usingBackupGetVanillaRenderedChunks) {
			try {
				LevelRenderer levelRenderer = MC.levelRenderer;
				#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
				LinkedHashSet<LevelRenderer.RenderChunkInfo> chunks = levelRenderer.renderChunkStorage.get().renderChunks;
				#elif MC_VERSION_1_16_5 || MC_VERSION_1_17_1
				Collection<LevelRenderer.RenderChunkInfo> chunks = levelRenderer.renderChunks;
				#endif
				return (chunks.stream().map((chunk) -> {
                	#if MC_VERSION_1_18_2
					AABB chunkBoundingBox = chunk.chunk.getBoundingBox();
                	#elif MC_VERSION_1_16_5 || MC_VERSION_1_17_1 || MC_VERSION_1_18_1
					AABB chunkBoundingBox = chunk.chunk.bb;
                	#endif
					#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
					return FACTORY.createChunkPos(Math.floorDiv((int) chunkBoundingBox.minX, 16),
							Math.floorDiv((int) chunkBoundingBox.minZ, 16));
					#elif MC_VERSION_1_16_5 || MC_VERSION_1_17_1
					return FACTORY.createChunkPos(Math.floorDiv((int) chunkBoundingBox.minX, 16),
							Math.floorDiv((int) chunkBoundingBox.minZ, 16));
					#endif
				}).collect(Collectors.toCollection(HashSet::new)));
			} catch (LinkageError e) {
				try {
					MinecraftClientWrapper.INSTANCE.sendChatMessage(
							"\u00A7e\u00A7l\u00A7uWARNING: Distant Horizons: getVanillaRenderedChunks method failed."
									+ " Using Backup Method.");
					MinecraftClientWrapper.INSTANCE.sendChatMessage(
							"\u00A7eOverdraw prevention will be worse than normal.");
				} catch (Exception e2) {}
				ApiShared.LOGGER.error("getVanillaRenderedChunks Error: ", e);
				usingBackupGetVanillaRenderedChunks = true;
			}
		}
		return getMaximumRenderedChunks();
	}
	
	@Override
	public int[] getLightmapPixels()
	{
		LightTexture tex = GAME_RENDERER.lightTexture();
		//tex.tick(); // This call makes no sense, but it fixes pause menu flicker bug
		NativeImage lightMapPixels = tex.lightTexture.getPixels();
		LightMapWrapper lightMap = new LightMapWrapper(lightMapPixels);
		
		
		int lightMapHeight = getLightmapTextureHeight();
		int lightMapWidth = getLightmapTextureWidth();
		
		int[] pixels = new int[lightMapWidth * lightMapHeight];
		for (int u = 0; u < lightMapWidth; u++)
		{
			for (int v = 0; v < lightMapWidth; v++)
			{
				// this could probably be kept as a int, but
				// it is easier to test and see the colors when debugging this way.
				// When creating a new release this should be changed to the int version.
				int col = lightMap.getLightValue(u, v);
				
				// these should both create a totally white image
//					int col =
//							Integer.MAX_VALUE;
//					int col =
//							0b11111111 + // red
//							(0b11111111 << 8) + // green
//							(0b11111111 << 16) + // blue
//							(0b11111111 << 24); // blue

//				int col =
//						((c.getRed() & 0xFF) << 16) | // blue
//								((c.getGreen() & 0xFF) << 8) | // green
//								((c.getBlue() & 0xFF)) | // red
//								((c.getAlpha() & 0xFF) << 24); // alpha
				
				// 2D array stored in a 1D array.
				// Thank you Tim from College ;)
				pixels[u * lightMapWidth + v] = col;
			}
		}
		
		return pixels;
	}

	@Override
	public ILightMapWrapper getLightmapWrapper() {
		return new LightMapWrapper(GAME_RENDERER.lightTexture());
	}



	@Override
	public int getLightmapTextureHeight()
	{
		int height = -1;
		
		LightTexture lightTexture = GAME_RENDERER.lightTexture();
		if (lightTexture != null)
		{
			NativeImage tex = lightTexture.lightPixels;
			if (tex != null)
			{
				height = tex.getHeight();
			}
		}
		
		return height;
	}
	
	@Override
	public int getLightmapTextureWidth()
	{
		int width = -1;
		
		LightTexture lightTexture = GAME_RENDERER.lightTexture();
		if (lightTexture != null)
		{
			NativeImage tex = lightTexture.lightPixels;
			if (tex != null)
			{
				width = tex.getWidth();
			}
		}
		
		return width;
	}
	
	
	@Override
	public int getLightmapGLFormat() {
		int glFormat = -1;
		
		LightTexture lightTexture = GAME_RENDERER.lightTexture();
		if (lightTexture != null) {
			NativeImage tex = lightTexture.lightPixels;
			if (tex != null) {
				glFormat = tex.format().glFormat();
			}
		}
		
		return glFormat;
	}
	
	@Override
	public boolean isFogStateSpecial() {
		#if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
		Entity entity = GAME_RENDERER.getMainCamera().getEntity();
		boolean isBlind = (entity instanceof LivingEntity) && ((LivingEntity)entity).hasEffect(MobEffects.BLINDNESS);
		return GAME_RENDERER.getMainCamera().getFluidInCamera() != FogType.NONE || isBlind;
		#elif MC_VERSION_1_16_5
		Camera camera = GAME_RENDERER.getMainCamera();
		FluidState fluidState = camera.getFluidInCamera();
		Entity entity = camera.getEntity();
		boolean isUnderWater = (entity instanceof LivingEntity) && ((LivingEntity)entity).hasEffect(MobEffects.BLINDNESS);
			isUnderWater |= fluidState.is(FluidTags.WATER);
			isUnderWater |= fluidState.is(FluidTags.LAVA);
		return isUnderWater;
		#endif
	}
	
	@Override
	public boolean tryDisableVanillaFog() {
		return true; // Handled via MixinFogRenderer in both forge and fabric
	}
}
