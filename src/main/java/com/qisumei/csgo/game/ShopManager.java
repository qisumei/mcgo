package com.qisumei.csgo.game;

/**
 * 商店管理器类，用于生成带有特定商品交易列表的村民实体 NBT 数据。
 * 提供了获取 CT 和 T 阵营商店村民的方法，并支持动态添加药水效果。
 */
public class ShopManager {

    // CT阵营村民（Counter-Terrorist）
    public static final String CT_VILLAGER_NBT =
"{NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,"
+ "VillagerData:{profession:\"minecraft:toolsmith\",level:5,type:\"minecraft:plains\"},"
+ "Offers:{Recipes:["
+ "{buy:{id:\"minecraft:diamond\",count:2b},sell:{id:\"create:cardboard_package_10x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{count:24,id:\"pointblank:ammo45acp\"},slot:0},{item:{components:{\"geckolib:stack_animatable_id\":23L,\"pointblank:custom_tag\":{aim:0b,ammo:12,ammox:{},as:[{id:\"pointblank:ar_suppressor\",rmv:1b}],fmid:[I;-873864239,204353568,-1725201366,-303887115],lid:-6497976646019321318L,mid:-1381984303620799690L,sa:{scope:\"/\"},seed:4237641250468994479L},\"pointblank:ts\":1759538419362L},count:1,id:\"pointblank:a1_hkusp45\"},slot:1}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:5b},sell:{id:\"create:cardboard_package_10x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{count:64,id:\"pointblank:ammo57\"},slot:0},{item:{count:36,id:\"pointblank:ammo57\"},slot:1},{item:{components:{\"geckolib:stack_animatable_id\":55L,\"pointblank:custom_tag\":{aim:0b,ammo:20,ammox:{},fmid:[I;-40991536,655307330,-1520901064,-2100264513],lid:-7786430056054241269L,mid:-7100100859711172722L,sa:{scope:\"/\"},seed:3856571643519438126L},\"pointblank:ts\":1759544291726L},count:1,id:\"pointblank:a1_fnfs\"},slot:2}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:7b},sell:{id:\"create:cardboard_package_12x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{count:35,id:\"pointblank:ammo50ae\"},slot:0},{item:{components:{\"geckolib:stack_animatable_id\":57L,\"pointblank:custom_tag\":{aim:0b,ammo:7,ammox:{},fmid:[I;-520846650,310392400,-1831547144,686187342],lid:-4818453684502574095L,mid:-3330365311413174079L,sa:{scope:\"/\"},seed:-8656540983744125708L},\"pointblank:ts\":1759544492224L},count:1,id:\"pointblank:deserteagle\"},slot:1}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:29b},sell:{id:\"create:cardboard_package_12x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{components:{\"geckolib:stack_animatable_id\":31L,\"pointblank:custom_tag\":{aim:0b,ammo:30,ammox:{},fmid:[I;2040213901,1452882424,-1099782951,-2024296167],lid:-7437219635923161388L,mid:-1563003994237613494L,sa:{scope:\"/\"},seed:5805240003977742749L},\"pointblank:ts\":1759538804881L},count:1,id:\"pointblank:m4a1\"},slot:0},{item:{count:64,id:\"pointblank:ammo556\"},slot:1},{item:{count:26,id:\"pointblank:ammo556\"},slot:2}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:33b},sell:{id:\"create:cardboard_package_12x10\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{count:64,id:\"pointblank:ammo556\"},slot:0},{item:{count:26,id:\"pointblank:ammo556\"},slot:1},{item:{components:{\"geckolib:stack_animatable_id\":32L,\"pointblank:custom_tag\":{aim:0b,ammo:30,ammox:{},fmid:[I;1182836176,173555536,-1085696160,1966875892],lid:-4810204951641112595L,mid:6162450879816159093L,sa:{scope:\"/\"},seed:7769273819545944947L},\"pointblank:ts\":1759539045725L},count:1,id:\"pointblank:aug\"},slot:2}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:48b},sell:{id:\"create:cardboard_package_10x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{components:{\"geckolib:stack_animatable_id\":42L,\"pointblank:custom_tag\":{aim:0b,ammo:5,ammox:{},as:[{id:\"pointblank:hawk_scope\",rmv:1b}],fmid:[I;573397678,1834630192,-1285657842,-1809198908],lid:-7902711423806119341L,mid:-3352332155803909643L,sa:{scope:\"//hawk_scope\"},seed:-1941778996112074291L},\"pointblank:ts\":1759541616343L},count:1,id:\"pointblank:l96a1\"},slot:0},{item:{count:30,id:\"pointblank:ammo338lapua\"},slot:1}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:4b},sell:{id:\"minecraft:iron_helmet\",count:1b,components:{\"minecraft:unbreakable\":{}}}},"
+ "{buy:{id:\"minecraft:diamond\",count:7b},sell:{id:\"minecraft:iron_chestplate\",count:1b,components:{\"minecraft:unbreakable\":{}}}},"
+ "{buy:{id:\"minecraft:diamond\",count:4b},sell:{id:\"pointblank:grenade\",count:1b}}]}}";

    // T阵营村民（Terrorist）
    public static final String T_VILLAGER_NBT =
"{NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,"
+ "VillagerData:{profession:\"minecraft:toolsmith\",level:5,type:\"minecraft:plains\"},"
+ "Offers:{Recipes:["
+ "{buy:{id:\"minecraft:diamond\",count:2b},sell:{id:\"create:cardboard_package_10x8\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{components:{\"geckolib:stack_animatable_id\":10L,\"pointblank:custom_tag\":{aim:0b,ammo:17,ammox:{},fmid:[I;1531864238,-1356841432,-1369723404,-1033649185],lid:-5184087396665648791L,mid:-9004525231275293029L,sa:{scope:\"/\"},seed:3936175470680917490L},\"pointblank:ts\":1759497899857L},count:1,id:\"pointblank:a1_g17\"},slot:0},{item:{count:64,id:\"pointblank:ammo9mm\"},slot:1},{item:{count:56,id:\"pointblank:ammo9mm\"},slot:2}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:7b},sell:{id:\"create:cardboard_package_12x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{count:35,id:\"pointblank:ammo50ae\"},slot:0},{item:{components:{\"geckolib:stack_animatable_id\":57L,\"pointblank:custom_tag\":{aim:0b,ammo:7,ammox:{},fmid:[I;-520846650,310392400,-1831547144,686187342],lid:-4818453684502574095L,mid:-3330365311413174079L,sa:{scope:\"/\"},seed:-8656540983744125708L},\"pointblank:ts\":1759544492224L},count:1,id:\"pointblank:deserteagle\"},slot:1}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:5b},sell:{id:\"create:cardboard_package_10x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{count:64,id:\"pointblank:ammo57\"},slot:0},{item:{count:36,id:\"pointblank:ammo57\"},slot:1},{item:{components:{\"geckolib:stack_animatable_id\":55L,\"pointblank:custom_tag\":{aim:0b,ammo:20,ammox:{},fmid:[I;-40991536,655307330,-1520901064,-2100264513],lid:-7786430056054241269L,mid:-7100100859711172722L,sa:{scope:\"/\"},seed:3856571643519438126L},\"pointblank:ts\":1759544291726L},count:1,id:\"pointblank:a1_fnfs\"},slot:2}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:27b},sell:{id:\"create:cardboard_package_12x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{components:{\"geckolib:stack_animatable_id\":61L,\"pointblank:custom_tag\":{aim:0b,ammo:30,ammox:{},fmid:[I;-1715201077,884686046,-1746174375,857311309],lid:-6118338128067359654L,mid:-1859299674774614774L,sa:{scope:\"/\"},seed:-6319021369777717572L},\"pointblank:ts\":1759545570359L},count:1,id:\"pointblank:ak47\"},slot:0},{item:{count:64,id:\"pointblank:ammo762\"},slot:1},{item:{count:26,id:\"pointblank:ammo762\"},slot:2}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:30b},sell:{id:\"create:cardboard_package_10x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{count:64,id:\"pointblank:ammo556\"},slot:0},{item:{count:26,id:\"pointblank:ammo556\"},slot:1},{item:{components:{\"geckolib:stack_animatable_id\":59L,\"pointblank:custom_tag\":{aim:0b,ammo:30,ammox:{},as:[{id:\"pointblank:lty_lowgrain\",rmv:1b},{id:\"pointblank:j_sigjuliet\",rmv:1b}],fmid:[I;-1103560140,-360041443,-1992890606,-747647625],lid:-6300280937745072662L,mid:4094828213119501745L,sa:{scope:\"//j_sigjuliet\"},seed:-4938133471847281153L},\"pointblank:ts\":1759545443195L},count:1,id:\"pointblank:a4_sg553\"},slot:2}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:52b},sell:{id:\"create:cardboard_package_10x12\",count:1b,components:{\"create:package_address\":\"\",\"create:package_contents\":[{item:{components:{\"geckolib:stack_animatable_id\":53L,\"pointblank:custom_tag\":{aim:0b,ammo:100,ammox:{},fmid:[I;-1035269231,-1050988176,-1692405250,-822147727],lid:-7568618470194677918L,mid:-3365118316045254505L,sa:{scope:\"/\"},seed:3588360488820813235L},\"pointblank:ts\":1759544394579L},count:1,id:\"pointblank:m249\"},slot:0},{item:{count:64,id:\"pointblank:ammo556\"},slot:1}]}}},"
+ "{buy:{id:\"minecraft:diamond\",count:3b},sell:{id:\"pointblank:grenade\",count:1b}}]}}";

    /**
     * 向基础NBT字符串中注入指定持续时间的抗性提升效果。
     *
     * @param baseNbt 原始的村民NBT数据字符串
     * @param duration 药水效果的持续时间（单位：tick）
     * @return 添加了抗性提升效果后的完整NBT字符串
     */
    private static String addEffectsToNbt(String baseNbt, int duration) {
        // 抗性提升 (id:11), 4级=V, 免疫伤害
        String effectsNbt = "ActiveEffects:[{Id:11,Amplifier:4b,Duration:" + duration + "}]";
        // 将效果插入到NBT的第一个 '{' 之后
        return "{" + effectsNbt + "," + baseNbt.substring(1);
    }

    /**
     * 获取带有指定持续时间抗性效果的CT阵营商店村民NBT数据。
     *
     * @param duration 效果持续时间（单位：tick）
     * @return 完整的CT阵营村民NBT字符串
     */
    public static String getCtVillagerNbt(int duration) {
        return addEffectsToNbt(CT_VILLAGER_NBT, duration);
    }

    /**
     * 获取带有指定持续时间抗性效果的T阵营商店村民NBT数据。
     *
     * @param duration 效果持续时间（单位：tick）
     * @return 完整的T阵营村民NBT字符串
     */
    public static String getTVillagerNbt(int duration) {
        return addEffectsToNbt(T_VILLAGER_NBT, duration);
    }

}
