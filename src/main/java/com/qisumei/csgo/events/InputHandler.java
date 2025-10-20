package com.qisumei.csgo.events;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.gui.ShopScreen;
import com.qisumei.csgo.input.KeyMappings;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * 客户端输入事件处理器。
 * <p>
 * 这个类只在客户端加载 ({@code value = Dist.CLIENT})，负责监听和处理玩家的键盘输入事件。
 * </p>
 *
 * @author SelfAbandonment, Qisumei
 */
@EventBusSubscriber(modid = QisCSGO.MODID, value = Dist.CLIENT)
public final class InputHandler {

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private InputHandler() {}

    /**
     * 监听按键输入事件。
     * 当玩家按下绑定的“打开商店”键时，打开商店GUI。
     *
     * @param event 按键输入事件对象。
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // 检查 "打开商店" 按键是否被按下
        // consumeClick() 会处理按键的按下事件，并防止重复触发
        if (KeyMappings.OPEN_SHOP.consumeClick()) {
            // 在主线程中打开商店界面，以确保线程安全
            Minecraft.getInstance().execute(() -> {
                // 只有当玩家在游戏中且当前没有打开任何其他界面时才打开商店
                if (Minecraft.getInstance().screen == null && Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().setScreen(new ShopScreen());
                }
            });
        }
    }
}
