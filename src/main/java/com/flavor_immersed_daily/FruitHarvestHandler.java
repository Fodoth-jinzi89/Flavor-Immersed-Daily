package com.flavor_immersed_daily;

import com.flavor_immersed_daily.block.FallingFruitBlock;
import com.flavor_immersed_daily.block.FruitingLeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = FlavorImmersedDaily.MODID)
public class FruitHarvestHandler {

    private static final int SEARCH_RADIUS = 7;
    private static final float EXHAUSTION_COST = 6.0F;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // 必须手持木棍
        if (!stack.is(Items.STICK)) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // 必须右键点击树干（原木）
        if (!(state.getBlock() instanceof RotatedPillarBlock)) return;

        // 检查玩家是否有足够的食物
        if (player.getFoodData().getFoodLevel() <= 0) return;

        // 客户端只播放挥动动画
        if (level.isClientSide) {
            player.swing(event.getHand());
            return;
        }

        // === 服务端逻辑 ===
        boolean harvested = false;

        // 扫描范围内所有方块，寻找结果子的树叶和悬挂的果子
        for (BlockPos searchPos : BlockPos.betweenClosed(
                pos.getX() - SEARCH_RADIUS, pos.getY() - SEARCH_RADIUS, pos.getZ() - SEARCH_RADIUS,
                pos.getX() + SEARCH_RADIUS, pos.getY() + SEARCH_RADIUS, pos.getZ() + SEARCH_RADIUS)) {
            BlockPos immutablePos = searchPos.immutable();
            BlockState searchState = level.getBlockState(immutablePos);
            Block searchBlock = searchState.getBlock();

            if (searchBlock instanceof FruitingLeavesBlock leavesBlock) {
                if (searchState.getValue(FruitingLeavesBlock.FRUITING)) {
                    // 采摘结果子的树叶
                    level.setBlock(immutablePos, searchState.setValue(FruitingLeavesBlock.FRUITING, false), 3);
                    Block.popResource(level, immutablePos, new ItemStack(leavesBlock.getFruitItem()));
                    harvested = true;
                }
            } else if (searchBlock instanceof FallingFruitBlock fallingBlock) {
                // 采摘悬挂的果子
                level.removeBlock(immutablePos, false);
                Block.popResource(level, immutablePos, new ItemStack(fallingBlock.getFruitItem()));
                harvested = true;
            }
        }

        if (harvested) {
            // 消耗饱食度
            player.getFoodData().addExhaustion(EXHAUSTION_COST);

            // 播放横扫特效
            ServerLevel serverLevel = (ServerLevel) level;
            serverLevel.playSound(null, pos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        event.setCanceled(true);
    }
}