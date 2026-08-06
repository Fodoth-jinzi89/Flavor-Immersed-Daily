package com.flavor_immersed_daily.effect;

import com.flavor_immersed_daily.Config;
import com.flavor_immersed_daily.FlavorImmersedDaily;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.List;

/**
 * 火爆狂攻（fury_assault）效果处理器
 * 触发规则：
 *  - 玩家食用物品后，副手持有 fid:seasoning 标签物品，或食物 NBT 标签 seasoning 非空
 *  - 其中副手为 chillipowder（辣椒粉）或食物 NBT seasoning 为 flavor_immersed_daily:chillipowder 时，
 *    给予 45 秒 fury_assault 效果，并消耗副手调味料一个
 * 效果：拥有 fury_assault 的玩家每进行一次近战攻击：
 *  - 消耗玩家 config 点生命值（默认 1）
 *  - 玩家前方 config 格扇形范围（半角 30°）内的生物被点燃，并受到 config 点火焰伤害（默认 2）
 *  - 触发冲击粒子特效
 */
@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class FuryAssaultEffectHandler {

    private static final int DURATION_TICKS = 45 * 20;
    /** 扇形半角 30° 的余弦值（用于角度过滤） */
    private static final double CONE_COS = Math.cos(Math.toRadians(30.0));
    /** 点燃持续时间（tick） */
    private static final int FIRE_TICKS = 100;

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!Config.furyAssaultEnabled) return;

        // 副手是否为调味料
        ItemStack offhand = player.getOffhandItem();
        boolean offhandIsSeasoning = offhand.is(FlavorImmersedDaily.SEASONING_TAG);

        // 食用的食物 NBT 文本标签 seasoning（1.21.1 存于 CUSTOM_DATA 组件中）
        CompoundTag tag = event.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean foodHasSeasoning = tag.contains("seasoning", Tag.TAG_STRING);
        String foodSeasoning = foodHasSeasoning ? tag.getString("seasoning") : "";

        // 触发条件：副手是调味料 或 食物 seasoning 标签非空
        if (!offhandIsSeasoning && !foodHasSeasoning) return;

        // 专属 buff：副手是辣椒粉（chillipowder）或 食物 seasoning 标签为 flavor_immersed_daily:chillipowder → 火爆狂攻 45 秒
        boolean isChiliPowder = offhand.is(FlavorImmersedDaily.CHILLIPOWDER.get())
                || "flavor_immersed_daily:chillipowder".equals(foodSeasoning);
        if (isChiliPowder) {
            player.addEffect(new MobEffectInstance(FlavorImmersedDaily.FURY_ASSAULT, DURATION_TICKS, 0));
            // 副手持有调味料时，获得 buff 的同时消耗一个
            if (offhandIsSeasoning) {
                offhand.shrink(1);
            }
        }
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!Config.furyAssaultEnabled) return;
        if (event.getEntity().level().isClientSide) return;

        DamageSource source = event.getSource();
        // 仅近战：直接攻击者与伤害来源相同（排除箭矢等投射物）
        if (source.getDirectEntity() != source.getEntity()) return;
        if (!(source.getEntity() instanceof Player player)) return;
        if (!player.hasEffect(FlavorImmersedDaily.FURY_ASSAULT)) return;

        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 1. 消耗玩家生命值（精确扣血，无视护甲；若不足则直接击杀）
        float health = player.getHealth() - (float) Config.furyAssaultHealthCost;
        if (health <= 0.0F) {
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        } else {
            player.setHealth(health);
        }

        // 2. 前方扇形范围（距离 ≤ 范围，半角 30°）内的生物：点燃 + 火焰伤害
        double range = Config.furyAssaultRange;
        Vec3 facing = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        AABB area = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e ->
                e != player && e.isAlive() && e.distanceToSqr(player) <= range * range);

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position()
                    .add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(eye)
                    .normalize();
            if (toTarget.dot(facing) < CONE_COS) continue;

            target.setRemainingFireTicks(FIRE_TICKS);
            target.hurt(serverLevel.damageSources().onFire(), (float) Config.furyAssaultFireDamage);
        }

        // 3. 扇形火焰粒子标识：在扇形范围内覆盖火焰特效
        double yaw = Math.toRadians(player.getYRot());
        double baseX = -Math.sin(yaw);
        double baseZ = Math.cos(yaw);
        double particleY = player.getY() + 0.5;
        for (double d = 1.0; d <= range; d += 1.0) {
            for (double ang = -30.0; ang <= 30.0; ang += 10.0) {
                double rad = Math.toRadians(ang);
                double dirX = baseX * Math.cos(rad) - baseZ * Math.sin(rad);
                double dirZ = baseZ * Math.cos(rad) + baseX * Math.sin(rad);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        player.getX() + dirX * d, particleY, player.getZ() + dirZ * d,
                        1, 0.1, 0.1, 0.1, 0);
            }
        }
    }
}
