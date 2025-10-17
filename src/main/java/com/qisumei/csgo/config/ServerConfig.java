package com.qisumei.csgo.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

/**
 * 服务器端游戏配置类，用于定义和管理CSGO模组中的各种规则、经济系统、武器分类及初始装备等设置。
 * <p>
 * 此类使用 NeoForge 的 {@link ModConfigSpec} 来构建可序列化的配置文件，并提供静态字段供运行时访问这些配置值。
 * 配置项分为多个逻辑分组（如 Game Rules、Economy、Kill Rewards 等），便于管理和理解。
 */
public class ServerConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // --- 新增配置项 ---
    public static final ModConfigSpec.IntValue PISTOL_ROUND_STARTING_MONEY_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CT_PISTOL_ROUND_GEAR_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> T_PISTOL_ROUND_GEAR_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> INVENTORY_PROTECTED_ITEMS_SPEC;
    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE_ENABLED_SPEC;

    // --- 原有配置项 ---
    public static final ModConfigSpec.IntValue BUY_PHASE_SECONDS_SPEC;
    public static final ModConfigSpec.IntValue ROUND_END_SECONDS_SPEC;
    public static final ModConfigSpec.IntValue WIN_REWARD_SPEC;
    public static final ModConfigSpec.IntValue LOSS_REWARD_SPEC;
    public static final ModConfigSpec.IntValue LOSS_STREAK_BONUS_SPEC;
    public static final ModConfigSpec.IntValue MAX_LOSS_STREAK_BONUS_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_KNIFE_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_PISTOL_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_SMG_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_HEAVY_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_RIFLE_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_AWP_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_GRENADE_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_KNIFE_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_PISTOL_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_SMG_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_HEAVY_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_RIFLE_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_AWP_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_GRENADE_SPEC;

    // --- 新增静态变量 ---
    /**
     * 手枪局或换边后的起始金钱数量。
     */
    public static int pistolRoundStartingMoney;

    /**
     * 反恐精英（CT）在手枪局中获得的初始装备列表。
     */
    public static List<String> ctPistolRoundGear;

    /**
     * 恐怖分子（T）在手枪局中获得的初始装备列表。
     */
    public static List<String> tPistolRoundGear;

    /**
     * 在回合开始时不会被清空的物品 ID 列表（例如护甲、钻石等重要道具）。
     */
    public static List<String> inventoryProtectedItems;

    // --- 原有静态变量 ---
    /**
     * 购买阶段持续的时间（单位：秒）。
     */
    public static int buyPhaseSeconds;

    /**
     * 回合结束后展示结果的时间（单位：秒）。
     */
    public static int roundEndSeconds;

    /**
     * 回合胜利的基础奖励金额。
     */
    public static int winReward;

    /**
     * 回合失败的基础奖励金额。
     */
    public static int lossReward;

    /**
     * 连续失败每一局增加的额外奖金。
     */
    public static int lossStreakBonus;

    /**
     * 最大连败奖励上限（不包括基础失败奖励）。
     */
    public static int maxLossStreakBonus;

    /**
     * 使用近战武器击杀敌人所获得的金钱奖励。
     */
    public static int killRewardKnife;

    /**
     * 使用手枪击杀敌人所获得的金钱奖励。
     */
    public static int killRewardPistol;

    /**
     * 使用冲锋枪击杀敌人所获得的金钱奖励。
     */
    public static int killRewardSmg;

    /**
     * 使用重型武器（如霰弹枪/机枪）击杀敌人所获得的金钱奖励。
     */
    public static int killRewardHeavy;

    /**
     * 使用枪击杀敌人所获得的金钱奖励。
     */
    public static int killRewardRifle;

    /**
     * 使用 AWP 类狙击枪击杀敌人所获得的金钱奖励。
     */
    public static int killRewardAwp;

    /**
     * 使用投掷物击杀敌人所获得的金钱奖励。
     */
    public static int killRewardGrenade;

    /**
     * 被归类为“刀”的物品 ID 列表。
     */
    public static List<String> weaponsKnife;

    /**
     * 被归类为“手枪”的物品 ID 列表。
     */
    public static List<String> weaponsPistol;

    /**
     * 被归类为“冲锋枪”的物品 ID 列表。
     */
    public static List<String> weaponsSmg;

    /**
     * 被归类为“重型武器”（如霰弹枪、轻机枪）的物品 ID 列表。
     */
    public static List<String> weaponsHeavy;

    /**
     * 被归类为“步枪”的物品 ID 列表。
     */
    public static List<String> weaponsRifle;

    /**
     * 被归类为“AWP”类型的物品 ID 列表。
     */
    public static List<String> weaponsAwp;

    /**
     * 被归类为“投掷物”的物品 ID 列表。
     */
    public static List<String> weaponsGrenade;

    /**
     * 是否启用友军伤害功能。
     */
    public static boolean friendlyFireEnabled;

    static {
        // 定义游戏规则相关配置项
        BUILDER.push("Game Rules");
        FRIENDLY_FIRE_ENABLED_SPEC = BUILDER.comment("是否启用友军伤害").define("friendlyFireEnabled", false);
        BUY_PHASE_SECONDS_SPEC = BUILDER.comment("购买阶段持续时间 (秒)").defineInRange("buyPhaseSeconds", 15, 5, 60);
        ROUND_END_SECONDS_SPEC = BUILDER.comment("回合结束展示时间 (秒)").defineInRange("roundEndSeconds", 5, 1, 30);
        BUILDER.pop();

        // 经济系统相关配置项
        BUILDER.push("Economy");
        PISTOL_ROUND_STARTING_MONEY_SPEC = BUILDER.comment("手枪局或换边后第一局的起始资金").defineInRange("pistolRoundStartingMoney", 8, 0, 1000);
        WIN_REWARD_SPEC = BUILDER.comment("回合胜利基础奖励").defineInRange("winReward", 33, 0, 1000);
        LOSS_REWARD_SPEC = BUILDER.comment("回合失败基础奖励").defineInRange("lossReward", 14, 0, 1000);
        LOSS_STREAK_BONUS_SPEC = BUILDER.comment("每额外连败一回合的奖励").defineInRange("lossStreakBonus", 5, 0, 1000);
        MAX_LOSS_STREAK_BONUS_SPEC = BUILDER.comment("连败奖励的上限 (不含基础失败奖励)").defineInRange("maxLossStreakBonus", 35, 0, 1000);
        BUILDER.pop();

        // 击杀奖励配置项
        BUILDER.push("Kill Rewards");
        KILL_REWARD_KNIFE_SPEC = BUILDER.defineInRange("knife", 15, 0, 1000);
        KILL_REWARD_PISTOL_SPEC = BUILDER.defineInRange("pistol", 3, 0, 1000);
        KILL_REWARD_SMG_SPEC = BUILDER.defineInRange("smg", 6, 0, 1000);
        KILL_REWARD_HEAVY_SPEC = BUILDER.defineInRange("heavy", 3, 0, 1000);
        KILL_REWARD_RIFLE_SPEC = BUILDER.defineInRange("rifle", 3, 0, 1000);
        KILL_REWARD_AWP_SPEC = BUILDER.defineInRange("awp", 1, 0, 1000);
        KILL_REWARD_GRENADE_SPEC = BUILDER.defineInRange("grenade", 3, 0, 1000);
        BUILDER.pop();

        // 初始装备配置项
        BUILDER.push("Initial Gear");
        CT_PISTOL_ROUND_GEAR_SPEC = BUILDER.comment("CT方在手枪局获得的初始装备列表").defineList("ctPistolRoundGear", List.of("pointblank:a1_hkusp45"), obj -> obj instanceof String);
        T_PISTOL_ROUND_GEAR_SPEC = BUILDER.comment("T方在手枪局获得的初始装备列表").defineList("tPistolRoundGear", List.of("pointblank:a1_g17"), obj -> obj instanceof String);
        INVENTORY_PROTECTED_ITEMS_SPEC = BUILDER.comment("在回合开始清理背包时, 不会被清除的物品ID列表 (如货币, 护甲等)").defineList("inventoryProtectedItems", List.of("minecraft:diamond", "minecraft:iron_helmet", "minecraft:iron_chestplate"), obj -> obj instanceof String);
        BUILDER.pop();

        // 武器类别映射配置项
        BUILDER.push("Weapon Categories");
        WEAPONS_KNIFE_SPEC = BUILDER.comment("被视为'刀'的物品ID列表").defineList("knifes", List.of("minecraft:diamond_sword", "minecraft:netherite_sword"), obj -> obj instanceof String);
        WEAPONS_PISTOL_SPEC = BUILDER.comment("被视为'手枪'的物品ID列表").defineList("pistols", List.of("pointblank:a1_hkusp45", "pointblank:a1_g17", "pointblank:deserteagle"), obj -> obj instanceof String);
        WEAPONS_SMG_SPEC = BUILDER.comment("被视为'冲锋枪'的物品ID列表").defineList("smgs", List.of("pointblank:mp7", "pointblank:p90", "pointblank:a2_pp19b"), obj -> obj instanceof String);
        WEAPONS_HEAVY_SPEC = BUILDER.comment("被视为'重型武器'的物品ID列表").defineList("heavies", List.of("pointblank:m590", "pointblank:m1014", "pointblank:m249"), obj -> obj instanceof String);
        WEAPONS_RIFLE_SPEC = BUILDER.comment("被视为'步枪'的物品ID列表").defineList("rifles", List.of("pointblank:m4a1", "pointblank:aug", "pointblank:ak47", "pointblank:a4_sg553"), obj -> obj instanceof String);
        WEAPONS_AWP_SPEC = BUILDER.comment("被视为'AWP'的物品ID列表").defineList("awps", List.of("pointblank:l96a1", "pointblank:a8_l96"), obj -> obj instanceof String);
        WEAPONS_GRENADE_SPEC = BUILDER.comment("被视为'投掷物'的物品ID列表").defineList("grenades", List.of("pointblank:grenade"), obj -> obj instanceof String);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /**
     * 将所有配置项从 ModConfigSpec 中读取并赋值到对应的静态变量上，以便在游戏中直接调用。
     * <p>
     * 注意：由于泛型擦除问题，部分列表需要进行强制类型转换。
     */
    @SuppressWarnings("unchecked")
    public static void bake() {
        pistolRoundStartingMoney = PISTOL_ROUND_STARTING_MONEY_SPEC.get();
        ctPistolRoundGear = (List<String>) CT_PISTOL_ROUND_GEAR_SPEC.get();
        tPistolRoundGear = (List<String>) T_PISTOL_ROUND_GEAR_SPEC.get();
        inventoryProtectedItems = (List<String>) INVENTORY_PROTECTED_ITEMS_SPEC.get();
        friendlyFireEnabled = FRIENDLY_FIRE_ENABLED_SPEC.get();
        buyPhaseSeconds = BUY_PHASE_SECONDS_SPEC.get();
        roundEndSeconds = ROUND_END_SECONDS_SPEC.get();
        winReward = WIN_REWARD_SPEC.get();
        lossReward = LOSS_REWARD_SPEC.get();
        lossStreakBonus = LOSS_STREAK_BONUS_SPEC.get();
        maxLossStreakBonus = MAX_LOSS_STREAK_BONUS_SPEC.get();
        killRewardKnife = KILL_REWARD_KNIFE_SPEC.get();
        killRewardPistol = KILL_REWARD_PISTOL_SPEC.get();
        killRewardSmg = KILL_REWARD_SMG_SPEC.get();
        killRewardHeavy = KILL_REWARD_HEAVY_SPEC.get();
        killRewardRifle = KILL_REWARD_RIFLE_SPEC.get();
        killRewardAwp = KILL_REWARD_AWP_SPEC.get();
        killRewardGrenade = KILL_REWARD_GRENADE_SPEC.get();
        weaponsKnife = (List<String>) WEAPONS_KNIFE_SPEC.get();
        weaponsPistol = (List<String>) WEAPONS_PISTOL_SPEC.get();
        weaponsSmg = (List<String>) WEAPONS_SMG_SPEC.get();
        weaponsHeavy = (List<String>) WEAPONS_HEAVY_SPEC.get();
        weaponsRifle = (List<String>) WEAPONS_RIFLE_SPEC.get();
        weaponsAwp = (List<String>) WEAPONS_AWP_SPEC.get();
        weaponsGrenade = (List<String>) WEAPONS_GRENADE_SPEC.get();
    }
}
