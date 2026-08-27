package com.flavor_immersed_daily.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SeedableFruitItem extends Item {

    public SeedableFruitItem(Properties properties, String seedItemId) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // GoldenTweaks structural behavior: fruit use keeps the normal item
        // interaction and does not expose a sneak-only seed conversion.
        return super.use(level, player, hand);
    }

}
