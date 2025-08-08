package com.ssssslug.tacz_headshot_extension.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class CustomHeadshotEvent extends LivingEvent {
    private final DamageSource source;
    private final Entity bullet;
    private final float originalDamage;
    private float headshotMultiplier;

    /*
    * KubeJS兼容非必要，鸽了先。*/
    public CustomHeadshotEvent(LivingEntity entity, DamageSource source, Entity bullet, float originalDamage, float headshotMultiplier) {
        super(entity);
        this.source = source;
        this.bullet = bullet;
        this.originalDamage = originalDamage;
        this.headshotMultiplier = headshotMultiplier;
    }

    public DamageSource getSource() {
        return source;
    }

    public Entity getBullet() {
        return bullet;
    }

    public float getOriginalDamage() {
        return originalDamage;
    }

    public float getHeadshotMultiplier() {
        return headshotMultiplier;
    }

    public void setHeadshotMultiplier(float headshotMultiplier) {
        this.headshotMultiplier = headshotMultiplier;
    }
}
