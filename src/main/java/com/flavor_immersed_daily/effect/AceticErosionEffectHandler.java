package com.flavor_immersed_daily.effect;

import com.flavor_immersed_daily.Config;
import com.flavor_immersed_daily.FlavorImmersedDaily;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * 醋酸侵蚀效果处理器
 * 触发规则：
 *  - 玩家食用物品后，副手持有 fid:seasoning 标签物品，或食物 NBT 标签 seasoning 非空
 *  - 其中副手为 vinegar（醋）或食物 NBT seasoning 为 flavor_immersed_daily:vinegar 时，
 *    给予 45 秒 acetic_erosion 效果，并消耗副手调味料一个
 * 效果：拥有 acetic_erosion 的实体造成近战攻击时，被攻击者的每件盔甲
 *       耐久损耗额外增加 config 中的数值（默认 1）
 */
@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class AceticErosionEffectHandler {

    private static final int DURATION_TICKS = 45 * 20;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!Config.aceticErosionEnabled) return;

        // 副手是否为调味料
        ItemStack offhand = player.getOffhandItem();
        boolean offhandIsSeasoning = offhand.is(FlavorImmersedDaily.SEASONING_TAG);

        // 食用的食物 NBT 文本标签 seasoning（1.21.1 存于 CUSTOM_DATA 组件中）
        CompoundTag tag = event.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean foodHasSeasoning = tag.contains("seasoning", Tag.TAG_STRING);
        String foodSeasoning = foodHasSeasoning ? tag.getString("seasoning") : "";

        // 触发条件：副手是调味料 或 食物 seasoning 标签非空
        if (!offhandIsSeasoning && !foodHasSeasoning) return;

        // 专属 buff：副手是醋 或 食物 seasoning 标签为 flavor_immersed_daily:vinegar → 醋酸侵蚀 45 秒
        boolean isVinegar = offhand.is(FlavorImmersedDaily.VINEGAR.get())
                || "flavor_immersed_daily:vinegar".equals(foodSeasoning);
        if (isVinegar) {
            player.addEffect(new MobEffectInstance(FlavorImmersedDaily.ACETIC_EROSION, DURATION_TICKS, 0));
            // 副手持有调味料时，获得 buff 的同时消耗一个
            if (offhandIsSeasoning) {
                offhand.shrink(1);
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!Config.aceticErosionEnabled) return;
        if (event.getEntity().level().isClientSide) return;

        DamageSource source = event.getSource();
        // 仅近战：直接攻击者与伤害来源相同，且持有醋酸侵蚀效果
        if (source.getDirectEntity() != source.getEntity()) return;
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        if (!attacker.hasEffect(FlavorImmersedDaily.ACETIC_EROSION)) return;

        int extra = Config.aceticErosionExtraDurability;
        if (extra <= 0) return;

        // 被攻击者每件可损耗的盔甲额外损耗
        LivingEntity target = event.getEntity();
        int index = 0;
        for (ItemStack armor : target.getArmorSlots()) {
            if (!armor.isEmpty() && armor.isDamageableItem()) {
                armor.hurtAndBreak(extra, target, ARMOR_SLOTS[index]);
            }
            index++;
        }
    }
}
