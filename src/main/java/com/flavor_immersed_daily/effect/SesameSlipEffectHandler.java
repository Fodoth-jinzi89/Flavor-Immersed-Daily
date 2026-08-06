package com.flavor_immersed_daily.effect;

import com.flavor_immersed_daily.Config;
import com.flavor_immersed_daily.FlavorImmersedDaily;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 调味料 buff 触发与芝麻滑行效果处理器
 * 触发规则：
 *  - 玩家食用物品后，副手持有 fid:seasoning 标签物品，或食物 NBT 标签 seasoning 非空
 *  - 其中副手为 sesameoil（香油）或食物 NBT seasoning 为 flavor_immersed_daily:sesameoil 时，
 *    给予 45 秒 sesame_slip 效果
 * 效果：拥有 sesame_slip 期间，实体的行走高度提升为 config 中的数值（默认 2.1 格）
 */
@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class SesameSlipEffectHandler {

    private static final int DURATION_TICKS = 45 * 20;
    private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(FlavorImmersedDaily.MODID, "sesame_slip_step_height");

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!Config.sesameSlipEnabled) return;

        // 副手是否为调味料
        ItemStack offhand = player.getOffhandItem();
        boolean offhandIsSeasoning = offhand.is(FlavorImmersedDaily.SEASONING_TAG);

        // 食用的食物 NBT 文本标签 seasoning（1.21.1 存于 CUSTOM_DATA 组件中）
        CompoundTag tag = event.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean foodHasSeasoning = tag.contains("seasoning", Tag.TAG_STRING);
        String foodSeasoning = foodHasSeasoning ? tag.getString("seasoning") : "";

        // 触发条件：副手是调味料 或 食物 seasoning 标签非空
        if (!offhandIsSeasoning && !foodHasSeasoning) return;

        // 专属 buff：副手是香油（sesameoil）或 食物 seasoning 标签为 flavor_immersed_daily:sesameoil → 香油滑步 45 秒
        boolean isSesameOil = offhand.is(FlavorImmersedDaily.SESAMEOIL.get())
                || "flavor_immersed_daily:sesameoil".equals(foodSeasoning);
        if (isSesameOil) {
            player.addEffect(new MobEffectInstance(FlavorImmersedDaily.SESAME_SLIP, DURATION_TICKS, 0));
            // 副手持有调味料时，获得 buff 的同时消耗一个
            if (offhandIsSeasoning) {
                offhand.shrink(1);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide) return;

        AttributeInstance stepHeight = entity.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight == null) return;

        boolean active = Config.sesameSlipEnabled && entity.hasEffect(FlavorImmersedDaily.SESAME_SLIP);
        if (active && !stepHeight.hasModifier(STEP_HEIGHT_MODIFIER_ID)) {
            // 原版默认步高 0.6，修正值 = 目标高度 - 0.6
            double value = Config.sesameSlipHeight - 0.6;
            stepHeight.addTransientModifier(new AttributeModifier(
                    STEP_HEIGHT_MODIFIER_ID, value, AttributeModifier.Operation.ADD_VALUE));
        } else if (!active && stepHeight.hasModifier(STEP_HEIGHT_MODIFIER_ID)) {
            stepHeight.removeModifier(STEP_HEIGHT_MODIFIER_ID);
        }
    }
}
