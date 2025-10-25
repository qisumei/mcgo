package com.qisumei.csgo.weapon;

import org.junit.jupiter.api.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 武器注册表测试类
 * 测试WeaponRegistry的所有核心功能
 */
@DisplayName("WeaponRegistry Tests")
class WeaponRegistryTest {

    @BeforeEach
    void setUp() {
        // 清空注册表，确保测试隔离
        WeaponRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        // 测试后清理
        WeaponRegistry.clear();
    }

    @Test
    @DisplayName("应该能注册武器")
    void testRegisterWeapon() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .killReward(3)
            .ammoType(AmmoType.AMMO_762)
            .bothTeams()
            .build();
        
        WeaponRegistry.register(weapon);
        
        Optional<WeaponDefinition> retrieved = WeaponRegistry.getWeapon("test:ak47");
        assertTrue(retrieved.isPresent(), "应该能找到注册的武器");
        assertEquals("AK-47", retrieved.get().getDisplayName(), "武器名称应该正确");
    }

    @Test
    @DisplayName("获取不存在的武器应该返回空Optional")
    void testGetNonExistentWeapon() {
        Optional<WeaponDefinition> weapon = WeaponRegistry.getWeapon("nonexistent:weapon");
        assertTrue(weapon.isEmpty(), "不存在的武器应该返回空Optional");
    }

    @Test
    @DisplayName("应该能检查武器是否已注册")
    void testIsRegistered() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:glock", "Glock", WeaponType.PISTOL)
            .price(2)
            .bothTeams()
            .build();
        
        assertFalse(WeaponRegistry.isRegistered("test:glock"), "注册前应该返回false");
        
        WeaponRegistry.register(weapon);
        
        assertTrue(WeaponRegistry.isRegistered("test:glock"), "注册后应该返回true");
    }

    @Test
    @DisplayName("应该能获取所有武器")
    void testGetAllWeapons() {
        WeaponDefinition weapon1 = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .bothTeams()
            .build();
        WeaponDefinition weapon2 = new WeaponDefinition.Builder("test:m4a1", "M4A1", WeaponType.RIFLE)
            .price(31)
            .bothTeams()
            .build();
        
        WeaponRegistry.register(weapon1);
        WeaponRegistry.register(weapon2);
        
        Collection<WeaponDefinition> allWeapons = WeaponRegistry.getAllWeapons();
        assertEquals(2, allWeapons.size(), "应该返回所有注册的武器");
    }

    @Test
    @DisplayName("应该能按类型获取武器")
    void testGetWeaponsByType() {
        WeaponDefinition pistol = new WeaponDefinition.Builder("test:glock", "Glock", WeaponType.PISTOL)
            .price(2)
            .bothTeams()
            .build();
        WeaponDefinition rifle1 = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .bothTeams()
            .build();
        WeaponDefinition rifle2 = new WeaponDefinition.Builder("test:m4a1", "M4A1", WeaponType.RIFLE)
            .price(31)
            .bothTeams()
            .build();
        
        WeaponRegistry.register(pistol);
        WeaponRegistry.register(rifle1);
        WeaponRegistry.register(rifle2);
        
        List<WeaponDefinition> rifles = WeaponRegistry.getWeaponsByType(WeaponType.RIFLE);
        assertEquals(2, rifles.size(), "应该返回2把步枪");
        
        List<WeaponDefinition> pistols = WeaponRegistry.getWeaponsByType(WeaponType.PISTOL);
        assertEquals(1, pistols.size(), "应该返回1把手枪");
        
        List<WeaponDefinition> snipers = WeaponRegistry.getWeaponsByType(WeaponType.SNIPER);
        assertEquals(0, snipers.size(), "应该返回0把狙击枪");
    }

    @Test
    @DisplayName("应该能按队伍获取武器")
    void testGetWeaponsForTeam() {
        WeaponDefinition bothTeams = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .bothTeams()
            .build();
        WeaponDefinition ctOnly = new WeaponDefinition.Builder("test:m4a1", "M4A1", WeaponType.RIFLE)
            .price(31)
            .ctOnly()
            .build();
        WeaponDefinition tOnly = new WeaponDefinition.Builder("test:sg553", "SG 553", WeaponType.RIFLE)
            .price(30)
            .tOnly()
            .build();
        
        WeaponRegistry.register(bothTeams);
        WeaponRegistry.register(ctOnly);
        WeaponRegistry.register(tOnly);
        
        List<WeaponDefinition> ctWeapons = WeaponRegistry.getWeaponsForTeam("CT");
        assertEquals(2, ctWeapons.size(), "CT队应该能使用2把武器");
        
        List<WeaponDefinition> tWeapons = WeaponRegistry.getWeaponsForTeam("T");
        assertEquals(2, tWeapons.size(), "T队应该能使用2把武器");
    }

    @Test
    @DisplayName("应该能按类型和队伍获取武器")
    void testGetWeaponsByTypeAndTeam() {
        WeaponDefinition pistolBoth = new WeaponDefinition.Builder("test:glock", "Glock", WeaponType.PISTOL)
            .price(2)
            .bothTeams()
            .build();
        WeaponDefinition rifleCT = new WeaponDefinition.Builder("test:m4a1", "M4A1", WeaponType.RIFLE)
            .price(31)
            .ctOnly()
            .build();
        WeaponDefinition rifleT = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .tOnly()
            .build();
        WeaponDefinition rifleBoth = new WeaponDefinition.Builder("test:aug", "AUG", WeaponType.RIFLE)
            .price(33)
            .bothTeams()
            .build();
        
        WeaponRegistry.register(pistolBoth);
        WeaponRegistry.register(rifleCT);
        WeaponRegistry.register(rifleT);
        WeaponRegistry.register(rifleBoth);
        
        List<WeaponDefinition> ctRifles = WeaponRegistry.getWeaponsByTypeAndTeam(WeaponType.RIFLE, "CT");
        assertEquals(2, ctRifles.size(), "CT队应该有2把步枪");
        
        List<WeaponDefinition> tRifles = WeaponRegistry.getWeaponsByTypeAndTeam(WeaponType.RIFLE, "T");
        assertEquals(2, tRifles.size(), "T队应该有2把步枪");
        
        List<WeaponDefinition> ctPistols = WeaponRegistry.getWeaponsByTypeAndTeam(WeaponType.PISTOL, "CT");
        assertEquals(1, ctPistols.size(), "CT队应该有1把手枪");
    }

    @Test
    @DisplayName("初始化应该只执行一次")
    void testInitializeOnce() {
        WeaponRegistry.initialize();
        int firstCount = WeaponRegistry.getAllWeapons().size();
        assertTrue(firstCount > 0, "初始化后应该有武器");
        
        WeaponRegistry.initialize();
        int secondCount = WeaponRegistry.getAllWeapons().size();
        assertEquals(firstCount, secondCount, "第二次初始化不应该重复添加武器");
    }

    @Test
    @DisplayName("初始化应该注册所有预定义的武器")
    void testInitializeRegistersAllWeapons() {
        WeaponRegistry.initialize();
        
        Collection<WeaponDefinition> allWeapons = WeaponRegistry.getAllWeapons();
        assertTrue(allWeapons.size() > 0, "应该注册了武器");
        
        // 检查是否注册了各类武器
        List<WeaponDefinition> pistols = WeaponRegistry.getWeaponsByType(WeaponType.PISTOL);
        assertTrue(pistols.size() > 0, "应该注册了手枪");
        
        List<WeaponDefinition> rifles = WeaponRegistry.getWeaponsByType(WeaponType.RIFLE);
        assertTrue(rifles.size() > 0, "应该注册了步枪");
        
        List<WeaponDefinition> smgs = WeaponRegistry.getWeaponsByType(WeaponType.SMG);
        assertTrue(smgs.size() > 0, "应该注册了冲锋枪");
    }

    @Test
    @DisplayName("清空应该移除所有武器")
    void testClear() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .bothTeams()
            .build();
        
        WeaponRegistry.register(weapon);
        assertTrue(WeaponRegistry.isRegistered("test:ak47"), "武器应该被注册");
        
        WeaponRegistry.clear();
        assertFalse(WeaponRegistry.isRegistered("test:ak47"), "清空后武器应该被移除");
        assertEquals(0, WeaponRegistry.getAllWeapons().size(), "清空后应该没有武器");
    }

    @Test
    @DisplayName("清空后应该能重新初始化")
    void testClearAndReinitialize() {
        WeaponRegistry.initialize();
        int initialCount = WeaponRegistry.getAllWeapons().size();
        
        WeaponRegistry.clear();
        assertEquals(0, WeaponRegistry.getAllWeapons().size(), "清空后应该没有武器");
        
        WeaponRegistry.initialize();
        assertEquals(initialCount, WeaponRegistry.getAllWeapons().size(), 
            "重新初始化后应该恢复相同数量的武器");
    }

    @Test
    @DisplayName("注册相同ID的武器应该覆盖原有武器")
    void testRegisterDuplicateId() {
        WeaponDefinition weapon1 = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .bothTeams()
            .build();
        WeaponDefinition weapon2 = new WeaponDefinition.Builder("test:ak47", "AK-47 Modified", WeaponType.RIFLE)
            .price(30)
            .bothTeams()
            .build();
        
        WeaponRegistry.register(weapon1);
        WeaponRegistry.register(weapon2);
        
        Optional<WeaponDefinition> retrieved = WeaponRegistry.getWeapon("test:ak47");
        assertTrue(retrieved.isPresent(), "应该找到武器");
        assertEquals("AK-47 Modified", retrieved.get().getDisplayName(), 
            "应该使用最后注册的武器定义");
        assertEquals(30, retrieved.get().getPrice(), "价格应该是最后注册的值");
    }
}
