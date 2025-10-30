package com.qisumei.csgo.weapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 武器定义类 - 封装武器的所有属性和行为
 * 这个类作为 TaCZ 武器系统的抽象层，使得添加新武器更加简单
 */
public class WeaponDefinition {
    private final String weaponId;           // 武器的物品ID（如 "tacz:ak47"）
    private final String displayName;        // 显示名称
    private final WeaponType type;           // 武器类型
    private final int price;                 // 价格
    private final int killReward;            // 击杀奖励
    private final AmmoType ammoType;         // 弹药类型
    private final int defaultAmmoAmount;     // 默认赠送的弹药数量
    private final List<WeaponAttachment> defaultAttachments; // 默认附件
    private final boolean availableForCT;    // CT队是否可用
    private final boolean availableForT;     // T队是否可用

    private WeaponDefinition(Builder builder) {
        this.weaponId = builder.weaponId;
        this.displayName = builder.displayName;
        this.type = builder.type;
        this.price = builder.price;
        this.killReward = builder.killReward;
        this.ammoType = builder.ammoType;
        this.defaultAmmoAmount = builder.defaultAmmoAmount;
        this.defaultAttachments = new ArrayList<>(builder.defaultAttachments);
        this.availableForCT = builder.availableForCT;
        this.availableForT = builder.availableForT;
    }

    // Getters
    public String getWeaponId() {
        return weaponId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public WeaponType getType() {
        return type;
    }

    public int getPrice() {
        return price;
    }

    public int getKillReward() {
        return killReward;
    }

    public AmmoType getAmmoType() {
        return ammoType;
    }

    public int getDefaultAmmoAmount() {
        return defaultAmmoAmount;
    }

    public List<WeaponAttachment> getDefaultAttachments() {
        return new ArrayList<>(defaultAttachments);
    }

    public boolean isAvailableForCT() {
        return availableForCT;
    }

    public boolean isAvailableForT() {
        return availableForT;
    }

    public boolean isAvailableForTeam(String team) {
        if ("CT".equalsIgnoreCase(team)) {
            return availableForCT;
        } else if ("T".equalsIgnoreCase(team)) {
            return availableForT;
        }
        return false;
    }

    /**
     * 获取默认瞄准镜附件（如果有）
     */
    public Optional<WeaponAttachment> getDefaultScope() {
        return defaultAttachments.stream()
            .filter(att -> att.getType() == WeaponAttachment.AttachmentType.SCOPE)
            .findFirst();
    }

    /**
     * Builder 模式用于构建武器定义
     */
    public static class Builder {
        private final String weaponId;
        private final String displayName;
        private final WeaponType type;
        
        private int price;
        private int killReward;
        private AmmoType ammoType = AmmoType.NONE;
        private int defaultAmmoAmount = 64;
        private final List<WeaponAttachment> defaultAttachments = new ArrayList<>();
        private boolean availableForCT = true;
        private boolean availableForT = true;

        public Builder(String weaponId, String displayName, WeaponType type) {
            this.weaponId = weaponId;
            this.displayName = displayName;
            this.type = type;
            this.killReward = type.getDefaultKillReward();
            this.price = type.getDefaultPrice();
        }

        public Builder price(int price) {
            this.price = price;
            return this;
        }

        public Builder killReward(int killReward) {
            this.killReward = killReward;
            return this;
        }

        public Builder ammoType(AmmoType ammoType) {
            this.ammoType = ammoType;
            return this;
        }

        public Builder defaultAmmoAmount(int amount) {
            this.defaultAmmoAmount = amount;
            return this;
        }

        public Builder addAttachment(WeaponAttachment attachment) {
            this.defaultAttachments.add(attachment);
            return this;
        }

        public Builder availableForCT(boolean available) {
            this.availableForCT = available;
            return this;
        }

        public Builder availableForT(boolean available) {
            this.availableForT = available;
            return this;
        }

        public Builder bothTeams() {
            this.availableForCT = true;
            this.availableForT = true;
            return this;
        }

        public Builder ctOnly() {
            this.availableForCT = true;
            this.availableForT = false;
            return this;
        }

        public Builder tOnly() {
            this.availableForCT = false;
            this.availableForT = true;
            return this;
        }

        public WeaponDefinition build() {
            return new WeaponDefinition(this);
        }
    }
}
