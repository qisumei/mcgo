package com.qisumei.csgo.events;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.input.KeyMappings;
import com.qisumei.csgo.gui.ShopScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.minecraft.client.Minecraft;
/**
 * 创建人：  @author SelfAbandonment
 * 创建时间: 2025-10-19 12:38
 */


// 客户端事件订阅
@EventBusSubscriber(modid = QisCSGO.MODID, value = Dist.CLIENT)
public class InputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        // 确保游戏窗口有焦点且玩家存在
        if (mc.screen == null && mc.player != null) {
            // 检查购买键是否被按下
            while (KeyMappings.OPEN_SHOP.consumeClick()) {
                // 打开购买界面
                openShopScreen();
            }
        }
    }

    private static void openShopScreen() {
        Minecraft mc = Minecraft.getInstance();
        // 确保在主线程打开界面
        mc.execute(() -> {
            // 这里假设你已经实现了ShopScreen
            mc.setScreen(new ShopScreen());
        });
    }
}