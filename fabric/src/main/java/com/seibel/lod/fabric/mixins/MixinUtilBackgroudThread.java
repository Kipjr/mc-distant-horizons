package com.seibel.lod.fabric.mixins;

import java.util.concurrent.Executor;

import com.seibel.lod.fabric.ClientProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.seibel.lod.common.wrappers.DependencySetupDoneCheck;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;

import net.minecraft.Util;

@Mixin(Util.class)
public class MixinUtilBackgroudThread
{
	private static boolean doTriggerOverride() {
		try {
			return DependencySetupDoneCheck.getIsCurrentThreadDistantGeneratorThread.get();
		} catch (NullPointerException e) {
			return false;
		}
	}
	
	@Inject(method = "wrapThreadWithTaskName(Ljava/lang/String;Ljava/lang/Runnable;)Ljava/lang/Runnable;",
			at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$wrapThreadWithTaskName(String string, Runnable r, CallbackInfoReturnable<Runnable> ci)
	{
		if (ClientProxy.isGenerationThreadChecker != null && ClientProxy.isGenerationThreadChecker.get())
		{
			// ApiShared.LOGGER.info("util wrapThreadWithTaskName(Runnable) triggered");
			ci.setReturnValue(r);
		}
	}

	@Inject(method = "backgroundExecutor", at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$backgroundExecutor(CallbackInfoReturnable<Executor> ci)
	{
		if (ClientProxy.isGenerationThreadChecker != null && ClientProxy.isGenerationThreadChecker.get())
		{
			// ApiShared.LOGGER.info("util backgroundExecutor triggered");
			ci.setReturnValue(Runnable::run);
		}
	}
}
