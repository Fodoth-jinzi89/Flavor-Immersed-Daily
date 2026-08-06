package com.flavor_immersed_daily.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 冻结 — 标记实体处于冻结状态（AI 行为暂停，只能被攻击）
 * 实际 AI 暂停逻辑由 {@link FrozenEffectHandler} 在实体 tick 中执行
 */
public class FrozenEffect extends MobEffect {

    public FrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0x92F2F2);
    }
}
