package com.qisumei.csgo.weapon;

/**
 * 弹药类型枚举 - 定义所有弹药类型及其对应的物品ID
 */
public enum AmmoType {
    AMMO_9MM("pointblank:ammo9mm", "9mm"),
    AMMO_45ACP("pointblank:ammo45acp", ".45 ACP"),
    AMMO_50AE("pointblank:ammo50ae", ".50 AE"),
    AMMO_46("pointblank:ammo46", "4.6mm"),
    AMMO_57("pointblank:ammo57", "5.7mm"),
    AMMO_556("pointblank:ammo556", "5.56mm"),
    AMMO_762("pointblank:ammo762", "7.62mm"),
    AMMO_338("pointblank:ammo338lapua", ".338 Lapua"),
    NONE("", "无弹药");

    private final String itemId;
    private final String displayName;

    AmmoType(String itemId, String displayName) {
        this.itemId = itemId;
        this.displayName = displayName;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean hasAmmo() {
        return !itemId.isEmpty();
    }
}
