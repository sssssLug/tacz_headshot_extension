package com.ssssslug.tacz_headshot_extension.event_hander;

import com.ssssslug.tacz_headshot_extension.Config;
import com.ssssslug.tacz_headshot_extension.event.CustomHeadshotEvent;
import com.ssssslug.tacz_headshot_extension.init.ModTagsRegistry;
import com.ssssslug.tacz_headshot_extension.network.MessageFromServerCustomHeadshot;
import com.ssssslug.tacz_headshot_extension.network.NetworkHandler;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class ExHeadshotHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurtByProjectile(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        if(direct instanceof Projectile bullet && !source.is(ModTagsRegistry.EXCLUDED_FROM_HEADSHOT)) {
            ResourceLocation bulletId = ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
            if(Config.testInBlackList(bulletId)) return;

            //原版EntityHitResult并不会储存弹射物具体的命中位置。用TACZ的逻辑重新判断。
            boolean flag = isHeadshot(event.getEntity(), direct.position(), direct.position().add(direct.getDeltaMovement()));
            if(!flag)return;
            float f = Config.testInList(bulletId);
            if(bulletId.toString().equals("minecraft:tipped_arrow")){
                String potionId = getPotionIdString(direct.getPersistentData());
                f = Config.testInPotionList(potionId, f);
            }

            CustomHeadshotEvent event1 = new CustomHeadshotEvent(event.getEntity(), source, direct, event.getAmount(), f);
            if(MinecraftForge.EVENT_BUS.post(event1)) return;
            float dmgMultiplier = event1.getHeadshotMultiplier();
            //最终倍率不大于0的话就终止处理。
            if(dmgMultiplier <= 0.0F) return;
            event.setAmount(event.getAmount() * f);
            //发包调用下准心特效和音效。很明显光靠这个监听器判断不了实体是否被击杀。
            Entity attacker = source.getEntity();
            if(attacker instanceof ServerPlayer && !attacker.level().isClientSide()) {
                NetworkHandler.sendToClientPlayer(new MessageFromServerCustomHeadshot(ResourceLocation.parse(Config.TEMPLATE_TACZ_WEAPON.get()), DefaultAssets.DEFAULT_GUN_DISPLAY_ID, !Config.USE_TACZ_HEADSHOT_SOUND.get()), (Player) attacker);
            }
        }
    }

    private static boolean isHeadshot(Entity victim, Vec3 startVec, Vec3 endVec) {
        //因为“弹射物命中和造成伤害”在监听时点已经确定发生了，所以不像原生方法那样搞判定箱补正也不会有太大问题。
        AABB boundingBox = (victim instanceof Player && !victim.isCrouching()) ? victim.getBoundingBox().expandTowards(0, 0.0625, 0) : victim.getBoundingBox();
        Vec3 hitPos = (Vec3)boundingBox.clip(startVec, endVec).orElse((Vec3) null);
        if (hitPos == null) {
            return false;
        } else {
            boolean headshot = false;
            Vec3 hitBoxPos = hitPos.subtract(victim.position());
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
            if (entityId != null) {
                AABB aabb = HeadShotAABBConfigRead.getAABB(entityId);
                if (aabb != null) {
                    headshot = aabb.contains(hitBoxPos);
                }
            }

            //与某配置项同步
            if(!Config.DISABLE_GLOBAL_HEADSHOT_BOX.get()){
                float eyeHeight = victim.getEyeHeight();
                if (!headshot && (double) eyeHeight - (double) 0.25F < hitBoxPos.y && hitBoxPos.y < (double) eyeHeight + (double) 0.25F) {
                    headshot = true;
                }
            }
            return headshot;
        }
    }

    private static String getPotionIdString(CompoundTag tag) {
        CompoundTag itemTag = tag.getCompound("Item");
        return itemTag.getCompound("tag").getString("Potion");
    }
}
