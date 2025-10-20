package com.qisumei.csgo.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.qisumei.csgo.QisCSGO;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;

/**
 * 按键映射注册类。
 * <p>
 * 这个类只在客户端加载，负责定义和注册模组中所有的自定义按键绑定，
 * 例如打开商店的快捷键。这些按键会出现在游戏的控制设置菜单中。
 * </p>
 *
 * @author SelfAbandonment, Qisumei
 */
@EventBusSubscriber(modid = QisCSGO.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class KeyMappings {

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private KeyMappings() {}

    /**
     * 打开商店界面的按键绑定。
     * 默认绑定到 'P' 键。
     */
    public static final KeyMapping OPEN_SHOP = new KeyMapping(
            "key.qiscsgo.open_shop",  // 语言文件中的翻译键
            KeyConflictContext.IN_GAME,  // 仅在游戏中生效，避免在菜单中冲突
            InputConstants.Type.KEYSYM,  // 按键类型为标准键盘按键
            GLFW.GLFW_KEY_P,             // 默认绑定的按键 (P)
            "category.qiscsgo.csgo"      // 在控制菜单中的分类
    );

    /**
     * 监听按键映射注册事件，并将我们定义的所有按键绑定注册到游戏中。
     *
     * @param event 按键映射注册事件对象。
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SHOP);
    }
}
