package com.flavor_immersed_daily.effect;

import com.flavor_immersed_daily.Config;
import com.flavor_immersed_daily.FlavorImmersedDaily;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * 黄油投手效果处理器
 * 触发规则：
 *  - 玩家食用物品后，副手持有 fid:seasoning 标签物品，或食物 NBT 标签 seasoning 非空
 *  - 其中副手为 butter（黄油）或食物 NBT seasoning 为 flavor_immersed_daily:butter 时，
 *    给予 45 秒 butter_pitcher 效果，并消耗副手调味料一个
 * 效果：持有 butter_pitcher 效果的玩家发射弹射物命中目标时：
 *  - 目标非玩家（绝对不能是玩家）
 *  - 目标非 Boss（可配置开关，默认开启）
 *  - 按配置概率（默认 25%）将目标游戏冻结（冰冻）数秒（默认 5 秒）
 */
@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class ButterPitcherEffectHandler {

    private static final int DURATION_TICKS = 45 * 20;

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!Config.butterPitcherEnabled) return;

        // 副手是否为调味料
        ItemStack offhand = player.getOffhandItem();
        boolean offhandIsSeasoning = offhand.is(FlavorImmersedDaily.SEASONING_TAG);

        // 食用的食物 NBT 文本标签 seasoning（1.21.1 存于 CUSTOM_DATA 组件中）
        CompoundTag tag = event.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean foodHasSeasoning = tag.contains("seasoning", Tag.TAG_STRING);
        String foodSeasoning = foodHasSeasoning ? tag.getString("seasoning") : "";

        // 触发条件：副手是调味料 或 食物 seasoning 标签非空
        if (!offhandIsSeasoning && !foodHasSeasoning) return;

        // 专属 buff：副手是黄油 或 食物 seasoning 标签为 flavor_immersed_daily:butter → 黄油投手 45 秒
        boolean isButter = offhand.is(FlavorImmersedDaily.BUTTER.get())
                || "flavor_immersed_daily:butter".equals(foodSeasoning);
        if (isButter) {
            player.addEffect(new MobEffectInstance(FlavorImmersedDaily.BUTTER_PITCHER, DURATION_TICKS, 0));
            // 副手持有调味料时，获得 buff 的同时消耗一个
            if (offhandIsSeasoning) {
                offhand.shrink(1);
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!Config.butterPitcherEnabled) return;

        HitResult ray = event.getRayTraceResult();
        if (!(ray instanceof EntityHitResult entityHit)) return;

        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide) return;

        // 投掷者必须是持有效果的玩家
        if (!(projectile.getOwner() instanceof Player player)) return;
        if (!player.hasEffect(FlavorImmersedDaily.BUTTER_PITCHER)) return;

        // 目标绝不能是玩家
        Entity target = entityHit.getEntity();
        if (target instanceof Player) return;
        if (!(target instanceof LivingEntity livingTarget)) return;

        // 非 Boss 检测（可开关）
        if (Config.butterPitcherExcludeBoss && isBoss(target)) return;

        // 概率判定
        if (player.getRandom().nextDouble() >= Config.butterPitcherFreezeChance) return;

        // 施加冻结效果：暂停目标 AI 行为指定秒数
        livingTarget.addEffect(new MobEffectInstance(
                FlavorImmersedDaily.FROZEN,
                (int) (Config.butterPitcherFreezeDuration * 20),
                0), player);
    }

    /**
     * 判定实体是否为 Boss：末影龙、凋灵、循声守卫
     */
    private static boolean isBoss(Entity entity) {
        return entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity.getType() == EntityType.WARDEN;
    }
}
