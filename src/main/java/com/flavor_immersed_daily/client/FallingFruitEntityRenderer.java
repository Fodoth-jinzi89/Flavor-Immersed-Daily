package com.flavor_immersed_daily.client;

import com.flavor_immersed_daily.entity.FallingFruitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class FallingFruitEntityRenderer extends EntityRenderer<FallingFruitEntity> {

    public FallingFruitEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FallingFruitEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        BlockState blockState = entity.getBlockState();
        if (blockState.isAir()) {
            return;
        }

        poseStack.pushPose();
        
        // 根据水果类型确定Y轴偏移，以适配不同水果的模型
        String fruitId = entity.getFruitId();
        float yOffset = getYOffsetForFruit(fruitId);
        
        poseStack.translate(-0.5, yOffset, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                blockState, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    // 根据水果ID返回适当的Y轴偏移量 — 统一 -0.4f 避免浮空
    private float getYOffsetForFruit(String fruitId) {
        return -0.4f;
    }

    @Override
    public ResourceLocation getTextureLocation(FallingFruitEntity entity) {
        return null;
    }
}
