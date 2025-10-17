package com.qisumei.csgo.c4.sound;

import com.qisumei.csgo.QisCSGO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
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
    /**
     * 注册名为"tw"的音效事件。
     * 该音效用于表示某种游戏内事件（如炸弹爆炸倒计时）。
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> TW_SOUND =
        SOUND_EVENTS.register("tw",
            () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(QisCSGO.MODID, "tw")
            ));

    /**
     * 注册名为"ctw"的音效事件。
     * 该音效可能用于反恐精英相关事件提示。
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> CTW_SOUND =
        SOUND_EVENTS.register("ctw",
            () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(QisCSGO.MODID, "ctw")
            ));

    /**
     * 注册名为"c4_place"的音效事件。
     * 该音效在C4炸弹放置时播放。
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> C4_PLACE =
        SOUND_EVENTS.register("c4_place",
            () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(QisCSGO.MODID, "c4_place")
            ));

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
