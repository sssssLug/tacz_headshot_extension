package com.ssssslug.tacz_headshot_extension.mixin.util;


import com.ssssslug.tacz_headshot_extension.Config;
import com.tacz.guns.util.EntityUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(value = EntityUtil.class, remap = false, priority = 900)
public abstract class EntityUtilMixin {

    /*
    * 非常轻量的混入，应该不会引起什么严重的兼容性问题。*/
    @ModifyVariable(method = "getHitResult", at = @At(value = "STORE"), name = "headshot")
    private static boolean hijackHeadshotResult(boolean o) {
        return o && !Config.Common.DISABLE_GLOBAL_HEADSHOT_JUDGEMENT.get();
    }
}
