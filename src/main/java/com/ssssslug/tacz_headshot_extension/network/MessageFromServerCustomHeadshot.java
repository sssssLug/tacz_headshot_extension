package com.ssssslug.tacz_headshot_extension.network;

import com.ssssslug.tacz_headshot_extension.event_hander.ExRenderCrosshairEventHandler;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.event.RenderCrosshairEvent;
import com.tacz.guns.client.sound.SoundPlayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class MessageFromServerCustomHeadshot {
    private final ResourceLocation templateGunId;
    private final ResourceLocation templateGunDisplayId;
    private final boolean useVanillaCriticalSound;

    public MessageFromServerCustomHeadshot(ResourceLocation templateGunId, ResourceLocation templateGunDisplayId, boolean defaultHeadshotSound) {
        this.templateGunId = templateGunId;
        this.templateGunDisplayId = templateGunDisplayId;
        this.useVanillaCriticalSound = defaultHeadshotSound;
    }

    public static void encode(MessageFromServerCustomHeadshot message, FriendlyByteBuf buf) {
        buf.writeResourceLocation(message.templateGunId);
        buf.writeResourceLocation(message.templateGunDisplayId);
        buf.writeBoolean(message.useVanillaCriticalSound);
    }

    public static MessageFromServerCustomHeadshot decode(FriendlyByteBuf buf) {
        ResourceLocation templateGunId = buf.readResourceLocation();
        ResourceLocation templateGunDisplayId = buf.readResourceLocation();
        boolean defaultHeadshotSound = buf.readBoolean();

        return new MessageFromServerCustomHeadshot(templateGunId, templateGunDisplayId, defaultHeadshotSound);
    }

    public static void handle(MessageFromServerCustomHeadshot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = (NetworkEvent.Context)contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> actHeadshot(message));
        }

        context.setPacketHandled(true);
    }


    @OnlyIn(Dist.CLIENT)
    private static void actHeadshot(MessageFromServerCustomHeadshot message) {
        Minecraft mc = Minecraft.getInstance();
//        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if(player == null)return;

        ExRenderCrosshairEventHandler.markedHeadshotTimestamp();
        RenderCrosshairEvent.markHitTimestamp();
        RenderCrosshairEvent.markHeadShotTimestamp();

        if(message.useVanillaCriticalSound){
            ((LocalPlayer) player).playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
        } else {
            TimelessAPI.getGunDisplay(message.templateGunDisplayId, message.templateGunId).ifPresent((index) -> SoundPlayManager.playHeadHitSound(mc.player, index));
        }
    }
}
