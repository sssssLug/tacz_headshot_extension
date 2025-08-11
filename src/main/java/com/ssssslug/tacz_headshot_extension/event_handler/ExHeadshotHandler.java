package com.ssssslug.tacz_headshot_extension.event_handler;

import com.mojang.logging.LogUtils;
import com.ssssslug.tacz_headshot_extension.Config;
import com.ssssslug.tacz_headshot_extension.event.CustomHeadshotEvent;
import com.ssssslug.tacz_headshot_extension.init.ModTagsRegistry;
import com.ssssslug.tacz_headshot_extension.mixin.ArrowAccessor;
import com.ssssslug.tacz_headshot_extension.network.MessageFromServerCustomHeadshot;
import com.ssssslug.tacz_headshot_extension.network.NetworkHandler;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
import org.slf4j.Logger;

@Mod.EventBusSubscriber
public class ExHeadshotHandler {
//    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurtByProjectile(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        if(direct instanceof Projectile bullet && !source.is(ModTagsRegistry.EXCLUDED_FROM_HEADSHOT)) {
            ResourceLocation bulletTypeId = ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
            if(Config.testInBlacklist(bulletTypeId)) return;

            //原版EntityHitResult并不会储存弹射物具体的命中位置。用TACZ的逻辑重新判断。
            boolean flag = isHeadshot(event.getEntity(), bullet, direct.position(), direct.position().add(direct.getDeltaMovement()));
            if(!flag)return;
            float f = Config.testInList(bulletTypeId);
            if(bulletTypeId.toString().equals("minecraft:arrow")){
                ResourceLocation potionType = ForgeRegistries.POTIONS.getKey(((ArrowAccessor)bullet).getPotion());
                f = Config.testInPotionList(potionType, f);
            }

            CustomHeadshotEvent event1 = new CustomHeadshotEvent(event.getEntity(), source, direct, event.getAmount(), f);
            if(MinecraftForge.EVENT_BUS.post(event1)) return;
            float dmgMultiplier = event1.getHeadshotMultiplier();
            //最终倍率不大于0的话就终止处理。
            //其实也可以不管，这样就能在造成0伤害时仍然调用特效。
            //没必要吧。。。
            if(dmgMultiplier <= 0.0F) return;
            event.setAmount(event.getAmount() * dmgMultiplier);
            //发包调用下准心特效和音效。很明显光靠这个监听器判断不了实体是否被击杀。
            //发包是为了和前置模组逻辑相似，本质非必要行为，以后可能会改。
            Entity attacker = source.getEntity();
            if(attacker instanceof Player && !attacker.level().isClientSide()) {
                //发个服务端数据假装发包是有意义的*
                int bulletId = bullet.getId();
                NetworkHandler.sendToClientPlayer(new MessageFromServerCustomHeadshot(bulletId), (Player) attacker);
            }
        }
    }

    private static boolean isHeadshot(Entity victim, Entity bullet, Vec3 startVec, Vec3 endVec) {
        //因为“弹射物命中和造成伤害”在监听时点已经确定发生了，所以逻辑可以简化一下。
        //与TaCZ原生逻辑的不同点：游戏没有找到实体类型的注册ID时，TaCZ还会尝试用缺省逻辑判定爆头，这边则不会。
        //接受差异，不认为这是BUG。
        boolean headshot = false;
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        if(entityId != null) {
            Vec3 victimPos = victim.position();
            AABB aabb = HeadShotAABBConfigRead.getAABB(entityId);
            //考虑到很多弹射物（比如最常见的箭）碰撞箱都有不可忽视的大小，给个额外补正减少“擦头皮”的几率。
            AABB aabb1 = bullet.getBoundingBox();
            double trimX = 0.25D * aabb1.getXsize();
            double trimY = 0.5D * aabb1.getYsize();
            double trimZ = 0.25D * aabb1.getZsize();
            if(aabb != null) {
                //实体的坐标并不是3D碰撞箱的中心，而是碰撞箱底面的中心，所以Y轴补正要往负方向挪。
                aabb = aabb.inflate(trimX, 0, trimZ).expandTowards(0, -trimY, 0);
                Vec3 hitPos = aabb.clip(startVec.subtract(victimPos), endVec.subtract(victimPos)).orElse((Vec3) null);
                headshot = hitPos != null;
            }//与某配置项同步
            else if(!Config.Common.DISABLE_GLOBAL_HEADSHOT_JUDGEMENT.get()){
                double eyePosHeight = victim.getEyeY();
                //对Y方向的补正与前面同理。
                //顺便一提TaCZ原生逻辑有一个问题，就是命中生物碰撞箱顶面的“灌顶”攻击基本无法判定爆头。
                //虽然已经进行了额外的补正，但考虑到以常见弹射物的飞行速度在命中实体时往往已经有明显的向下角度了，这个问题的影响会比枪弹大不少。
                //尽量给游戏中每一个生物配置专用的判定箱比使用这种模糊处理要好得多，还节省性能。
                //所以建议整合包开发者不要偷懒。
                AABB boundingBox = (victim instanceof Player && !victim.isCrouching()) ? victim.getBoundingBox().expandTowards(0, 0.0625D - trimY, 0).inflate(trimX, 0, trimZ) : victim.getBoundingBox().expandTowards(0, -trimY, 0).inflate(trimX, trimY, trimZ);
                Vec3 bodyHitPos = (Vec3)boundingBox.clip(startVec, endVec).orElse((Vec3) null);
                if (bodyHitPos != null) {
                    if (eyePosHeight - trimY - (double) 0.251F < bodyHitPos.y && bodyHitPos.y < eyePosHeight + 0.5F * trimY + (double) 0.251F) {
                        headshot = true;
                    }
                }
            }
        }

        return headshot;
    }

//    调用实体NBT的方法不稳定。
/*
    private static String getPotionIdString(CompoundTag tag) {
        CompoundTag itemTag = tag.getCompound("item");
        return itemTag.getCompound("tag").getString("Potion");
    }
*/
}
