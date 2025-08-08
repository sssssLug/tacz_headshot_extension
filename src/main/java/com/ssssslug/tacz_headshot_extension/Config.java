package com.ssssslug.tacz_headshot_extension;

import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Mod.EventBusSubscriber(modid = TACZHeadshotExtension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final Map<ResourceLocation, Float> HEADSHOT_MULTIPLIER_CACHE = Maps.newHashMap();
    private static final Map<String, Float> ARROW_POTION_MULTIPLIER_CACHE = Maps.newHashMap();
    private static final Pattern REG = Pattern.compile("^([a-z0-9_.-]+:[a-z0-9/._-]+) *?= *?([-+]?[0-9]*\\.?[0-9]+)$");
    private static final String MOD_CONFIG_ID = TACZHeadshotExtension.MODID + "-common.toml";

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static final ForgeConfigSpec.ConfigValue<List<? extends String>> BULLET_ENTITY_TYPE_LIST_MAIN;
    static final ForgeConfigSpec.ConfigValue<List<? extends String>> BULLET_ENTITY_TYPE_LIST_POTION;
    static final ForgeConfigSpec.ConfigValue<List<? extends String>> BULLET_ENTITY_TYPE_LIST_BLACKLIST;
    public static final ForgeConfigSpec.DoubleValue GENERIC_HEADSHOT_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue USE_TACZ_HEADSHOT_SOUND;
    public static final ForgeConfigSpec.ConfigValue<String> TEMPLATE_TACZ_WEAPON;
    public static final ForgeConfigSpec.BooleanValue DISABLE_GLOBAL_HEADSHOT_BOX;

    static final ForgeConfigSpec SPEC;

    static {
        /*
        * 说实话主名单和黑名单是可以合并的。。。
        * 保持现状吧还是。*/
        BULLET_ENTITY_TYPE_LIST_MAIN = BUILDER.comment("Any damage-source with a valid projectile (extended from vanilla Projectile class) to cause damage will be executed for headshot calculation.")
                .comment("There'll be a global headshot multiplier.If you need some special values for specific bullet entity types, write them below.")
                .comment("e.g. \"minecraft:arrow=2.0\"").worldRestart()
                .define("Bullet Type List", Lists.newArrayList());

        BULLET_ENTITY_TYPE_LIST_POTION = BUILDER.comment("").comment("In particular, if the projectile is a vanilla tipped-arrow and has a valid Potion type-")
                .comment("-(Attention: Only potion type is usable, Custom Effects NBT will be ignored), you can write them here to get a special multiplier.")
                .comment("Make yourself aware of the difference between Potion and Effect.")
                .comment("e.g. \"minecraft:harming=2.5\", \"minecraft:strong_harming=3.0\"")
                .comment("Then if there's \"minecraft:tipped_arrow=2.0\" in \"Bullet Type List\", the final multiplier will be 250% for an Arrow of Harming and 300% for the strong version, despite the former.")
                .worldRestart()
                .define("Tipped Arrow Potion List", Lists.newArrayList());

        BULLET_ENTITY_TYPE_LIST_BLACKLIST = BUILDER.comment("").comment("Some projectiles might not be suitable for performing a \"Headshot\".That's why there's a blacklist.")
                .comment("There's also a blacklist of damage-types, or in other words, a tag as \"" + TACZHeadshotExtension.MODID + ":excluded_from_headshot\".")
                .comment("No need to add \"tacz:bullet\" (TACZ bullet projectile), because it will be automatically ignored.")
                .comment("And obviously writing \"minecraft:tipped_arrow\" in will make \"Tipped Arrow Potion List\" useless.")
                .worldRestart()
                .define("Bullet Type Blacklist", List.of("minecraft:shulker_bullet", "minecraft:snowball", "minecraft:ender_pearl"));

        GENERIC_HEADSHOT_MULTIPLIER = BUILDER.comment("").defineInRange("Generic Headshot Multiplier", 1.5D, 0, 255D);

        USE_TACZ_HEADSHOT_SOUND = BUILDER.comment("").comment("Switch this on to use TACZ headshot sound.")
                .comment("If this is off(by default), vanilla critical hit sound will be played instead(Just like TAC does), and \"Sound Effect Template\" will become meaningless.")
                .define("Use TACZ sound effect", false);

        TEMPLATE_TACZ_WEAPON = BUILDER.comment("").comment("The headshot sound effect will be decided by this gun id.")
                .comment("Be sure to write a valid gun item id here. Or keep \"Use TACZ sound effect\" false.")
                .worldRestart()
                .define("Sound Effect Template", "tacz:glock_17");

        BUILDER.push("Others");

        DISABLE_GLOBAL_HEADSHOT_BOX = BUILDER.comment("When TACZ meets an entity that doesn't have a configured headshot box, the mod will generate one using a vague logic.")
                .comment("For some reason you might dislike this mechanic, Then you can switch this on to disable it.")
                .comment("Be aware that this function will use Mixins. If you encounter some potential compatibility issues, turn it off.")
                .worldRestart()
                .define("Stop TACZ From Global Bounding-box Generation", false);

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.@NotNull Loading event) {
        if(Objects.equals(event.getConfig().getFileName(), MOD_CONFIG_ID)) {
            initCache();
        }
    }

    public static void initCache() {
        //主名单注册缓存，黑名单不需要缓存。
        HEADSHOT_MULTIPLIER_CACHE.clear();
        ARROW_POTION_MULTIPLIER_CACHE.clear();

        for(String s1 : BULLET_ENTITY_TYPE_LIST_MAIN.get()) {
            addCheck(s1, 0);
        }

        //药箭药水名单也要缓存
        for(String s2 : BULLET_ENTITY_TYPE_LIST_POTION.get()) {
            addCheck(s2, 1);
        }
    }

    public static void addCheck(String s, Integer i) {
        Matcher m = REG.matcher(s);
        if(m.find()) {
            switch (i) {
                case 0 -> HEADSHOT_MULTIPLIER_CACHE.put(ResourceLocation.parse(m.group(1)), Float.parseFloat(m.group(2)));
                case 1 -> ARROW_POTION_MULTIPLIER_CACHE.put(m.group(1), Float.parseFloat(m.group(2)));
                default -> {}
            }
        }
    }

    public static boolean testInBlackList(ResourceLocation r) {
        return r == null || testInBlackList(r.toString());
    }
    public static boolean testInBlackList(String s) {
        return BULLET_ENTITY_TYPE_LIST_BLACKLIST.get().contains(s);
    }

    public static float testInPotionList(String s, Float defaultValue) {
        return ARROW_POTION_MULTIPLIER_CACHE.getOrDefault(s, defaultValue) ;
    }

    public static float testInList(ResourceLocation r) {
        return HEADSHOT_MULTIPLIER_CACHE.getOrDefault(r, GENERIC_HEADSHOT_MULTIPLIER.get().floatValue());
    }
}
