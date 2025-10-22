// 新建文件: src/main/java/com/qisumei/csgo/mixin/CampfireSmokeParticleMixin.java
package com.qisumei.csgo.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 这是一个针对 CampfireSmokeParticle (篝火烟雾粒子) 的 Mixin 类。
 * Mixin 是一种在运行时向现有 Java 类中注入代码的技术。
 * @Mixin(CampfireSmokeParticle.class) 注解告诉 Mixin 处理器，我们的目标是修改这个类。
 */
@Mixin(CampfireSmokeParticle.class)
public abstract class CampfireSmokeParticleMixin extends Particle {

    /**
     * 这是一个必需的“影子”构造函数。
     * Mixin 需要它来匹配父类的构造函数，但我们不需要在这里写任何代码。
     * @param pLevel 粒子所在的客户端世界
     * @param pX 粒子的 X 坐标
     * @param pY 粒子的 Y 坐标
     * @param pZ 粒子的 Z 坐标
     */
    protected CampfireSmokeParticleMixin(ClientLevel pLevel, double pX, double pY, double pZ) {
        // 调用父类的构造函数是必需的。
        super(pLevel, pX, pY, pZ);
    }

    /**
     * 这是一个注入方法。
     * 我们在这里同时修改了粒子的生命周期和大小。
     * @Inject 注解告诉 Mixin 在目标方法的指定位置注入我们的代码。
     * method = "<init>": 目标方法是构造函数 (constructor)。
     * at = @At("RETURN"): 注入点在构造函数执行完毕并即将返回 (RETURN) 的那一刻。
     * @param pLevel 构造函数参数：粒子所在的客户端世界
     * @param pX 构造函数参数：粒子的 X 坐标
     * @param pY 构造函数参数：粒子的 Y 坐标
     * @param pZ 构造函数参数：粒子的 Z 坐标
     * @param pXSpeed 构造函数参数：粒子在 X 轴上的速度
     * @param pYSpeed 构造函数参数：粒子在 Y 轴上的速度
     * @param pZSpeed 构造函数参数：粒子在 Z 轴上的速度
     * @param pSignal 构造函数参数：是否为信号烟雾（更高、更远）
     * @param ci Mixin需要的回调信息对象，我们不需要使用它，但必须声明。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, boolean pSignal, CallbackInfo ci) {
        // 在原版构造函数执行完毕后，我们强行修改粒子的 lifetime 属性。
        // this.lifetime 是从父类 Particle 继承来的受保护字段。

        // 原版篝火粒子的 lifetime 大约在 160 到 200 ticks (8-10秒) 之间。
        // 我们可以将它设置为一个更大的值，例如延长一倍。
        // 这里设置为 320 ticks (16秒) 的基础值，再加上一个随机值，使其消失得更自然。
        this.lifetime = 600 + this.random.nextInt(80); // 最终生命周期为 16-20 秒

        // this.scale() 是从父类 Particle 继承来的公共方法。
        // 它会将粒子的 quadSize (渲染大小) 乘以指定的倍数。
        // 传入 2.0F 表示将粒子的大小放大到原来的 2 倍。
        this.scale(5.0F);
    }
}