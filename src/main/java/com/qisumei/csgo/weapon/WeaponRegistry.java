package com.qisumei.csgo.weapon;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 武器注册表 - 管理所有武器定义
 * 这个类是武器系统的核心，集中管理所有武器的定义和注册
 */
public class WeaponRegistry {
    private static final Map<String, WeaponDefinition> WEAPONS = new LinkedHashMap<>();
    private static boolean initialized = false;

    /**
     * 注册一个武器定义
     */
    public static void register(WeaponDefinition definition) {
        WEAPONS.put(definition.getWeaponId(), definition);
    }

    /**
     * 根据武器ID获取武器定义
     */
    public static Optional<WeaponDefinition> getWeapon(String weaponId) {
        return Optional.ofNullable(WEAPONS.get(weaponId));
    }

    /**
     * 获取所有武器定义
     */
    public static Collection<WeaponDefinition> getAllWeapons() {
        return new ArrayList<>(WEAPONS.values());
    }

    /**
     * 根据武器类型获取所有武器
     */
    public static List<WeaponDefinition> getWeaponsByType(WeaponType type) {
        return WEAPONS.values().stream()
            .filter(w -> w.getType() == type)
            .collect(Collectors.toList());
    }

    /**
     * 获取指定队伍可用的所有武器
     */
    public static List<WeaponDefinition> getWeaponsForTeam(String team) {
        return WEAPONS.values().stream()
            .filter(w -> w.isAvailableForTeam(team))
            .collect(Collectors.toList());
    }

    /**
     * 获取指定队伍和类型的武器
     */
    public static List<WeaponDefinition> getWeaponsByTypeAndTeam(WeaponType type, String team) {
        return WEAPONS.values().stream()
            .filter(w -> w.getType() == type && w.isAvailableForTeam(team))
            .collect(Collectors.toList());
    }

    /**
     * 检查武器是否已注册
     */
    public static boolean isRegistered(String weaponId) {
        return WEAPONS.containsKey(weaponId);
    }

    /**
     * 初始化所有武器定义
     * 这个方法应该在模组加载时调用一次
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        // 注册所有武器
        registerPistols();
        registerSmgs();
        registerRifles();
        registerSnipers();
        registerGrenades();
        registerArmor();

        initialized = true;
    }

    /**
     * 注册手枪
     */
    private static void registerPistols() {
        register(new WeaponDefinition.Builder("tacz:glock_17", "Glock-17", WeaponType.PISTOL)
            .price(2)
            .killReward(3)
            .ammoType(AmmoType.AMMO_9MM)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("tacz:m9", "M9", WeaponType.PISTOL)
            .price(3)
            .killReward(3)
            .ammoType(AmmoType.AMMO_9MM)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("tacz:deagle", "沙漠之鹰", WeaponType.PISTOL)
            .price(7)
            .killReward(3)
            .ammoType(AmmoType.AMMO_50AE)
            .bothTeams()
            .build());
    }

    /**
     * 注册冲锋枪
     */
    private static void registerSmgs() {
        register(new WeaponDefinition.Builder("tacz:mp7", "MP7", WeaponType.SMG)
            .price(15)
            .killReward(6)
            .ammoType(AmmoType.AMMO_46)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("tacz:ump45", "UMP-45", WeaponType.SMG)
            .price(12)
            .killReward(6)
            .ammoType(AmmoType.AMMO_45ACP)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("tacz:p90", "P90", WeaponType.SMG)
            .price(23)
            .killReward(6)
            .ammoType(AmmoType.AMMO_57)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("tacz:mp5", "MP5", WeaponType.SMG)
            .price(14)
            .killReward(6)
            .ammoType(AmmoType.AMMO_9MM)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("tacz:vector", "Vector", WeaponType.SMG)
            .price(22)
            .killReward(6)
            .ammoType(AmmoType.AMMO_45ACP)
            .bothTeams()
            .build());
    }

    /**
     * 注册步枪
     */
    private static void registerRifles() {
        register(new WeaponDefinition.Builder("tacz:ak47", "AK-47", WeaponType.RIFLE)
            .price(27)
            .killReward(3)
            .ammoType(AmmoType.AMMO_762)
            .addAttachment(WeaponAttachment.ACOG_SCOPE)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("tacz:m4a1", "M4A1", WeaponType.RIFLE)
            .price(31)
            .killReward(3)
            .ammoType(AmmoType.AMMO_556)
            .addAttachment(WeaponAttachment.ACOG_SCOPE)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("tacz:aug", "AUG", WeaponType.RIFLE)
            .price(33)
            .killReward(3)
            .ammoType(AmmoType.AMMO_556)
            .addAttachment(WeaponAttachment.ACOG_SCOPE)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("tacz:sg552", "SG 552", WeaponType.RIFLE)
            .price(30)
            .killReward(3)
            .ammoType(AmmoType.AMMO_556)
            .addAttachment(WeaponAttachment.ACOG_SCOPE)
            .bothTeams()
            .build());
    }

    /**
     * 注册狙击枪
     */
    private static void registerSnipers() {
        register(new WeaponDefinition.Builder("tacz:awp", "AWP", WeaponType.SNIPER)
            .price(47)
            .killReward(1)
            .ammoType(AmmoType.AMMO_338)
            .addAttachment(WeaponAttachment.SCOPE_8X)
            .bothTeams()
            .build());
    }

    /**
     * 注册投掷物
     */
    private static void registerGrenades() {
        register(new WeaponDefinition.Builder("tacz:frag_grenade", "手雷", WeaponType.GRENADE)
            .price(3)
            .killReward(3)
            .bothTeams()
            .build());
        register(new WeaponDefinition.Builder("qiscsgo:smoke_grenade", "烟雾弹", WeaponType.GRENADE)
            .price(3)
            .killReward(0)
            .bothTeams()
            .build());
    }

    /**
     * 注册护甲（虽然不是武器，但也在商店中）
     */
    private static void registerArmor() {
        register(new WeaponDefinition.Builder("minecraft:leather_chestplate", "护甲", WeaponType.HEAVY)
            .price(3)
            .killReward(0)
            .bothTeams()
            .build());

        register(new WeaponDefinition.Builder("minecraft:iron_chestplate", "护甲+头盔", WeaponType.HEAVY)
            .price(10)
            .killReward(0)
            .bothTeams()
            .build());
    }

    /**
     * 清空注册表（用于测试）
     */
    public static void clear() {
        WEAPONS.clear();
        initialized = false;
    }
}
