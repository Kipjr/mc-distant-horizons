package com.seibel.distanthorizons.common.wrappers.gui;

import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.config.ConfigBase;
import com.seibel.distanthorizons.core.config.gui.JavaScreenHandlerScreen;
import net.minecraft.client.gui.screens.Screen;

public class GetConfigScreen
{
	public static EType useScreen = EType.Classic;
	
	public enum EType
	{
		Classic,
		JavaSwing;
	}
	
	public static Screen getScreen(Screen parent)
	{
		switch (useScreen)
		{
			case Classic:
				return ClassicConfigGUI.getScreen(ConfigBase.INSTANCE, parent, "client");
			case JavaSwing:
				//return MinecraftScreen.getScreen(parent, new JavaScreenHandlerScreen(new ConfigScreen()), ModInfo.ID + ".title");
				return MinecraftScreen.getScreen(parent, new JavaScreenHandlerScreen(new JavaScreenHandlerScreen.ExampleScreen()), ModInfo.ID + ".title");
			default:
				throw new IllegalArgumentException("No config screen implementation defined for ["+useScreen+"].");
		}
	}
	
}