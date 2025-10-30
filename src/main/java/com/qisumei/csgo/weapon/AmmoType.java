package com.qisumei.csgo.weapon;

/**
 * 弹药类型枚举 - 定义所有弹药类型及其对应的物品ID
 */
public enum AmmoType {
    AMMO_9MM("tacz:ammo/9mm", "9mm"),
    AMMO_45ACP("tacz:ammo/45acp", ".45 ACP"),
    AMMO_50AE("tacz:ammo/50ae", ".50 AE"),
    AMMO_46("tacz:ammo/46x30mm", "4.6mm"),
    AMMO_57("tacz:ammo/57x28mm", "5.7mm"),
    AMMO_556("tacz:ammo/556x45mm", "5.56mm"),
    AMMO_762("tacz:ammo/762x39mm", "7.62mm"),
    AMMO_338("tacz:ammo/338", ".338 Lapua"),
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
