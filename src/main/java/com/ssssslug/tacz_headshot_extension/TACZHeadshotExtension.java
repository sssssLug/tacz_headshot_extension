package com.ssssslug.tacz_headshot_extension;

import com.mojang.logging.LogUtils;
import com.ssssslug.tacz_headshot_extension.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TACZHeadshotExtension.MODID)
public class TACZHeadshotExtension {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "tacz_headshot_extension";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public TACZHeadshotExtension() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.Common.SPEC_COMMON);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.Server.SPEC_SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.Client.SPEC_CLIENT);
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.setup();
            LOGGER.info("TACZ Extension Mod network initialized!");
        });
    }

}
