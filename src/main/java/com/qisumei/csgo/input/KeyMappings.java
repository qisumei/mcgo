package com.qisumei.csgo.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.qisumei.csgo.QisCSGO;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * 创建人：  @author SelfAbandonment
 * 创建时间: 2025-10-19 12:31
 */
@EventBusSubscriber(modid = QisCSGO.MODID, value = Dist.CLIENT)
public class KeyMappings {
    // 定义购买界面按键映射
    public static final KeyMapping OPEN_SHOP = new KeyMapping(
            "key.qiscsgo.open_shop",  // 翻译键
            KeyConflictContext.IN_GAME,  // 游戏内生效
            KeyModifier.NONE,  // 无修饰键
            InputConstants.Type.KEYSYM,  // 按键类型
            GLFW.GLFW_KEY_P,  // 绑定p键（可根据需要修改）
            "category.qiscsgo.csgo"  // 分类名称
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // 注册按键映射
        event.register(OPEN_SHOP);
    }
}