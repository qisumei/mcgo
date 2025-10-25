package com.qisumei.csgo.economy;

import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 虚拟货币管理器测试类
 * 测试VirtualMoneyManager的所有核心功能
 */
@DisplayName("VirtualMoneyManager Tests")
class VirtualMoneyManagerTest {

    @Mock
    private ServerPlayer mockPlayer;

    private VirtualMoneyManager manager;
    private UUID testPlayerUUID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = VirtualMoneyManager.getInstance();
        testPlayerUUID = UUID.randomUUID();
        when(mockPlayer.getUUID()).thenReturn(testPlayerUUID);
        
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
        int money = manager.getMoney(mockPlayer);
        assertEquals(0, money, "新玩家的货币应该为0");
    }

    @Test
    @DisplayName("通过UUID获取新玩家货币应该为0")
    void testGetMoneyByUUIDForNewPlayer() {
        int money = manager.getMoney(testPlayerUUID);
        assertEquals(0, money, "新玩家的货币应该为0");
    }

    @Test
    @DisplayName("设置货币应该成功")
    void testSetMoney() {
        manager.setMoney(mockPlayer, 800);
        assertEquals(800, manager.getMoney(mockPlayer), "货币应该被设置为800");
    }

    @Test
    @DisplayName("设置负数货币应该被限制为0")
    void testSetMoneyNegative() {
        manager.setMoney(mockPlayer, -100);
        assertEquals(0, manager.getMoney(mockPlayer), "负数货币应该被限制为0");
    }

    @Test
    @DisplayName("设置超过最大值的货币应该被限制")
    void testSetMoneyOverMax() {
        manager.setMoney(mockPlayer, 100000);
        assertEquals(65535, manager.getMoney(mockPlayer), "货币应该被限制在最大值65535");
    }

    @Test
    @DisplayName("增加货币应该成功")
    void testAddMoney() {
        manager.setMoney(mockPlayer, 500);
        manager.addMoney(mockPlayer, 300);
        assertEquals(800, manager.getMoney(mockPlayer), "货币应该增加到800");
    }

    @Test
    @DisplayName("增加货币到新玩家应该成功")
    void testAddMoneyToNewPlayer() {
        manager.addMoney(mockPlayer, 800);
        assertEquals(800, manager.getMoney(mockPlayer), "新玩家增加货币后应该为800");
    }

    @Test
    @DisplayName("增加负数或零货币应该被忽略")
    void testAddMoneyNegativeOrZero() {
        manager.setMoney(mockPlayer, 500);
        manager.addMoney(mockPlayer, -100);
        assertEquals(500, manager.getMoney(mockPlayer), "负数增加应该被忽略");
        
        manager.addMoney(mockPlayer, 0);
        assertEquals(500, manager.getMoney(mockPlayer), "零增加应该被忽略");
    }

    @Test
    @DisplayName("增加货币不应该超过最大值")
    void testAddMoneyWithOverflow() {
        manager.setMoney(mockPlayer, 65000);
        manager.addMoney(mockPlayer, 1000);
        assertEquals(65535, manager.getMoney(mockPlayer), "货币应该被限制在最大值65535");
    }

    @Test
    @DisplayName("扣除货币应该成功")
    void testTakeMoney() {
        manager.setMoney(mockPlayer, 800);
        boolean success = manager.takeMoney(mockPlayer, 300);
        assertTrue(success, "扣除货币应该成功");
        assertEquals(500, manager.getMoney(mockPlayer), "剩余货币应该为500");
    }

    @Test
    @DisplayName("余额不足时扣除应该失败")
    void testTakeMoneyInsufficientFunds() {
        manager.setMoney(mockPlayer, 200);
        boolean success = manager.takeMoney(mockPlayer, 500);
        assertFalse(success, "余额不足时扣除应该失败");
        assertEquals(200, manager.getMoney(mockPlayer), "货币不应该被扣除");
    }

    @Test
    @DisplayName("扣除负数或零货币应该总是成功")
    void testTakeMoneyNegativeOrZero() {
        manager.setMoney(mockPlayer, 500);
        assertTrue(manager.takeMoney(mockPlayer, -100), "扣除负数应该总是成功");
        assertTrue(manager.takeMoney(mockPlayer, 0), "扣除零应该总是成功");
        assertEquals(500, manager.getMoney(mockPlayer), "货币不应该改变");
    }

    @Test
    @DisplayName("检查余额应该正确")
    void testHasMoney() {
        manager.setMoney(mockPlayer, 800);
        assertTrue(manager.hasMoney(mockPlayer, 500), "应该有足够的货币");
        assertTrue(manager.hasMoney(mockPlayer, 800), "应该有刚好的货币");
        assertFalse(manager.hasMoney(mockPlayer, 1000), "应该没有足够的货币");
    }

    @Test
    @DisplayName("清除玩家货币应该成功")
    void testClearMoney() {
        manager.setMoney(mockPlayer, 800);
        manager.clearMoney(mockPlayer);
        assertEquals(0, manager.getMoney(mockPlayer), "清除后货币应该为0");
    }

    @Test
    @DisplayName("清除所有货币应该成功")
    void testClearAll() {
        ServerPlayer mockPlayer2 = mock(ServerPlayer.class);
        UUID uuid2 = UUID.randomUUID();
        when(mockPlayer2.getUUID()).thenReturn(uuid2);
        
        manager.setMoney(mockPlayer, 800);
        manager.setMoney(mockPlayer2, 1000);
        
        manager.clearAll();
        
        assertEquals(0, manager.getMoney(mockPlayer), "所有货币应该被清除");
        assertEquals(0, manager.getMoney(mockPlayer2), "所有货币应该被清除");
    }

    @Test
    @DisplayName("传入null玩家应该抛出异常")
    void testNullPlayerThrowsException() {
        assertThrows(NullPointerException.class, () -> manager.getMoney((ServerPlayer) null),
            "传入null玩家应该抛出NullPointerException");
        assertThrows(NullPointerException.class, () -> manager.setMoney(null, 800),
            "传入null玩家应该抛出NullPointerException");
        assertThrows(NullPointerException.class, () -> manager.addMoney(null, 100),
            "传入null玩家应该抛出NullPointerException");
        assertThrows(NullPointerException.class, () -> manager.takeMoney(null, 100),
            "传入null玩家应该抛出NullPointerException");
        assertThrows(NullPointerException.class, () -> manager.hasMoney(null, 100),
            "传入null玩家应该抛出NullPointerException");
        assertThrows(NullPointerException.class, () -> manager.clearMoney(null),
            "传入null玩家应该抛出NullPointerException");
    }

    @Test
    @DisplayName("传入null UUID应该抛出异常")
    void testNullUUIDThrowsException() {
        assertThrows(NullPointerException.class, () -> manager.getMoney((UUID) null),
            "传入null UUID应该抛出NullPointerException");
    }

    @Test
    @DisplayName("并发操作应该线程安全")
    void testConcurrentOperations() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        
        // 初始货币
        manager.setMoney(mockPlayer, 5000);
        
        // 创建多个线程并发增加货币
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    manager.addMoney(mockPlayer, 1);
                }
            });
            threads[i].start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证最终结果
        int expectedMoney = 5000 + (threadCount * operationsPerThread);
        assertEquals(expectedMoney, manager.getMoney(mockPlayer),
            "并发操作后货币应该正确累加");
    }
}
