package com.seibel.distanthorizons.forge.mixins.client;

import com.seibel.distanthorizons.api.enums.config.EDhApiUpdateBranch;
import com.seibel.distanthorizons.common.wrappers.gui.updater.UpdateModScreen;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.jar.installer.GitlabGetter;
import com.seibel.distanthorizons.core.jar.installer.ModrinthGetter;
import com.seibel.distanthorizons.core.jar.updater.SelfUpdater;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.IVersionConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * At the moment this is only used for the auto updater
 *
 * @author coolGi
 */
@Mixin(Minecraft.class)
public class MixinMinecraft
{
	@Unique
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MixinMinecraft.class.getSimpleName());
	
	
	
	#if MC_VER < MC_1_20_2
	#if MC_VER == MC_1_20_1
	@Redirect(
			method = "Lnet/minecraft/client/Minecraft;setInitialScreen(Lcom/mojang/realmsclient/client/RealmsClient;Lnet/minecraft/server/packs/resources/ReloadInstance;Lnet/minecraft/client/main/GameConfig$QuickPlayData;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
	)
	public void onOpenScreen(Minecraft instance, Screen guiScreen)
	{
	#else
	@Redirect(
			method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
	)
	public void onOpenScreen(Minecraft instance, Screen guiScreen)
	{
	#endif
		if (!Config.Client.Advanced.AutoUpdater.enableAutoUpdater.get()) // Don't do anything if the user doesn't want it
		{
			instance.setScreen(guiScreen); // Sets the screen back to the vanilla screen as if nothing ever happened
			return;
		}
		
		if (SelfUpdater.onStart())
		{
			try
			{
				instance.setScreen(new UpdateModScreen(
						new TitleScreen(false), // We don't want to use the vanilla title screen as it would fade the buttons
						(Config.Client.Advanced.AutoUpdater.updateBranch.get() == EDhApiUpdateBranch.STABLE ? ModrinthGetter.getLatestIDForVersion(SingletonInjector.INSTANCE.get(IVersionConstants.class).getMinecraftVersion()): GitlabGetter.INSTANCE.projectPipelines.get(0).get("sha"))
				));
				return;
			}
			catch (Exception e)
			{
				// info instead of error since this can be ignored and probably just means
				// there isn't a new DH version available
				LOGGER.info("Unable to show DH update screen, reason: ["+e.getMessage()+"].");
			}
		}
		
		// Sets the screen back to the vanilla screen as if nothing ever happened
		// if not done the game will crash
		instance.setScreen(guiScreen);
	}
	#endif
	
	#if MC_VER >= MC_1_20_2
	@Redirect(
			method = "Lnet/minecraft/client/Minecraft;onGameLoadFinished(Lnet/minecraft/client/Minecraft$GameLoadCookie;)V",
			at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V")
	)
	private void buildInitialScreens(Runnable runnable)
	{
		boolean showUpdater = SelfUpdater.onStart();  // always needs to be called, otherwise auto update setup won't be completed
	
		// TODO merge logic for forge, neo, and fabric
		if (
				showUpdater
				&& Config.Client.Advanced.AutoUpdater.enableAutoUpdater.get() // Don't do anything if the user doesn't want it
			)
		{
			runnable = () -> 
			{
				String versionId;
				EDhApiUpdateBranch updateBranch = EDhApiUpdateBranch.convertAutoToStableOrNightly(Config.Client.Advanced.AutoUpdater.updateBranch.get());
				if (updateBranch == EDhApiUpdateBranch.STABLE)
				{
					versionId = ModrinthGetter.getLatestIDForVersion(SingletonInjector.INSTANCE.get(IVersionConstants.class).getMinecraftVersion());
				}
				else
				{
					versionId = GitlabGetter.INSTANCE.projectPipelines.get(0).get("sha");
				}
				
				if (versionId != null)
				{
					try
					{
						
						Minecraft.getInstance().setScreen(new UpdateModScreen(
								// TODO: Change to runnable, instead of tittle screen
								new TitleScreen(false), // We don't want to use the vanilla title screen as it would fade the buttons
								versionId
						));
					}
					catch (Exception e)
					{
						// info instead of error since this can be ignored and probably just means
						// there isn't a new DH version available
						LOGGER.info("Unable to show DH update screen, reason: ["+e.getMessage()+"].");
					}
				}
				else
				{
					LOGGER.info("Unable to find new DH update for the ["+updateBranch+"] branch. Assuming DH is up to date...");
				}
			};
			runnable.run();
		}
	}
	#endif
	
	@Inject(at = @At("HEAD"), method = "close()V", remap = false)
	public void close(CallbackInfo ci)
	{
		SelfUpdater.onClose();
	}
	
}
