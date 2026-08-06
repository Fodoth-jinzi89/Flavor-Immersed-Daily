package com.flavor_immersed_daily;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class ItemInteractionHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (stack.is(FlavorImmersedDaily.CHINESE_LEAVES.get()) && player.isShiftKeyDown()) {
            if (!player.level().isClientSide) {
                stack.shrink(1);
                ItemStack result = new ItemStack(FlavorImmersedDaily.CUT_CHINESE_CABBAGE.get(), 3);
                if (!player.getInventory().add(result)) {
                    player.drop(result, false);
                }
            }
            event.setCanceled(true);
        }
    }
}
