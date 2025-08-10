package com.ssssslug.tacz_headshot_extension.network;

import com.ssssslug.tacz_headshot_extension.Config;
import com.ssssslug.tacz_headshot_extension.event_handler.ExRenderCrosshairEventHandler;
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

@SuppressWarnings({"FieldCanBeLocal", "ClassCanBeRecord"})
public class MessageFromServerCustomHeadshot {
    private final int bulletId;

    public MessageFromServerCustomHeadshot(int templateGunDisplayId) {
        this.bulletId = templateGunDisplayId;
    }

    public static void encode(MessageFromServerCustomHeadshot message, FriendlyByteBuf buf) {
        buf.writeInt(message.bulletId);
    }

    public static MessageFromServerCustomHeadshot decode(FriendlyByteBuf buf) {
        int bulletId = buf.readInt();

        return new MessageFromServerCustomHeadshot(bulletId);
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

        ResourceLocation templateGunId = ResourceLocation.parse(Config.Client.TEMPLATE_TACZ_WEAPON.get());
        if(Config.Client.USE_TACZ_HEADSHOT_SOUND.get()){
            TimelessAPI.getGunDisplay((ResourceLocation) null, templateGunId).ifPresent((index) -> SoundPlayManager.playHeadHitSound(player, index));
        } else {
            ((LocalPlayer) player).playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
        }
    }
}
