package com.flavor_immersed_daily.block;

import com.flavor_immersed_daily.FlavorImmersedDaily;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public class EggBreakingMachineBlock extends Block implements EntityBlock {

    public static final MapCodec<EggBreakingMachineBlock> CODEC =
            simpleCodec(EggBreakingMachineBlock::new);

    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 1);

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1, 1);

    public EggBreakingMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ---- EntityBlock ----

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EggBreakingMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type == FlavorImmersedDaily.EGG_BREAKING_MACHINE_ENTITY.get()) {
            return (lvl, pos, st, be) -> EggBreakingMachineBlockEntity.serverTick(lvl, pos, st, (EggBreakingMachineBlockEntity) be);
        }
        return null;
    }

    // ---- 交互 ----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (state.getValue(STAGE) != 0) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider(
                    (containerId, inv, p) -> new com.flavor_immersed_daily.screen.EggBreakingMachineMenu(
                            containerId, inv, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("block.flavor_immersed_daily.eggbreakingmachine")
            ), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    // ---- 掉落 ----

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return super.getDrops(state, params);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EggBreakingMachineBlockEntity machine) {
                machine.dropProcessingResults(level, pos);
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }
}
