package com.seibel.lod.fabric.mixins.unsafe;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.DebugBuffer;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Semaphore;

/**
 * NOTE: THIS IS NOT A FIX TO THE PROBLEM.
 * TODO: Do/Find an actual fix to this
 *
 * @author Ran
 */
@Mixin(PalettedContainer.class)
public class MixinPalettedContainer {
    @Mutable
    private Semaphore lock;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setSemaphore(CallbackInfo ci) {
        this.lock = new Semaphore(2);
    }
}
