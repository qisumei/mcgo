package com.qisumei.csgo.game;

import com.qisumei.csgo.config.ServerConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 经济管理器测试类
 * 测试EconomyManager的奖励计算逻辑
 * 
 * 注意：由于EconomyManager依赖Minecraft组件，部分方法需要在实际游戏环境中测试
 * 这里主要测试可以独立测试的逻辑部分
 */
@DisplayName("EconomyManager Tests")
class EconomyManagerTest {

    @BeforeAll
    static void setUp() {
        // 确保ServerConfig被初始化
        // 在实际测试中，这些值应该从配置文件加载
        // 这里我们假设默认值已经设置
    }

    @Test
    @DisplayName("空物品应该返回默认手枪奖励")
    void testGetRewardForKillWithEmptyItem() {
        ItemStack emptyStack = ItemStack.EMPTY;
        int reward = EconomyManager.getRewardForKill(emptyStack);
        assertEquals(ServerConfig.killRewardPistol, reward, 
            "空物品应该返回默认手枪击杀奖励");
    }

    @Test
    @DisplayName("null物品应该返回默认手枪奖励")
    void testGetRewardForKillWithNullItem() {
        int reward = EconomyManager.getRewardForKill(null);
        assertEquals(ServerConfig.killRewardPistol, reward, 
            "null物品应该返回默认手枪击杀奖励");
    }

    @Test
    @DisplayName("普通物品应该返回默认手枪奖励")
    void testGetRewardForKillWithRegularItem() {
        // 由于需要NBT数据来匹配武器，普通物品会返回默认值
        ItemStack regularItem = new ItemStack(Items.DIAMOND);
        int reward = EconomyManager.getRewardForKill(regularItem);
        assertEquals(ServerConfig.killRewardPistol, reward, 
            "未匹配的武器应该返回默认手枪击杀奖励");
    }

    @Test
    @DisplayName("ServerConfig奖励值应该是正数")
    void testRewardValuesArePositive() {
        assertTrue(ServerConfig.killRewardKnife >= 0, "近战击杀奖励应该是非负数");
        assertTrue(ServerConfig.killRewardPistol >= 0, "手枪击杀奖励应该是非负数");
        assertTrue(ServerConfig.killRewardSmg >= 0, "冲锋枪击杀奖励应该是非负数");
        assertTrue(ServerConfig.killRewardRifle >= 0, "步枪击杀奖励应该是非负数");
        assertTrue(ServerConfig.killRewardAwp >= 0, "狙击枪击杀奖励应该是非负数");
        assertTrue(ServerConfig.killRewardGrenade >= 0, "手雷击杀奖励应该是非负数");
        assertTrue(ServerConfig.killRewardHeavy >= 0, "重型武器击杀奖励应该是非负数");
    }

    @Test
    @DisplayName("ServerConfig奖励配置应该符合CSGO平衡")
    void testRewardBalancing() {
        // 验证奖励配置符合CSGO的平衡理念
        // 通常：近战 > 手雷 >= 冲锋枪 > 步枪/手枪 > 狙击枪
        assertTrue(ServerConfig.killRewardKnife >= ServerConfig.killRewardSmg,
            "近战击杀奖励应该 >= 冲锋枪奖励");
        assertTrue(ServerConfig.killRewardSmg >= ServerConfig.killRewardRifle,
            "冲锋枪击杀奖励应该 >= 步枪奖励");
        assertTrue(ServerConfig.killRewardRifle >= ServerConfig.killRewardAwp,
            "步枪击杀奖励应该 >= 狙击枪奖励");
    }

    /**
     * 集成测试说明：
     * 
     * 以下方法需要在实际Minecraft环境中测试，因为它们依赖于：
     * - ServerPlayer 对象
     * - VirtualMoneyManager 状态
     * - 游戏消息系统
     * 
     * 建议在游戏中使用以下方式测试：
     * 1. giveMoney(ServerPlayer, int) - 检查货币增加和消息显示
     * 2. takeMoney(ServerPlayer, int) - 检查货币扣除和余额不足处理
     * 3. getMoney(ServerPlayer) - 检查货币查询
     * 4. setMoney(ServerPlayer, int) - 检查货币设置
     * 5. clearMoney(ServerPlayer) - 检查货币清除
     * 
     * 这些方法的正确性可以通过 VirtualMoneyManagerTest 间接验证，
     * 因为 EconomyManager 主要是对 VirtualMoneyManager 的包装。
     */
    
    @Test
    @DisplayName("EconomyManager不应该被实例化")
    void testCannotInstantiate() {
        // EconomyManager是工具类，构造函数应该是私有的
        // 这个测试验证类的设计意图
        
        // 由于Java反射可以访问私有构造函数，这里主要是文档性测试
        // 实际使用中应该通过静态方法访问
        assertDoesNotThrow(() -> {
            // 尝试通过反射访问构造函数应该能成功（因为它存在）
            var constructor = EconomyManager.class.getDeclaredConstructor();
            assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
                "构造函数应该是私有的");
        });
    }
}
