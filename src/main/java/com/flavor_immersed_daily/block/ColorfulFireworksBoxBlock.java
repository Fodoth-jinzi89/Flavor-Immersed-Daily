package com.flavor_immersed_daily.block;

import com.flavor_immersed_daily.FlavorImmersedDaily;
import com.flavor_immersed_daily.screen.ColorfulFireworksBoxConfigScreen;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 箱装烟花 — 带四个水平方向转向，潜行右键配置，右键/红石触发发射烟花
 */
public class ColorfulFireworksBoxBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<ColorfulFireworksBoxBlock> CODEC =
            simpleCodec(ColorfulFireworksBoxBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ColorfulFireworksBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ==================== 交互 ====================

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            // 潜行右键：打开配置界面
            if (level.isClientSide) {
                Minecraft.getInstance().setScreen(new ColorfulFireworksBoxConfigScreen(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 非潜行右键：发射烟花
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof ColorfulFireworksBoxBlockEntity be) {
                be.launch(level, state.getValue(FACING));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ==================== BlockEntity ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ColorfulFireworksBoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> ColorfulFireworksBoxBlockEntity.serverTick(lvl, pos, st,
                (ColorfulFireworksBoxBlockEntity) be);
    }
}
