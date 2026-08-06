package com.flavor_immersed_daily;

import com.flavor_immersed_daily.client.ClientSeasoningTooltip;
import com.flavor_immersed_daily.item.SeasoningTooltip;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

/**
 * 客户端 MOD 总线事件 — 注册自定义 TooltipComponent 的客户端渲染工厂
 */
@EventBusSubscriber(modid = FlavorImmersedDaily.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FlavorImmersedDailyClientModEvents {

    @SubscribeEvent
    static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(SeasoningTooltip.class, ClientSeasoningTooltip::new);
    }
}
