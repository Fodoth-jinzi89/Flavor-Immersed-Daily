package com.flavor_immersed_daily.effect;

import com.flavor_immersed_daily.Config;
import com.flavor_immersed_daily.FlavorImmersedDaily;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
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
 * 百味之基（flavor_base）效果处理器
 * 触发规则：
 *  - 玩家食用物品后，副手持有 fid:seasoning 标签物品，或食物 NBT 标签 seasoning 非空
 *  - 其中副手为 salt（盐）或食物 NBT seasoning 为 flavor_immersed_daily:salt 时，
 *    给予 45 秒 flavor_base 效果，并消耗副手调味料一个
 * 效果：拥有 flavor_base 期间，玩家身上每同时有 1 种注册名以 "flavor" 开头的
 * 本模组或附属模组 buff，则：
 *  - 攻击伤害 +config（默认 +1）
 *  - 移动速度 +config（默认 +0.1）
 * 最多叠加 config 次（默认 10 次）
 */
@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class FlavorBaseEffectHandler {

    private static final int DURATION_TICKS = 45 * 20;
    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(FlavorImmersedDaily.MODID, "flavor_base_attack_damage");
    private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(FlavorImmersedDaily.MODID, "flavor_base_movement_speed");

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!Config.flavorBaseEnabled) return;

        // 副手是否为调味料
        ItemStack offhand = player.getOffhandItem();
        boolean offhandIsSeasoning = offhand.is(FlavorImmersedDaily.SEASONING_TAG);

        // 食用的食物 NBT 文本标签 seasoning（1.21.1 存于 CUSTOM_DATA 组件中）
        CompoundTag tag = event.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean foodHasSeasoning = tag.contains("seasoning", Tag.TAG_STRING);
        String foodSeasoning = foodHasSeasoning ? tag.getString("seasoning") : "";

        // 触发条件：副手是调味料 或 食物 seasoning 标签非空
        if (!offhandIsSeasoning && !foodHasSeasoning) return;

        // 专属 buff：副手是盐（salt）或 食物 seasoning 标签为 flavor_immersed_daily:salt → 百味之基 45 秒
        boolean isSalt = offhand.is(FlavorImmersedDaily.SALT.get())
                || "flavor_immersed_daily:salt".equals(foodSeasoning);
        if (isSalt) {
            player.addEffect(new MobEffectInstance(FlavorImmersedDaily.FLAVOR_BASE, DURATION_TICKS, 0));
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

        boolean active = Config.flavorBaseEnabled && entity.hasEffect(FlavorImmersedDaily.FLAVOR_BASE);
        if (!active) {
            removeModifiers(entity);
            return;
        }

        // 统计身上注册名以 "flavor" 开头的 buff 数量（本模组及附属模组），最多叠加 config 上限
        int count = 0;
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            ResourceLocation key = instance.getEffect().unwrapKey().map(rk -> rk.location()).orElse(null);
            if (key != null && key.toString().startsWith("flavor")) {
                count++;
            }
        }
        int stacks = Math.min(count, Config.flavorBaseMaxStacks);

        if (stacks > 0) {
            updateModifier(entity, ATTACK_DAMAGE_MODIFIER_ID, Attributes.ATTACK_DAMAGE,
                    stacks * Config.flavorBaseDamageBonus);
            updateModifier(entity, MOVEMENT_SPEED_MODIFIER_ID, Attributes.MOVEMENT_SPEED,
                    stacks * Config.flavorBaseSpeedBonus);
        } else {
            removeModifiers(entity);
        }
    }

    private static void updateModifier(LivingEntity entity, ResourceLocation id, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier modifier = instance.getModifier(id);
        if (modifier == null) {
            instance.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
        } else if (modifier.amount() != value) {
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeModifiers(LivingEntity entity) {
        AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null && attackDamage.hasModifier(ATTACK_DAMAGE_MODIFIER_ID)) {
            attackDamage.removeModifier(ATTACK_DAMAGE_MODIFIER_ID);
        }
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && movementSpeed.hasModifier(MOVEMENT_SPEED_MODIFIER_ID)) {
            movementSpeed.removeModifier(MOVEMENT_SPEED_MODIFIER_ID);
        }
    }
}
