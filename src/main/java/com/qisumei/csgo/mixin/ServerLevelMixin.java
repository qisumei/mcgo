package com.qisumei.csgo.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow
    public List<ServerPlayer> players;

    @Inject(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            at = @At("HEAD"), cancellable = true)
    private <T extends ParticleOptions> void interceptSendParticles(T type,
                                                                    double posX, double posY, double posZ,
                                                                    int particleCount,
                                                                    double xOffset, double yOffset, double zOffset,
                                                                    double speed,
                                                                    CallbackInfoReturnable<Integer> cir) {
        // 修改篝火烟雾粒子
        if (type.getType() == ParticleTypes.CAMPFIRE_COSY_SMOKE || type.getType() == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE) {
            cir.cancel();

            double customRange = 1280.0;
            double customRangeSq = customRange * customRange;
            ServerLevel self = (ServerLevel) (Object) this;

            int sent = 0;
            for (ServerPlayer player : this.players) {
                if (player.distanceToSqr(posX, posY, posZ) < customRangeSq) {
                    //true，强制客户端渲染
                    boolean ok = self.sendParticles(player, type, true, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed);
                    if (ok) sent++;
                }
            }

            cir.setReturnValue(sent);
        }
    }
}
