package com.qisumei.csgo.grenade;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.entity.ModEntityTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;



/**
 * 烟雾弹实体类。
 * 继承自抽象投掷物实体基类，负责实现撞击后的烟雾效果。
 */
public class SmokeGrenadeEntity extends AbstractGrenadeEntity {
    // --- 烟雾效果常量 ---
    private static final int SMOKE_DURATION_TICKS = 2 * 20; // 烟雾持续25秒
    private static final float SMOKE_RADIUS = 6.0F; // 烟雾半径
    private static final int PARTICLES_PER_TICK = 20; // 每tick生成的粒子数量
    // --- 状态变量 ---
    private boolean isSmoking = false; // 是否正在产生烟雾
    private int smokeTicksLeft = 0; // 烟雾剩余持续时间
    public SmokeGrenadeEntity(Level world, LivingEntity owner) {


        super(ModEntityTypes.SMOKE_GRENADE.get(), owner, world, 20, 0.4F);
    }
    public SmokeGrenadeEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }
    @Override
    protected void onImpact(HitResult result) {
        if (!this.level().isClientSide && !isSmoking) {
            this.isSmoking = true;
            this.smokeTicksLeft = SMOKE_DURATION_TICKS;
            //this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (isSmoking && !this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            if (smokeTicksLeft > 0) {
                smokeTicksLeft--;
                spawnSmokeCloud(serverLevel);
            } else {
                this.discard();
            }
        }
    }
    /**
     * 生成更大更浓的烟雾粒子。
     */
    private void spawnSmokeCloud(ServerLevel serverLevel) {
    // 粒子类型（CAMPFIRE_COSY_SMOKE 比较柔和，CAMPFIRE_SIGNAL_SMOKE更大）
    ParticleOptions mainSmoke = ParticleTypes.CAMPFIRE_COSY_SMOKE;
    ParticleOptions denseSmoke = ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;

    for (int i = 0; i < PARTICLES_PER_TICK * 2; i++) {
        // === ✅ 随机生成一个球体内的点 ===
        double theta = this.random.nextDouble() * 2 * Math.PI;   // 水平角度
        double phi = Math.acos(2 * this.random.nextDouble() - 1); // 垂直角度
        double r = SMOKE_RADIUS * Math.cbrt(this.random.nextDouble()); // 半径（立方根保证分布均匀）

        // 球坐标转笛卡尔坐标
        double offsetX = r * Math.sin(phi) * Math.cos(theta);
        double offsetY = r * Math.cos(phi); // ✅ 保证上下都有烟
        double offsetZ = r * Math.sin(phi) * Math.sin(theta);
        // === ✅ 上升速度（轻微浮动） ===
        double velocityX = (this.random.nextDouble() - 0.5D) * 0.02D;
        double velocityY = 0.05D + this.random.nextDouble() * 0.1D;
        double velocityZ = (this.random.nextDouble() - 0.5D) * 0.02D;
        // === ✅ 中心更浓，边缘更淡 ===
        double densityChance = 1.0 - (r / SMOKE_RADIUS); // 越靠近中心越浓
        ParticleOptions particle = (this.random.nextDouble() < densityChance * 0.7)
                ? denseSmoke : mainSmoke;
        serverLevel.sendParticles(
            particle,
            this.getX() + offsetX,
            this.getY() + offsetY,
            this.getZ() + offsetZ,
            1,
            0, 0, 0,
            0
        );
    }
}
    @Override
    protected Item getDefaultItem() {
        return QisCSGO.SMOKE_GRENADE_ITEM.get();
    }
}