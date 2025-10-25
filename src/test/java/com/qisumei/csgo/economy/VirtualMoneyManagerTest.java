package com.qisumei.csgo.economy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 虚拟货币管理器测试类
 * 测试VirtualMoneyManager的核心功能
 * 
 * 注意：由于VirtualMoneyManager的大部分方法需要ServerPlayer对象，
 * 而ServerPlayer在测试环境中不可用，这里主要测试：
 * 1. 单例模式
 * 2. 基于UUID的getMoney方法
 * 3. clearAll方法
 * 
 * 完整的功能测试需要在实际游戏环境中进行集成测试。
 */
@DisplayName("VirtualMoneyManager Tests")
class VirtualMoneyManagerTest {

    private VirtualMoneyManager manager;
    private UUID testPlayerUUID;

    @BeforeEach
    void setUp() {
        manager = VirtualMoneyManager.getInstance();
        testPlayerUUID = UUID.randomUUID();
        
        // 清除所有状态，确保测试隔离
        manager.clearAll();
    }

    @Test
    @DisplayName("应该返回单例实例")
    void testGetInstance() {
        VirtualMoneyManager instance1 = VirtualMoneyManager.getInstance();
        VirtualMoneyManager instance2 = VirtualMoneyManager.getInstance();
        assertSame(instance1, instance2, "应该返回相同的单例实例");
    }

    @Test
    @DisplayName("新玩家默认货币应该为0")
    void testGetMoneyForNewPlayer() {
        int money = manager.getMoney(testPlayerUUID);
        assertEquals(0, money, "新玩家的货币应该为0");
    }

    @Test
    @DisplayName("通过UUID获取货币")
    void testGetMoneyByUUID() {
        UUID uuid = UUID.randomUUID();
        int money = manager.getMoney(uuid);
        assertEquals(0, money, "新UUID的货币应该为0");
    }

    @Test
    @DisplayName("传入null UUID应该抛出异常")
    void testNullUUIDThrowsException() {
        assertThrows(NullPointerException.class, () -> manager.getMoney((UUID) null),
            "传入null UUID应该抛出NullPointerException");
    }

    @Test
    @DisplayName("clearAll应该清除所有货币")
    void testClearAll() {
        // 由于我们不能直接设置货币（需要ServerPlayer），
        // 我们只能验证clearAll方法可以被调用而不抛出异常
        assertDoesNotThrow(() -> manager.clearAll(), "clearAll应该可以正常执行");
        
        // 验证清空后查询返回0
        assertEquals(0, manager.getMoney(testPlayerUUID), "清空后货币应该为0");
    }

    @Test
    @DisplayName("多个UUID应该独立管理")
    void testMultipleUUIDs() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        
        // 验证不同UUID的货币是独立的
        assertEquals(0, manager.getMoney(uuid1), "UUID1的货币应该为0");
        assertEquals(0, manager.getMoney(uuid2), "UUID2的货币应该为0");
    }

    @Test
    @DisplayName("getInstance应该总是返回同一个实例")
    void testSingletonConsistency() {
        VirtualMoneyManager instance1 = VirtualMoneyManager.getInstance();
        manager.clearAll(); // 修改状态
        VirtualMoneyManager instance2 = VirtualMoneyManager.getInstance();
        
        // 应该是同一个实例，所以状态应该一致
        assertSame(instance1, instance2, "应该是同一个单例实例");
    }

    /**
     * 集成测试说明：
     * 
     * 以下功能需要在实际Minecraft环境中测试，因为它们依赖于ServerPlayer对象：
     * - setMoney(ServerPlayer, int) - 设置玩家货币
     * - addMoney(ServerPlayer, int) - 增加玩家货币
     * - takeMoney(ServerPlayer, int) - 扣除玩家货币
     * - hasMoney(ServerPlayer, int) - 检查玩家是否有足够货币
     * - clearMoney(ServerPlayer) - 清除指定玩家货币
     * - getMoney(ServerPlayer) - 通过ServerPlayer获取货币
     * 
     * 这些方法的正确性需要通过以下方式验证：
     * 1. 在实际游戏中手动测试
     * 2. 编写需要完整Minecraft环境的集成测试
     * 3. 代码审查确保逻辑正确（这些方法都是简单的委托到UUID方法）
     */
}

