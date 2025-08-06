package com.ssssslug.tacz_headshot_extension.network;

import com.ssssslug.tacz_headshot_extension.TACZHeadshotExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    public static final String VERSION = "1.0";
    private static int ID = 0;
    public static SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(TACZHeadshotExtension.MODID, "network"),
            () -> VERSION,
            (version) -> version.equals(VERSION),
            (version) -> version.equals(VERSION)
    );;
    private static boolean registerFlag = false;

    public static int nextID() {
        return ID++;
    }

    public static void registerMessage() {
        if(registerFlag) return;

        INSTANCE.registerMessage(nextID(), MessageFromServerCustomHeadshot.class, MessageFromServerCustomHeadshot::encode, MessageFromServerCustomHeadshot::decode, MessageFromServerCustomHeadshot::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerFlag = true;
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::registerMessage);
    }
}
