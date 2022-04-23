package com.seibel.lod.fabric.quilt;

/**
 * Used to call classes that are in Quilt
 * @author Ran
 */
public class QuiltUtils {
    public static boolean isModLoaded(String modId) {
        try {
            return (boolean) Class.forName("org.quiltmc.loader.api.QuiltLoader").getDeclaredMethod("isModLoaded", String.class).invoke(null, modId);
        } catch (Exception ignored) { }
        return false;
    }
}
