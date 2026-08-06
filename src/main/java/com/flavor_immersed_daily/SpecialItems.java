// 修正后的SpecialItems.java文件 - 使用SimpleBlock实现
package com.flavor_immersed_daily;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.properties.Property;

public class SpecialItems {
    
    // 特殊物品：可以放置也可以食用的物品
    public static class PlaceableFoodItem extends Item {
        private final Block blockToPlace;
        private final FoodProperties foodProperties;
        
        public PlaceableFoodItem(Block block, Properties properties, FoodProperties foodProperties) {
            super(properties.component(DataComponents.FOOD, foodProperties));
            this.blockToPlace = block;
            this.foodProperties = foodProperties;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            
            // 检查是否对空使用，如果是，则食用
            BlockHitResult rayTraceResult = getPlayerPOVHitResult(world, player, net.minecraft.world.level.ClipContext.Fluid.NONE);
            if (rayTraceResult.getType() == net.minecraft.world.phys.BlockHitResult.Type.MISS) {
                // 对空使用，食用
                if (player.canEat(true)) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(stack);
                }
                return InteractionResultHolder.fail(stack);
            } else {
                // 对方块使用，返回PASS让useOn处理
                return InteractionResultHolder.pass(stack);
            }
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            Level world = context.getLevel();
            BlockPos pos = context.getClickedPos();
            Direction facing = context.getClickedFace();
            ItemStack stack = context.getItemInHand();
            
            if (player == null) return InteractionResult.PASS;

            // 计算放置位置
            BlockPos placePos = pos.relative(facing);
            
            // 检查是否可以放置方块
            if (world.getBlockState(placePos).canBeReplaced() && 
                player.mayUseItemAt(placePos, facing, stack)) {
                
                BlockState newState = blockToPlace.defaultBlockState();
                if (world.setBlock(placePos, newState, 11)) {
                    world.playSound(player, placePos, newState.getBlock().defaultBlockState().getSoundType().getPlaceSound(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    
                    return InteractionResult.sidedSuccess(world.isClientSide);
                }
            }
            
            return InteractionResult.PASS;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
            if (entity instanceof Player player) {
                player.awardStat(Stats.ITEM_USED.get(this));
                
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                
                // 应用食物效果
                player.eat(world, stack.copyWithCount(1));
                
                return stack.isEmpty() ? new ItemStack(this) : stack;
            }
            
            return stack;
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity entity) {
            return 32; // 食用时间
        }

        public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
            return net.minecraft.world.item.UseAnim.EAT;
        }
        
        public Block getBlockToPlace() {
            return blockToPlace;
        }
        
        public FoodProperties getFoodProperties() {
            return foodProperties;
        }
    }
    
    // 简化的多阶段交互方块类（不继承HorizontalDirectionalBlock）
    public static class MultiStageInteractiveBlock extends Block {
        public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2); // 0, 1, 2
        
        // 使用统一的碰撞箱，尺寸为高6、长宽12
        private static final VoxelShape COLLISION_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D);
        
        // 存储每个方块实例的特定属性
        private final int nutrition;
        private final float saturation;
        private final String name;

        public MultiStageInteractiveBlock(int nutrition, float saturation, String name, Properties properties) {
            super(properties.sound(SoundType.WOOD));
            this.nutrition = nutrition;
            this.saturation = saturation;
            this.name = name;
            this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
        }

        @Override
        public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
            return COLLISION_SHAPE; // 统一使用相同的碰撞箱
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(STAGE);
        }

        // 使用正确的交互方法 - useWithoutItem
        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
            int currentStage = state.getValue(STAGE);
            
            if (currentStage < 2) {
                // 增加阶段
                level.setBlock(pos, state.setValue(STAGE, currentStage + 1), 3);
                
                // 播放音效
                level.playSound(null, pos, SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.5F, 1.0F);
                
                // 给予玩家食物奖励
                net.minecraft.world.food.FoodProperties foodProps = new net.minecraft.world.food.FoodProperties.Builder()
                        .nutrition(this.nutrition)
                        .saturationModifier(this.saturation)
                        .build();
                
                ItemStack food = new ItemStack(getCorrespondingItem(this.name));
                player.eat(level, food.copyWithCount(1));
                
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                // 阶段2，移除方块
                level.destroyBlock(pos, true);
                
                // 播放破坏音效
                level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.8F, 0.8F);
                
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        
        // 根据方块名称获取对应的物品
        private Item getCorrespondingItem(String blockName) {
            String itemName = blockName; // 通常情况下，物品名称与方块名称相同
            ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(com.flavor_immersed_daily.FlavorImmersedDaily.MODID, blockName);
            java.util.Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
            return item.orElse(Items.AIR);
        }
    }
}
