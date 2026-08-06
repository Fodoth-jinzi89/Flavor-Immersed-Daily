package com.flavor_immersed_daily.effect;

import com.flavor_immersed_daily.FlavorImmersedDaily;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冻结状态处理器 — 拥有 frozen 效果的生物暂停一切 AI 行为：
 *  - 停止移动与寻路
 *  - 停止攻击与其它行为（NoAI）
 *  - 只能被攻击
 * 效果消失后自动恢复 AI
 */
@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class FrozenEffectHandler {

    /** 记录由本 mod 冻结的实体，用于效果结束后恢复 AI（避免干扰原本就是 NoAI 的生物） */
    private static final Set<UUID> FROZEN = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide) return;

        UUID id = mob.getUUID();
        if (mob.hasEffect(FlavorImmersedDaily.FROZEN)) {
            FROZEN.add(id);
            if (!mob.isNoAi()) {
                mob.setNoAi(true);
            }
            // 完全静止：停止移动与击退
            mob.setDeltaMovement(0, 0, 0);
            mob.getNavigation().stop();
        } else if (FROZEN.remove(id)) {
            // 效果消失，恢复 AI
            mob.setNoAi(false);
        }
    }
}
