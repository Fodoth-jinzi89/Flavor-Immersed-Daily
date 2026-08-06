package com.flavor_immersed_daily.effect;

import com.flavor_immersed_daily.Config;
import com.flavor_immersed_daily.FlavorImmersedDaily;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * 浩克大葱（hulk_leek）效果处理器
 * 触发规则：
 *  - 玩家食用物品后，副手持有 fid:seasoning 标签物品，或食物 NBT 标签 seasoning 非空
 *  - 其中副手为 onionpowder（洋葱粉）或食物 NBT seasoning 为 flavor_immersed_daily:onionpowder 时，
 *    给予 45 秒 hulk_leek 效果，并消耗副手调味料一个
 * 效果：拥有 hulk_leek 的玩家近战攻击幼年实体时，将其转化为成年状态
 */
@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class HulkLeekEffectHandler {

    private static final int DURATION_TICKS = 45 * 20;

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!Config.hulkLeekEnabled) return;

        // 副手是否为调味料
        ItemStack offhand = player.getOffhandItem();
        boolean offhandIsSeasoning = offhand.is(FlavorImmersedDaily.SEASONING_TAG);

        // 食用的食物 NBT 文本标签 seasoning（1.21.1 存于 CUSTOM_DATA 组件中）
        CompoundTag tag = event.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean foodHasSeasoning = tag.contains("seasoning", Tag.TAG_STRING);
        String foodSeasoning = foodHasSeasoning ? tag.getString("seasoning") : "";

        // 触发条件：副手是调味料 或 食物 seasoning 标签非空
        if (!offhandIsSeasoning && !foodHasSeasoning) return;

        // 专属 buff：副手是洋葱粉（onionpowder）或 食物 seasoning 标签为 flavor_immersed_daily:onionpowder → 浩克大葱 45 秒
        boolean isOnionPowder = offhand.is(FlavorImmersedDaily.ONIONPOWDER.get())
                || "flavor_immersed_daily:onionpowder".equals(foodSeasoning);
        if (isOnionPowder) {
            player.addEffect(new MobEffectInstance(FlavorImmersedDaily.HULK_LEEK, DURATION_TICKS, 0));
            // 副手持有调味料时，获得 buff 的同时消耗一个
            if (offhandIsSeasoning) {
                offhand.shrink(1);
            }
        }
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!Config.hulkLeekEnabled) return;
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        DamageSource source = event.getSource();
        // 仅近战：直接攻击者与伤害来源相同（排除箭矢等投射物）
        if (source.getDirectEntity() != source.getEntity()) return;
        if (!(source.getEntity() instanceof Player player)) return;
        if (!player.hasEffect(FlavorImmersedDaily.HULK_LEEK)) return;

        // 幼年实体转化为成年状态
        if (target instanceof Mob mob && mob.isBaby()) {
            mob.setBaby(false);
        }
    }
}
