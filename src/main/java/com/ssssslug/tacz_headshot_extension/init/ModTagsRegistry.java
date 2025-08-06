package com.ssssslug.tacz_headshot_extension.init;

import com.ssssslug.tacz_headshot_extension.TACZHeadshotExtension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public class ModTagsRegistry {
    public static final TagKey<DamageType> EXCLUDED_FROM_HEADSHOT = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TACZHeadshotExtension.MODID, "excluded_from_headshot"));
}
