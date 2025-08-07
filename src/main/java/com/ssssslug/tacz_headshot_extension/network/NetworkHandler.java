package com.ssssslug.tacz_headshot_extension.network;

import com.ssssslug.tacz_headshot_extension.TACZHeadshotExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    public static final String VERSION = "1.0";
    private static int ID = 0;
    public static SimpleChannel INSTANCE;

    public static int nextID() {
        return ID++;
    }

    public static void setup() {

        INSTANCE = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(TACZHeadshotExtension.MODID, "network"),
                () -> VERSION,
                VERSION::equals,
                VERSION::equals
        );
        registerMessage();
    }

    public static void registerMessage() {

        INSTANCE.registerMessage(nextID(),
                MessageFromServerCustomHeadshot.class,
                MessageFromServerCustomHeadshot::encode,
                MessageFromServerCustomHeadshot::decode,
                MessageFromServerCustomHeadshot::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendToClientPlayer(Object message, Player player) {
        if (INSTANCE == null) {
            throw new IllegalStateException("Network channel not initialized!");
        }
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)player), message);
    }
}
