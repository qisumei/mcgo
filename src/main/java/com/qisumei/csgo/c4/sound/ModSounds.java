package com.qisumei.csgo.c4.sound;

import com.qisumei.csgo.QisCSGO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 音效注册类，用于注册模组中使用的自定义音效事件。
 */
public class ModSounds {
    /**
     * 音效事件的延迟注册器，用于向Minecraft注册表中注册自定义音效。
     */
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, QisCSGO.MODID);

    // 使用 fromNamespaceAndPath 创建 ResourceLocation

    // 原版音效引用也使用新方法
    /**
     * 获取原版Minecraft中的音效事件"block.note_block.hat"。
     *
     * @return 返回对应的SoundEvent对象
     */
    public static SoundEvent ALARM_SOUND() {
        return BuiltInRegistries.SOUND_EVENT.get(
            ResourceLocation.fromNamespaceAndPath("minecraft", "block.note_block.hat")
        );
    }

    /**
     * 将音效注册器绑定到事件总线，完成实际注册过程。
     *
     * @param bus NeoForge的事件总线实例
     */
    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
