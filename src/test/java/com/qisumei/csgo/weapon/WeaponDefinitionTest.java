package com.qisumei.csgo.weapon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 武器定义测试类
 * 测试WeaponDefinition和Builder模式的正确性
 */
@DisplayName("WeaponDefinition Tests")
class WeaponDefinitionTest {

    @Test
    @DisplayName("应该能通过Builder创建基本武器")
    void testBasicWeaponCreation() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .killReward(3)
            .bothTeams()
            .build();
        
        assertEquals("test:ak47", weapon.getWeaponId(), "武器ID应该正确");
        assertEquals("AK-47", weapon.getDisplayName(), "显示名称应该正确");
        assertEquals(WeaponType.RIFLE, weapon.getType(), "武器类型应该正确");
        assertEquals(27, weapon.getPrice(), "价格应该正确");
        assertEquals(3, weapon.getKillReward(), "击杀奖励应该正确");
    }

    @Test
    @DisplayName("应该能设置弹药类型")
    void testAmmoType() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .ammoType(AmmoType.AMMO_762)
            .bothTeams()
            .build();
        
        assertEquals(AmmoType.AMMO_762, weapon.getAmmoType(), "弹药类型应该正确");
    }

    @Test
    @DisplayName("应该能设置默认弹药数量")
    void testDefaultAmmoAmount() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .defaultAmmoAmount(90)
            .bothTeams()
            .build();
        
        assertEquals(90, weapon.getDefaultAmmoAmount(), "默认弹药数量应该正确");
    }

    @Test
    @DisplayName("应该能添加附件")
    void testAddAttachments() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:l96a1", "L96A1", WeaponType.SNIPER)
            .price(47)
            .addAttachment(WeaponAttachment.SCOPE_8X)
            .addAttachment(WeaponAttachment.ACOG_SCOPE)
            .bothTeams()
            .build();
        
        List<WeaponAttachment> attachments = weapon.getDefaultAttachments();
        assertEquals(2, attachments.size(), "应该有2个附件");
        assertTrue(attachments.contains(WeaponAttachment.SCOPE_8X), "应该包含8倍镜");
        assertTrue(attachments.contains(WeaponAttachment.ACOG_SCOPE), "应该包含ACOG瞄准镜");
    }

    @Test
    @DisplayName("应该能获取默认瞄准镜")
    void testGetDefaultScope() {
        WeaponDefinition weaponWithScope = new WeaponDefinition.Builder("test:l96a1", "L96A1", WeaponType.SNIPER)
            .price(47)
            .addAttachment(WeaponAttachment.SCOPE_8X)
            .bothTeams()
            .build();
        
        Optional<WeaponAttachment> scope = weaponWithScope.getDefaultScope();
        assertTrue(scope.isPresent(), "应该有默认瞄准镜");
        assertEquals(WeaponAttachment.SCOPE_8X, scope.get(), "默认瞄准镜应该是8倍镜");
        
        WeaponDefinition weaponWithoutScope = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .bothTeams()
            .build();
        
        assertFalse(weaponWithoutScope.getDefaultScope().isPresent(), "没有瞄准镜时应该返回空");
    }

    @Test
    @DisplayName("bothTeams应该设置两个队伍都可用")
    void testBothTeams() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .bothTeams()
            .build();
        
        assertTrue(weapon.isAvailableForCT(), "CT队应该可用");
        assertTrue(weapon.isAvailableForT(), "T队应该可用");
        assertTrue(weapon.isAvailableForTeam("CT"), "isAvailableForTeam(CT)应该返回true");
        assertTrue(weapon.isAvailableForTeam("T"), "isAvailableForTeam(T)应该返回true");
    }

    @Test
    @DisplayName("ctOnly应该只对CT队可用")
    void testCtOnly() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:m4a1", "M4A1", WeaponType.RIFLE)
            .price(31)
            .ctOnly()
            .build();
        
        assertTrue(weapon.isAvailableForCT(), "CT队应该可用");
        assertFalse(weapon.isAvailableForT(), "T队不应该可用");
        assertTrue(weapon.isAvailableForTeam("CT"), "isAvailableForTeam(CT)应该返回true");
        assertFalse(weapon.isAvailableForTeam("T"), "isAvailableForTeam(T)应该返回false");
    }

    @Test
    @DisplayName("tOnly应该只对T队可用")
    void testTOnly() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .tOnly()
            .build();
        
        assertFalse(weapon.isAvailableForCT(), "CT队不应该可用");
        assertTrue(weapon.isAvailableForT(), "T队应该可用");
        assertFalse(weapon.isAvailableForTeam("CT"), "isAvailableForTeam(CT)应该返回false");
        assertTrue(weapon.isAvailableForTeam("T"), "isAvailableForTeam(T)应该返回true");
    }

    @Test
    @DisplayName("isAvailableForTeam应该对无效队伍返回false")
    void testIsAvailableForInvalidTeam() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .bothTeams()
            .build();
        
        assertFalse(weapon.isAvailableForTeam("INVALID"), "无效队伍应该返回false");
        assertFalse(weapon.isAvailableForTeam(""), "空字符串应该返回false");
        assertFalse(weapon.isAvailableForTeam(null), "null应该返回false");
    }

    @Test
    @DisplayName("isAvailableForTeam应该不区分大小写")
    void testIsAvailableForTeamCaseInsensitive() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .bothTeams()
            .build();
        
        assertTrue(weapon.isAvailableForTeam("ct"), "小写ct应该可用");
        assertTrue(weapon.isAvailableForTeam("Ct"), "混合大小写Ct应该可用");
        assertTrue(weapon.isAvailableForTeam("t"), "小写t应该可用");
        assertTrue(weapon.isAvailableForTeam("T"), "大写T应该可用");
    }

    @Test
    @DisplayName("getDefaultAttachments应该返回副本而非原始列表")
    void testGetDefaultAttachmentsReturnsCopy() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:l96a1", "L96A1", WeaponType.SNIPER)
            .price(47)
            .addAttachment(WeaponAttachment.SCOPE_8X)
            .bothTeams()
            .build();
        
        List<WeaponAttachment> attachments1 = weapon.getDefaultAttachments();
        List<WeaponAttachment> attachments2 = weapon.getDefaultAttachments();
        
        assertNotSame(attachments1, attachments2, "每次调用应该返回新的列表副本");
        
        // 修改返回的列表不应该影响原始数据
        attachments1.clear();
        assertEquals(1, weapon.getDefaultAttachments().size(), 
            "修改返回的列表不应该影响原始数据");
    }

    @Test
    @DisplayName("应该能创建没有弹药的武器（如护甲）")
    void testWeaponWithoutAmmo() {
        WeaponDefinition armor = new WeaponDefinition.Builder("minecraft:iron_chestplate", "护甲", WeaponType.HEAVY)
            .price(10)
            .killReward(0)
            .defaultAmmoAmount(0)
            .bothTeams()
            .build();
        
        assertEquals(AmmoType.NONE, armor.getAmmoType(), "护甲的弹药类型应该是NONE");
        assertEquals(0, armor.getDefaultAmmoAmount(), "护甲默认弹药数量应该为0");
    }

    @Test
    @DisplayName("应该能创建手雷类型武器")
    void testGrenadeWeapon() {
        WeaponDefinition grenade = new WeaponDefinition.Builder("pointblank:grenade", "手雷", WeaponType.GRENADE)
            .price(3)
            .killReward(3)
            .bothTeams()
            .build();
        
        assertEquals(WeaponType.GRENADE, grenade.getType(), "类型应该是手雷");
        assertEquals(3, grenade.getPrice(), "手雷价格应该正确");
    }

    @Test
    @DisplayName("价格和奖励应该可以是0")
    void testZeroPriceAndReward() {
        WeaponDefinition weapon = new WeaponDefinition.Builder("test:free_weapon", "免费武器", WeaponType.PISTOL)
            .price(0)
            .killReward(0)
            .bothTeams()
            .build();
        
        assertEquals(0, weapon.getPrice(), "价格可以是0");
        assertEquals(0, weapon.getKillReward(), "击杀奖励可以是0");
    }
}
