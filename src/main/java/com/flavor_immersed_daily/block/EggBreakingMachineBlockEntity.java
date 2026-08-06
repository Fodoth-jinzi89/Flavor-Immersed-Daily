package com.flavor_immersed_daily.block;

import com.flavor_immersed_daily.FlavorImmersedDaily;
import com.flavor_immersed_daily.recipe.EggBreakingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EggBreakingMachineBlockEntity extends BlockEntity {

    private int progress;
    private int totalTime;
    private final List<ItemStack> processingResults = new ArrayList<>();

    public EggBreakingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(FlavorImmersedDaily.EGG_BREAKING_MACHINE_ENTITY.get(), pos, state);
    }

    public void startProcessing(EggBreakingRecipe recipe) {
        if (level == null || level.isClientSide() || recipe == null) return;

        processingResults.clear();
        for (ItemStack r : recipe.getResults()) {
            processingResults.add(r.copy());
        }
        totalTime = recipe.getCookingTime();
        progress = 0;

        level.setBlock(worldPosition, getBlockState().setValue(EggBreakingMachineBlock.STAGE, 1), 3);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EggBreakingMachineBlockEntity be) {
        if (be.totalTime > 0) {
            be.progress++;
            if (be.progress >= be.totalTime) {
                be.spawnResults(level, pos);
                be.progress = 0;
                be.totalTime = 0;
                level.setBlock(pos, state.setValue(EggBreakingMachineBlock.STAGE, 0), 3);
            }
            be.setChanged();
        }
    }

    private void spawnResults(Level level, BlockPos pos) {
        for (ItemStack stack : processingResults) {
            if (!stack.isEmpty()) {
                ItemEntity entity = new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        stack.copy());
                entity.setDeltaMovement(0, 0.1, 0);
                level.addFreshEntity(entity);
            }
        }
        processingResults.clear();
    }

    public void dropProcessingResults(Level level, BlockPos pos) {
        for (ItemStack stack : processingResults) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
        processingResults.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", progress);
        tag.putInt("totalTime", totalTime);
        if (!processingResults.isEmpty()) {
            CompoundTag resultsTag = new CompoundTag();
            for (int i = 0; i < processingResults.size(); i++) {
                resultsTag.put("item" + i, processingResults.get(i).save(registries));
            }
            resultsTag.putInt("count", processingResults.size());
            tag.put("processingResults", resultsTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("progress");
        totalTime = tag.getInt("totalTime");
        processingResults.clear();
        if (tag.contains("processingResults")) {
            CompoundTag resultsTag = tag.getCompound("processingResults");
            int count = resultsTag.getInt("count");
            for (int i = 0; i < count; i++) {
                ItemStack.parse(registries, resultsTag.getCompound("item" + i))
                        .ifPresent(processingResults::add);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
