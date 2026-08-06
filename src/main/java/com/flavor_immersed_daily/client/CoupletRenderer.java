package com.flavor_immersed_daily.client;

import com.flavor_immersed_daily.block.CoupletBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 对联文字渲染器 — 在方块表面渲染文字
 * 横幅（antithetical_couplet_1）：1行文字从左到右横排
 * 竖联（antithetical_couplet_2）：文字从上到下竖排
 */
public class CoupletRenderer implements BlockEntityRenderer<CoupletBlockEntity> {

    private final Font font;

    public CoupletRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(CoupletBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        String[] lines = blockEntity.getLines();
        int color = blockEntity.getColor();

        // 通过方块ID判断是横幅还是竖联
        String path = state.getBlock().builtInRegistryHolder().key().location().getPath();
        boolean isVertical = path.contains("antithetical_couplet_2");

        // 文字颜色：0=黑色(0xFF000000), 1=黄色(0xFFFFFF00)
        int textColor = color == 1 ? 0xFFFFFF00 : 0xFF000000;

        poseStack.pushPose();

        // 1. 移动到方块中心
        poseStack.translate(0.5, 0.5, 0.5);

        // 2. 旋转使文字朝向玩家
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot() - 180.0F));

        // 3. 移动到方块表面
        // 模型表面在 z=15.9/16=0.99375，从中心偏移 0.49375
        // 使用 SEE_THROUGH 渲染模式彻底禁用深度测试，避免深度冲突导致文字不可见
        poseStack.translate(0, 0, 0.49375f + 0.008f);

        // 4. 缩放字体大小
        float scale = 0.025f;
        poseStack.scale(-scale, -scale, 1.0f);

        if (isVertical) {
            // ---- 竖联：从上到下逐字排列 ----
            // 编辑界面把每个字存在独立的 lines[0]~lines[3] 中，渲染时从各索引读取
            int maxChars = 0;
            for (int i = 0; i < 4; i++) {
                if (lines[i] != null && !lines[i].isEmpty()) {
                    maxChars = i + 1;
                }
            }
            if (maxChars > 0) {
                // 使用原版告示牌的逐行渲染方式：y = i * lineHeight - totalHeight/2
                int lineHeight = 10;
                int totalHeight = maxChars * lineHeight;
                int startY = -totalHeight / 2;
                for (int i = 0; i < maxChars; i++) {
                    String charStr = lines[i];
                    if (charStr == null || charStr.isEmpty()) continue;
                    int charWidth = this.font.width(charStr);
                    Component component = Component.literal(charStr)
                            .setStyle(Style.EMPTY.withBold(true));
                    int x = -charWidth / 2;
                    int y = startY + i * lineHeight;
                    this.font.drawInBatch(component, x, y, textColor, false,
                            poseStack.last().pose(), bufferSource,
                            Font.DisplayMode.SEE_THROUGH, 0, packedLight);
                }
            }
        } else {
            // ---- 横幅：只渲染第一行（横批） ----
            String text = lines[0];
            if (text != null && !text.isEmpty()) {
                Component component = Component.literal(text)
                        .setStyle(Style.EMPTY.withBold(true));
                int textWidth = this.font.width(component);
                // 由于 scale 取反，x 取正来居中；再左移37像素
                int x = textWidth / 2 - 37;
                // 由于 scale 取反，y 取反；往上2像素
                int y = -4;
                this.font.drawInBatch(component, x, y, textColor, false,
                        poseStack.last().pose(), bufferSource,
                        Font.DisplayMode.SEE_THROUGH, 0, packedLight);
            }
        }

        poseStack.popPose();
    }
}