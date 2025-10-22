package com.qisumei.csgo.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 游戏内按键绑定
 */
public class KeyBindings {

    public static final String KEY_CATEGORY = "key.categories.qiscsgo";

    public static final KeyMapping OPEN_SHOP = new KeyMapping(
        "key.qiscsgo.open_shop",  // 翻译键
        KeyConflictContext.IN_GAME,  // 只在游戏内生效
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_P,  // P键
        KEY_CATEGORY
    );
}

