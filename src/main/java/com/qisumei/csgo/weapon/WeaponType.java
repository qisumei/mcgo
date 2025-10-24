package com.qisumei.csgo.weapon;

/**
 * 武器类型枚举 - 定义所有武器类别
 */
public enum WeaponType {
    KNIFE("近战", 1500, 300),
    PISTOL("手枪", 300, 300),
    SMG("冲锋枪", 600, 600),
    HEAVY("重型武器", 300, 300),
    RIFLE("步枪", 300, 300),
    SNIPER("狙击枪", 100, 100),
    GRENADE("投掷物", 300, 300);

    private final String displayName;
    private final int defaultKillReward;
    private final int defaultPrice;

    WeaponType(String displayName, int defaultKillReward, int defaultPrice) {
        this.displayName = displayName;
        this.defaultKillReward = defaultKillReward;
        this.defaultPrice = defaultPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultKillReward() {
        return defaultKillReward;
    }

    public int getDefaultPrice() {
        return defaultPrice;
    }
}
