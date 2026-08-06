package com.flavor_immersed_daily.effect;

import com.flavor_immersed_daily.Config;
import com.flavor_immersed_daily.FlavorImmersedDaily;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * 蘸豆，爽！（bean_fury）效果处理器
 * 触发规则：
 *  - 玩家食用物品后，副手持有 fid:seasoning 标签物品，或食物 NBT 标签 seasoning 非空
 *  - 其中副手为 thickbroadbeansauce（豆瓣酱）或食物 NBT seasoning 为
 *    flavor_immersed_daily:thickbroadbeansauce 时，给予 45 秒 bean_fury 效果，并消耗副手调味料一个
 * 效果：拥有该效果的玩家进行近战攻击时，按配置概率（默认 25%）触发暴击：
 *  - 伤害提升 1.5 倍（与原版跳跃暴击相同）
 *  - 播放暴击粒子与音效
 *  - 无需跳跃攻击
 */
@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class BeanFuryEffectHandler {

    /** 原版跳跃暴击的伤害倍率 */
    private static final float CRIT_MULTIPLIER = 1.5F;
    private static final int DURATION_TICKS = 45 * 20;

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!Config.beanFuryEnabled) return;

        // 副手是否为调味料
        ItemStack offhand = player.getOffhandItem();
        boolean offhandIsSeasoning = offhand.is(FlavorImmersedDaily.SEASONING_TAG);

        // 食用的食物 NBT 文本标签 seasoning（1.21.1 存于 CUSTOM_DATA 组件中）
        CompoundTag tag = event.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean foodHasSeasoning = tag.contains("seasoning", Tag.TAG_STRING);
        String foodSeasoning = foodHasSeasoning ? tag.getString("seasoning") : "";

        // 触发条件：副手是调味料 或 食物 seasoning 标签非空
        if (!offhandIsSeasoning && !foodHasSeasoning) return;

        // 专属 buff：副手是豆瓣酱 或 食物 seasoning 标签为 flavor_immersed_daily:thickbroadbeansauce → 蘸豆，爽！ 45 秒
        boolean isBeanPaste = offhand.is(FlavorImmersedDaily.THICKBROADBEANSAUCE.get())
                || "flavor_immersed_daily:thickbroadbeansauce".equals(foodSeasoning);
        if (isBeanPaste) {
            player.addEffect(new MobEffectInstance(FlavorImmersedDaily.BEAN_FURY, DURATION_TICKS, 0));
            // 副手持有调味料时，获得 buff 的同时消耗一个
            if (offhandIsSeasoning) {
                offhand.shrink(1);
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!Config.beanFuryEnabled) return;
        if (event.getEntity().level().isClientSide) return;

        DamageSource source = event.getSource();
        // 仅近战：直接攻击者与伤害来源相同（排除箭矢等投射物）
        if (source.getDirectEntity() != source.getEntity()) return;
        if (!(source.getEntity() instanceof Player player)) return;
        if (!player.hasEffect(FlavorImmersedDaily.BEAN_FURY)) return;

        // 概率判定
        if (player.getRandom().nextDouble() >= Config.beanFuryCritChance) return;

        // 暴击：1.5 倍伤害
        event.setAmount(event.getAmount() * CRIT_MULTIPLIER);

        // 暴击粒子与音效
        LivingEntity target = event.getEntity();
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    10, 0.2, 0.2, 0.2, 0.1);
            serverLevel.playSound(null, target.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
