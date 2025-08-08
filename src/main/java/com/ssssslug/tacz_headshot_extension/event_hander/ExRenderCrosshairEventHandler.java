package com.ssssslug.tacz_headshot_extension.event_hander;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ExRenderCrosshairEventHandler {
    private static final ResourceLocation HIT_ICON = ResourceLocation.fromNamespaceAndPath("tacz", "textures/crosshair/hit/hit_marker.png");
    private static long headShotTimestamp = -1L;

    /*
    * 见com.tacz.guns.client.event.RenderCrosshairEvent.
    * 优先级比原型方法低，这样玩家手上是枪械时就不会调用这边的处理。*/
    @SubscribeEvent(receiveCanceled = false, priority = EventPriority.LOW)
    public static void onRenderCrossHair(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            if (!IGun.mainHandHoldGun(player)) {
                long remainHeadShotTime = System.currentTimeMillis() - headShotTimestamp;
                if (remainHeadShotTime <= 300L) {
                    renderHeadshotMarker(event.getGuiGraphics(), event.getWindow());
                }
            }
        }
    }

    private static void renderHeadshotMarker(GuiGraphics graphics, Window window) {
        long remainHeadShotTime = System.currentTimeMillis() - headShotTimestamp;
        float offset = ((Double) RenderConfig.HIT_MARKET_START_POSITION.get()).floatValue();

        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        float x = (float)width / 2.0F - 8.0F;
        float y = (float)height / 2.0F - 8.0F;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F - (float) remainHeadShotTime / 300.0F);

        graphics.blit(HIT_ICON, (int)(x - offset), (int)(y - offset), 0.0F, 0.0F, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int)(x + 8.0F + offset), (int)(y - offset), 8.0F, 0.0F, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int)(x - offset), (int)(y + 8.0F + offset), 0.0F, 8.0F, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int)(x + 8.0F + offset), (int)(y + 8.0F + offset), 8.0F, 8.0F, 8, 8, 16, 16);
    }

    public static void markedHeadshotTimestamp() {
        headShotTimestamp = System.currentTimeMillis();
    }
}
