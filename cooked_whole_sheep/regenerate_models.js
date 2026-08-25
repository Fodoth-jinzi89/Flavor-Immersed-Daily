// 从 cooked_whole_sheepN.java（Blockbench 导出原件）逐字提取 createBodyLayer 方法体，
// 重新生成项目模型类 CookedWholeSheepModelN.java（整个模型原封不动）。
const fs = require('fs');
const path = require('path');

const DIR = __dirname;
const MODEL_DIR = path.join(DIR, '..', 'src', 'main', 'java', 'com', 'flavor_immersed_daily', 'client', 'model');

function extractMethodBody(src, methodMarker) {
    const idx = src.indexOf(methodMarker);
    if (idx < 0) throw new Error('marker not found: ' + methodMarker);
    const start = idx + methodMarker.length;
    let depth = 0;
    for (let i = start; i < src.length; i++) {
        const c = src[i];
        if (c === '{') depth++;
        else if (c === '}') {
            if (depth === 0) return src.slice(start, i); // 方法闭合
            depth--;
        }
    }
    throw new Error('unbalanced braces');
}

const METHOD_MARKER = 'public static LayerDefinition createBodyLayer() {';

let allOk = true;
for (let stage = 0; stage <= 9; stage++) {
    const exportPath = path.join(DIR, `cooked_whole_sheep${stage}.java`);
    const exportSrc = fs.readFileSync(exportPath, 'utf8');
    const body = extractMethodBody(exportSrc, METHOD_MARKER);

    // 规范化（去掉空白）用于与现有类对比
    const norm = s => s.replace(/\s+/g, ' ').trim();
    const modelPath = path.join(MODEL_DIR, `CookedWholeSheepModel${stage}.java`);
    let currentSame = false;
    if (fs.existsSync(modelPath)) {
        const cur = fs.readFileSync(modelPath, 'utf8');
        try {
            const curBody = extractMethodBody(cur, METHOD_MARKER);
            currentSame = norm(curBody) === norm(body);
        } catch (e) { /* 提取失败视为不同 */ }
    }

    // 生成新类
    const src = `package com.flavor_immersed_daily.client.model;

import com.flavor_immersed_daily.FlavorImmersedDaily;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;

public class CookedWholeSheepModel${stage} extends CookedWholeSheepModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FlavorImmersedDaily.MODID, "cooked_whole_sheep_${stage}"), "main");

    public CookedWholeSheepModel${stage}(ModelPart root) {
        super(root, ${stage === 9 ? 'false' : 'true'});
    }

    public static LayerDefinition createBodyLayer() {
${body}
    }
}
`;
    fs.writeFileSync(modelPath, src);
    console.log(`stage ${stage}: 已重新生成 (${body.trim().split(/\s+/).length} tokens) 之前${currentSame ? '一致' : '不一致 → 已修正'}`);
    if (!currentSame) allOk = false;
}
console.log(allOk ? '全部模型此前已一致，本次原样重写' : '部分模型此前被打乱，现已从导出原件恢复');
