package com.seibel.distanthorizons.forge.mixins;

import net.minecraft.client.ClientBrandRetriever;
import net.minecraftforge.fml.ModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

#if MC_VER >= MC_1_18_2
import net.minecraftforge.internal.BrandingControl;
#elif MC_VER >= MC_1_17_1
import net.minecraftforge.fmllegacy.BrandingControl;
#else // < 1.17.1
import net.minecraftforge.fml.BrandingControl;
#endif

import java.util.List;
import java.util.Set;

/**
 * @author coolGi
 * @author cortex
 */
public class ForgeMixinPlugin implements IMixinConfigPlugin
{
	
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		try
		{
			if (!ClientBrandRetriever.getClientModName().equals("forge"))
				return false;
		}
		catch (RuntimeException e)
		{
			if (!BrandingControl.getServerBranding().equals("forge"))
				return false;
		}
		
		if (mixinClassName.contains(".mods."))
		{ // If the mixin wants to go into a mod then we check if that mod is loaded or not
			return ModList.get().isLoaded(
					mixinClassName
							// What these 2 regex's do is get the mod name that we are checking out of the mixinClassName
							// Eg. "com.seibel.distanthorizons.mixins.mods.sodium.MixinSodiumChunkRenderer" turns into "sodium"
							.replaceAll("^.*mods.", "") // Replaces everything before the mods
							.replaceAll("\\..*$", "") // Replaces everything after the mod name
			);
		}
		return true;
	}
	
	
	@Override
	public void onLoad(String mixinPackage)
	{
		
	}
	
	@Override
	public String getRefMapperConfig()
	{
		return null;
	}
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
	{
		
	}
	
	@Override
	public List<String> getMixins()
	{
		return null;
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
		
	}
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
		
	}
	
}