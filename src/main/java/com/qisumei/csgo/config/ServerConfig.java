package com.qisumei.csgo.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

/**
 * 服务器端游戏配置类，用于定义和管理CSGO模组中的各种游戏规则。
 *
 * <p>此类使用 NeoForge 的 {@link ModConfigSpec} 来构建一个结构清晰、带有注释的配置文件。
 * 所有配置项都被定义为静态的 {@code ModConfigSpec} 值，并在 {@link #bake()} 方法中被加载到静态字段中，
 * 以便于在游戏逻辑中进行高效访问。</p>
 *
 * @author Qisumei
 */
public class ServerConfig {
    /**
     * 用于构建配置规范的构建器实例。
     */
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // **[修复]** 将所有 _SPEC 字段的访问权限从 private 修改为 public
    // --- 比赛规则 (Match Rules) ---
    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE_ENABLED_SPEC;
    public static final ModConfigSpec.IntValue BUY_PHASE_SECONDS_SPEC;
    public static final ModConfigSpec.IntValue ROUND_END_SECONDS_SPEC;

    // --- 经济系统 (Economy) ---
    public static final ModConfigSpec.IntValue PISTOL_ROUND_STARTING_MONEY_SPEC;
    public static final ModConfigSpec.IntValue WIN_REWARD_SPEC;
    public static final ModConfigSpec.IntValue LOSS_REWARD_SPEC;
    public static final ModConfigSpec.IntValue LOSS_STREAK_BONUS_SPEC;
    public static final ModConfigSpec.IntValue MAX_LOSS_STREAK_BONUS_SPEC;

    // --- 击杀奖励 (Kill Rewards) ---
    public static final ModConfigSpec.IntValue KILL_REWARD_KNIFE_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_PISTOL_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_SMG_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_HEAVY_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_RIFLE_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_AWP_SPEC;
    public static final ModConfigSpec.IntValue KILL_REWARD_GRENADE_SPEC;

    // --- 装备配置 (Gear) ---
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CT_PISTOL_ROUND_GEAR_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> T_PISTOL_ROUND_GEAR_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> INVENTORY_PROTECTED_ITEMS_SPEC;

    // --- 武器分类 (Weapon Categories) ---
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_KNIFE_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_PISTOL_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_SMG_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_HEAVY_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_RIFLE_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_AWP_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPONS_GRENADE_SPEC;


    // 静态代码块，用于定义所有配置项
    static {
        // --- 比赛规则定义 ---
        BUILDER.push("Game Rules");
        FRIENDLY_FIRE_ENABLED_SPEC = BUILDER.comment("如果设置为true, 玩家可以对队友造成伤害。")
                .define("friendlyFireEnabled", false);
        BUY_PHASE_SECONDS_SPEC = BUILDER.comment("每回合开始时的购买阶段持续时间（秒）。")
                .defineInRange("buyPhaseSeconds", 15, 5, 60);
        ROUND_END_SECONDS_SPEC = BUILDER.comment("回合结束后，显示回合结果的等待时间（秒）。")
                .defineInRange("roundEndSeconds", 5, 1, 30);
        BUILDER.pop();

        // --- 经济系统定义 ---
        BUILDER.push("Economy");
        PISTOL_ROUND_STARTING_MONEY_SPEC = BUILDER.comment("手枪局或换边后第一局的起始资金。")
                .defineInRange("pistolRoundStartingMoney", 8, 0, 1000);
        WIN_REWARD_SPEC = BUILDER.comment("回合胜利的基础奖励金额。")
                .defineInRange("winReward", 33, 0, 1000);
        LOSS_REWARD_SPEC = BUILDER.comment("回合失败的基础奖励金额。")
                .defineInRange("lossReward", 14, 0, 1000);
        LOSS_STREAK_BONUS_SPEC = BUILDER.comment("每额外连败一回合增加的奖励金额。")
                .defineInRange("lossStreakBonus", 5, 0, 1000);
        MAX_LOSS_STREAK_BONUS_SPEC = BUILDER.comment("连败奖励的上限（不包含基础失败奖励）。")
                .defineInRange("maxLossStreakBonus", 35, 0, 1000);
        BUILDER.pop();

        // --- 击杀奖励定义 ---
        BUILDER.push("Kill Rewards");
        KILL_REWARD_KNIFE_SPEC = BUILDER.comment("使用近战武器击杀的奖励。").defineInRange("knife", 15, 0, 1000);
        KILL_REWARD_PISTOL_SPEC = BUILDER.comment("使用手枪击杀的奖励。").defineInRange("pistol", 3, 0, 1000);
        KILL_REWARD_SMG_SPEC = BUILDER.comment("使用冲锋枪击杀的奖励。").defineInRange("smg", 6, 0, 1000);
        KILL_REWARD_HEAVY_SPEC = BUILDER.comment("使用重型武器（霰弹枪/机枪）击杀的奖励。").defineInRange("heavy", 3, 0, 1000);
        KILL_REWARD_RIFLE_SPEC = BUILDER.comment("使用步枪击杀的奖励。").defineInRange("rifle", 3, 0, 1000);
        KILL_REWARD_AWP_SPEC = BUILDER.comment("使用AWP狙击步枪击杀的奖励。").defineInRange("awp", 1, 0, 1000);
        KILL_REWARD_GRENADE_SPEC = BUILDER.comment("使用手榴弹击杀的奖励。").defineInRange("grenade", 3, 0, 1000);
        BUILDER.pop();

        // --- 装备配置定义 ---
        BUILDER.push("Initial Gear");
        CT_PISTOL_ROUND_GEAR_SPEC = BUILDER.comment("CT方在手枪局获得的初始装备列表。格式: 'minecraft:item_id[component=value,...]'")
                .defineList("ctPistolRoundGear", List.of("minecraft:iron_sword[minecraft:enchantments={levels:{\"minecraft:sharpness\":27}},minecraft:unbreakable={}]"), obj -> obj instanceof String);
        T_PISTOL_ROUND_GEAR_SPEC = BUILDER.comment("T方在手枪局获得的初始装备列表。格式: 'minecraft:item_id[component=value,...]'")
                .defineList("tPistolRoundGear", List.of("minecraft:iron_sword[minecraft:enchantments={levels:{\"minecraft:sharpness\":27}},minecraft:unbreakable={}]"), obj -> obj instanceof String);
        INVENTORY_PROTECTED_ITEMS_SPEC = BUILDER.comment("在回合开始清理背包时，不会被清除的物品ID列表（例如货币、护甲等）。")
                .defineList("inventoryProtectedItems", List.of("minecraft:diamond", "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_sword"), obj -> obj instanceof String);
        BUILDER.pop();

        // --- 武器分类定义 ---
        BUILDER.push("Weapon Categories");
        WEAPONS_KNIFE_SPEC = BUILDER.comment("被视为'刀'的物品ID列表。").defineList("knifes", List.of("minecraft:iron_sword", "minecraft:netherite_sword"), obj -> obj instanceof String);
        WEAPONS_PISTOL_SPEC = BUILDER.comment("被视为'手枪'的物品ID列表。").defineList("pistols", List.of("pointblank:a1_hkusp45", "pointblank:a1_g17", "pointblank:deserteagle"), obj -> obj instanceof String);
        WEAPONS_SMG_SPEC = BUILDER.comment("被视为'冲锋枪'的物品ID列表。").defineList("smgs", List.of("pointblank:mp7", "pointblank:p90", "pointblank:a2_pp19b"), obj -> obj instanceof String);
        WEAPONS_HEAVY_SPEC = BUILDER.comment("被视为'重型武器'的物品ID列表。").defineList("heavies", List.of("pointblank:m590", "pointblank:m1014", "pointblank:m249"), obj -> obj instanceof String);
        WEAPONS_RIFLE_SPEC = BUILDER.comment("被视为'步枪'的物品ID列表。").defineList("rifles", List.of("pointblank:m4a1", "pointblank:aug", "pointblank:ak47", "pointblank:a4_sg553"), obj -> obj instanceof String);
        WEAPONS_AWP_SPEC = BUILDER.comment("被视为'AWP'的物品ID列表。").defineList("awps", List.of("pointblank:l96a1", "pointblank:a8_l96"), obj -> obj instanceof String);
        WEAPONS_GRENADE_SPEC = BUILDER.comment("被视为'投掷物'的物品ID列表。").defineList("grenades", List.of("pointblank:grenade"), obj -> obj instanceof String);
        BUILDER.pop();

        // 构建最终的配置规范
        SPEC = BUILDER.build();
    }

    /**
     * 最终构建完成的配置规范实例。
     */
    public static final ModConfigSpec SPEC;

    // --- 用于在游戏中直接访问的静态变量 ---
    public static boolean friendlyFireEnabled;
    public static int buyPhaseSeconds;
    public static int roundEndSeconds;
    public static int pistolRoundStartingMoney;
    public static int winReward;
    public static int lossReward;
    public static int lossStreakBonus;
    public static int maxLossStreakBonus;
    public static int killRewardKnife;
    public static int killRewardPistol;
    public static int killRewardSmg;
    public static int killRewardHeavy;
    public static int killRewardRifle;
    public static int killRewardAwp;
    public static int killRewardGrenade;
    public static List<String> ctPistolRoundGear;
    public static List<String> tPistolRoundGear;
    public static List<String> inventoryProtectedItems;
    public static List<String> weaponsKnife;
    public static List<String> weaponsPistol;
    public static List<String> weaponsSmg;
    public static List<String> weaponsHeavy;
    public static List<String> weaponsRifle;
    public static List<String> weaponsAwp;
    public static List<String> weaponsGrenade;

    /**
     * 将配置文件中的值加载到静态字段中。
     * 这个过程被称为 "baking"，它会在配置文件加载或重载时被调用。
     */
    @SuppressWarnings("unchecked")
    public static void bake() {
        friendlyFireEnabled = FRIENDLY_FIRE_ENABLED_SPEC.get();
        buyPhaseSeconds = BUY_PHASE_SECONDS_SPEC.get();
        roundEndSeconds = ROUND_END_SECONDS_SPEC.get();
        pistolRoundStartingMoney = PISTOL_ROUND_STARTING_MONEY_SPEC.get();
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
        
        // 列表类型需要进行非检查性转换
        ctPistolRoundGear = (List<String>) CT_PISTOL_ROUND_GEAR_SPEC.get();
        tPistolRoundGear = (List<String>) T_PISTOL_ROUND_GEAR_SPEC.get();
        inventoryProtectedItems = (List<String>) INVENTORY_PROTECTED_ITEMS_SPEC.get();
        weaponsKnife = (List<String>) WEAPONS_KNIFE_SPEC.get();
        weaponsPistol = (List<String>) WEAPONS_PISTOL_SPEC.get();
        weaponsSmg = (List<String>) WEAPONS_SMG_SPEC.get();
        weaponsHeavy = (List<String>) WEAPONS_HEAVY_SPEC.get();
        weaponsRifle = (List<String>) WEAPONS_RIFLE_SPEC.get();
        weaponsAwp = (List<String>) WEAPONS_AWP_SPEC.get();
        weaponsGrenade = (List<String>) WEAPONS_GRENADE_SPEC.get();
    }
}

