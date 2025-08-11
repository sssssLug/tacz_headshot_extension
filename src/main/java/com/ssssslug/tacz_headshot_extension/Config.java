package com.ssssslug.tacz_headshot_extension;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Mod.EventBusSubscriber(modid = TACZHeadshotExtension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final Map<ResourceLocation, Float> HEADSHOT_MULTIPLIER_CACHE = Maps.newHashMap();
    private static final Map<ResourceLocation, Float> ARROW_POTION_MULTIPLIER_CACHE = Maps.newHashMap();
    private static final Set<String> HEADSHOT_BLACKLIST_CACHE = new HashSet<>();
    private static final Pattern REG = Pattern.compile("^([a-z0-9_.-]+:[a-z0-9/._-]+) *?= *?([-+]?[0-9]*\\.?[0-9]+)$");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SERVER_CONFIG_ID = TACZHeadshotExtension.MODID + "-server.toml";


    @SubscribeEvent
    static void onLoad(final ModConfigEvent.@NotNull Loading event) {
        if(Objects.equals(event.getConfig().getFileName(), SERVER_CONFIG_ID)) {
            initCache();
        }
    }


    public static void initCache() {
        //三名单缓存。
        HEADSHOT_MULTIPLIER_CACHE.clear();
        ARROW_POTION_MULTIPLIER_CACHE.clear();
        HEADSHOT_BLACKLIST_CACHE.clear();

        for(String s1 : Server.BULLET_ENTITY_TYPE_LIST_MAIN.get()) {
            addCheck(s1, 0);
        }

        for(String s2 : Server.BULLET_ENTITY_TYPE_LIST_POTION.get()) {
            addCheck(s2, 1);
        }

        HEADSHOT_BLACKLIST_CACHE.addAll(Server.BULLET_ENTITY_TYPE_LIST_BLACKLIST.get());
    }

    public static void addCheck(String s, Integer index) {
        Matcher m = REG.matcher(s);
        if(m.find()) {
            switch (index) {
                case 0 -> HEADSHOT_MULTIPLIER_CACHE.put(ResourceLocation.parse(m.group(1)), Float.parseFloat(m.group(2)));
                case 1 -> ARROW_POTION_MULTIPLIER_CACHE.put(ResourceLocation.parse(m.group(1)), Float.parseFloat(m.group(2)));
                default -> {}
            }
        }else LOGGER.warn("Cache error: can't read current element: \"{}\". There may be some formatting mistake.", s);
    }


    public static boolean testInBlacklist(ResourceLocation r) {
        return r == null || testInBlacklist(r.toString());
    }
    public static boolean testInBlacklist(String s) {
        return HEADSHOT_BLACKLIST_CACHE.contains(s);
    }

    public static float testInPotionList(ResourceLocation r, Float defaultValue) {
        return ARROW_POTION_MULTIPLIER_CACHE.getOrDefault(r, defaultValue) ;
    }

    public static float testInList(ResourceLocation r) {
        return HEADSHOT_MULTIPLIER_CACHE.getOrDefault(r, Common.GENERIC_HEADSHOT_MULTIPLIER.get().floatValue());
    }


    public static class Common {

        static final ForgeConfigSpec SPEC_COMMON;
        public static final ForgeConfigSpec.DoubleValue GENERIC_HEADSHOT_MULTIPLIER;
        public static final ForgeConfigSpec.BooleanValue DISABLE_GLOBAL_HEADSHOT_JUDGEMENT;

        private static final ForgeConfigSpec.Builder BUILDER_COMMON = new ForgeConfigSpec.Builder();

        static {

            GENERIC_HEADSHOT_MULTIPLIER = BUILDER_COMMON.defineInRange("Generic Headshot Multiplier", 1.5D, 0, 255D);

            DISABLE_GLOBAL_HEADSHOT_JUDGEMENT = BUILDER_COMMON.comment("").comment("When TACZ meets an entity that doesn't have a configured headshot box, the mod will decide with a vague logic.")
                    .comment("For some reason you might dislike this mechanic, Then you can switch this on to disable it.")
                    .comment("Be aware that this function will use Mixins. If you encounter any potential compatibility issues, turn it off.")
                    .worldRestart()
                    .define("Stop Vague Headshot Logic in TaCZ", false);

            SPEC_COMMON = BUILDER_COMMON.build();
        }
    }

    public static class Server {

        static final ForgeConfigSpec SPEC_SERVER;
        static final ForgeConfigSpec.ConfigValue<List<? extends String>> BULLET_ENTITY_TYPE_LIST_MAIN;
        static final ForgeConfigSpec.ConfigValue<List<? extends String>> BULLET_ENTITY_TYPE_LIST_POTION;
        static final ForgeConfigSpec.ConfigValue<List<? extends String>> BULLET_ENTITY_TYPE_LIST_BLACKLIST;

        private static final ForgeConfigSpec.Builder BUILDER_SERVER = new ForgeConfigSpec.Builder();

        static {
            /*
             * 主名单和黑名单某种意义上是可以合并的。先保持现状。*/
            BULLET_ENTITY_TYPE_LIST_MAIN = BUILDER_SERVER.comment("Any damage-source with a valid projectile (extended from vanilla Projectile class) to cause damage will be executed for headshot calculation.")
                    .comment("There'll be a global headshot multiplier.If you need some special values for specific bullet entity types, write them below.")
                    .comment("e.g. \"minecraft:arrow=2.0\"").worldRestart()
                    .<String>defineList("Bullet Type List", List.of("minecraft:trident=2.5"), (obj) -> {
                        return obj instanceof String;
                    });

            BULLET_ENTITY_TYPE_LIST_POTION = BUILDER_SERVER.comment("").comment("In particular, if the projectile is a vanilla tipped-arrow and has a valid Potion type-")
                    .comment("-(Attention: Only potion type is usable, Custom Effects NBT will be ignored), you can write them here to get a special multiplier.")
                    .comment("Make yourself aware of the difference between Potion and Effect.")
                    .comment("BTW minecraft tipped arrow entity use \"minecraft:arrow\" as its type-id, not \"minecraft:tipped_arrow\". Keep that in mind.")
                    .comment("e.g. \"minecraft:harming=1.5\", \"minecraft:strong_harming=2.5\"")
                    .comment("Then if there's \"minecraft:arrow=2.0\" in \"Bullet Type List\", the final multiplier will be 150% for an Arrow of Harming and 250% for the strong version, despite the former.")
                    .worldRestart()
                    .<String>defineList("Tipped Arrow Potion List", List.of("minecraft:harming=2.5", "minecraft:strong_harming=3.0"), (obj) -> {
                        return obj instanceof String;
                    });

            BULLET_ENTITY_TYPE_LIST_BLACKLIST = BUILDER_SERVER.comment("").comment("Some projectiles might not be suitable for performing a \"Headshot\".That's why there's a blacklist.")
                    .comment("There's also a blacklist of damage-types, or in other words, a tag as \"" + TACZHeadshotExtension.MODID + ":excluded_from_headshot\".")
                    .comment("No need to add \"tacz:bullet\" (TACZ bullet projectile), because it will be automatically ignored.")
                    .comment("And obviously writing \"minecraft:arrow\" in will make \"Tipped Arrow Potion List\" useless.")
                    .worldRestart()
                    .<String>defineList("Bullet Type Blacklist", List.of("minecraft:shulker_bullet", "minecraft:snowball", "minecraft:ender_pearl"), (obj) -> {
                        return obj instanceof String;
                    });

            SPEC_SERVER = BUILDER_SERVER.build();
        }
    }

    public static class Client {

        public static final ForgeConfigSpec.BooleanValue USE_TACZ_HEADSHOT_SOUND;
        public static final ForgeConfigSpec.ConfigValue<String> TEMPLATE_TACZ_WEAPON;
        static final ForgeConfigSpec SPEC_CLIENT;

        private static final ForgeConfigSpec.Builder BUILDER_CLIENT = new ForgeConfigSpec.Builder();

        static {

            USE_TACZ_HEADSHOT_SOUND = BUILDER_CLIENT.comment("Switch this on to use TACZ headshot sound.")
                    .comment("If this is off(by default), vanilla critical hit sound will be played instead(Just like TAC does), and \"Sound Effect Template\" will become meaningless.")
                    .define("Use TACZ sound effect", false);

            TEMPLATE_TACZ_WEAPON = BUILDER_CLIENT.comment("").comment("The headshot sound effect will be decided by this gun id.")
                    .comment("Be sure to write a valid gun item id here. Or keep \"Use TACZ sound effect\" false.")
                    .define("Sound Effect Template", "tacz:glock_17");

            SPEC_CLIENT = BUILDER_CLIENT.build();
        }
    }
}
