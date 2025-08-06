package com.ssssslug.tacz_headshot_extension.mixin.util;


import com.ssssslug.tacz_headshot_extension.Config;
import com.tacz.guns.util.EntityUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(value = EntityUtil.class, remap = false, priority = 900)
public abstract class EntityUtilMixin {
    @ModifyVariable(method = "getHitResult", at = @At(value = "STORE"), name = "headshot")
    private static boolean hijackHeadshotResult(boolean o) {
        return o && !Config.DISABLE_GLOBAL_HEADSHOT_BOX.get();
    }
}
