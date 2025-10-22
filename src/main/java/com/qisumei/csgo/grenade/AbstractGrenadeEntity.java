// 文件路径: com/qisumei/csgo/grenade/AbstractGrenadeEntity.java
package com.qisumei.csgo.grenade;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * AbstractGrenadeEntity —— 一个抽象的手榴弹实体类。
 * 它实现了自定义重力、空气阻力以及可控的弹跳行为，而不是使用原版的默认物理引擎。
 */
public abstract class AbstractGrenadeEntity extends ThrowableItemProjectile {

    // =================================================================================
    // 核心物理参数
    // =================================================================================

    /**
     * 剩余弹跳次数。每弹跳一次，该值减 1。当减至 0 时，手榴弹将不再弹跳并触发 onImpact。
     */
    private int bouncesLeft;

    /**
     * 弹跳能量系数。这是一个乘数，决定了每次弹跳后保留多少速度。
     * - 1.0f: 完美弹跳，几乎不损失速度（像超级球）。
     * - 0.0f: 完全不弹跳，撞击后速度变为0。
     * - (0.0f, 1.0f): 每次弹跳都会损失一部分能量/速度，这是最真实的情况。
     */
    private final float bouncePower;

    /**
     * 自定义重力加速度。此值在每个 tick (游戏刻，1/20秒) 被减去 Y 轴速度。
     * 它直接决定了手榴弹下落的速度。
     */
    private final double customGravity;

    /**
     * 空气阻力系数。这是一个乘数，在每个 tick 应用于速度向量。
     * 它模拟了手榴弹在空中飞行时的减速效果。
     * - 1.0: 无空气阻力。
     * - (0.0, 1.0): 速度会随时间衰减。越接近1，阻力越小。
     */
    private final double airDrag;

    // =================================================================================
    // 构造函数
    // =================================================================================

    /**
     * 主要构造函数，用于在游戏中由玩家或其他实体抛出时创建。
     * @param entityType 实体类型
     * @param owner 抛出者
     * @param world 所在世界
     * @param maxBounces 最大弹跳次数
     * @param bouncePower 弹跳能量系数
     */
    public AbstractGrenadeEntity(EntityType<? extends ThrowableItemProjectile> entityType, LivingEntity owner, Level world, int maxBounces, float bouncePower) {
        super(entityType, owner, world); // 调用父类构造函数
        this.bouncesLeft = maxBounces; // 初始化最大弹跳次数
        this.bouncePower = bouncePower; // 初始化弹跳能量
        this.customGravity = 0.05D; // 设置一个标准的重力值
        this.airDrag = 0.99D; // 设置一个标准的空气阻力值
        this.setNoGravity(true); // 关键：关闭 Minecraft 的默认重力，以便我们使用自定义重力
    }

    /**
     * 次要构造函数，主要用于实体加载等情况。
     * @param entityType 实体类型
     * @param world 所在世界
     */
    public AbstractGrenadeEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world); // 调用父类构造函数
        this.bouncesLeft = 0; // 默认不弹跳
        this.bouncePower = 0.0f; // 默认无弹跳能量
        this.customGravity = 0.03D; // 默认重力
        this.airDrag = 0.999D; // 默认空气阻力
        this.setNoGravity(true); // 同样需要关闭默认重力
    }


    // =================================================================================
    // 核心物理循环 (tick)
    // =================================================================================

    /**
     * 每个游戏刻都会调用的方法，用于更新实体状态。
     */
    @Override
    public void tick() {
        // 步骤 1: 获取当前的运动向量 (速度)
        Vec3 motion = this.getDeltaMovement();

        // 步骤 2: 应用自定义重力。每一 tick，Y 轴速度都会减少 customGravity 的值。
        motion = motion.add(0.0D, -this.customGravity, 0.0D);

        // 步骤 3: 应用空气阻力。将当前速度向量的每个分量都乘以 airDrag 系数，使其减速。
        motion = motion.scale(this.airDrag);

        // 步骤 4: 将计算后的新速度应用回实体。
        this.setDeltaMovement(motion);

        // 步骤 5: 调用父类的 tick()。这非常重要，因为它会根据 setDeltaMovement 设定的速度来更新实体位置，并处理碰撞检测。
        super.tick();

        // 步骤 6 (可选): 在客户端生成一些尾迹粒子，用于调试或视觉效果。
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }

        // 步骤 7: 添加一个速度阈值判断，防止手榴弹在地面上无限抖动。
        Vec3 currentMotion = this.getDeltaMovement(); // 再次获取速度
        double speed = currentMotion.length(); // 计算速度大小
        // 当速度非常小 (小于0.02) 并且实体在地面上时
        if (speed < 0.02D && this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO); // 强行停止所有运动
            // 在这里你可以选择直接触发 onImpact，或者等待其自然消失
        }
    }


    // =================================================================================
    // 碰撞与弹跳处理
    // =================================================================================

    /**
     * 当实体发生碰撞时调用。
     * @param result 碰撞结果信息
     */
    @Override
    protected void onHit(@Nonnull HitResult result) {
        super.onHit(result); // 调用父类方法

        // 判断碰撞类型
        if (result.getType() == HitResult.Type.BLOCK) { // 如果撞到方块
            if (this.bouncesLeft > 0) { // 如果还有剩余弹跳次数
                performBounce((BlockHitResult) result); // 执行弹跳逻辑
                this.bouncesLeft--; // 弹跳次数减 1
            } else { // 如果没有弹跳次数了
                onImpact(result); // 触发最终效果
            }
        } else if (result.getType() == HitResult.Type.ENTITY) { // 如果撞到实体
            onImpact(result); // 直接触发最终效果，通常手榴弹撞到人不会弹开
        }
    }

    /**
     * 执行弹跳的具体逻辑。
     * @param result 带有方块碰撞信息的 HitResult
     */
    private void performBounce(BlockHitResult result) {
        Vec3 motion = this.getDeltaMovement(); // 获取当前速度
        double motionMagnitude = motion.length(); // 获取速度大小

        // 如果撞击时速度已经很小，就没必要弹跳了，直接触发效果，防止无限微小弹跳
        if (motionMagnitude < 0.1D) {
            this.bouncesLeft = 0;
            onImpact(result);
            return;
        }

        // 计算反射向量，这是物理学中的标准反射公式
        // 1. 获取碰撞面的法线向量
        Vec3 normal = new Vec3(result.getDirection().step());
        // 2. 计算反射向量: reflection = motion - 2 * (motion · normal) * normal
        Vec3 reflection = motion.subtract(normal.scale(2.0D * motion.dot(normal)));

        // 将反射后的向量乘以弹跳能量系数，模拟能量损失
        Vec3 newMotion = reflection.scale(this.bouncePower);

        // 更新实体的速度为弹跳后的新速度
        this.setDeltaMovement(newMotion);

        // 如果弹跳后的速度过小，也直接触发效果
        if (newMotion.length() < 0.05D) {
            this.bouncesLeft = 0;
            onImpact(result);
        }

        // (可选) 在弹跳点生成一些粒子效果
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    /**
     * 抽象方法，用于定义手榴弹在碰撞或弹跳结束后应该做什么（例如爆炸、产生烟雾等）。
     * @param result 碰撞结果信息
     */
    protected abstract void onImpact(HitResult result);
}