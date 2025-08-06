package com.ssssslug.tacz_headshot_extension.network;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.event.RenderCrosshairEvent;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.message.event.ServerMessageGunHurt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.logging.Level;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class MessageFromServerCustomHeadshot {
    private final ResourceLocation templateGunId;
    private final ResourceLocation templateGunDisplayId;

    public MessageFromServerCustomHeadshot(ResourceLocation templateGunId, ResourceLocation templateGunDisplayId) {
        this.templateGunId = templateGunId;
        this.templateGunDisplayId = templateGunDisplayId;
    }

    public static void encode(MessageFromServerCustomHeadshot message, FriendlyByteBuf buf) {
        buf.writeResourceLocation(message.templateGunId);
        buf.writeResourceLocation(message.templateGunDisplayId);
    }

    public static MessageFromServerCustomHeadshot decode(FriendlyByteBuf buf) {
        ResourceLocation templateGunId = buf.readResourceLocation();
        ResourceLocation templateGunDisplayId = buf.readResourceLocation();

        return new MessageFromServerCustomHeadshot(templateGunId, templateGunDisplayId);
    }

    public static void handle(MessageFromServerCustomHeadshot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = (NetworkEvent.Context)contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> displayHitMark(message));
        }

        context.setPacketHandled(true);
    }


    @OnlyIn(Dist.CLIENT)
    private static void displayHitMark(MessageFromServerCustomHeadshot message) {
        Minecraft mc = Minecraft.getInstance();
//        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if(player == null)return;
        RenderCrosshairEvent.markHitTimestamp();
        RenderCrosshairEvent.markHeadShotTimestamp();
        TimelessAPI.getGunDisplay(message.templateGunDisplayId, message.templateGunId).ifPresent((index) -> SoundPlayManager.playHeadHitSound(mc.player, index));
    }
}
